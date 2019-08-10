package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.ConfirmationType;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.defaults.BukkitCommand;
import org.bukkit.entity.Player;

public class ConfirmCommand extends BukkitCommand {
	public ConfirmCommand(String name) {
		super(name);
		this.description = "Confirm command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(CommandSender commandSender, String s, String[] strings) {
		if (commandSender instanceof Player) {
			Player player = (Player) commandSender;
			Resident resident;
			try {
				resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			} catch (TownyException e) {
				return true;
			}
			if (resident != null) {
				if (resident.getConfirmationType() != null) {
					try {
						ConfirmationHandler.handleConfirmation(resident, resident.getConfirmationType());
						return true;
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
						return true;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("no_confirmations_open"));
					return true;
				}
			}
		} else {
			// Must be a console.
			if (!ConfirmationHandler.consoleConfirmationType.equals(ConfirmationType.NULL)) {
				try {
					ConfirmationHandler.handleConfirmation(ConfirmationHandler.consoleConfirmationType);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(Bukkit.getConsoleSender(), e.getMessage());
					return true;
				}
				return true;
			} else { 
				TownyMessaging.sendMsg(TownySettings.getLangString("no_confirmations_open"));
				return true;
			}
		}
		return true;
	}
}
