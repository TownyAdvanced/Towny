package com.palmergames.bukkit.towny;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;

/**
 * Explicit replacements for Towny's command handlers. Ordinary addon command
 * registrations retain their existing precedence.
 *
 * <p>Overrides receive the original Bukkit command, label and complete argument
 * array. They replace all built-in handling for the matched path, including
 * permissions, validation and help. The registering plugin is responsible for
 * those checks. Overrides are inactive while Towny is in safe mode.</p>
 */
public final class TownyCommandOverrideAPI {

	private static final Set<String> ROOTS = Set.of("town", "nation", "resident", "plot",
		"towny", "townyadmin", "townyworld", "invite");
	private static final List<OverrideRegistration> overrides = new CopyOnWriteArrayList<>();

	private TownyCommandOverrideAPI() {
	}

	/**
	 * Registers a replacement, using the executor's TabCompleter if it implements it.
	 * Otherwise completion is empty inside the replaced path.
	 *
	 * @see #registerOverride(Plugin, String, String, CommandExecutor, TabCompleter)
	 */
	public static boolean registerOverride(@NotNull Plugin owner, @NotNull String command,
			@NotNull String path, @NotNull CommandExecutor executor) {
		return registerOverride(owner, command, path, executor,
			executor instanceof TabCompleter completer ? completer : null);
	}

	/**
	 * Registers a replacement for a subcommand and all its descendants.
	 *
	 * @param owner plugin owning the registration; registrations are removed on disable
	 * @param command canonical root name, e.g. "nation" (also covers /n and /nat)
	 * @param path space-separated, case-insensitive path, e.g. "ally" or "set board";
	 *             "*" matches one argument, e.g. "town * set board" for an admin path;
	 *             an empty path replaces the entire root, including no-argument calls
	 * @param executor replacement receiving all original arguments, including the path;
	 *                 its return value never causes fall-through to Towny's handler
	 * @param completer replacement receiving all original arguments; null means no suggestions
	 * @return false if an overlapping path of the same length is already registered;
	 *         otherwise true. Of matching paths with different lengths, the longest wins.
	 * @throws IllegalArgumentException if the root is not a canonical Towny command
	 */
	public static synchronized boolean registerOverride(@NotNull Plugin owner, @NotNull String command,
			@NotNull String path, @NotNull CommandExecutor executor, @Nullable TabCompleter completer) {
		Objects.requireNonNull(owner, "owner");
		Objects.requireNonNull(executor, "executor");
		String root = normalizeRoot(command);
		List<String> parts = normalizePath(path);
		for (OverrideRegistration existing : overrides) {
			if (existing.root().equals(root) && overlaps(existing.path(), parts))
				return false;
		}
		overrides.add(new OverrideRegistration(owner, root, parts, executor, completer));
		return true;
	}

	/** Removes only the specified owner's registration. */
	public static boolean unregisterOverride(@NotNull Plugin owner, @NotNull String command, @NotNull String path) {
		Objects.requireNonNull(owner, "owner");
		String root = normalizeRoot(command);
		List<String> parts = normalizePath(path);
		return overrides.removeIf(entry -> entry.owner() == owner && entry.root().equals(root) && entry.path().equals(parts));
	}

	/** Removes all registrations belonging to a plugin. Called automatically on disable. */
	public static void unregisterOverrides(@NotNull Plugin owner) {
		Objects.requireNonNull(owner, "owner");
		overrides.removeIf(entry -> entry.owner() == owner);
	}

	@ApiStatus.Internal
	public static void clearOverrides() {
		overrides.clear();
	}

	private static String normalizeRoot(String command) {
		String root = command.toLowerCase(Locale.ROOT);
		if (!ROOTS.contains(root))
			throw new IllegalArgumentException("Expected a canonical Towny command name: " + command);
		return root;
	}

	private static List<String> normalizePath(String path) {
		String normalized = path.trim().toLowerCase(Locale.ROOT);
		return normalized.isEmpty() ? List.of() : List.copyOf(Arrays.asList(normalized.split("\\s+")));
	}

	private static boolean overlaps(List<String> first, List<String> second) {
		if (first.size() != second.size())
			return false;
		for (int i = 0; i < first.size(); i++) {
			if (!first.get(i).equals("*") && !second.get(i).equals("*") && !first.get(i).equals(second.get(i)))
				return false;
		}
		return true;
	}

	private static OverrideRegistration findOverride(Command command, String[] args, int completedArguments) {
		OverrideRegistration match = null;
		for (OverrideRegistration entry : overrides) {
			if (!entry.root().equalsIgnoreCase(command.getName()) || !entry.owner().isEnabled()
					|| entry.path().size() > completedArguments
					|| (match != null && entry.path().size() <= match.path().size()))
				continue;
			boolean matches = true;
			for (int i = 0; i < entry.path().size(); i++) {
				String part = entry.path().get(i);
				if (!part.equals("*") && !part.equalsIgnoreCase(args[i])) {
					matches = false;
					break;
				}
			}
			if (matches)
				match = entry;
		}
		return match;
	}

	/** Wraps a root handler without changing its behavior when no override matches. */
	@ApiStatus.Internal
	public static TabExecutor wrapExecutor(@NotNull CommandExecutor original, @NotNull BooleanSupplier safeMode) {
		Objects.requireNonNull(original, "original");
		Objects.requireNonNull(safeMode, "safeMode");
		return new TabExecutor() {
			@Override
			public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command,
					@NotNull String label, @NotNull String[] args) {
				OverrideRegistration match = safeMode.getAsBoolean() ? null : findOverride(command, args, args.length);
				if (match != null)
					return match.executor().onCommand(sender, command, label, args);
				return original.onCommand(sender, command, label, args);
			}

			@Override
			public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command,
					@NotNull String alias, @NotNull String[] args) {
				// The final argument is still being completed, so it cannot select a new path.
				OverrideRegistration match = safeMode.getAsBoolean() ? null : findOverride(command, args, Math.max(0, args.length - 1));
				if (match != null) {
					List<String> suggestions = match.completer() == null ? null : match.completer().onTabComplete(sender, command, alias, args);
					return suggestions == null ? List.of() : suggestions;
				}
				return original instanceof TabCompleter completer ? completer.onTabComplete(sender, command, alias, args) : null;
			}
		};
	}

	private record OverrideRegistration(Plugin owner, String root, List<String> path,
			CommandExecutor executor, TabCompleter completer) {
	}
}
