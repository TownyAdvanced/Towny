package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;
import java.util.Map;
import java.util.HashMap;

/**
 * This class is responsible for removing post spawn damage immunity
 *
 * @author Goosius
 */
public class RemovePostSpawnDamageImmunity {

	/**
	 * This method cycles through all online players
	 * It determines which players are currently damage immune, but have reached the immunity time limit - then removes the immunity
	 */
    public static void removePostSpawnDamageImmunity() {
    	try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Map<Player, Long> postSpawnDamageImmunityPlayerEndTimeMap = new HashMap<>(universe.getPostSpawnDamageImmunityPlayerEndTimeMap());

			for (Map.Entry<Player, Long> playerEndTimeEntry : postSpawnDamageImmunityPlayerEndTimeMap.entrySet()) {
				/*
				 * We are running in an Async thread so MUST verify all objects.
				 */
				if (playerEndTimeEntry.getKey().isOnline() && System.currentTimeMillis() > playerEndTimeEntry.getValue()) {
					universe.removeEntryFromPostSpawnDamageImmunityMap(playerEndTimeEntry.getKey());
				}
			}
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg("Problem removing post spawn damage immunity");
			e.printStackTrace();
		}
    }
}
