package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.util.TimeMgmt;
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
	 * 1. Remove town from nation
	 * 2. Set mayor to NPC
	 * 3. Enable all perms
	 * 4. Now, the residents cannot run /plot commands, and some /t commands
	 * 5. Later, Town will be deleted after 2 upkeep cycles
	 */
	public static void putTownIntoRuinedState(Town town, Towny plugin) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if(town.isRuined())
			return; //Town already ruined. Do not run code as it would reset ruin status to 888 (ie phase 1)

		//Remove town from nation, otherwise after we change the mayor to NPC and if the nation falls, the npc would receive nation refund.
		try {
			if (town.hasNation()) {
				Nation nation = town.getNation();
				townyUniverse.getDataSource().removeTownFromNation(plugin, town, nation);
				if(nation.getTowns().size() == 0) {
					TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), nation));
				}
			}
		} catch (NotRegisteredException e) {}

		//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion
		try {
			TownyAdminCommand adminCommand = new TownyAdminCommand(plugin);
			adminCommand.adminSet(new String[]{"mayor", town.getName(), "npc"});
		} catch (TownyException e) {
			e.printStackTrace();
		}

		//Remove siege if any
		if(town.hasSiege())
			townyUniverse.getDataSource().removeSiege(town.getSiege());

		town.setRecentlyRuinedEndTime(System.currentTimeMillis() + (long)(TownySettings.getWarSiegeRuinsRemovalDelayHours() * TimeMgmt.ONE_HOUR_IN_MILLIS));
		town.setPublic(false);
		town.setOpen(false);

		//Return town blocks to the basic, unowned, type
		for(TownBlock townBlock: town.getTownBlocks()) {
			townBlock.setType(0);
			townBlock.setPlotPrice(-1);
			townBlock.setResident(null);
			townBlock.removePlotObjectGroup();
			townyUniverse.getDataSource().saveTownBlock(townBlock);
		}

		//Set town level perms
		try {
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
		} catch (Exception e) {
			e.printStackTrace();
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

	/**
	 * Processes a player request to reclaim a ruined town
	 *
	 * @param player the player
	 */
	public static void processRuinedTownReclaimRequest(Player player, Towny plugin) {
		try {
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			Resident resident = townyUniverse.getDataSource().getResident(player.getName());

			if(!resident.hasTown())
				throw new TownyException(TownySettings.getLangString("msg_err_dont_belong_town"));

			//Ensure town is in a standard ruined state (not active, or a zero-resident-ruin)
			Town town = resident.getTown();
			if(town.getRecentlyRuinedEndTime() < 1)
				throw new TownyException(TownySettings.getLangString("msg_err_cannot_reclaim_town_unless_ruined"));

			//Validate if player can pay
			double townReclaimCost = TownySettings.getReclaimTownPrice();
			if (TownySettings.isUsingEconomy() && !resident.getAccount().canPayFromHoldings(townReclaimCost))
				throw new TownyException(TownySettings.getLangString("msg_err_no_money"));

			//Validate if player can remove at this time
			long timeUntilDeletionMillis = town.getRecentlyRuinedEndTime() - System.currentTimeMillis();
			long maximumRuinDurationMillis = (long)(TownySettings.getWarSiegeRuinsRemovalDelayHours() * TimeMgmt.ONE_HOUR_IN_MILLIS);
			long estimatedRuinsDurationMillis = maximumRuinDurationMillis - timeUntilDeletionMillis;
			long minimumRuinsDurationMillis = (long)(TownySettings.getWarSiegeMinimumRuinsDurationHours() * TimeMgmt.ONE_HOUR_IN_MILLIS);
			if(estimatedRuinsDurationMillis < minimumRuinsDurationMillis) {
				long remainingRuinsDurationMillis = minimumRuinsDurationMillis - estimatedRuinsDurationMillis;
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_cannot_reclaim_town_yet"), TimeMgmt.getFormattedTimeValue(remainingRuinsDurationMillis)));
			}

			//Recover Town now
			resident.getAccount().pay(townReclaimCost, "Cost of town reclaim.");
			town.setRecentlyRuinedEndTime(0);
			
			//Set player as mayor (and remove npc)
			//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion
			try {
				TownyAdminCommand adminCommand = new TownyAdminCommand(plugin);
				adminCommand.adminSet(new String[]{"mayor", town.getName(), resident.getName()});
			} catch (TownyException e) {
				e.printStackTrace();
			}
		
			//Set town level perms
			try {
				for (String element : new String[] { "residentBuild",
					"residentDestroy", "residentSwitch",
					"residentItemUse", "outsiderBuild",
					"outsiderDestroy", "outsiderSwitch",
					"outsiderItemUse", "allyBuild", "allyDestroy",
					"allySwitch", "allyItemUse", "nationBuild", "nationDestroy",
					"nationSwitch", "nationItemUse",
					"pvp", "fire", "explosion", "mobs"})
				{
					town.getPermissions().set(element, false);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}

			//Propogate perm changes to individual plots
			try {
				TownCommand.townSet(null, new String[]{"perm", "reset"}, true, town);
			} catch (TownyException e) {
				e.printStackTrace();
			}

			townyUniverse.getDataSource().saveTown(town);
			plugin.resetCache();

			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_town_reclaimed"), resident.getName(), town.getName()));
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player,e.getMessage());
		}
	}
}
