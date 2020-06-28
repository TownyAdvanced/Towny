package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;

import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.xml.ws.http.HTTPException;

/**
 * @author ElgarL
 * 
 */
public class GatherResidentUUIDTask extends Thread {

	Towny plugin;
	private final static Queue<Resident> queue = new ConcurrentLinkedQueue<>();

	/**
	 * @param plugin reference to Towny
	 * @param sender reference to CommandSender
	 * @param deleteTime time at which resident is purged (long)
	 * @param townless if resident should be 'Townless'
	 */
	public GatherResidentUUIDTask(Towny plugin) {

		super();
		this.plugin = plugin;
		this.setPriority(NORM_PRIORITY);
	}

	@Override
	public void run() {
		if (queue.isEmpty()) {
			TownyTimerHandler.toggleGatherResidentUUIDTask(false);
			return;
		}
		Resident resident = queue.peek();
		if (resident.hasUUID())
			return;
		if (resident.isNPC())
			resident.setUUID(UUID.randomUUID());
		UUID uuid = BukkitTools.getUUIDSafely(resident.getName());

		if (uuid != null)
			applyUUID(resident, uuid, "cache");
		else {
			try {
				uuid = BukkitTools.getUUIDFromResident(resident);
			} catch (HTTPException e) {
				TownyMessaging.sendErrorMsg("HTTP Response Code " + e.getStatusCode() + " - Mojang says " + resident.getName() + " has never had an account.");
				// TODO: Decide what to do with these residents.
				queue.remove(resident);
				return;
			}
			if (uuid != null)
				applyUUID(resident, uuid, "Mojang");
			else {
				TownyMessaging.sendDebugMsg("Could not resolve UUID for resident: " + resident.getName() + ", sorry! Gather task will try again in a minute.");
				queue.remove(resident);
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
		TownyUniverse.getInstance().getDataSource().saveResident(resident);
		queue.remove(resident);
		TownySettings.uuidCount++;
		TownyMessaging.sendDebugMsg("UUID stored for " + resident.getName() + " received from " + source + ". Progress: " + TownySettings.getUUIDPercent() + ".");
		checkEnd();
	}
	
	private void checkEnd() {
		if (TownySettings.getUUIDPercent().equals("100%")) {
			TownyTimerHandler.toggleGatherResidentUUIDTask(false);
			System.out.println("[Towny] Resident UUID Gathering Complete - Shutting Down Background Task - Your Towny database is ready for UUID conversion.");
		}		
	}
}
