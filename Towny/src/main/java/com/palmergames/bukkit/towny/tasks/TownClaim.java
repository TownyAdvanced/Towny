package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.town.TownUnclaimEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeMgmt;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author ElgarL
 * 
 */
public class TownClaim implements Runnable {

	private static final Lock lock = new ReentrantLock(true);
	private final Towny plugin;
	private final Player player;
	private Location outpostLocation;
	private final Town town;
	private final List<WorldCoord> selection;
	private boolean outpost;
	private final boolean claim;
	private final boolean forced;
	private double runningRefund = 0.0;
	private double insufficientFunds = 0.0;
	private boolean successfulRun = false;
	private boolean isOverClaim = false;

	/**
	 * @param plugin reference to towny
	 * @param player Doing the claiming, or null
	 * @param town The claiming town
	 * @param selection List of WoorldCoords to claim/unclaim
	 * @param claim or unclaim
	 * @param forced admin forced claim/unclaim
	 * @param isOutpost if claim is/was an Outpost   
	 */
	public TownClaim(Towny plugin, Player player, Town town, List<WorldCoord> selection, boolean isOutpost, boolean claim, boolean forced) {
		this.plugin = plugin;
		this.player = player;
		if (this.player != null)
			this.outpostLocation = player.getLocation();
		this.town = town;
		this.selection = selection;
		this.outpost = isOutpost;
		this.claim = claim;
		this.forced = forced;
		this.runningRefund = 0.0;
	}

	public TownClaim(Towny plugin, Player player, Town town, List<WorldCoord> selection, boolean isOutpost, boolean claim, boolean forced, boolean isOverClaim) {
		this.plugin = plugin;
		this.player = player;
		if (this.player != null)
			this.outpostLocation = player.getLocation();
		this.town = town;
		this.selection = selection;
		this.outpost = isOutpost;
		this.claim = claim;
		this.forced = forced;
		this.runningRefund = 0.0;
		this.isOverClaim = isOverClaim;
	}

	@Override
	public void run() {
		lock.lock();

		try {
			if (player != null)
				TownyMessaging.sendMsg(player, claim ? Translatable.of("msg_process_town_claim") : Translatable.of("msg_process_town_unclaim"));

			if (selection != null) // Selection is never null unless a resident has done /t unclaim all.
				processSelection();
			else if (!claim) // Selection was null, someone has used /t unclaim all.
				runUnclaimAll();

			if (successfulRun) {
				if (town != null)
					town.save();

				plugin.resetCache();
				sendFeedback();
			}
		} finally {
			lock.unlock();
		}
	}

	private void processSelection() {
		List<WorldCoord> disallowedWorldCoords = new ArrayList<>();

		for (WorldCoord worldCoord : selection) {
			try {
				if (claim)
					townClaim(worldCoord);
				else
					townUnclaim(worldCoord);

				// If we have had at least one successful claim/unclaim, mark this as successful.
				successfulRun = true;
			} catch (TownyException x) {
				// Based on the selection filtering that runs before we start TownClaim, the selection size should never drop below 0.
				TownyMessaging.sendErrorMsg(player, x.getMessage(player));
				disallowedWorldCoords.add(worldCoord);
			}
		}

		if (!disallowedWorldCoords.isEmpty()) {
			if (insufficientFunds != 0.0) 
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", TownyEconomyHandler.getFormattedBalance(insufficientFunds)));
			for (WorldCoord remove : disallowedWorldCoords)
				selection.remove(remove);
		}

		// Handle refund-for-unclaiming rules.
		if (!claim && selection.size() > 0 && runningRefund != 0.0)
			TownyEconomyHandler.economyExecutor().execute(() -> refundForUnclaim(runningRefund, selection.size()));
	}

