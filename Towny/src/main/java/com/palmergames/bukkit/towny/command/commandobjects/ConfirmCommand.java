package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;

import com.palmergames.bukkit.towny.object.Translatable;
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
	public boolean execute(@NotNull CommandSender sender, @NotNull String label, String[] args) {
		
		// Check if confirmation is available.
		if (!ConfirmationHandler.hasConfirmation(sender)) {
			TownyMessaging.sendMsg(sender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		// Handle the confirmation.
		ConfirmationHandler.acceptConfirmation(sender);
		return true;
	}
}
