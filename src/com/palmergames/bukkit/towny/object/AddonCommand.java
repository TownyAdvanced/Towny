package com.palmergames.bukkit.towny.object;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class AddonCommand extends Command {
    private CommandType commandType;
    private String name;
    private CommandExecutor commandExecutor;
    private Map<Integer, List<String>> tabCompletions = new HashMap<Integer, List<String>>();

    public AddonCommand(CommandType commandType, String name, CommandExecutor commandExecutor) {
    	super(name);
        this.commandType = commandType;
        this.name = name;
        this.commandExecutor = commandExecutor;
    }

	@Override
	public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
		return commandExecutor.onCommand(sender, this, label, args);
	}

    public CommandType getCommandType() {
        return commandType;
    }

    public String getName() {
        return name;
    }

    public CommandExecutor getCommandExecutor() {
        return commandExecutor;
    }

    public Map<Integer, List<String>> getTabCompletions() {
        return tabCompletions;
    }

    public void setCommandType(CommandType commandType) {
        this.commandType = commandType;
    }
    
    public boolean setName(String name) {
        this.name = name;
        return true;
    }

    public void setCommandExecutor(CommandExecutor commandExecutor) {
        this.commandExecutor = commandExecutor;
    }

    public void setTabCompletions(Map<Integer, List<String>> tabCompletions) {
        this.tabCompletions = tabCompletions;
    }

    public void setTabCompletion(int index, List<String> completions) {
        tabCompletions.put(index, completions);
    }

    public List<String> getTabCompletion(int index) {
        List<String> suggestions = tabCompletions.get(index-2);
        if (suggestions == null)
            return Collections.emptyList();
        
        return suggestions;
    }
}
