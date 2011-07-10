package ca.xshade.bukkit.towny.event;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockListener;
import org.bukkit.event.block.BlockPlaceEvent;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.PlayerCache;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.PlayerCache.TownBlockStatus;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.object.Coord;
import ca.xshade.bukkit.towny.object.TownBlock;
import ca.xshade.bukkit.towny.object.TownyPermission;
import ca.xshade.bukkit.towny.object.TownyWorld;
import ca.xshade.bukkit.towny.object.WorldCoord;


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
	
	private boolean onBurn(Block block) {

		Location loc = block.getLocation();
		Coord coord = Coord.parseCoord(loc);
		
		try {
			TownyWorld townyWorld = plugin.getTownyUniverse().getWorld(loc.getWorld().getName());
			TownBlock townBlock = townyWorld.getTownBlock(coord);
			if (!townBlock.getTown().isFire() || plugin.getTownyUniverse().isWarTime()) {
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