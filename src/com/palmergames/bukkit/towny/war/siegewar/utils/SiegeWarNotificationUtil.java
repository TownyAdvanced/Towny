package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import java.util.ArrayList;
import java.util.List;

public class SiegeWarNotificationUtil {

	public static void informSiegeParticipants(SiegeZone siegeZone, String message) {

		try {
			//Inform attackers
			List<Nation> attackingNations = new ArrayList<>();
			attackingNations.add(siegeZone.getAttackingNation());
			attackingNations.addAll(siegeZone.getAttackingNation().getMutualAllies());
			for (Nation nation : attackingNations) {
				TownyMessaging.sendPrefixedNationMessage(nation, message);
			}

			//Inform defenders
			if (siegeZone.getDefendingTown().hasNation()) {
				List<Nation> defendingNations = new ArrayList<>();
				defendingNations.add(siegeZone.getDefendingTown().getNation());
				defendingNations.addAll(siegeZone.getDefendingTown().getNation().getMutualAllies());
				for (Nation nation : defendingNations) {
					TownyMessaging.sendPrefixedNationMessage(nation, message);
				}
			} else {
				TownyMessaging.sendPrefixedTownMessage(siegeZone.getDefendingTown(), message);
			}
		} catch (Exception e) {
			System.out.println("Problem informing siege participants");
			System.out.println("Message : " + message);
			e.printStackTrace();
		}
	}
}
