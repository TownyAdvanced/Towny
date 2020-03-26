package com.palmergames.bukkit.towny.war.siegewar.utils;


import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * This class contains utility functions related to the dynmap
 *
 * @author Goosius
 */
public class SiegeWarDynmapUtil {

	private static Set<Player> dynmapVisiblePlayers = new HashSet<>();

	public static void clearDynmapVisiblePlayers() {
		dynmapVisiblePlayers.clear();
	}

	public static void addDynmapVisiblePlayers(List<Player> players) {
		dynmapVisiblePlayers.addAll(players);
	}

	//This method is called by dynmap code
	public static Set<Player> getDynmapVisiblePlayers() {
		return new HashSet<>(dynmapVisiblePlayers);
	}
	/**
	 * Evaluate the visibility of players on the dynmap
	 *
	 * Kings, generals - always visible
	 * Others - can become map-invisible via the following methods
	 * 1. Equip shield
	 * 2. Mount horse
	 * 3. Pilot boat
	 * 4. Invisibility potion
	 */
	public static void evaluateTacticalVisibilityOfPlayers() {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();

			for(Player player: BukkitTools.getOnlinePlayers()) {

				if (universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_ATTACK.getNode())) {
					dynmapVisiblePlayers.add(player);

				} else if (player.getInventory().getItemInOffHand().getType() == Material.SHIELD
						|| player.getInventory().getItemInOffHand().getType() == Material.COMPASS) {
					//player is not visible on map

				} else if (player.getPotionEffect(PotionEffectType.INVISIBILITY) != null){
					//player is not visible on map

				} else {
					dynmapVisiblePlayers.add(player);
				}
			}
		} catch (Exception e) {
			System.out.println("Problem evaluating tactical map visibility");
			e.printStackTrace();
			dynmapVisiblePlayers.addAll(BukkitTools.getOnlinePlayers());
		}
	}
}
