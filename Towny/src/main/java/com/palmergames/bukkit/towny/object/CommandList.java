package com.palmergames.bukkit.towny.object;

import com.google.common.base.Preconditions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

public class CommandList {
	private static final Pattern REMOVE_LEADING_SPACE = Pattern.compile("^[/ ]{1,2}");
	private static final Pattern MATCH_NAMESPACE = Pattern.compile("^(.+:)");
	
	public static class CommandNode {
		final Map<String, CommandNode> children = new HashMap<>();
		final String command;
		boolean endOfWord = false;
		
		public CommandNode(String command) {
			this.command = command;
		}
	}
	
	final CommandNode root = new CommandNode("");
	
	public CommandList(Collection<String> commands) {
		for (String command : commands) {
			addCommand(command);
		}
	}
	
	public void addCommand(@NotNull String command) {
		Preconditions.checkNotNull(command, "command");
		final String normalized = normalizeCommand(command.toLowerCase(Locale.ROOT));
		if (normalized.isEmpty())
			return;

		CommandNode current = root;

		for (String part : normalized.split(" ")) {
			current = current.children.computeIfAbsent(part, k -> new CommandNode(part));
		}
		
		current.endOfWord = true;
	}
	
	@ApiStatus.Internal
	public static String normalizeCommand(String command) {
		// Replace slash and/or space from the start of a command
		command = REMOVE_LEADING_SPACE.matcher(command).replaceAll("");
		
		// Strip namespace
		return MATCH_NAMESPACE.matcher(command).replaceAll("");
	}
	
	public boolean containsCommand(@NotNull String command) {
		Preconditions.checkNotNull(command, "command");
		final String normalized = normalizeCommand(command.toLowerCase(Locale.ROOT));
		
		CommandNode current = root;
		for (String part : normalized.split(" ")) {
			current = current.children.get(part);
			if (current == null)
				break;
			
			if (current.endOfWord)
				return true;
		}
		
		return false;
	}
}
