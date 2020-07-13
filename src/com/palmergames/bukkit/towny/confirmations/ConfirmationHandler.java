package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;

import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that handles the processing confirmations sent in Towny.
 * 
 * @author Lukas Mansour (ArticDive)
 * @author Suneet Tipirneni (Siris)
 */
public class ConfirmationHandler {
	
	private static final class ConfirmationContext {
		final Confirmation confirmation;
		final int taskID;
		
		ConfirmationContext(Confirmation confirmation, int taskID) {
			this.confirmation = confirmation;
			this.taskID = taskID;
		}
	}
	
	private static Towny plugin;
	public static Map<CommandSender, ConfirmationContext> confirmations = new ConcurrentHashMap<>();

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}

	/**
	 * Revokes the confirmation associated with the given sender.
	 * 
	 * @param sender The sender to get the confirmation from.
	 */
	public static void revokeConfirmation(CommandSender sender) {
		ConfirmationContext context = confirmations.get(sender);
		
		Bukkit.getScheduler().cancelTask(context.taskID);
		Confirmation confirmation = context.confirmation;
		
		// Run the cancel handler.
		confirmation.getCancelHandler().run();
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
			revokeConfirmation(sender);
		}
		
		// Send the confirmation message.
		String title = confirmation.getTitle();
		TownyMessaging.sendConfirmationMessage(sender, title, null, null, null);
		
		int duration = confirmation.getDuration();
		
		Runnable handler = () -> {
			// Show cancel messages only if the confirmation exists.
			if (hasConfirmation(sender)) {
				confirmations.remove(sender);
				TownyMessaging.sendErrorMsg(sender, "Confirmation Timed out.");
			}
		};
		
		int taskID;
		long ticks = 20L * duration;
		if (confirmation.isAsync()) {
			taskID = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, handler, ticks).getTaskId();
		} else {
			taskID = Bukkit.getScheduler().runTaskLater(plugin, handler, ticks).getTaskId();
		}

		// Cache the task.
		confirmations.put(sender, new ConfirmationContext(confirmation, taskID));
	}

	/**
	 * Internal use only.
	 * 
	 * @param sender The sender using the confirmation.
	 */
	public static void acceptConfirmation(CommandSender sender) {
		// Get confirmation
		ConfirmationContext context = confirmations.get(sender);

		// Get handler
		Runnable handler = context.confirmation.getAcceptHandler();

		// Execute handler.
		handler.run();

		// Cancel task.
		Bukkit.getScheduler().cancelTask(context.taskID);
		
		// Remove confirmation as it's been handled.
		confirmations.remove(sender);
	}
	
	public static boolean hasConfirmation(CommandSender sender) {
		return confirmations.containsKey(sender);
	}
}
