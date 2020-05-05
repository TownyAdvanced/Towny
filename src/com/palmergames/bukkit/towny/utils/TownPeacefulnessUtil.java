package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class TownPeacefulnessUtil {

	/**
	 * This method adjust the peacefulness counters of all towns, where required
	 */
	public static void updateTownPeacefulnessCounters() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only adjust counter for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) && !town.isRuined())
				updateTownPeacefulnessCounters(town);
		}
	}

	public static void updateTownPeacefulnessCounters(Town town) {
		String messageKey;

		if(town.getPeacefulnessChangeConfirmationCounterDays() != 0) {
			town.decrementPeacefulnessChangeConfirmationCounterDays();

			if(town.getPeacefulnessChangeConfirmationCounterDays() < 1) {
				town.flipPeaceful();

				if(town.isPeaceful()) {
					if(TownySettings.getWarSiegeEnabled())
						messageKey = "msg_war_siege_town_became_peaceful";					
					else 
						messageKey = "msg_war_common_town_became_peaceful";
				} else {
					if(TownySettings.getWarSiegeEnabled())
						messageKey = "msg_war_siege_town_became_non_peaceful";
					else
						messageKey = "msg_war_common_town_became_non_peaceful";
				}

				TownyMessaging.sendGlobalMessage(
					String.format(TownySettings.getLangString(messageKey),
						town.getFormattedName()));
			}

			TownyUniverse.getInstance().getDataSource().saveTown(town);
		}
	}

	public static boolean doesPlayerHaveTownRelatedPeacefulness(Player player) {
		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			if(resident.hasTown()) {
				/* 
				 * True if:
				 * Current town is peaceful,
				 * current town is declared peaceful,
				 * or player just left a peaceful town
				*/
				return resident.getTown().isPeaceful() 
					|| resident.getTown().getDesiredPeacefulnessValue()
					|| resident.isPostTownLeavePeacefulEnabled();
			} else {
				//True if if player just left a peaceful town
				return resident.isPostTownLeavePeacefulEnabled();
			}

		} catch (NotRegisteredException e) { return false; }
}

	public static void grantPostTownLeavePeacefulnessToResident(Resident resident) {
		int hours = TownySettings.getWarCommonPeacefulTownsResidentPostLeavePeacefulnessDurationHours();
		String timeString = TimeMgmt.getFormattedTimeValue(hours * TimeMgmt.ONE_HOUR_IN_MILLIS);
		String message;
		if(TownySettings.getWarSiegeEnabled()) {
			message  = String.format(TownySettings.getLangString("msg_war_siege_resident_left_peaceful_town"), timeString);
		} else {
			message  = String.format(TownySettings.getLangString("msg_war_common_resident_left_peaceful_town"), timeString);
		}
		resident.setPostTownLeavePeacefulEnabled(true);
		resident.setPostTownLeavePeacefulHoursRemaining(hours);
		TownyMessaging.sendMsg(resident, message);
	}

	public static void updatePostTownLeavePeacefulnessCounters() {
		TownyDataSource townyDataSource = TownyUniverse.getInstance().getDataSource();
		
		List<Resident> residents = new ArrayList<>(townyDataSource.getResidents());
		ListIterator<Resident> residentItr = residents.listIterator();
		Resident resident;

		while (residentItr.hasNext()) {
			resident = residentItr.next();
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyDataSource.hasResident(resident.getName())) {
				if(resident.isPostTownLeavePeacefulEnabled()) {
					resident.decrementPostTownLeavePeacefulHoursRemaining();

					if(resident.getPostTownLeavePeacefulHoursRemaining() < 1) {
						resident.setPostTownLeavePeacefulEnabled(false);
					}

					townyDataSource.saveResident(resident);
				}
			}
		}
	}
}
