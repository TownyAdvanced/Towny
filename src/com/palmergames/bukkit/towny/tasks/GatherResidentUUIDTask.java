package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.MojangException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;

import java.io.IOException;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * @author LlmDl
 * 
 */
public class GatherResidentUUIDTask implements Runnable {

	@SuppressWarnings("unused")
	private Towny plugin;
	private final static Queue<Resident> queue = new ConcurrentLinkedQueue<>();
	private static boolean offlineModeDetected = false;

	/**
	 * @param plugin reference to Towny
	 */
	public GatherResidentUUIDTask(Towny plugin) {

		super();
		this.plugin = plugin;
	}

	@Override
	public void run() {
		if (queue.isEmpty()) {
			TownyTimerHandler.toggleGatherResidentUUIDTask(false);
			return;
		}
		Resident resident = queue.poll();
		if (resident.hasUUID()) {
			// We already have the UUID but we can still test if server is in offline mode. 
			if (!offlineModeDetected && resident.getUUID().version() == 3) // True offline servers return a v3 UUID instead of v4.
				offlineModeDetected = true;
			
			return;
		}
		if (resident.isNPC()) // This is one of our own NPC residents, lets give them a UUID if they don't already have one.
			applyUUID(resident, UUID.randomUUID(), "Towny");
		
		UUID uuid = BukkitTools.getUUIDSafely(resident.getName()); // Get a UUID from the server's playercache without calling to Mojang. 

		if (uuid != null) { // The player has been online recently enough to be in the cache.
			if (!offlineModeDetected && uuid.version() == 3) // True offline servers return a v3 UUID instead of v4.
				offlineModeDetected = true;
			
			applyUUID(resident, uuid, "cache"); 

		} else if (!offlineModeDetected) { // If the server is in true offline mode the following test would result always return 204, wiping the database.
			try {
				uuid = BukkitTools.getUUIDFromResident(resident); // This will call mojang for the player's UUID.
			} catch (MojangException e) {
				// 204 is thrown when the player account no longer exists, they will not be logging in again so they can be deleted.
				TownyMessaging.sendErrorMsg("HTTP Response Code 204 - Mojang says " + resident.getName() + " no longer has an account, this could be in error. Unable to gather UUID.");
				return;	
			} catch (IOException e) {
				TownyMessaging.sendErrorMsg("Resident: " + resident.getName() + " caused an IOException in the GatheringResidentUUID task. Unable to gather UUID.");
				return;
			}
			if (uuid != null)
				applyUUID(resident, uuid, "Mojang");
			else {
				// The mojang API could not be reached so lets just shut down the task for a minute.
				TownyMessaging.sendDebugMsg("Could not resolve UUID for resident: " + resident.getName() + ", sorry! Gather task will try again in a minute.");
				queue.add(resident);
				TownyTimerHandler.toggleGatherResidentUUIDTask(false);
				TownyTimerHandler.toggleGatherResidentUUIDTask(true);
			}			
		}
	}
	
	public static void addResident(Resident resident) {
		queue.add(resident);
	}

	private void applyUUID(Resident resident, UUID uuid, String source) {
		resident.setUUID(uuid);
		try {
			TownyUniverse.getInstance().registerResidentUUID(resident);
		} catch (AlreadyRegisteredException e) {
			TownyMessaging.sendErrorMsg(String.format("Error registering resident UUID. Resident '%s' already has a UUID registered!", resident.getName()));
		}
		resident.save();
		TownySettings.incrementUUIDCount();
		TownyMessaging.sendDebugMsg("UUID stored for " + resident.getName() + " received from " + source + ". Progress: " + TownySettings.getUUIDPercent() + ".");
	}
	
	public static void markOfflineMode() {
		offlineModeDetected = true;
	}
	
}
