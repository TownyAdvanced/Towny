package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;

import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.jetbrains.annotations.NotNull;

public class ConfirmCommand extends BukkitCommand {
	public ConfirmCommand(String name) {
		super(name);
		this.description = "Confirm command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, String[] strings) {
		
		// Check if confirmation is available.
		if (!ConfirmationHandler.hasConfirmation(commandSender)) {
			TownyMessaging.sendMsg(commandSender, Translation.of("no_confirmations_open"));
			return true;
		}
		
		// Handle the confirmation.
		ConfirmationHandler.acceptConfirmation(commandSender);
		return true;
	}
}
