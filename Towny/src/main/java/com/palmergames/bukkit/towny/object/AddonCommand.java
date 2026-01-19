package com.palmergames.bukkit.towny.object;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;

import com.palmergames.util.StringMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AddonCommand extends Command {
	private CommandType commandType;
	private String name;
	private CommandExecutor commandExecutor;
	private TabCompleter tabCompleter = new LegacyTabCompleter(this);
	private Map<Integer, List<String>> tabCompletions = new HashMap<>();

	public AddonCommand(CommandType commandType, String name, CommandExecutor commandExecutor) {
		super(name);
		this.commandType = commandType;
		this.name = name;
		this.commandExecutor = commandExecutor;

		if (commandExecutor instanceof TabCompleter tabCompleter)
			this.tabCompleter = tabCompleter;
	}

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		return execute(sender, args);
	}

	public boolean execute(@NotNull CommandSender sender, @NotNull String[] args) {
		return commandExecutor.onCommand(sender, this, this.name, StringMgmt.remFirstArg(args));
	}

	/**
	 * Used when an AddonCommand should receive a TownyObject name as a preceding argument.
	 * 
	 * @param sender CommandSender using the addon command.
	 * @param args String[] args which will have the Town or Nation name as the first String.
	 * @param object A TownyObject, not actually used except to specify the desire to use this method.
	 * @return true if the {@link CommandExecutor#onCommand(CommandSender, Command, String, String[])} is successful.
	 */
	public boolean execute(@NotNull CommandSender sender, @NotNull String[] args, TownyObject object) {
		return commandExecutor.onCommand(sender, this, this.name, args);
	}

	public CommandType getCommandType() {
		return commandType;
	}

	public @NotNull String getName() {
		return name;
	}

	public CommandExecutor getCommandExecutor() {
		return commandExecutor;
	}

	public TabCompleter getTabCompleter() {
		return tabCompleter;
	}

	public Map<Integer, List<String>> getTabCompletions() {
		return tabCompletions;
	}

	public void setCommandType(CommandType commandType) {
		this.commandType = commandType;
	}

	@Override
	public boolean setName(@NotNull String name) {
		this.name = name;
		return super.setName(name);
	}

	public void setCommandExecutor(CommandExecutor commandExecutor) {
		this.commandExecutor = commandExecutor;

		if (commandExecutor instanceof TabCompleter tabCompleter)
			this.tabCompleter = tabCompleter;
	}

	/**
	 * Sets the tab completer for this command. If the provided CommandExecutor
	 * already implements TabCompleter, this function is not required.
	 * 
	 * @param tabCompleter The tab completer to set.
	 */
	public void setTabCompleter(@Nullable TabCompleter tabCompleter) {
		if (tabCompleter == null)
			this.tabCompleter = new LegacyTabCompleter(this);
		else
			this.tabCompleter = tabCompleter;
	}

	public void setTabCompletions(Map<Integer, List<String>> tabCompletions) {
		this.tabCompletions = tabCompletions;
	}

	public void setTabCompletion(int index, List<String> completions) {
		tabCompletions.put(index, completions);
	}

	public List<String> getTabCompletion(CommandSender sender, String[] args) {
		return tabCompleter.onTabComplete(sender, this, this.name, StringMgmt.remFirstArg(args));
	}

	private record LegacyTabCompleter(AddonCommand command) implements TabCompleter {
		@Override
		public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
			int precedingArgs = getPrecedingArguments(command().getCommandType());
			return command().tabCompletions.get(args.length - precedingArgs) == null ? Collections.emptyList()
					: command().tabCompletions.get(args.length - precedingArgs);
		}
	}

	private static int getPrecedingArguments(CommandType type) {
		if (type.equals(CommandType.TOWNYADMIN_NATION) || type.equals(CommandType.TOWNYADMIN_TOWN))
			return 2;
		return 1;
	}
}
