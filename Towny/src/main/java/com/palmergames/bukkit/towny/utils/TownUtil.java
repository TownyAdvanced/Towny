package com.palmergames.bukkit.towny.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;

public class TownUtil {
	
	private TownUtil() {
		throw new IllegalStateException("Utility Class");
	}

	/**
	 * Makes a list of {@linkplain Resident}s who haven't logged in in the given days.
	 * NPCs and Mayors are not included.
	 * 
	 * @param resList List of Residents from which to test for inactivity.
	 * @param days Number of days after which players are considered inactive.
	 * @since 0.97.0.7
	 */
	public static List<Resident> gatherInactiveResidents(List<Resident> resList, int days) {
		return resList.stream()
				.filter(res -> !res.isNPC() && !res.isMayor() && !BukkitTools.isOnline(res.getName()) && (System.currentTimeMillis() - res.getLastOnline() > TimeTools.getMillis(days + "d")))
				.collect(Collectors.toList());
	}

	public static void checkNationResidentsRequirementsOfTown(Town town) {
		Nation nation = town.getNationOrNull();
		if (nation == null)
			return;

		// Check non-capital rules first, towns must maintain a number of residents to be a part of a nation.
		if (!town.isCapital() && !town.hasEnoughResidentsToJoinANation()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_not_enough_residents_left_nation", town.getName()));
			town.removeNation();
			return;
		}

		// Check the nation-creation rules that apply to nation capitals. Towns must maintain a number of residents to do so.
		if (town.isCapital() && !town.hasEnoughResidentsToBeANationCapital()) {
			// If a new capital can be found we don't delete the nation.
			if (findNewCapital(town, nation))
				return;

			List<Player> onlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation);
			// No new capital found, delete the nation and potentially refund the capital town.
			if (!TownyUniverse.getInstance().getDataSource().removeNation(nation, DeleteNationEvent.Cause.NOT_ENOUGH_RESIDENTS))
				return;

			onlinePlayers.forEach(p -> TownyMessaging.sendMsg(p, Translatable.of("msg_nation_disbanded_town_not_enough_residents", town.getName())));
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation.getName()));

			if (TownyEconomyHandler.isActive() && TownySettings.isRefundNationDisbandLowResidents()) {
				town.getAccount().deposit(TownySettings.getNewNationPrice(), "nation refund");
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_not_enough_residents_refunded", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice())));
			}
		} 
	}

	private static boolean findNewCapital(Town town, Nation nation) {
		for (Town newCapital : nation.getTowns())
			if (newCapital.hasEnoughResidentsToBeANationCapital()) {
				// We've found a suitable new capital that has enough residents.
				nation.setCapital(newCapital);
				// Check if the old capital can remain in the nation as a non-capital town.
				if (!town.hasEnoughResidentsToJoinANation()) {
					town.removeNation();
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_capital_not_enough_residents_left_nation", town.getName()));
				}
				// Announce the new capital and return true.
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_not_enough_residents_no_longer_capital", newCapital.getName()));
				return true;
			}
		// Return false and require the nation to be deleted.
		return false;
	}

	public static boolean townHasEnoughResidentsToBeANationCapital(Town town) {
		if (TownySettings.getNumResidentsCreateNation() < 1)
			return true;
		return town.getNumResidents() >= TownySettings.getNumResidentsCreateNation();
	}

	public static boolean townHasEnoughResidentsToJoinANation(Town town) {
		if (TownySettings.getNumResidentsJoinNation() < 1)
			return true;
		return town.getNumResidents() >= TownySettings.getNumResidentsJoinNation();
	}

	public static boolean townCanHaveThisAmountOfResidents(Town town, int residentCount, boolean isCapital) {
		int maxResidents = !isCapital
				? !town.hasNation() ? getMaxAllowedNumberOfResidentsWithoutNation(town) : TownySettings.getMaxResidentsPerTown()
				: TownySettings.getMaxResidentsPerTownCapitalOverride();

		return maxResidents == 0 || residentCount <= maxResidents;
	}

	public static int getMaxAllowedNumberOfResidentsWithoutNation(Town town) {
		int maxResidents = TownySettings.getMaxNumResidentsWithoutNation() > 0 ? TownySettings.getMaxNumResidentsWithoutNation() : TownySettings.getMaxResidentsPerTown();
		return maxResidents == 0 ? Integer.MAX_VALUE : maxResidents;
	}
}
