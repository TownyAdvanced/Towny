package com.palmergames.bukkit.towny.event;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import org.bukkit.event.block.BlockPhysicsEvent;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.PlayerCache;
import com.palmergames.bukkit.towny.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.BlockLocation;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.NeedsPlaceholder;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.townywar.TownyWar;
import com.palmergames.bukkit.townywar.TownyWarConfig;


public class TownyBlockListener extends BlockListener {
	private final Towny plugin;

	public TownyBlockListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onBlockPhysics(BlockPhysicsEvent event) {

		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		//long start = System.currentTimeMillis();
		
		Block block = event.getBlock();
		BlockLocation blockLocation = new BlockLocation(block.getLocation());
		
		// if this is a placeholder remove it, as it's no longer needed.
		if(plugin.getTownyUniverse().isPlaceholder(block)) {
			plugin.getTownyUniverse().removePlaceholder(block);
	        block.setTypeId(0, false);
		}
		
		if (plugin.getTownyUniverse().hasProtectionRegenTask(blockLocation)) {
			//Cancel any physics events as we will be replacing this block
			event.setCancelled(true);
		} else {
			// Check the block below and cancel the event if that block is going to be replaced.			
			Block blockBelow = block.getRelative(BlockFace.DOWN);
			blockLocation = new BlockLocation(blockBelow.getLocation());
			
			if (plugin.getTownyUniverse().hasProtectionRegenTask(blockLocation)
					&& (NeedsPlaceholder.contains(block.getType()))) {
				//System.out.print("Cancelling for Below on - " + block.getType().toString());
				event.setCancelled(true);
			}
		}

		//plugin.sendDebugMsg("onBlockPhysics took " + (System.currentTimeMillis() - start) + "ms ("+event.isCancelled() +")");
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		//long start = System.currentTimeMillis();

		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(TownyUniverse.getWorld(block.getWorld().getName()), Coord.parseCoord(block));
			
			//Get build permissions (updates if none exist)
			boolean bDestroy = TownyUniverse.getCachePermissions().getCachePermission(player, block.getLocation(), TownyPermission.ActionType.DESTROY);
			
			PlayerCache cache = plugin.getCache(player);
			TownBlockStatus status = cache.getStatus();
			
			if ((status == TownBlockStatus.UNCLAIMED_ZONE) && (plugin.hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId(), TownyPermission.ActionType.DESTROY)))
				return;

