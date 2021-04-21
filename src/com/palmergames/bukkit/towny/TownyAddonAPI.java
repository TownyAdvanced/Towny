package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.object.AddonCommand;

import org.bukkit.command.CommandExecutor;
import org.jetbrains.annotations.Nullable;

/**
 * @author Warriorrrr
 * @since 0.96.7.14
 */
public class TownyAddonAPI {
    private static List<AddonCommand> addedCommands = new ArrayList<AddonCommand>();

    public enum CommandType {
        RESIDENT,
        NATION,
        TOWN,
        PLOT,
        TOWNY,
        TOWNYADMIN
    }

    public static boolean addSubCommand(CommandType commandType, String subCommandName, CommandExecutor commandExecutor) {
        return addedCommands.add(new AddonCommand(commandType, subCommandName, commandExecutor));
    }

    public static boolean addSubCommand(AddonCommand command) {
        return addedCommands.add(command);
    }

    public static boolean removeSubCommand(CommandType commandType, String name) {
        return addedCommands.remove(getAddonCommand(commandType, name));
    }

    public static boolean removeSubCommand(AddonCommand command) {
        return addedCommands.remove(command);
    }

    public static boolean hasCommand(CommandType commandType, String name) {
        for (AddonCommand command : addedCommands) {
            if (command.getCommandType() == commandType && command.getName().equalsIgnoreCase(name))
                return true;
        }

        return false;
    }

    /**
     * @param commandType The commandType to check for.
     * @param name The name of the addon command to check for.
     * @return The command or null if it does not exist.
     */
    @Nullable
    public static AddonCommand getAddonCommand(CommandType commandType, String name) {
        for (AddonCommand command : addedCommands) {
            if (command.getCommandType() == commandType && command.getName().equalsIgnoreCase(name))
                return command;
        }
        return null;
    }

    public static List<String> getTabCompletes(CommandType commandType) {
        return addedCommands.stream().filter(command -> command.getCommandType() == commandType).map(AddonCommand::getName).collect(Collectors.toList());
    }

    public static List<AddonCommand> getAddedCommands() {
        return addedCommands;
    }
}
