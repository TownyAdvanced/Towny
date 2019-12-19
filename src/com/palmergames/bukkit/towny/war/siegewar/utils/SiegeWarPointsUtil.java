package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * This class contains utility functions related to siege points
 * 
 * @author Goosius
 */
public class SiegeWarPointsUtil {

	/**
	 * This method calculates who has won a siege
	 * 
	 * Defending Town - The defending town has won the siege if all of the siege zones are in negative points.
	 * Attacking Nation - an attacking nation has won the siege if its siege points are positive,
	 *                    and higher than the siegepoints of any other attacker.
	 *
	 * @param siege the siege
	 * @return the winner of the siege
	 */
	public static TownyObject calculateSiegeWinner(Siege siege) {
        TownyObject winner = siege.getDefendingTown();
        int winningPoints = 0;

        for(Map.Entry<Nation, SiegeZone> entry: siege.getSiegeZones().entrySet()) {
            if(entry.getValue().getSiegePoints() > winningPoints) {
                winner = entry.getKey();
                winningPoints = entry.getValue().getSiegePoints();
            }
        }

        return winner;
    }

	/**
	 * This method determines if a players is in the 'point scoring zone' of a siegezone
	 * 
	 * - Must be in same world as flag
	 * - Must be in wilderness  (This is important, otherwise the defender could create a 'safe space' 
	 *                           inside a perm-protected town block, and gain points there with no threat.)
	 * - Must be within 1 townblock length of the flag
	 *
	 * @param player the player
	 * @param siegeZone the siege zone
	 * @return true if a player in in the siege point zone
	 */
	public static boolean isPlayerInSiegePointZone(Player player, SiegeZone siegeZone) {

		return player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
				&& !TownyAPI.getInstance().hasTownBlock(player.getLocation())
				&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize();
	}
}