			if (!bDestroy) {
			    long delay = TownySettings.getRegenDelay();
			    if(delay > 0) {
			        if(!plugin.getTownyUniverse().isPlaceholder(block)) {
				    	if (!plugin.getTownyUniverse().hasProtectionRegenTask(new BlockLocation(block.getLocation()))) {
	        				ProtectionRegenTask task = new ProtectionRegenTask(plugin.getTownyUniverse(), block, true);
	        				task.setTaskId(plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, task, 20*delay));
	        				plugin.getTownyUniverse().addProtectionRegenTask(task);
				    	}
			        } else {
			            plugin.getTownyUniverse().removePlaceholder(block);
			            block.setTypeId(0, false);
			        }
			    }
	            event.setCancelled(true);
	        }
			
			if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			

		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
		}

		//plugin.sendDebugMsg("onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms ("+event.getPlayer().getName()+", "+event.isCancelled() +")");
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		//long start = System.currentTimeMillis();

		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(TownyUniverse.getWorld(block.getWorld().getName()), Coord.parseCoord(block));
			
			//Get build permissions (updates if none exist)
			boolean bBuild = TownyUniverse.getCachePermissions().getCachePermission(player, block.getLocation(), TownyPermission.ActionType.BUILD);
			
			PlayerCache cache = plugin.getCache(player);
			TownBlockStatus status = cache.getStatus();
			
			if ((status == TownBlockStatus.UNCLAIMED_ZONE) && (plugin.hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId(), TownyPermission.ActionType.BUILD)))
				return;

			if ((status == TownBlockStatus.ENEMY && TownyWarConfig.isAllowingAttacks())
					&& event.getBlock().getType() == TownyWarConfig.getFlagBaseMaterial()) {
					//&& plugin.hasPlayerMode(player, "warflag")) {
				try {
					if (TownyWar.callAttackCellEvent(plugin, player, block, worldCoord))
						return;
				} catch (TownyException e) {
					plugin.sendErrorMsg(player, e.getMessage());
				}
				
				event.setBuild(false);
				event.setCancelled(true);
				
			} else {
				if (!bBuild) {
					event.setBuild(false);
					event.setCancelled(true);
				}
			}
			
			if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
		}

		//plugin.sendDebugMsg("onBlockPlacedEvent took " + (System.currentTimeMillis() - start) + "ms ("+event.getPlayer().getName()+", "+event.isCancelled() +")");
	}
	
	// prevent blocks igniting if within a protected town area when fire spread is set to off.
	@Override
	public void onBlockBurn(BlockBurnEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		if (onBurn(event.getBlock()))
			event.setCancelled(true);
	}
			
	@Override
	public void onBlockIgnite(BlockIgniteEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		if (onBurn(event.getBlock()))
			event.setCancelled(true);
		
	}
	
	@Override
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		//fetch the piston base
		Block block = event.getBlock();
		
		if (block.getType() != Material.PISTON_STICKY_BASE)
			return;
		
		//Get the block attached to the PISTON_EXTENSION of the PISTON_STICKY_BASE
		block = block.getRelative(event.getDirection()).getRelative(event.getDirection());
		
		if ((block.getType() != Material.AIR) && (!block.isLiquid())) {
			
			//check the block to see if it's going to pass a plot boundary
			if (testBlockMove(block, event.getDirection().getOppositeFace()))
				event.setCancelled(true);			
		}		
	}
	
	@Override
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}

		List<Block> blocks = event.getBlocks();
		
		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {				
				if (testBlockMove(block, event.getDirection()))
					event.setCancelled(true);
			}
		}	
	}
	
	private boolean testBlockMove(Block block, BlockFace direction) {
		
		
		Block blockTo = block.getRelative(direction);
		Location loc = block.getLocation();
		Location locTo = blockTo.getLocation();
		Coord coord = Coord.parseCoord(loc);
		Coord coordTo = Coord.parseCoord(locTo);
		
		TownyWorld townyWorld = null;
		TownBlock CurrentTownBlock = null, destinationTownBlock = null;
		
		try {
			townyWorld = TownyUniverse.getWorld(loc.getWorld().getName());
			CurrentTownBlock = townyWorld.getTownBlock(coord);
		} catch (NotRegisteredException e) {
			//System.out.print("Failed to fetch TownBlock");
		}
		
		try {
			destinationTownBlock = townyWorld.getTownBlock(coordTo);
		} catch (NotRegisteredException e1) {
			//System.out.print("Failed to fetch TownBlockTo");
		}
		
		if (CurrentTownBlock != destinationTownBlock) {

			// Cancel if either is not null, but other is (wild to town).
			if ((CurrentTownBlock == null && destinationTownBlock != null)
			|| (CurrentTownBlock != null && destinationTownBlock == null)) {
				//event.setCancelled(true);
				return true;
			}
			
			// If both blocks are owned by the town.
			if (!CurrentTownBlock.hasResident() && !destinationTownBlock.hasResident())
				return false;
			
			try {
				if ((!CurrentTownBlock.hasResident() && destinationTownBlock.hasResident())
						|| (CurrentTownBlock.hasResident() && !destinationTownBlock.hasResident())
						|| (CurrentTownBlock.getResident() != destinationTownBlock.getResident())
		
						|| (CurrentTownBlock.getPlotPrice() != -1)
						|| (destinationTownBlock.getPlotPrice() != -1)) {
					return true;
				}
			} catch (NotRegisteredException e) {
				// Failed to fetch a resident
				return true;
			}
		}
		
		return false;
	}
	
	private boolean onBurn(Block block) {

		Location loc = block.getLocation();
		Coord coord = Coord.parseCoord(loc);

		try {
			TownyWorld townyWorld = TownyUniverse.getWorld(loc.getWorld().getName());
			TownBlock townBlock = townyWorld.getTownBlock(coord);
			if (townyWorld.isUsingTowny())
				if ((block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN && !townBlock.getTown().isFire() && !townyWorld.isForceFire())
						|| (block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN && plugin.getTownyUniverse().isWarTime() && !townBlock.getTown().hasNation())) {
				plugin.sendDebugMsg("onBlockIgnite: Canceled " + block.getTypeId() + " from igniting within "+coord.toString()+".");
				return true;
			}
		} catch (TownyException x) {
		}	
		
		return false;
	}
	
}