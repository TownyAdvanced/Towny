package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.TimeTools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.BufferedWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A class that handles the processing confirmations sent in Towny.
 * 
 * @author Lukas Manour (ArticDive)
 * @author Suneet Tipirneni (Siris)
 */
public class ConfirmationHandler {
	private static Towny plugin;

	public static void initialize(Towny plugin) {
		ConfirmationHandler.plugin = plugin;
	}
	
	public static ConfirmationType consoleConfirmationType = ConfirmationType.NULL;
	public static Map<Resident, Confirmation> confirmations = new HashMap<>();
	
	public static void cancelConfirmation(Resident r) {
		confirmations.remove(r);
		TownyMessaging.sendMsg(r, TownySettings.getLangString("successful_cancel"));
	}

	/** 
	 * Removes confirmations for the console.
	 * 
	 * @param type of ConfirmationType
	 * @param successful if the calling operation was successful or not.
	 * @author LlmDl
	 */
	public static void removeConfirmation(final ConfirmationType type, boolean successful) {
		boolean sendmessage = false;
		if (!consoleConfirmationType.equals(ConfirmationType.NULL) && !successful) {
			sendmessage = true;
		}
		consoleConfirmationType = ConfirmationType.NULL;
		if (sendmessage) {
			TownyMessaging.sendMsg(TownySettings.getLangString("successful_cancel"));
		}
		
	}
	
	/**
	 * Handles confirmations sent via the console.
	 * 
	 * @param type of ConfirmationType.
	 * @throws TownyException - Generic TownyException
	 * @author LlmDl
	 */
	public static void handleConfirmation(ConfirmationType type) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
	}

	/**
	 * Registers and begins the timeout timer for the confirmation.
	 * 
	 * @param confirmation The confirmation to add.
	 */
	public static void registerConfirmation(Confirmation confirmation) {
		// Add the confirmation to the map.
		confirmations.put(confirmation.getResident(), confirmation);
		
		// Remove the confirmation after 20 seconds.
		Bukkit.getScheduler().runTaskLater(plugin, () -> {
			confirmations.remove(confirmation.getResident());
			TownyMessaging.sendErrorMsg("Confirmation Timed out.");
		}, 20L * 20);
	}

	/**
	 * Internal use only.
	 * 
	 * @param resident The resident using the confirmation.
	 */
	public static void handleConfirmation(Resident resident) {
		// Get confirmation
		Confirmation confirmation = confirmations.get(resident);

		// Get handler
		Runnable handler = confirmation.getHandler();

		// Execute handler.
		handler.run();
		
		// Remove confirmation as it's been handled.
		confirmations.remove(resident);
	}
}
