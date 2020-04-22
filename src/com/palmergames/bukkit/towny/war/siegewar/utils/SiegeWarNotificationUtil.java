package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;

import java.util.ArrayList;
import java.util.List;

public class SiegeWarNotificationUtil {

	public static void informSiegeParticipants(Siege siege, String message) {

		try {
			//Inform attackers
			List<Nation> attackingNations = new ArrayList<>();
			attackingNations.add(siege.getAttackingNation());
			attackingNations.addAll(siege.getAttackingNation().getMutualAllies());
			for (Nation nation : attackingNations) {
				TownyMessaging.sendPrefixedNationMessage(nation, message);
			}

			//Inform defenders
			if (siege.getDefendingTown().hasNation()) {
				List<Nation> defendingNations = new ArrayList<>();
				defendingNations.add(siege.getDefendingTown().getNation());
				defendingNations.addAll(siege.getDefendingTown().getNation().getMutualAllies());
				for (Nation nation : defendingNations) {
					TownyMessaging.sendPrefixedNationMessage(nation, message);
				}
			} else {
				TownyMessaging.sendPrefixedTownMessage(siege.getDefendingTown(), message);
			}
		} catch (Exception e) {
			System.out.println("Problem informing siege participants");
			System.out.println("Message : " + message);
			e.printStackTrace();
		}
	}
}
