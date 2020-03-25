package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.ConfirmationType;
import com.palmergames.bukkit.towny.event.TownClaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
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
public class TownClaim extends Thread {

	Towny plugin;
	private volatile Player player;
	private Location outpostLocation;
	private volatile Town town;
	private List<WorldCoord> selection;
	private boolean outpost, claim, forced;

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

		List<TownyWorld> worlds = new ArrayList<>();
		List<Town> towns = new ArrayList<>();
		TownyWorld world = null;
		if (player != null)
			TownyMessaging.sendMsg(player, "Processing " + ((claim) ? "Town Claim..." : "Town unclaim..."));

		if (selection != null) {

			for (WorldCoord worldCoord : selection) {

				try {
					world = worldCoord.getTownyWorld();
					if (!worlds.contains(world))
						worlds.add(world);

					if (claim) {
						// Claim						
						townClaim(town, worldCoord, outpost);
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
					

				} catch (NotRegisteredException e) {
					// Invalid world
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_err_not_configured"));
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}

			}
		
			if (!claim && TownySettings.getClaimRefundPrice() > 0.0) {
				try {
					town.getAccount().collect(TownySettings.getClaimRefundPrice()*selection.size(), "Town Unclaim Refund");
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("refund_message"), TownySettings.getClaimRefundPrice()*selection.size(), selection.size()));
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}

		} else if (!claim) {

			if (town == null) {
				TownyMessaging.sendMsg(player, "Nothing to unclaim!");
				return;
			}

			Resident resident = null;
			try {
				resident = townyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException e) {
				// Yeah the resident has to exist!
			}
			if (resident == null) {
				return;
			}
			int townSize = town.getTownBlocks().size();
			// Send confirmation message,
			try {
				ConfirmationHandler.addConfirmation(resident, ConfirmationType.UNCLAIM_ALL, null);
				TownyMessaging.sendConfirmationMessage(player, null, null, null, null);
			} catch (TownyException e) {
				e.printStackTrace();
				// Also shouldn't be possible if resident is parsed correctly, since this can only be run form /town unclaim all a.s.o
			}
			if (TownySettings.getClaimRefundPrice() > 0.0) {
				try {
					town.getAccount().collect(TownySettings.getClaimRefundPrice()*townSize, "Town Unclaim Refund");
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("refund_message"), TownySettings.getClaimRefundPrice()*townSize, townSize));
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}
		}

		if (!towns.isEmpty()) {
			for (Town test : towns) {
				townyUniverse.getDataSource().saveTown(test);
			}
		}

		if (!worlds.isEmpty()) {
			for (TownyWorld test : worlds) {
				townyUniverse.getDataSource().saveWorld(test);
			}
		}

		plugin.resetCache();

		if (player != null) {
			if (claim) {
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_annexed_area"), (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if (world != null && world.isUsingPlotManagementRevert())
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_wait_locked"));
			} else if (forced) {
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_admin_unclaim_area"), (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if ((town != null) && (world != null && world.isUsingPlotManagementRevert()))
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_wait_locked"));
			}
		}
	}

	private void townClaim(Town town, WorldCoord worldCoord, boolean isOutpost) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			try {
				throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), townBlock.getTown().getName()));
			} catch (NotRegisteredException e) {
				throw new AlreadyRegisteredException(TownySettings.getLangString("msg_already_claimed_2"));
			}
		} catch (NotRegisteredException e) {
			final TownBlock townBlock = worldCoord.getTownyWorld().newTownBlock(worldCoord);
			townBlock.setTown(town);
			if (!town.hasHomeBlock())
				town.setHomeBlock(townBlock);

			// Set the plot permissions to mirror the towns.
			townBlock.setType(townBlock.getType());
			if (isOutpost) {
				townBlock.setOutpost(true); // set this to true to fineally find our problem!
				town.addOutpostSpawn(outpostLocation);
			}

			if (worldCoord.getTownyWorld().isUsingPlotManagementRevert() && (TownySettings.getPlotManagementSpeed() > 0)) {
				PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
				if (plotChunk != null) {
					TownyRegenAPI.deletePlotChunk(plotChunk); // just claimed so stop regeneration.
					townBlock.setLocked(false);
				} else {

					TownyRegenAPI.addWorldCoord(townBlock.getWorldCoord());
					townBlock.setLocked(true);
				}
			}
			
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			townyUniverse.getDataSource().saveTownBlock(townBlock);
			townyUniverse.getDataSource().saveTownBlockList();
			
			// Raise an event for the claim
			BukkitTools.getPluginManager().callEvent(new TownClaimEvent(townBlock));
				
		}
	}

	// Unclaim event comes later in removeTownBlock().
	private void townUnclaim(final Town town, final WorldCoord worldCoord, boolean force) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			final TownBlock townBlock = worldCoord.getTownBlock();
			if (town != townBlock.getTown() && !force) {
				throw new TownyException(TownySettings.getLangString("msg_area_not_own"));
			}
			if (!townBlock.isOutpost() && townBlock.hasTown()) {
				if (townyUniverse.isTownBlockLocContainedInTownOutposts(townBlock.getTown().getAllOutpostSpawns(), townBlock)) {
					townBlock.setOutpost(true);
				}
			}

			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

				
				townyUniverse.getDataSource().removeTownBlock(townBlock);
				
			}, 1);
			

		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_not_claimed_1"));
		}
	}

	// Unclaim event comes later in removeTownBlock().
	public static void townUnclaimAll(Towny plugin, final Town town) {

		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {

			TownyUniverse.getInstance().getDataSource().removeTownBlocks(town);
			TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getLangString("msg_abandoned_area_1"));

		}, 1);

	}
}
