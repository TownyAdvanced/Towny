package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ElgarL
 * 
 */
public class TownClaim implements Runnable {

	Towny plugin;
	private final Player player;
	private Location outpostLocation;
	private volatile Town town;
	private final List<WorldCoord> selection;
	private boolean outpost;
	private final boolean claim;
	private final boolean forced;
	private double runningRefund;

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

		super();
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

	@Override
	public void run() {

		TownyWorld world = null;
		boolean successfulRun = false;
		if (player != null)
			TownyMessaging.sendMsg(player, claim ? Translatable.of("msg_process_town_claim") : Translatable.of("msg_process_town_unclaim"));

		// Selection is never null unless a resident has done /t unclaim all.
		if (selection != null) {
			List<WorldCoord> disallowedWorldCoords = new ArrayList<>();

			for (WorldCoord worldCoord : selection) {
				world = worldCoord.getTownyWorld();
				try {
					if (claim)
						townClaim(worldCoord);
					else
						townUnclaim(worldCoord);

					// If we have had at least one successful claim/unclaim, mark this as successful.
					successfulRun = true;
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
					disallowedWorldCoords.add(worldCoord);
				}
			}
			// Based on the selection filtering that runs before we start TownClaim, the selection size should never drop below 0.
			for (WorldCoord remove : disallowedWorldCoords)
				selection.remove(remove);

			// Handle refund-for-unclaiming rules.
			if (!claim && selection.size() > 0 && TownyEconomyHandler.isActive() && runningRefund != 0.0)
				refundForUnclaim(runningRefund, selection.size());

		// Selection was null, someone has used /t unclaim all.
		} else if (!claim) {
			if (!runUnclaimAll())
				return;

			successfulRun = true;
		}

		if (successfulRun) {
			town.save();
			plugin.resetCache();
			sendFeedback(world);
		}
	}

