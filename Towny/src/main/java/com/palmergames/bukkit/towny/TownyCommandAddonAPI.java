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
    private static int args;

    public enum CommandType {
        RESIDENT(1),
        RESIDENT_SET(2),
        RESIDENT_TOGGLE(2),
        NATION(1),
        NATION_LIST_BY(3),
        NATION_SET(2),
        NATION_TOGGLE(2),
        TOWN(1),
		TOWN_BUY(2),
		TOWN_LIST_BY(3),
        TOWN_SET(2),
        TOWN_TOGGLE(2),
        PLOT(1),
        PLOT_SET(2),
        PLOT_TOGGLE(2),
        TOWNY(1),
        TOWNYADMIN(1),
        /** This CommandType will always execute using a CommandExecutor with the Town name prepended to the String[] args to which this command should apply. */
        TOWNYADMIN_TOWN(3),
        /** This CommandType will always execute using a CommandExecutor with the Nation name prepended to the String[] args to which this command should apply. */
        TOWNYADMIN_NATION(3),
        TOWNYADMIN_SET(1),
        TOWNYADMIN_TOGGLE(1),
		TOWNYADMIN_RELOAD(1),
        TOWNYWORLD(1),
        TOWNYWORLD_SET(2),
        TOWNYWORLD_TOGGLE(2);

		CommandType(int i) {
			args = i;
		}

		public int getTabCompletionPrecedingArgNumber() {
			return args;
		}
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
