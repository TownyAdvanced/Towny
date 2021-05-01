package com.palmergames.bukkit.towny.object;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.exceptions.TownyException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class AddonCommand {
    private CommandType commandType;
    private String name;
    private CommandExecutor commandExecutor;
    private Map<Integer, List<String>> tabCompletions = new HashMap<Integer, List<String>>();

    public AddonCommand(CommandType commandType, String name, CommandExecutor commandExecutor) {
        this.commandType = commandType;
        this.name = name;
        this.commandExecutor = commandExecutor;
    }

    public void run(CommandSender sender, Command command, String label, String[] args) throws TownyException {
        if (!commandExecutor.onCommand(sender, command, label, args))
            throw new TownyException();
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

    public void setName(String name) {
        this.name = name;
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