	private void sendFeedback(TownyWorld world) {
		if (player != null && selection.size() > 0) {
			String feedbackSlug = selection.size() > 5 ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]));
			if (claim) {
				// Something has been claimed.
				TownyMessaging.sendMsg(player, Translatable.of("msg_annexed_area", feedbackSlug));
				if (world != null && world.isUsingPlotManagementRevert())
					// Send plot is locked message.
					TownyMessaging.sendMsg(player, Translatable.of("msg_wait_locked"));
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

		if (TownyUniverse.getInstance().hasTownBlock(worldCoord))
			throw new AlreadyRegisteredException(Translatable.of("msg_already_claimed", worldCoord.getTownOrNull().getName()).forLocale(player));

		TownBlock townBlock = new TownBlock(worldCoord);
		townBlock.setTown(town);
		townBlock.setType(townBlock.getType()); // Sets the plot permissions to mirror the towns.

		if (outpost) {
			townBlock.setOutpost(true);
			town.addOutpostSpawn(outpostLocation);
			outpost = false; // Reset so we only flag the first plot as an outpost.
		}

		// Claiming land can influence the Revert on Unclaim feature.
		handleRevertOnUnclaimPossiblities(worldCoord, townBlock);

		// Save our new TownBlock in the DB.
		townBlock.save();

		// Raise an event for the claim
		BukkitTools.fireEvent(new TownClaimEvent(townBlock, player));
	}

	private void handleRevertOnUnclaimPossiblities(WorldCoord worldCoord, TownBlock townBlock) {
		if (!worldCoord.getTownyWorld().isUsingPlotManagementRevert() || TownySettings.getPlotManagementSpeed() == 0)
			return;

		// Check if the townblock is actively being regenerated.
		if (TownyRegenAPI.getRegenQueueList().contains(worldCoord)) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null) {
				TownyRegenAPI.removeFromActiveRegeneration(plotChunk); // just claimed so stop regeneration.
				townBlock.setLocked(false);
			}
			TownyRegenAPI.removeFromRegenQueueList(worldCoord);
		}
		// Check if a plot snapshot exists for this townblock already (inactive, unqueued regeneration.)
		if (!TownyUniverse.getInstance().getDataSource().hasPlotData(townBlock)) {
			// Queue to have a snapshot made if there is not already an earlier snapshot.
			TownyRegenAPI.addWorldCoord(worldCoord);
			townBlock.setLocked(true);
		}
	}

	private void townUnclaim(final WorldCoord worldCoord) throws TownyException {
		if (worldCoord.isWilderness())
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		if (!forced && town != null && !worldCoord.hasTown(town))
			throw new TownyException(Translatable.of("msg_area_not_own"));

		// Handle refund-for-unclaiming rules.
		double unclaimRefund = TownySettings.getClaimRefundPrice();
		if (TownyEconomyHandler.isActive() && unclaimRefund != 0.0) {
			// The runningRefund is used because this will consolidate the refund into one
			// transaction when there are multiple plots being unclaimed, easing strain on
			// the economy plugin and making the bankhistory book cleaner.
			runningRefund = runningRefund + unclaimRefund;
			// If the unclaim refund is negative (costing the town money,) make sure that
			// the Town can pay for the new runningCost total amount.
			if (unclaimRefund < 0 && !town.getAccount().canPayFromHoldings(Math.abs(runningRefund))) {
				runningRefund = runningRefund - unclaimRefund;
				throw new TownyException(Translatable.of("msg_err_your_town_cannot_afford_unclaim", TownyEconomyHandler.getFormattedBalance(Math.abs(unclaimRefund))));
			}
		}

		unclaimTownBlock(worldCoord.getTownBlockOrNull()); // Unclaim event comes later in removeTownBlock().
	}

	private boolean runUnclaimAll() {
		if (town == null) { // This should never occur.
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nothing_to_unclaim"));
			return false;
		}

		// Send confirmation message,
		Confirmation.runOnAccept(() -> {
			if (TownyEconomyHandler.isActive()) {
				int finalTownSize = town.getTownBlocks().size() - 1;
				double finalRefund = TownySettings.getClaimRefundPrice() * finalTownSize;

				if (finalRefund < 0.0 && !town.getAccount().canPayFromHoldings(Math.abs(finalRefund))) { // Town Cannot afford the negative refund (cost) to unclaim all.
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", TownyEconomyHandler.getFormattedBalance(finalRefund)));
					return;
				}
				if (finalRefund != 0.0) // There is a refund of some type occuring.
					refundForUnclaim(finalRefund, finalTownSize);
			}
			townUnclaimAll(town);
		}).sendTo(player);

		return true;
	}

	private void townUnclaimAll(final Town town) {
		new ArrayList<>(town.getTownBlocks()).stream()
			.filter(tb -> town.hasHomeBlock() && !tb.equals(town.getHomeBlockOrNull())) // Prevent removing the homeblock
			.forEach(tb -> unclaimTownBlock(tb)); // Unclaim event comes later in removeTownBlock().
	}

	/**
	 * Unclaims a single TownBlock in a task delayed by a tick.
	 * @param townBlock TownBlock to remove from the database.
	 */
	private void unclaimTownBlock(TownBlock townBlock) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin,
				() -> TownyUniverse.getInstance().getDataSource().removeTownBlock(townBlock), 1);
	}

	private void refundForUnclaim(double unclaimRefund, int numUnclaimed) {
		if (unclaimRefund > 0) {
			town.getAccount().deposit(unclaimRefund, "Town Unclaim Refund");
			TownyMessaging.sendMsg(player, Translatable.of("refund_message", TownyEconomyHandler.getFormattedBalance(unclaimRefund), numUnclaimed));
		}
		if (unclaimRefund < 0) {
			town.getAccount().withdraw(unclaimRefund, "Town Unclaim Cost");
			TownyMessaging.sendMsg(player, Translatable.of("msg_your_town_paid_x_to_unclaim", TownyEconomyHandler.getFormattedBalance(unclaimRefund)));
		}
	}
}
