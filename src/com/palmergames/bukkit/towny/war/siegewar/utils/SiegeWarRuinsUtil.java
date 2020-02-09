package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
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

	/**
	 * Put town into ruined state:
	 * 1. The mayor will be set to an NPC
	 * 2. All perms will be enabled
	 * 3. The residents cannot run /plot commands, and some /t commands
	 * 4. Town will be deleted after 2 upkeep cycles
	 */
	public static void putTownIntoRuinedState(Town town, Towny plugin) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion
		try {
			TownyAdminCommand adminCommand = new TownyAdminCommand(plugin);
			adminCommand.adminSet(new String[]{"mayor", town.getName(), "npc"});
		} catch (TownyException e) {
			e.printStackTrace();
		}

		town.setRecentlyRuinedEndTime(888);
		town.setPublic(false);
		town.setOpen(false);

		//Remove owners from all town blocks
		for(TownBlock townBlock: town.getTownBlocks()) {
			townBlock.setResident(null);
			townyUniverse.getDataSource().saveTownBlock(townBlock);
		}

		//Set town level perms
		for (String element : new String[] { "residentBuild",
			"residentDestroy", "residentSwitch",
			"residentItemUse", "outsiderBuild",
			"outsiderDestroy", "outsiderSwitch",
			"outsiderItemUse", "allyBuild", "allyDestroy",
			"allySwitch", "allyItemUse", "nationBuild", "nationDestroy",
			"nationSwitch", "nationItemUse",
			"pvp", "fire", "explosion", "mobs"})
		{
			town.getPermissions().set(element, true);
		}

		//Propogate perm changes to individual plots
		try {
			TownCommand.townSet(null, new String[]{"perm", "reset"}, true, town);
		} catch (TownyException e) {
			e.printStackTrace();
		}

		townyUniverse.getDataSource().saveTown(town);
		plugin.resetCache();
	}
}
