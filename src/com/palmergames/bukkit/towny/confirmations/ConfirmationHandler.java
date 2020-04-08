package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.HashMap;
import java.util.Map;

/**
 * A class that handles the processing confirmations sent in Towny.
 * 
 * @author Lukas Manour (ArticDive)
 * @author Suneet Tipirneni (Siris)
 */
public class ConfirmationHandler {
	
	private static Towny plugin;
	public static Map<CommandSender, Confirmation> confirmations = new HashMap<>();

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	/**
	 * Cancels the confirmation associated with the given sender.
	 * 
	 * @param sender The sender to get the confirmation from.
	 */
	public static void cancelConfirmation(CommandSender sender) {
		confirmations.remove(sender);
		TownyMessaging.sendMsg(sender, TownySettings.getLangString("successful_cancel"));
	}

	/**
	 * Registers and begins the timeout timer for the confirmation.
	 * 
	 * @param confirmation The confirmation to add.
	 */
	public static void registerConfirmation(Confirmation confirmation) {
		// Add the confirmation to the map.
		confirmations.put(confirmation.getSender(), confirmation);
		
		// Remove the confirmation after 20 seconds.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			confirmations.remove(confirmation.getSender());
			TownyMessaging.sendErrorMsg("Confirmation Timed out.");
		}, 20L * 20);
	}

	/**
	 * Internal use only.
	 * 
	 * @param sender The sender using the confirmation.
	 */
	public static void handleConfirmation(CommandSender sender) {
		// Get confirmation
		Confirmation confirmation = confirmations.get(sender);

		// Get handler
		Runnable handler = confirmation.getHandler();

		// Execute handler.
		handler.run();
		
		// Remove confirmation as it's been handled.
		confirmations.remove(sender);
	}
	
	public static boolean hasConfirmation(CommandSender sender) {
		return confirmations.containsKey(sender);
	}
}
