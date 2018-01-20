package com.palmergames.bukkit.towny.command.commandobjects;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public class ConfirmCommand extends BukkitCommand {
	public ConfirmCommand(String name) {
		super(name);
		this.description = "Confirm command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		return false;
	}
}
