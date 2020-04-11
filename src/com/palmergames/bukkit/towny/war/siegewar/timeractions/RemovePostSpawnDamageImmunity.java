package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import java.util.ArrayList;

import static com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDamageUtil.TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID;

/**
 * This class is responsible for removing post spawn damage immunity
 *
 * @author Goosius
 */
public class RemovePostSpawnDamageImmunity {

	/**
	 * This method cycles through all online players
	 * It determines which players are currently damage immune, and have also reached the immunity time limit - then removes the immunity
	 */
    public static void removePostSpawnDamageImmunity() {
    	for(Player player: new ArrayList<>(BukkitTools.getOnlinePlayers())) {
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if (player.isOnline() && player.hasMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID)) {
					if(System.currentTimeMillis() > player.getMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID).get(0).asLong()) {
						player.removeMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID, Towny.getPlugin());
					}
				}
			} catch (Exception e) {
				try {
					TownyMessaging.sendErrorMsg("Problem removing post spawn damage immunity for player " + player.getName());
				} catch (Exception e2) {
					TownyMessaging.sendErrorMsg("Problem removing post spawn damage immunity (could not read player name");
				}
				e.printStackTrace();
			}
		}
    }
}
