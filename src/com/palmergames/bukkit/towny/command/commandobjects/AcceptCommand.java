package com.palmergames.bukkit.towny.command.commandobjects;

import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public class AcceptCommand extends BukkitCommand {
	public AcceptCommand(String name) {
		super(name);
		this.description = "Accept command for Towny";
		this.usageMessage = "/" + name + " <Town>";
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		return false;
	}
}
