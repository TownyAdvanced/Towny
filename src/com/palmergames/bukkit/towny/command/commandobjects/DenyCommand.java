package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.command.InviteCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

public class DenyCommand extends BukkitCommand {
	public DenyCommand(String name) {
		super(name);
		this.description = "Deny command for Towny";
		this.usageMessage = "/" + name + " <Town>";
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		if (commandSender instanceof Player) {
			InviteCommand.parseDeny((Player) commandSender, strings);
			return true;
		} else {
			return true;
		}
	}
}
