package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public class RebelCommand extends BukkitCommand {
	public RebelCommand(String name) {
		super(name);
		this.description = "Rebel command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		if (!ConfirmationHandler.hasConfirmation(commandSender)) {
			TownyMessaging.sendErrorMsg(commandSender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		ConfirmationHandler.revokeConfirmation(commandSender);
		return true;
	}
}
