package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
        NATION_LIST_BY,
        NATION_SET,
        NATION_TOGGLE,
        TOWN,
		TOWN_BUY,
		TOWN_LIST_BY,
        TOWN_SET,
        TOWN_TOGGLE,
        PLOT,
        PLOT_SET,
        PLOT_TOGGLE,
		PLOT_GROUP_TOGGLE,
        TOWNY,
        TOWNYADMIN,
        /** This CommandType will always execute using a CommandExecutor with the Town name prepended to the String[] args to which this command should apply. */
        TOWNYADMIN_TOWN,
        /** This CommandType will always execute using a CommandExecutor with the Nation name prepended to the String[] args to which this command should apply. */
        TOWNYADMIN_NATION,
        TOWNYADMIN_SET,
        TOWNYADMIN_TOGGLE,
		TOWNYADMIN_RELOAD,
        TOWNYWORLD,
        TOWNYWORLD_SET,
        TOWNYWORLD_TOGGLE
    }

    public static boolean addSubCommand(@NotNull CommandType commandType, @NotNull String subCommandName, @NotNull CommandExecutor commandExecutor) {
		return addSubCommand(new AddonCommand(commandType, subCommandName, commandExecutor));
    }

    public static boolean addSubCommand(@NotNull AddonCommand command) {
		if (addedCommands.computeIfAbsent(command.getCommandType(), k -> new HashMap<>()).containsKey(command.getName().toLowerCase(Locale.ROOT)))
			return false;
		
        addedCommands.get(command.getCommandType()).put(command.getName().toLowerCase(Locale.ROOT), command);
		return true;
    }

    public static boolean removeSubCommand(@NotNull CommandType commandType, @NotNull String name) {
		if (!addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).containsKey(name.toLowerCase(Locale.ROOT)))
			return false;
		
		addedCommands.get(commandType).remove(name.toLowerCase(Locale.ROOT));
		return true;
    }

    public static boolean removeSubCommand(@NotNull AddonCommand command) {
        return removeSubCommand(command.getCommandType(), command.getName());
    }

    public static boolean hasCommand(@NotNull CommandType commandType, @NotNull String name) {
        return addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * @param commandType The commandType to check for.
     * @param name The name of the addon command to check for.
     * @return The command or null if it does not exist.
     */
    @Nullable
    public static AddonCommand getAddonCommand(@NotNull CommandType commandType, @NotNull String name) {
        return addedCommands.computeIfAbsent(commandType, k -> new HashMap<>()).get(name.toLowerCase(Locale.ROOT));
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
