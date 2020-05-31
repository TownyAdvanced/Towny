package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.command.InviteCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

public class AcceptCommand extends BukkitCommand {
	public AcceptCommand(String name) {
		super(name);
		this.description = "Accept command for Towny";
		this.usageMessage = "/" + name + " <Town>";
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		if (commandSender instanceof Player) {
			InviteCommand.parseAccept((Player) commandSender, strings);
		}
		return true;
	}
}
