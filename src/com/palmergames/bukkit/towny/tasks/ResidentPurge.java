package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;

/**
 * @author ElgarL
 * 
 */
public class ResidentPurge extends Thread {

	final Towny plugin;
	private final CommandSender sender;
	final long deleteTime;
	final boolean townless;

	/**
	 * @param plugin reference to Towny
	 * @param sender reference to CommandSender
	 * @param deleteTime time at which resident is purged (long)
	 * @param townless if resident should be 'Townless'
	 */
	public ResidentPurge(Towny plugin, CommandSender sender, long deleteTime, boolean townless) {

		super();
		this.plugin = plugin;
		this.deleteTime = deleteTime;
		this.setPriority(NORM_PRIORITY);
		this.townless = townless;
		this.sender = sender;
	}

	@Override
	public void run() {

		int count = 0;

		message("Scanning for old residents...");
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		for (Resident resident : new ArrayList<>(townyUniverse.getDataSource().getResidents())) {
			if (!resident.isNPC() && (System.currentTimeMillis() - resident.getLastOnline() > (this.deleteTime)) && !BukkitTools.isOnline(resident.getName())) {
				if (townless && resident.hasTown()) {
					continue;
				}
				count++;
				message("Deleting resident: " + resident.getName());
				townyUniverse.getDataSource().removeResident(resident);
			}
		}

		message("Resident purge complete: " + count + " deleted.");

	}

	private void message(String msg) {

		if (this.sender != null)
			TownyMessaging.sendMessage(this.sender, msg);
		else
			TownyMessaging.sendMsg(msg);

	}
}
