package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

/**
 * @author ElgarL
 *
 */
public class TownClaim extends Thread {
	
	Towny plugin;
	volatile Player player;
	protected Location outpostLocation;
	volatile Town town;
	List<WorldCoord> selection;
	boolean outpost, claim, forced;
	
    /**
     * @param plugin reference to towny
     * @param player Doing the claiming, or null
     * @param town The claiming town
     * @param selection List of WoorldCoords to claim/unclaim
     * @param claim or unclaim
     * @param forced admin forced claim/unclaim
     */
    public TownClaim(Towny plugin, Player player, Town town, List<WorldCoord> selection, boolean isOutpost, boolean claim, boolean forced) {
        super();
        this.plugin = plugin;
        this.player = player;
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
    	
    	List<TownyWorld> worlds = new ArrayList<TownyWorld>();
    	List<Town> towns = new ArrayList<Town>();
    	TownyWorld world;
    	
    	if (player != null) TownyMessaging.sendMsg(player, "Processing " + ((claim) ? "Town Claim..." : "Town unclaim..."));
    	
    	if (selection != null) {

			for (WorldCoord worldCoord : selection) {
				
				try {
					world = TownyUniverse.getDataSource().getWorld(worldCoord.getWorld().getName());
					if (!worlds.contains(world)) worlds.add(world);
				
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
					if (!towns.contains(town)) towns.add(town);
					
					
					
				} catch (NotRegisteredException e) {
					// Invalid world
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_err_not_configured"));
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}

			}
			
    	} else if (!claim){
			
			townUnclaimAll(town);
		}
		
		if (!towns.isEmpty())
			for (Town test : towns)
				TownyUniverse.getDataSource().saveTown(test);
		
		if (!worlds.isEmpty())
			for (TownyWorld test : worlds)
				TownyUniverse.getDataSource().saveWorld(test);
		
		plugin.updateCache();
					
		if (player != null) {
			if (claim) {
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_annexed_area"), (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if (town.getWorld().isUsingPlotManagementRevert())
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_wait_locked"));
			} else if (forced) {
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_admin_unclaim_area"), (selection.size() > 5) ? "Total TownBlocks: " + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				if (town.getWorld().isUsingPlotManagementRevert())
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
                TownBlock townBlock = worldCoord.getWorld().newTownBlock(worldCoord);
                townBlock.setTown(town);
                if (!town.hasHomeBlock())
                        town.setHomeBlock(townBlock);
                
                // Set the plot permissions to mirror the towns.
                townBlock.setType(townBlock.getType());
                if (isOutpost) {
                	townBlock.setOutpost(isOutpost);
                	town.addOutpostSpawn(outpostLocation);
                }
                TownyUniverse.getDataSource().saveTownBlock(townBlock);
                
                if (town.getWorld().isUsingPlotManagementRevert()) {
                	PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
            		if (plotChunk != null) {
            			TownyRegenAPI.deletePlotChunk(plotChunk); // just claimed so stop regeneration.
            			townBlock.setLocked(false);
            		} else {
            			//plotChunk = new PlotBlockData(townBlock); // Not regenerating so create a new snapshot.
            			//plotChunk.initialize();
            			
            			// Push the TownBlock location to the queue for a snapshot.
            			TownyRegenAPI.addWorldCoord(townBlock.getWorldCoord());
            			townBlock.setLocked(true);
            			
            			TownyUniverse.getDataSource().saveTownBlock(townBlock);
            		}
            		//if (!plotChunk.getBlockList().isEmpty() && !(plotChunk.getBlockList() == null))
            		//	TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
            		
            		plotChunk = null;
                }
        }
    }
    
    private void townUnclaim(Town town, WorldCoord worldCoord, boolean force) throws TownyException {
        try {
                final TownBlock townBlock = worldCoord.getTownBlock();
                if (town != townBlock.getTown() && !force)
                        throw new TownyException(TownySettings.getLangString("msg_area_not_own"));
                
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

        			@Override
        			public void run() {
        				TownyUniverse.getDataSource().removeTownBlock(townBlock);
        			}}, 1);
                
        } catch (NotRegisteredException e) {
                throw new TownyException(TownySettings.getLangString("msg_not_claimed_1"));
        }
    }
    
    private void townUnclaimAll(final Town town) {
    	Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

			@Override
			public void run() {
				TownyUniverse.getDataSource().removeTownBlocks(town);
		        TownyMessaging.sendTownMessage(town, TownySettings.getLangString("msg_abandoned_area_1"));
			}}, 1);
        
    }
}
