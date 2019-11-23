package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * @author Goosius
 */
public class SiegeWarSpawnUtil {

	public static void throwErrorIfSpawnPointIsInsideBesiegedTown(Location spawnLoc) throws TownyException{
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		TownyWorld townyWorld = townyUniverse.getDataSource().getWorld(spawnLoc.getWorld().getName());
		Coord spawnCoord = Coord.parseCoord(spawnLoc);
		if(townyWorld.hasTownBlock(spawnCoord)) {
			TownBlock townBlock = townyWorld.getTownBlock(spawnCoord);
			if(townBlock.hasTown() && townBlock.getTown().hasSiege() && townBlock.getTown().getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {
				throw new TownyException(TownySettings.getLangString("msg_err_siege_cannot_spawn_into_besieged_town"));
			}
		}
	}
}
