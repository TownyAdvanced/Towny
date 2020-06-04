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
		Bukkit.getScheduler().cancelTask(confirmations.get(sender).getTaskID());
		confirmations.remove(sender);
		TownyMessaging.sendMsg(sender, TownySettings.getLangString("successful_cancel"));
	}

	/**
	 * Registers and begins the timeout timer for the confirmation.
	 * 
	 * @param sender The sender to receive the confirmation.
	 * @param confirmation The confirmation to add.
	 */
	public static void sendConfirmation(CommandSender sender, Confirmation confirmation) {
		
		// Check if confirmation is already active and perform appropriate actions.
		if (confirmations.containsKey(sender)) {
			// Cancel prior Confirmation actions.
			cancelConfirmation(sender);
		}
		
		// Add the confirmation to the map.
		confirmations.put(sender, confirmation);
		
		// Send the confirmation message.
		String title = confirmation.getTitle();
		TownyMessaging.sendConfirmationMessage(sender, title, null, null, null);
		
		int duration = confirmation.getDuration();
		
		// Remove the confirmation after 20 seconds.
		int taskID = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Show cancel messages only if the confirmation exists.
			if (hasConfirmation(sender)) {
				confirmations.remove(sender);
				TownyMessaging.sendErrorMsg(sender, "Confirmation Timed out.");
			}
		}, 20L * duration).getTaskId();
		
		// Cache task ID
		confirmation.setTaskID(taskID);
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

		// Cancel task.
		Bukkit.getScheduler().cancelTask(confirmation.getTaskID());
		
		// Remove confirmation as it's been handled.
		confirmations.remove(sender);
		
	}
	
	public static boolean hasConfirmation(CommandSender sender) {
		return confirmations.containsKey(sender);
	}
}