	private void runUnclaimAll() {
		if (town == null) { // This should never occur.
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nothing_to_unclaim"));
			return;
		}

		// Send confirmation message before unclaiming everything, processing potential refund for unclaim.
		Confirmation.runOnAccept(() -> {
			try {
				townUnclaimAll(town);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				return;
			}

			// Handle refund-for-unclaiming rules, the townUnclaimAll will throw an
			// exception if the Town is not able to afford its refund costs.
			if (TownyEconomyHandler.isActive() && runningRefund != 0.0)
				TownyEconomyHandler.economyExecutor().execute(() -> refundForUnclaim(runningRefund, town.getTownBlocks().size() - 1));

			successfulRun = true;
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_unclaimed_everything_but_your_homeblock"));
		})
		.setTitle(Translatable.of("confirmation_did_you_want_to_unclaim_all"))
		.sendTo(player);
	}

	private void sendFeedback() {
		if (player != null && selection.size() > 0) {
			String feedbackSlug = selection.size() > 5 ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]));
			if (claim) {
				// Something has been claimed.
				TownyMessaging.sendMsg(player, Translatable.of("msg_annexed_area", feedbackSlug));
			} else if (forced) {
				// An admin has force-fully unclaimed an area.
				TownyMessaging.sendMsg(player, Translatable.of("msg_admin_unclaim_area", feedbackSlug));
			} else {
				// /t unclaim was used.
				TownyMessaging.sendMsg(player, Translatable.of("msg_abandoned_area",  feedbackSlug));
			}
		} else if (town != null && selection == null) {
			// /t unclaim all was used.
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_abandoned_area_1"));
		}
	}

	private void townClaim(WorldCoord worldCoord) throws TownyException {
		boolean alreadyClaimed = worldCoord.hasTownBlock();

		if (alreadyClaimed && !worldCoord.canBeStolen())
			throw new TownyException(Translatable.of("msg_already_claimed", worldCoord.getTownOrNull().getName()));

		TownBlock townBlock = !alreadyClaimed ? new TownBlock(worldCoord) : worldCoord.getTownBlockOrNull();
	
		// If this is an occaision where a town is stealing this land, do the
		// prep to clean the old town from the townblock.
		if (alreadyClaimed) {
			if (TownySettings.getOverclaimingCommandCooldownInSeconds() > 0 &&
				CooldownTimerTask.hasCooldown(town.getUUID().toString(), "overclaimingcooldown")) {
				long req = CooldownTimerTask.getCooldownRemaining(town.getUUID().toString(), "overclaimingcooldown") * 1000;
				throw new TownyException(Translatable.of("msg_err_your_cannot_overclaim_for_another", TimeMgmt.getFormattedTimeValue(req)));
			}

			Town oldTown = worldCoord.getTownOrNull();

			//  Fire an event for other plugins.
			BukkitTools.fireEvent(new TownUnclaimEvent(oldTown, worldCoord, isOverClaim));

			if (townBlock.hasResident())
				townBlock.setResident(null, false);

			oldTown.save();
			// Many other things are going to be handled by the townBlock.setTown(town) below, including:
			// - Removing the outpost if it exists.
			// - Removing the oldTown's homeblock.
			// - Removing the town's jail if it is.
			// - Removing the oldTown's nation spawn point.
			// - Updating the oldTown's TownBlockTypeCache.
			
			if (TownySettings.getOverclaimingCommandCooldownInSeconds() > 0)
				CooldownTimerTask.addCooldownTimer(town.getUUID().toString(), "overclaimingcooldown", TownySettings.getOverclaimingCommandCooldownInSeconds());
		}

		townBlock.setTown(town);
		townBlock.setType(!alreadyClaimed ? townBlock.getType() : TownBlockType.RESIDENTIAL); // Sets the plot permissions to mirror the towns.
		if (outpost) {
			townBlock.setOutpost(true);
			town.addOutpostSpawn(outpostLocation);
			outpost = false; // Reset so we only flag the first plot as an outpost.
		}

		if (!alreadyClaimed)
			// Claiming land can influence the Revert on Unclaim feature.
			handleRevertOnUnclaimPossiblities(worldCoord, townBlock);

		// Save the TownBlock in the DB.
		townBlock.save();

		// Raise an event for the claim
		BukkitTools.fireEvent(new TownClaimEvent(townBlock, player, isOverClaim));
	}

	private void handleRevertOnUnclaimPossiblities(WorldCoord worldCoord, TownBlock townBlock) {
		if (!worldCoord.getTownyWorld().isUsingPlotManagementRevert() || TownySettings.getPlotManagementSpeed() == 0)
			return;

		// Check if the townblock is actively being regenerated.
		if (TownyRegenAPI.getRegenQueueList().contains(worldCoord)) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null) {
				TownyRegenAPI.removeFromActiveRegeneration(plotChunk); // just claimed so stop regeneration.
			}
			TownyRegenAPI.removeFromRegenQueueList(worldCoord);
		}
		
		// Check if a plot snapshot exists for this townblock already (inactive, unqueued regeneration.)
		if (!TownyUniverse.getInstance().getDataSource().hasPlotData(townBlock)) {
			// Queue to have a snapshot made if there is not already an earlier snapshot.
			plugin.getScheduler().runAsync(() -> TownyRegenAPI.handleNewSnapshot(townBlock));
		}
	}

	private void townUnclaim(final WorldCoord worldCoord) throws TownyException {
		if (worldCoord.isWilderness())
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		if (!forced && town != null && !worldCoord.hasTown(town))
			throw new TownyException(Translatable.of("msg_area_not_own"));

		// Unclaim event comes later in removeTownBlock().
		if (unclaimTownBlock(worldCoord.getTownBlockOrNull())) {

			// Handle refund-for-unclaiming rules.
			double unclaimRefund = TownySettings.getClaimRefundPrice();
			if (TownyEconomyHandler.isActive() && unclaimRefund != 0.0) {
				// The runningRefund is used because this will consolidate the refund into one
				// transaction when there are multiple plots being unclaimed, easing strain on
				// the economy plugin and making the bankhistory book cleaner.
				runningRefund = runningRefund + unclaimRefund;

				// If the unclaim refund is negative (costing the town money,) make sure that
				// the Town can pay for the new runningCost total amount. 
				// All of this was already determined in the TownCommand class but a player
				// might be trying something tricky before accepting the confirmation.
				if (unclaimRefund < 0 && town != null && !town.getAccount().canPayFromHoldings(Math.abs(runningRefund))) {
					runningRefund = runningRefund - unclaimRefund;
					insufficientFunds = insufficientFunds + Math.abs(unclaimRefund);
					throw new TownyException(""); // This empty-messaged TownyException means that the player will not see a Error message every time they cannot pay.
				}
			}
		}
	}

	private void townUnclaimAll(final Town town) throws TownyException {
		for (TownBlock tb : new ArrayList<>(town.getTownBlocks())) {
			if (town.hasHomeBlock() && tb.equals(town.getHomeBlockOrNull()))
				continue;
			try {
				townUnclaim(tb.getWorldCoord());
			} catch (TownyException e) {
				throw new TownyException(e.getMessage(player));
			}
		}
	}

	/**
	 * @param townBlock TownBlock to remove from the database.
	 */
	private boolean unclaimTownBlock(TownBlock townBlock) {
		TownPreUnclaimEvent.Cause cause = forced ? TownPreUnclaimEvent.Cause.ADMIN_COMMAND : TownPreUnclaimEvent.Cause.COMMAND;
		return TownyUniverse.getInstance().getDataSource().removeTownBlock(townBlock, cause);
	}

	private void refundForUnclaim(double unclaimRefund, int numUnclaimed) {
		if (unclaimRefund > 0 && town.getAccount().deposit(unclaimRefund, "Town Unclaim Refund"))
			TownyMessaging.sendMsg(player, Translatable.of("refund_message", TownyEconomyHandler.getFormattedBalance(unclaimRefund), numUnclaimed));

		if (unclaimRefund < 0 && town.getAccount().withdraw(unclaimRefund, "Town Unclaim Cost"))
			TownyMessaging.sendMsg(player, Translatable.of("msg_your_town_paid_x_to_unclaim", TownyEconomyHandler.getFormattedBalance(unclaimRefund)));
	}
}
