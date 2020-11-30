package com.palmergames.bukkit.towny.war.common.townruin;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.TownyAdminCommand;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.event.town.TownReclaimedEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.util.TimeTools;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;


/**
 * This class contains utility functions related to ruins
 *
 * @author Goosius
 */
public class TownRuinUtil {

	/**
	 * This method returns true if the given player's town is ruined
	 * 
	 * @param player the player
	 * @return true if ruined, false if not
	 */
	public static boolean isPlayersTownRuined(Player player) {
		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());

			if(resident.hasTown() && resident.getTown().isRuined())
				return true;
			
		} catch (NotRegisteredException ignored) {}
		
		return false;
	}

	/**
	 * Put town into ruined state:
	 * 1. Remove town from nation
	 * 2. Set mayor to NPC
	 * 3. Enable all perms
	 * 4. Now, the residents cannot run /plot commands, and some /t commands
	 * 5. Town will later be deleted full, unless it is reclaimed
	 */
	public static void putTownIntoRuinedState(Town town, Towny plugin) {

		//Town already ruined.
		if (town.isRuined())
			return;

		//Remove town from nation, otherwise after we change the mayor to NPC and if the nation falls, the npc would receive nation refund.
		if (town.hasNation())
			town.removeNation();

		//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion 
		try { // TODO: Make this into a method somewhere instead of this hacky nonsense.
			TownyAdminCommand adminCommand = new TownyAdminCommand(plugin);
			adminCommand.adminSet(new String[]{"mayor", town.getName(), "npc"});
		} catch (TownyException e) {
			e.printStackTrace();
		}

		// Call the TownRuinEvent.
		TownRuinedEvent event = new TownRuinedEvent(town);
		Bukkit.getPluginManager().callEvent(event);
		
		// Set Town settings.
		town.setRuined(true);
		town.setRuinedTime(System.currentTimeMillis());
		town.setPublic(false);
		town.setOpen(false);
		town.getPermissions().setAll(true);

		//Return town blocks to the basic, unowned, type
		for(TownBlock townBlock: town.getTownBlocks()) {
			townBlock.getPermissions().setAll(true);
			townBlock.setType(0);
			townBlock.setPlotPrice(-1);
			townBlock.setResident(null);
			townBlock.removePlotObjectGroup();
			TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);
		}
		
		TownyUniverse.getInstance().getDataSource().saveTown(town);
		plugin.resetCache();
	}

	/**
	 * Processes a player request to reclaim a ruined town
	 *
	 * @param player the player
	 */
	public static void processRuinedTownReclaimRequest(Player player, Towny plugin) {
		Town town;
		try {
			TownyDataSource tds = TownyUniverse.getInstance().getDataSource();
			Resident resident = tds.getResident(player.getName());

			if (!resident.hasTown())
				throw new TownyException(Translation.of("msg_err_dont_belong_town"));

			//Ensure town is ruined
			town = resident.getTown();
			if (!town.isRuined())
				throw new TownyException(Translation.of("msg_err_cannot_reclaim_town_unless_ruined"));

			//Validate if player can pay
			double townReclaimCost = TownRuinSettings.getEcoPriceReclaimTown();
			if (TownySettings.isUsingEconomy() && !resident.getAccount().canPayFromHoldings(townReclaimCost))
				throw new TownyException(Translation.of("msg_err_no_money"));

			//Validate if player can remove at this time
			if (TownRuinSettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town) > 0)
				throw new TownyException(Translation.of("msg_err_cannot_reclaim_town_yet", TownRuinSettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town)));

			//Recover Town now
			resident.getAccount().withdraw(townReclaimCost, "Cost of town reclaim.");
			town.setRuined(false);
			town.setRuinedTime(0);

			//Set player as mayor (and remove npc)
			//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion
			try {
				TownyAdminCommand adminCommand = new TownyAdminCommand(plugin);
				adminCommand.adminSet(new String[]{"mayor", town.getName(), resident.getName()});
			} catch (TownyException e) {
				e.printStackTrace();
			}

			// Set permission line to the config's default settings.
			town.getPermissions().loadDefault(town);
			for (TownBlock townBlock : town.getTownBlocks()) {
				townBlock.getPermissions().loadDefault(town);
				townBlock.setChanged(false);
				tds.saveTownBlock(townBlock);
			}
			
			tds.saveTown(town);
			plugin.resetCache();
			
			TownReclaimedEvent event = new TownReclaimedEvent(town, resident);
			Bukkit.getPluginManager().callEvent(event);

			TownyMessaging.sendGlobalMessage(Translation.of("msg_town_reclaimed", resident.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player,e.getMessage());
		} catch (EconomyException ex) {
			TownyMessaging.sendErrorMsg(player,ex.getMessage());
		}
	}

	/**
	 * This method cycles through all towns
	 * If a town is in ruins, its remaining_ruin_time_hours counter is decreased
	 * If a counter hits 0, the town is deleted
	 */
    public static void evaluateRuinedTownRemovals() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only delete ruined town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) && town.isRuined() && town.getRuinedTime() != 0) {
				if(getTimeSinceRuining(town) > TownRuinSettings.getTownRuinsMaxDurationHours()) {
					//Ruin found & recently ruined end time reached. Delete town now.
					townyUniverse.getDataSource().removeTown(town, false);
				}
			}
		}
    }
    
	public static int getTimeSinceRuining(Town town) {
		return TimeTools.getHours(System.currentTimeMillis() - town.getRuinedTime());
	}
}
