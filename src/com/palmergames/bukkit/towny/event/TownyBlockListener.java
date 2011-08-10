package com.palmergames.bukkit.towny.event;

import java.util.List;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.PlayerCache;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;


public class TownyBlockListener extends BlockListener {
	private final Towny plugin;

	public TownyBlockListener(Towny instance) {
		plugin = instance;
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		//if (event.getDamageLevel() == BlockDamageLevel.STOPPED || event.getDamageLevel() == BlockDamageLevel.BROKEN || event.getDamageLevel() == BlockDamageLevel.STARTED) {
			long start = System.currentTimeMillis();

			onBlockBreakEvent(event, true);

			plugin.sendDebugMsg("onBlockBreakEvent took " + (System.currentTimeMillis() - start) + "ms ("+event.getPlayer().getName()+", "+event.isCancelled() +")");
		//}
	}
	
	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		
	}
	
	public void onBlockBreakEvent(BlockBreakEvent event, boolean firstCall) {	
		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && plugin.hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId(), TownyPermission.ActionType.DESTROY))
				return;
			if (!cache.getDestroyPermission())
				event.setCancelled(true);
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old destroy permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheDestroy(player, worldCoord, getDestroyPermission(player, status, worldCoord));
				onBlockBreakEvent(event, false);
			} else
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_updating_destroy_perms"));
		}
	}

	@Override
	public void onBlockPlace(BlockPlaceEvent event) {
		
		//System.out.println("[Towny] BlockPlaceEvent");
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}
		
		long start = System.currentTimeMillis();

		onBlockPlaceEvent(event, true, null);

		plugin.sendDebugMsg("onBlockPlacedEvent took " + (System.currentTimeMillis() - start) + "ms ("+event.getPlayer().getName()+", "+event.isCancelled() +")");
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
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {
		
		if (event.isCancelled()) {
			event.setCancelled(true);
			return;
		}

		List<Block> blocks = event.getBlocks();
		
		TownBlock CurrentTownBlock = null, destinationTownBlock = null;
		Location loc, locTo;
		Coord coord, coordTo;
		
		if (!blocks.isEmpty()) {
			//check each block to see if's going to pass a plot boundary
			for (Block block : blocks) {
				
				Block blockTo = block.getRelative(event.getDirection());
				loc = block.getLocation();
				locTo = blockTo.getLocation();
				coord = Coord.parseCoord(loc);
				coordTo = Coord.parseCoord(locTo);
				

					TownyWorld townyWorld = null;
					try {
						townyWorld = plugin.getTownyUniverse().getWorld(loc.getWorld().getName());
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

						//System.out.print("Testing TownBlocks");
						
						// Cancel if either is not null, but other is (wild to town).
						if ((CurrentTownBlock == null && destinationTownBlock != null)
						|| (CurrentTownBlock != null && destinationTownBlock == null)) {
							event.setCancelled(true);
							return;
						}
						
						try {
							if ((!CurrentTownBlock.hasResident() && destinationTownBlock.hasResident())
									|| (CurrentTownBlock.hasResident() && !destinationTownBlock.hasResident())
									|| (CurrentTownBlock.getResident() != destinationTownBlock.getResident())
									|| (CurrentTownBlock.isForSale() != -1)
									|| (destinationTownBlock.isForSale() != -1)) {
								event.setCancelled(true);
								return;
							}
						} catch (NotRegisteredException e) {
							// Failed to fetch a resident
							event.setCancelled(true);
							return;
						}
					}

			
			}
		}
		
		
	}
		

	public void onBlockPlaceEvent(BlockPlaceEvent event, boolean firstCall, String errMsg) {
		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		try {
			worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_not_configured"));
			event.setCancelled(true);
			return;
		}

		// Check cached permissions first
		try {
			PlayerCache cache = plugin.getCache(player);
			cache.updateCoord(worldCoord);
			TownBlockStatus status = cache.getStatus();
			if (status == TownBlockStatus.UNCLAIMED_ZONE && plugin.hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId(), TownyPermission.ActionType.BUILD))
				return;
			if (!cache.getBuildPermission()) { // If build cache is empty, throws null pointer
				event.setBuild(false);
				event.setCancelled(true);
			}
			if (cache.hasBlockErrMsg())
				plugin.sendErrorMsg(player, cache.getBlockErrMsg());
			return;
		} catch (NullPointerException e) {
			if (firstCall) {
				// New or old build permission was null, update it
				TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
				plugin.cacheBuild(player, worldCoord, getBuildPermission(player, status, worldCoord));
				onBlockPlaceEvent(event, false, errMsg);
			} else
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_updating_build_perms"));
		}
	}
	
	private boolean onBurn(Block block) {

		Location loc = block.getLocation();
		Coord coord = Coord.parseCoord(loc);
		
		try {
			TownyWorld townyWorld = plugin.getTownyUniverse().getWorld(loc.getWorld().getName());
			TownBlock townBlock = townyWorld.getTownBlock(coord);
			if (townyWorld.isUsingTowny())
				if ((!townBlock.getTown().isFire() && !townyWorld.isForceFire())
						|| (plugin.getTownyUniverse().isWarTime() && !townBlock.getTown().hasNation())) {
				plugin.sendDebugMsg("onBlockIgnite: Canceled " + block.getTypeId() + " from igniting within "+coord.toString()+".");
				return true;
			}
		} catch (TownyException x) {
		}	
		
		return false;
	}
	
	/*
	
	public void onBlockInteractEvent(BlockInteractEvent event, boolean firstCall, String errMsg) {
		if (event.getEntity() != null && event.getEntity() instanceof Player) {
			Player player = (Player)event.getEntity();
			Block block = event.getBlock();
			
			if (!TownySettings.isSwitchId(block.getTypeId()))
				return;

			WorldCoord worldCoord;
			try {
				worldCoord = new WorldCoord(plugin.getTownyUniverse().getWorld(block.getWorld().getName()), Coord.parseCoord(block));
			} catch (NotRegisteredException e1) {
				plugin.sendErrorMsg(player, "This world has not been configured by Towny.");
				event.setCancelled(true);
				return;
			}
	
			// Check cached permissions first
			try {
				PlayerCache cache = plugin.getCache(player);
				cache.updateCoord(worldCoord);
				TownBlockStatus status = cache.getStatus();
				if (status == TownBlockStatus.UNCLAIMED_ZONE && plugin.hasWildOverride(worldCoord.getWorld(), player, event.getBlock().getTypeId(), TownyPermission.ActionType.SWITCH))
					return;
				if (!cache.getSwitchPermission())
					event.setCancelled(true);
				if (cache.hasBlockErrMsg())
					plugin.sendErrorMsg(player, cache.getBlockErrMsg());
				return;
			} catch (NullPointerException e) {
				if (firstCall) {
					// New or old build permission was null, update it
					TownBlockStatus status = plugin.cacheStatus(player, worldCoord, plugin.getStatusCache(player, worldCoord));
					plugin.cacheSwitch(player, worldCoord, getSwitchPermission(player, status, worldCoord));
					onBlockInteractEvent(event, false, errMsg);
				} else
					plugin.sendErrorMsg(player, "Error updating switch permissions cache.");
			}
		}
	}
*/
	public boolean getBuildPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.BUILD);
	}
	
	public boolean getDestroyPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.DESTROY);
	}
	
	public boolean getSwitchPermission(Player player, TownBlockStatus status, WorldCoord pos) {
		return plugin.getPermission(player, status, pos, TownyPermission.ActionType.SWITCH);
	}
}