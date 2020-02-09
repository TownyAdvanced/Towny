package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;

import org.bukkit.entity.Player;


/**
 * This class contains utility functions related to ruins
 *
 * @author Goosius
 */
public class SiegeWarRuinsUtil {

	/**
	 * This method returns true if the given player's town is ruined
	 * 
	 * @param player the player
	 * @return true if ruined, false if not
	 */
	public static boolean isPlayerTownRuined(Player player) {
		try {
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());

			if(resident.hasTown()) {
				return resident.getTown().isRuined();
			} else {
				return false;
			}
		} catch (NotRegisteredException x) {
			return false;
		}
	}

}
