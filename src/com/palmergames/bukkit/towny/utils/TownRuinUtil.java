package com.palmergames.bukkit.towny.utils;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.confirmations.ConfirmationTransaction;
import com.palmergames.bukkit.towny.event.town.TownReclaimedEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;

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
	private TownRuinUtil() {
		// Privatize implied public constructor.
	}

	/**
	 * This method returns true if the given player's town is ruined
	 * 
	 * @param player the player
	 * @return true if ruined, false if not
	 */
	public static boolean isPlayersTownRuined(Player player) {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		return resident != null && resident.hasTown() && resident.getTownOrNull().isRuined();
	}

	/**
	 * Put town into ruined state:
	 * 1. Remove town from nation
	 * 2. Set mayor to NPC
	 * 3. Enable all perms
	 * 4. Now, the residents cannot run /plot commands, and some /t commands
	 * 5. Town will later be deleted full, unless it is reclaimed
	 * @param town The town to put into a "ruined" state.
	 */
	public static void putTownIntoRuinedState(Town town) {

		//Town already ruined.
		if (town.isRuined())
			return;

		//Remove town from nation, otherwise after we change the mayor to NPC and if the nation falls, the npc would receive nation refund.
		if (town.hasNation())
			town.removeNation();

		String oldMayorName = town.hasMayor()
				? town.getMayor().getName()
				: "none";
		
		//Set NPC mayor, otherwise mayor of ruined town cannot leave until full deletion 
		Resident resident = ResidentUtil.createAndGetNPCResident();
		try {
			resident.setTown(town);
		} catch (AlreadyRegisteredException ignored) {}
		resident.save();
		setMayor(town, resident);
		town.setHasUpkeep(false);

		// Call the TownRuinEvent.
		BukkitTools.fireEvent(new TownRuinedEvent(town, oldMayorName));
		
		// Set Town settings.
		town.setRuined(true);
		town.setRuinedTime(System.currentTimeMillis());
		town.setPublic(TownySettings.areRuinsMadePublic());
		town.setOpen(TownySettings.areRuinsMadeOpen());
		town.getPermissions().setAll(true);

		//Return town blocks to the basic, unowned, type
		for(TownBlock townBlock: town.getTownBlocks()) {
			if (townBlock.hasResident())
				townBlock.setResident(null);              // Removes any personal ownership.
			townBlock.setType(TownBlockType.RESIDENTIAL); // Sets the townblock's perm line to the Town's perm line set above.
			townBlock.setPlotPrice(-1);                   // Makes the plot not for sale.
			townBlock.removePlotObjectGroup();            // Removes plotgroup if it were present.
			townBlock.getPermissionOverrides().clear();   // Removes all permission overrides from the plot.
			townBlock.getTrustedResidents().clear();      // Removes all trusted residents.
			townBlock.save();
		}
		
		// Unregister the now empty plotgroups.
		if (town.getPlotGroups() != null)
			for (PlotGroup group : new ArrayList<>(town.getPlotGroups()))
				TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
		
		// Check if Town has more residents than it should be allowed (if it were the capital of a nation.)
		if (TownySettings.getMaxResidentsPerTown() > 0)
			ResidentUtil.reduceResidentCountToFitTownMaxPop(town);
		
		town.save();
		Towny.getPlugin().resetCache();
		
		TownyMessaging.sendGlobalMessage(Translatable.of("msg_ruin_town", town.getName()));
	}

	/**
	 * Processes a player request to reclaim a ruined town
	 *
	 * @param player The player reclaiming a ruined town.
	 */
	public static void processRuinedTownReclaimRequest(Player player) {
		try {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

			if (resident == null || !resident.hasTown())
				throw new TownyException(Translatable.of("msg_err_dont_belong_town"));

			//Ensure town is ruined
			Town town = resident.getTownOrNull();
			if (!town.isRuined())
				throw new TownyException(Translatable.of("msg_err_cannot_reclaim_town_unless_ruined"));

			//Validate if player can pay
			double townReclaimCost = TownySettings.getEcoPriceReclaimTown();
			if (TownyEconomyHandler.isActive() && !resident.getAccount().canPayFromHoldings(townReclaimCost))
				throw new TownyException(Translatable.of("msg_insuf_funds"));

			//Validate if player can remove at this time
			if (TownySettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town) > 0)
				throw new TownyException(Translatable.of("msg_err_cannot_reclaim_town_yet", TownySettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town)));

			if (TownyEconomyHandler.isActive() && townReclaimCost > 0) { 
				Confirmation.runOnAccept(() -> reclaimTown(resident, town))
					.setCost(new ConfirmationTransaction(() -> townReclaimCost, resident.getAccount(), "Cost of town reclaim.", Translatable.of("msg_insuf_funds")))
					.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(townReclaimCost)))
					.sendTo(player);
			} else {
				reclaimTown(resident, town);
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	public static void reclaimTown(Resident resident, Town town) {
		town.setRuined(false);
		town.setRuinedTime(0);

		// The admin unruin command would result in the NPC mayor being deleted without this check.
		if (!town.getMayor().equals(resident))
			setMayor(town, resident); //Set player as mayor (and remove npc)

		// Set permission line to the config's default settings.
		town.getPermissions().loadDefault(town);
		for (TownBlock townBlock : town.getTownBlocks()) {
			townBlock.getPermissions().loadDefault(town);
			townBlock.setChanged(false);
			townBlock.save();
		}
		
		town.save();
		Towny.getPlugin().resetCache();
		
		BukkitTools.fireEvent(new TownReclaimedEvent(town, resident));

		TownyMessaging.sendGlobalMessage(Translatable.of("msg_town_reclaimed", resident.getName(), town.getName()));
		
	}

	private static void setMayor(Town town, Resident newMayor) {
		Resident oldMayor = town.getMayor();
		town.setMayor(newMayor);
		if (oldMayor != null && oldMayor.isNPC()) {
			// Delete the resident if the old mayor was an NPC.
			oldMayor.removeTown();
			TownyUniverse.getInstance().getDataSource().removeResident(oldMayor);
			// set upkeep again
			town.setHasUpkeep(true);
		}
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", newMayor.getName()));
	}

	/**
	 * This method cycles through all towns
	 * If a town is in ruins, its remaining_ruin_time_hours counter is decreased
	 * If a counter hits 0, the town is deleted
	 */
    public static void evaluateRuinedTownRemovals() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only delete ruined town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.hasTown(town.getName()) && town.isRuined()
					&& town.getRuinedTime() != 0 && getTimeSinceRuining(town) > TownySettings
					.getTownRuinsMaxDurationHours()) {
				//Ruin found & recently ruined end time reached. Delete town now.
				townyUniverse.getDataSource().removeTown(town, false);
			}
		}
    }
    
	public static int getTimeSinceRuining(Town town) {
		return TimeTools.getHours(System.currentTimeMillis() - town.getRuinedTime());
	}
}
