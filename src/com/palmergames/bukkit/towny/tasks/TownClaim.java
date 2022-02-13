package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
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
import java.util.Collection;
import java.util.List;

/**
 * @author ElgarL
 * 
 */
public class TownClaim extends Thread {

	Towny plugin;
	private final Player player;
	private Location outpostLocation;
	private volatile Town town;
	private final List<WorldCoord> selection;
	private boolean outpost;
	private final boolean claim;
	private final boolean forced;

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
		this.setPriority(MIN_PRIORITY);
	}

	@Override
	public void run() {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>();
		TownyWorld world = null;
		if (player != null) {
			if (claim)
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_town_claim"));
			else 
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_town_unclaim"));
		}

		if (selection != null) {

			for (WorldCoord worldCoord : selection) {

				try {
					world = worldCoord.getTownyWorld();

					if (claim) {
						// Claim						
						townClaim(town, worldCoord, outpost, player);
						// Reset so we only flag the first plot as an outpost.
						outpost = false;
					} else {
						// Unclaim
						this.town = worldCoord.getTownBlock().getTown();
						townUnclaim(town, worldCoord, forced);
					}

					// Mark this town as modified for saving.
					if (!towns.contains(town))
						towns.add(town);
					
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}

			}
		
			double unclaimRefund = TownySettings.getClaimRefundPrice();
			if (!claim && unclaimRefund != 0.0) {
				double refund = Math.abs(unclaimRefund * selection.size());
				if (unclaimRefund > 0) {
					town.getAccount().deposit(refund, "Town Unclaim Refund");
					TownyMessaging.sendMsg(player, Translatable.of("refund_message", TownyEconomyHandler.getFormattedBalance(refund), selection.size()));
				}
				if (unclaimRefund < 0) {
					town.getAccount().withdraw(refund, "Town Unclaim Cost");
					TownyMessaging.sendMsg(player, Translatable.of("msg_your_town_paid_x_to_unclaim", TownyEconomyHandler.getFormattedBalance(refund)));
				}
			}

		} else if (!claim) {

			if (town == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nothing_to_unclaim"));
				return;
			}

			Resident resident = player != null ? townyUniverse.getResident(player.getUniqueId()) : null;
			if (resident == null) {
				return;
			}
			int townSize = town.getTownBlocks().size() - 1; // size() - 1 because the homeblock will not be unclaimed.
			double refund = TownySettings.getClaimRefundPrice() * townSize;
			// Send confirmation message,
			Confirmation.runOnAccept(() -> { 
				TownClaim.townUnclaimAll(plugin, town);
				if (TownyEconomyHandler.isActive() && refund > 0.0) {
					town.getAccount().deposit(TownySettings.getClaimRefundPrice()*townSize - 1, "Town Unclaim Refund"); 
					TownyMessaging.sendMsg(player, Translatable.of("refund_message", TownySettings.getClaimRefundPrice()*townSize, townSize));
				}
			})
			.sendTo(player);
		}

		if (!towns.isEmpty()) {
			for (Town test : towns) {
				test.save();
			}
		}

		plugin.resetCache();

		if (player != null) {
			if (claim) {
				TownyMessaging.sendMsg(player, Translatable.of("msg_annexed_area", (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if (world != null && world.isUsingPlotManagementRevert())
					TownyMessaging.sendMsg(player, Translatable.of("msg_wait_locked"));
			} else if (forced) {
				TownyMessaging.sendMsg(player, Translatable.of("msg_admin_unclaim_area", (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if ((town != null) && (world != null && world.isUsingPlotManagementRevert()))
					TownyMessaging.sendMsg(player, Translatable.of("msg_wait_locked"));
			}
		}
	}

	private void townClaim(Town town, WorldCoord worldCoord, boolean isOutpost, Player player) throws TownyException {

		if (TownyUniverse.getInstance().hasTownBlock(worldCoord))
				throw new AlreadyRegisteredException(Translatable.of("msg_already_claimed", "some town").forLocale(player));
		else {
			TownBlock townBlock = new TownBlock(worldCoord.getX(), worldCoord.getZ(), worldCoord.getTownyWorld());
			townBlock.setTown(town);
			// Set the plot permissions to mirror the towns.
			townBlock.setType(townBlock.getType());
			if (isOutpost) {
				townBlock.setOutpost(true);
				town.addOutpostSpawn(outpostLocation);
			}

			if (worldCoord.getTownyWorld().isUsingPlotManagementRevert() && TownySettings.getPlotManagementSpeed() > 0) {
				// Check if the townblock is actively being regenerated.
				if (TownyRegenAPI.getRegenQueueList().contains(townBlock.getWorldCoord())) {
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
					TownyRegenAPI.addWorldCoord(townBlock.getWorldCoord());
					townBlock.setLocked(true);
				}
			}
			
			townBlock.save();
			
			// Raise an event for the claim
			BukkitTools.getPluginManager().callEvent(new TownClaimEvent(townBlock, player));
				
		}
	}

	// Unclaim event comes later in removeTownBlock().
	private void townUnclaim(final Town town, final WorldCoord worldCoord, boolean force) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			final TownBlock townBlock = worldCoord.getTownBlock();
			if (town != townBlock.getTown() && !force) {
				throw new TownyException(Translatable.of("msg_area_not_own"));
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

				
				townyUniverse.getDataSource().removeTownBlock(townBlock);
				
			}, 1);
			

		} catch (NotRegisteredException e) {
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		}
	}

	// Unclaim event comes later in removeTownBlock().
	public static void townUnclaimAll(Towny plugin, final Town town) {

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

			// Prevent removing the homeblock
			Collection<TownBlock> townBlocks = new ArrayList<>(town.getTownBlocks());
			for (TownBlock townBlock : townBlocks) {
				try {
					if (!town.hasHomeBlock() || !townBlock.equals(town.getHomeBlock())) {
						TownyUniverse.getInstance().getDataSource().removeTownBlock(townBlock);
					}
				} catch (TownyException ignore) {
				}
			}
			
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_abandoned_area_1"));

		}, 1);

	}
}
