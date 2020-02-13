package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

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
		TownyUniverse universe = TownyUniverse.getInstance();
		List<Player> onlinePlayers = new ArrayList<>(BukkitTools.getOnlinePlayers());
		ListIterator<Player> playerItr = onlinePlayers.listIterator();
		Player player;
		Resident resident;

		while (playerItr.hasNext()) {
			player = playerItr.next();
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if(player.isOnline() && player.isInvulnerable()) {
					resident = universe.getDataSource().getResident(player.getName());
					if(System.currentTimeMillis() > resident.getDamageImmunityEndTime()) {
						player.setInvulnerable(false);
					}
				}
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Problem removing immunity from player " + player.getName());
				e.printStackTrace();
			}
		}
    }

}
