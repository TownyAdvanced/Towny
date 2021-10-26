package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.object.AddonCommand;

import org.bukkit.command.CommandExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author Warriorrrr
 * @since 0.97.0.1
 */
public class TownyCommandAddonAPI {
    private static final Map<CommandType, Map<String, AddonCommand>> addedCommands = new HashMap<>();

    public enum CommandType {
        RESIDENT,
        RESIDENT_SET,
        RESIDENT_TOGGLE,
        NATION,
        NATION_SET,
        NATION_TOGGLE,
        TOWN,
        TOWN_SET,
        TOWN_TOGGLE,
        PLOT,
        PLOT_SET,
        PLOT_TOGGLE,
        TOWNY,
        TOWNYADMIN,
        TOWNYADMIN_SET,
        TOWNYADMIN_TOGGLE,
        TOWNYWORLD,
        TOWNYWORLD_SET,
        TOWNYWORLD_TOGGLE
    }

    public static boolean addSubCommand(@NotNull CommandType commandType, @NotNull String subCommandName, @NotNull CommandExecutor commandExecutor) {
		return addSubCommand(new AddonCommand(commandType, subCommandName, commandExecutor));
    }

    public static boolean addSubCommand(@NotNull AddonCommand command) {
		if (addedCommands.computeIfAbsent(command.getCommandType(), k -> new HashMap<>()).containsKey(command.getName().toLowerCase()))
			return false;
		
        addedCommands.get(command.getCommandType()).put(command.getName().toLowerCase(), command);
		return true;
    }

    public static boolean removeSubCommand(@NotNull CommandType commandType, @NotNull String name) {
		if (!addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).containsKey(name.toLowerCase()))
			return false;
		
		addedCommands.get(commandType).remove(name.toLowerCase());
		return true;
    }

    public static boolean removeSubCommand(@NotNull AddonCommand command) {
        return removeSubCommand(command.getCommandType(), command.getName());
    }

    public static boolean hasCommand(@NotNull CommandType commandType, @NotNull String name) {
        return addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).containsKey(name.toLowerCase());
    }

    /**
     * @param commandType The commandType to check for.
     * @param name The name of the addon command to check for.
     * @return The command or null if it does not exist.
     */
    @Nullable
    public static AddonCommand getAddonCommand(@NotNull CommandType commandType, @NotNull String name) {
        return addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).get(name.toLowerCase());
    }

    public static List<String> getTabCompletes(@NotNull CommandType commandType, @NotNull List<String> addFrom) {
        List<String> suggestions = new ArrayList<>(addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).keySet());
        suggestions.addAll(addFrom);
        return suggestions;
    }

    public static Map<CommandType, Map<String, AddonCommand>> getAddedCommands() {
        return new HashMap<>(addedCommands);
    }
}
