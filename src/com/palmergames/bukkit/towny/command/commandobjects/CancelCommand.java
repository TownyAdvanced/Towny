package com.palmergames.bukkit.towny.command.commandobjects;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public class CancelCommand extends BukkitCommand {
	public CancelCommand(String name) {
		super(name);
		this.description = "Cancel command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		return true;
	}
}
