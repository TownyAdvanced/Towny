package com.palmergames.bukkit.towny.utils;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.confirmations.ConfirmationTransaction;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.plot.group.PlotGroupDeletedEvent;
import com.palmergames.bukkit.towny.event.town.TownPreReclaimEvent;
import com.palmergames.bukkit.towny.event.town.TownReclaimedEvent;
import com.palmergames.bukkit.towny.event.town.TownRuinedEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.stream.Collectors;


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
	public static boolean isPlayersTownRuined(@NotNull Player player) {
		final Town town = TownyAPI.getInstance().getTown(player);
		return town != null && town.isRuined();
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
		final Nation nation = town.getNationOrNull();
		if (nation != null) {
			if (TownyEconomyHandler.isActive() && TownySettings.areRuinedTownsBanksPaidToNation()) {
				double bankBalance = town.getAccount().getHoldingBalance();
				if (bankBalance > 0)
					town.getAccount().payTo(bankBalance, nation, String.format("Ruined Town (%s) Paid Remaining Bank To Nation", town.getName()));
			}
			town.removeNation();
		}

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
				townBlock.removeResident();               // Removes any personal ownership.
			townBlock.setType(TownBlockType.RESIDENTIAL); // Sets the townblock's perm line to the Town's perm line set above.
			townBlock.setPlotPrice(-1);                   // Makes the plot not for sale.
			townBlock.removePlotObjectGroup();            // Removes plotgroup if it were present.
			townBlock.setPermissionOverrides(null);       // Removes all permission overrides from the plot.
			townBlock.setTrustedResidents(null);          // Removes all trusted residents.
			townBlock.save();
		}
		
		// Unregister the now empty plotgroups.
		if (town.getPlotGroups() != null)
			for (PlotGroup group : new ArrayList<>(town.getPlotGroups()))
				if (!BukkitTools.isEventCancelled(new PlotGroupDeletedEvent(group, null, PlotGroupDeletedEvent.Cause.TOWN_DELETED)))
					TownyUniverse.getInstance().getDataSource().removePlotGroup(group);
		
		// Check if Town has more residents than it should be allowed (if it were the capital of a nation.)
		if (TownySettings.getMaxResidentsPerTown() > 0)
			ResidentUtil.reduceResidentCountToFitTownMaxPop(town);
		
		town.setForSale(false);
		
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
			final Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			final Town town = resident != null ? resident.getTownOrNull() : null;

			if (town == null)
				throw new TownyException(Translatable.of("msg_err_dont_belong_town"));

			//Ensure town is ruined
			if (!town.isRuined())
				throw new TownyException(Translatable.of("msg_err_cannot_reclaim_town_unless_ruined"));

			//Get cost of reclaiming.
			double townReclaimCost = TownySettings.getEcoPriceReclaimTown();

			//Validate if player can remove at this time
			if (TownySettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town) > 0)
				throw new TownyException(Translatable.of("msg_err_cannot_reclaim_town_yet", TownySettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town)));

			//Ask them to confirm they want to reclaim the town.
			Confirmation.runOnAccept(() -> reclaimTown(resident, town))
			.setCost(new ConfirmationTransaction(() -> townReclaimCost, resident, "Cost of town reclaim.", Translatable.of("msg_insuf_funds")))
			.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(townReclaimCost)))
			.setCancellableEvent(new TownPreReclaimEvent(town, resident, player))
			.sendTo(player);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	public static void reclaimTown(@NotNull Resident resident, @NotNull Town town) {
		// Re-test that the town is still ruined, because Confirmations can be accepted out-of-order.
		if (!town.isRuined()) {
			if (resident.isOnline())
				TownyMessaging.sendErrorMsg(resident.getPlayer(), Translatable.of("msg_err_cannot_reclaim_town_unless_ruined"));
			return;
		}

		town.setRuined(false);
		town.setRuinedTime(0);

		// The admin unruin command would result in the NPC mayor being deleted without this check.
		if (!resident.equals(town.getMayor()))
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
		List<Town> ruinedTowns = new ArrayList<>(townyUniverse.getTowns().stream().filter(Town::isRuined).collect(Collectors.toList()));
		ListIterator<Town> townItr = ruinedTowns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only delete ruined town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (!town.exists())
				continue;

			if (hasRuinTimeExpired(town) && townyUniverse.getDataSource().removeTown(town, DeleteTownEvent.Cause.RUINED)) {
				//Ruin found & recently ruined end time reached. Delete town now.
				TownyMessaging.sendMsg(Translatable.of("msg_ruined_town_being_deleted", town.getName(), TownySettings.getTownRuinsMaxDurationHours()));
				continue;
			}

			if (TownySettings.doRuinsPlotPermissionsProgressivelyAllowAll()) {
				final Town finalTown = town;
				// We are configured to slowly open up plots' permissions while a town is ruined.
				Towny.getPlugin().getScheduler().runAsync(() -> allowPermissionsOnRuinedTownBlocks(finalTown));
			}
		}
	}

	private static boolean hasRuinTimeExpired(Town town ) {
		return town.getRuinedTime() != 0 && getTimeSinceRuining(town) > TownySettings.getTownRuinsMaxDurationHours();
	}

	public static int getTimeSinceRuining(Town town) {
		return TimeTools.getHours(System.currentTimeMillis() - town.getRuinedTime());
	}

	public static void addRuinedComponents(Town town, StatusScreen screen, Translator translator) {
		screen.addComponentOf("ruinedTime", TownyFormatter.colourKey(translator.of("msg_time_remaining_before_full_removal", TownySettings.getTownRuinsMaxDurationHours() - getTimeSinceRuining(town))));
		if (TownySettings.getTownRuinsReclaimEnabled()) {
			if (TownRuinUtil.getTimeSinceRuining(town) < TownySettings.getTownRuinsMinDurationHours())
				screen.addComponentOf("reclaim", TownyFormatter.colourKeyImportant(translator.of("msg_time_until_reclaim_available", TownySettings.getTownRuinsMinDurationHours() - getTimeSinceRuining(town))));
			else 
				screen.addComponentOf("reclaim", TownyFormatter.colourKeyImportant(translator.of("msg_reclaim_available")));
		}
	}

	private static void allowPermissionsOnRuinedTownBlocks(Town town) {
		long unprotectedBlockCount = getNumTownblocksWithNoProtection(town);
		if (unprotectedBlockCount <= 0L)
			return;

		TownyPermission openPerms = new TownyPermission();
		openPerms.setAllNonEnvironmental(true);

		// List how many plots are getting their permission lines opened up.
		List<TownBlock> alteredBlocks = town.getTownBlocks().stream()
			.sorted(Comparator.comparingLong(TownBlock::getClaimedAt).reversed()) // Order them newest-claim to oldest-claim.
			.limit(unprotectedBlockCount) // Limit to only X plots.
			.filter(tb -> tryAndAllowPermsInPlot(tb, openPerms)) // Filter down to only the plots which are newly opened up to griefing.
			.collect(Collectors.toList());

		if (alteredBlocks.size() > 0) {
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_ruined_town_plot_permissions_allowed_on_x_plots", alteredBlocks.size()));
			TownyMessaging.sendMsg(Translatable.of("console_msg_ruined_town_plot_permissions_allowed_on_x_plots", town.getName(), alteredBlocks.size(), StringMgmt.join(alteredBlocks, ", ")));
		}
	}

	private static long getNumTownblocksWithNoProtection(Town town) {
		int hoursTotal = TownySettings.getTownRuinsMaxDurationHours();
		int timeSinceRuining = getTimeSinceRuining(town);
		int hoursLeft = hoursTotal - timeSinceRuining;
		if (hoursLeft >= hoursTotal)
			return 0L;

		if (hoursLeft == 0)
			return town.getNumTownBlocks();

		int numTownBlocks = town.getNumTownBlocks();
		if (numTownBlocks == 0)
			return 0L;
		int townBlocksPerHour = numTownBlocks / hoursLeft;
		double end = numTownBlocks > hoursTotal
			? townBlocksPerHour * timeSinceRuining       // We will be opening perms on 1 or more townblocks every hour.
			: numTownBlocks * ((double) timeSinceRuining / hoursTotal); // Single townblocks will open up every X hours.
		return (long) end;
	}

	private static boolean tryAndAllowPermsInPlot(TownBlock tb, TownyPermission openPerms) {
		// Filter out any null townblocks (shouldn't happen), and then parse out
		// townblocks which are already set to allow all BDSI.
		if (tb == null || tb.getPermissions().equalsNonEnvironmental(openPerms))
			return false;

		// Allows all BDSI perms and saves.
		Towny.getPlugin().getScheduler().runAsync(() -> {
			tb.getPermissions().setAllNonEnvironmental(true);
			tb.save();
		});
		return true;
	}
}