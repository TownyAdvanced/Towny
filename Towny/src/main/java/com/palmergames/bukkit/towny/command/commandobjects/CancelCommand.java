package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.object.Translatable;
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
		if (!ConfirmationHandler.hasConfirmation(commandSender)) {
			TownyMessaging.sendErrorMsg(commandSender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		ConfirmationHandler.revokeConfirmation(commandSender);
		return true;
	}
}
