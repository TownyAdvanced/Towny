package com.palmergames.bukkit.towny.confirmations;

import com.github.bsideup.jabel.Desugar;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationCancelEvent;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationConfirmEvent;
import com.palmergames.bukkit.towny.confirmations.event.ConfirmationSendEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A class that handles the processing confirmations sent in Towny.
 * 
 * @author Articdive
 * @author Suneet Tipirneni (Siris)
 */
public class ConfirmationHandler {

	private final static Towny plugin = Towny.getPlugin();
	private final static Map<CommandSender, ConfirmationContext> confirmations = new ConcurrentHashMap<>();
	
	@Desugar
	private record ConfirmationContext(Confirmation confirmation, int taskID) {}

	/**
	 * Revokes the confirmation associated with the given sender.
	 * 
	 * @param sender The sender to get the confirmation from.
	 */
	public static void revokeConfirmation(CommandSender sender) {
		ConfirmationContext context = confirmations.get(sender);
		
		// Only continue if player has an active confirmation
		if (context == null)
			return;
		
		Bukkit.getScheduler().cancelTask(context.taskID);
		Confirmation confirmation = context.confirmation;
		confirmations.remove(sender);
		
		// Run the cancel handler.
		if (confirmation.getCancelHandler() != null) {
			confirmation.getCancelHandler().run();
			
		} else {
			TownyMessaging.sendMsg(sender, Translatable.of("successful_cancel"));

		}
		
		Bukkit.getPluginManager().callEvent(new ConfirmationCancelEvent(confirmation, sender, false));
	}

	/**
	 * Registers and begins the timeout timer for the confirmation.
	 * 
	 * @param sender The sender to receive the confirmation.
	 * @param confirmation The confirmation to add.
	 */
	public static void sendConfirmation(CommandSender sender, Confirmation confirmation) {
		ConfirmationSendEvent event = new ConfirmationSendEvent(confirmation, sender);
		Bukkit.getPluginManager().callEvent(event);

		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(sender, event.getCancelMessage());
			return;
		}
		
		// Check if confirmation is already active and perform appropriate actions.
		if (confirmations.containsKey(sender)) {
			// Cancel prior Confirmation actions.
			revokeConfirmation(sender);
		}
		
		// Send the confirmation message.
		if (event.isSendingMessage())
			TownyMessaging.sendConfirmationMessage(sender, confirmation);

		// Set up the task to show the timeout message after the expiration.
		int taskID = Bukkit.getScheduler().runTaskLater(plugin, () -> {
			// Show cancel messages only if the confirmation still exists.
			if (hasConfirmation(sender)) {
				confirmations.remove(sender);
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_confirmation_timed_out"));
				Bukkit.getPluginManager().callEvent(new ConfirmationCancelEvent(confirmation, sender, true));
			}
		}, (20L * confirmation.getDuration())).getTaskId();

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
		
		if (context == null)
			return;
		
		ConfirmationConfirmEvent event = new ConfirmationConfirmEvent(context.confirmation, sender);
		Bukkit.getPluginManager().callEvent(event);
		
		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(event.getCancelMessage());
			return;
		}

		// Get handler
		Runnable handler = context.confirmation.getAcceptHandler();

		// Cancel task.
		Bukkit.getScheduler().cancelTask(context.taskID);

		// Remove confirmation as it's been handled.
		confirmations.remove(sender);

		// Execute handler.
		if (context.confirmation.isAsync()) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, handler);
		} else {
			Bukkit.getScheduler().runTask(plugin, handler);
		}
	}
	
	public static boolean hasConfirmation(CommandSender sender) {
		return confirmations.containsKey(sender);
	}
}
