package com.palmergames.bukkit.towny.war.eventwar.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;

import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;

public class StateCommand extends BukkitCommand {
	public StateCommand(String name) {
		super(name);
		this.description = "State command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		
		// Check if confirmation is available.
		if (!ConfirmationHandler.hasConfirmation(commandSender)) {
			TownyMessaging.sendMsg(commandSender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		// Handle the confirmation.
		ConfirmationHandler.acceptConfirmation(commandSender);
		return true;
	}
}
