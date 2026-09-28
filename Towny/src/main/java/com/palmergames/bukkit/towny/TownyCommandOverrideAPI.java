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

public final class TownyCommandOverrideAPI {

	private static final Set<String> ROOTS = Set.of("town", "nation", "resident", "plot",
		"towny", "townyadmin", "townyworld", "invite");
	private static final List<OverrideRegistration> overrides = new CopyOnWriteArrayList<>();

	private TownyCommandOverrideAPI() {
	}

	/**
	 * Uses the executor's {@link TabCompleter}, if implemented, or empty completion.
	 *
	 * @see #registerOverride(Plugin, String, String, CommandExecutor, TabCompleter)
	 */
	public static boolean registerOverride(@NotNull Plugin owner, @NotNull String command,
			@NotNull String path, @NotNull CommandExecutor executor) {
		return registerOverride(owner, command, path, executor,
			executor instanceof TabCompleter completer ? completer : null);
	}

	/**
	 * Overrides a command path and its descendants. The longest matching path wins.
	 * Callbacks receive the original command, label and full arguments; returning
	 * {@code false} does not invoke Towny's handler.
	 *
	 * @param owner owning plugin; registrations are removed on disable
	 * @param command canonical root name; aliases are covered automatically
	 * @param path space-separated, case-insensitive path; {@code *} matches one argument,
	 *             empty matches the entire root
	 * @param executor replacement handler
	 * @param completer replacement completer, or {@code null} for no suggestions
	 * @return {@code false} if an overlapping path of equal length is registered
	 * @throws IllegalArgumentException if the root name is invalid
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
	
	public static boolean unregisterOverride(@NotNull Plugin owner, @NotNull String command, @NotNull String path) {
		Objects.requireNonNull(owner, "owner");
		String root = normalizeRoot(command);
		List<String> parts = normalizePath(path);
		return overrides.removeIf(entry -> entry.owner() == owner && entry.root().equals(root) && entry.path().equals(parts));
	}
	
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
