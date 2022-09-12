package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public class CommandList {
	private static final Pattern NORMALIZER_PATTERN = Pattern.compile("[/ ]{1,2}");
	
	private final Set<String> commands = new HashSet<>();
	// Commands that were registered with no namespace.
	private final Set<String> commandsNoNamespace = new HashSet<>();
	
	public CommandList(Collection<String> commands) {
		for (String command : commands) {
			command = command.toLowerCase(Locale.ROOT);
			
			addCommand(command);
			
			if (command.contains(":")) {
				// If the given command to block has a namespace we should treat the key as a command to block as well.
				// i.e. /towny:town -> block /town too.
				String key = command.split(":")[1];
				
				addCommand(key);
			} else {
				// No namespace was defined, add the command to the commandsNoNamespace set.
				// This is used to block commands no matter the namespace.
				commandsNoNamespace.add(normalizeCommand(command));
			}
		}
	}
	
	private void addCommand(String command) {
		if (command.startsWith("/"))
			command = command.substring(1);

		commands.add(command);
		commands.add("/" + command);

		// Block commands containing spaces.
		// '/ town' is in fact a valid command.
		if (command.startsWith(" ")) {
			commands.add(command.substring(1));
			commands.add("/" + command.substring(1));
		} else {
			commands.add(" " + command);
			commands.add("/ " + command);
		}
	}
	
	private String normalizeCommand(String command) {
		// Replace slash and/or space from the start of a command
		command = NORMALIZER_PATTERN.matcher(command).replaceAll("");
		
		// Strip namespace
		if (command.contains(":"))
			return command.split(":")[1];
		
		return command;
	}
	
	public boolean containsCommand(@NotNull String command) {
		command = command.toLowerCase(Locale.ROOT);
		
		if (command.contains(":") && commandsNoNamespace.contains(normalizeCommand(command)))
			return true;
		
		return commands.contains(command);
	}
}
