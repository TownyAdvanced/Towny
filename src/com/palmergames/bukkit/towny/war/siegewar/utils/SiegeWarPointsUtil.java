package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.entity.Player;

import java.util.Map;

public class SiegeWarPointsUtil {
	public static TownyObject calculateSiegeWinner(Siege siege) {
        //If all siege zones are negative points, defender wins
        //Otherwise, the siege zone attacker with the highest points wins

        TownyObject winner = siege.getDefendingTown();
        int winningPoints = 0;

        for(Map.Entry<Nation, SiegeZone> entry: siege.getSiegeZones().entrySet()) {
            if(entry.getValue().isActive()
                && entry.getValue().getSiegePoints() > winningPoints) {
                winner = entry.getKey();
                winningPoints = entry.getValue().getSiegePoints();
            }
        }

        return winner;
    }

	//Must be in same world as flag
	//Must be within 1 townblock length of flag
	//Must be in wilderness
	public static boolean isPlayerInSiegePointZone(Player player, SiegeZone siegeZone) {

		if (player.getLocation().getWorld() == siegeZone.getFlagLocation().getWorld()
				&& player.getLocation().distance(siegeZone.getFlagLocation()) < TownySettings.getTownBlockSize()) {

			TownBlock townBlock = TownyUniverse.getTownBlock(player.getLocation());
			if (townBlock == null) {
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}
	}
}
