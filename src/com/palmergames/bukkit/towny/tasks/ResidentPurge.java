package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ElgarL
 * 
 */
public class ResidentPurge extends Thread {

	final Towny plugin;
	private final CommandSender sender;
	final long deleteTime;
	final boolean townless;
	final Town town;

	/**
	 * @param plugin reference to Towny
	 * @param sender reference to CommandSender
	 * @param deleteTime time at which resident is purged (long)
	 * @param townless if resident should be 'Townless'
	 */
	public ResidentPurge(Towny plugin, CommandSender sender, long deleteTime, boolean townless, @Nullable Town town) {

		super();
		this.plugin = plugin;
		this.deleteTime = deleteTime;
		this.setPriority(NORM_PRIORITY);
		this.townless = townless;
		this.sender = sender;
		this.town = town;
	}

	@Override
	public void run() {

		int count = 0;

		message(Translatable.of("msg_scanning_for_old_residents"));
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Resident> residentList;
		if (town != null) {
			residentList = new ArrayList<>(town.getResidents());
		} else {
			residentList = new ArrayList<>(townyUniverse.getResidents());
		}
		for (Resident resident : residentList) {
			if (!resident.isNPC() && (System.currentTimeMillis() - resident.getLastOnline() > (this.deleteTime)) && !BukkitTools.isOnline(resident.getName())) {
				if (townless && resident.hasTown()) {
					continue;
				}
				count++;
				message(Translatable.of("msg_deleting_resident", resident.getName()));
				townyUniverse.getDataSource().removeResident(resident);
			}
		}

		message(Translatable.of("msg_purge_complete", count));

	}

	private void message(Translatable msg) {

		if (this.sender != null)
			TownyMessaging.sendMsg(this.sender, msg);
		else
			TownyMessaging.sendMsg(msg);

	}
}
