package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
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
public class ResidentPurge implements Runnable {

	private final CommandSender sender;
	private final long deleteTime;
	private final boolean townless;
	private final Town town;
	private final boolean removeTown;

	/**
	 * @param sender Reference to CommandSender, or {@code null} to send messages to the console.
	 * @param deleteTime The time in milliseconds for which a resident needs to be offline for to be deleted.
	 * @param townless Whether only residents without a town will be deleted
	 * @param town If non-null, only residents from the given town will be purged.
	 * @param removeTown Whether    
	 */
	public ResidentPurge(CommandSender sender, long deleteTime, boolean townless, @Nullable Town town, boolean removeTown) {
		this.deleteTime = deleteTime;
		this.townless = townless;
		this.sender = sender;
		this.town = town;
		this.removeTown = removeTown;
	}
	
	public ResidentPurge(CommandSender sender, long deleteTime, boolean townless, @Nullable Town town) {
		this(sender, deleteTime, townless, town, TownySettings.isDeletingOldResidentsRemovingTownOnly());
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
				if (townless && resident.hasTown())
					continue;

				if (removeTown && resident.hasTown()) {
					resident.removeTown();
					count++;
					continue;
				}

				count++;
				townyUniverse.getDataSource().removeResident(resident);
				if (count < 50)
					message(Translatable.of("msg_deleting_resident", resident.getName()));
				if (count == 50)
					message(Translatable.of("msg_purge_reached_50_residents"));
			}
		}

		message(Translatable.of(townless ? "msg_purge_complete_x_removed_from_towns" : "msg_purge_complete", count));

	}

	private void message(Translatable msg) {

		if (this.sender != null)
			TownyMessaging.sendMsg(this.sender, msg);
		else
			TownyMessaging.sendMsg(msg);

	}
}
