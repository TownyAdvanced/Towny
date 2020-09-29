package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import java.util.List;

public class TownyBlockListener implements Listener {

	private final Towny plugin;

	public TownyBlockListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBreak(BlockBreakEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getBlock();

		//Get build permissions (updates cache if none exist)
		boolean bDestroy = PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.DESTROY);
		
		// Allow destroy if we are permitted
		if (bDestroy)
			return;

		/*
		 * Fetch the players cache
		 */
		PlayerCache cache = plugin.getCache(player);

		if ((cache.getStatus() == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War
				|| (TownyAPI.getInstance().isWarTime() && cache.getStatus() == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
			if (!WarZoneConfig.isEditableMaterialInWarZone(block.getType())) {
				event.setCancelled(true);
				TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_edit_material", "destroy", block.getType().toString().toLowerCase()));
			}
			return;
		}

		event.setCancelled(true);

		/* 
		 * display any error recorded for this plot
		 */
		if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
			TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Player player = event.getPlayer();
		Block block = event.getBlock();
		WorldCoord worldCoord;
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			TownyWorld world = townyUniverse.getDataSource().getWorld(block.getWorld().getName());
			worldCoord = new WorldCoord(world.getName(), Coord.parseCoord(block));

			//Get build permissions (updates if none exist)
			boolean bBuild = PlayerCacheUtil.getCachePermission(player, block.getLocation(), block.getType(), TownyPermission.ActionType.BUILD);

			// Allow build if we are permitted
			if (bBuild)
				return;
			
			/*
			 * Fetch the players cache
			 */
			PlayerCache cache = plugin.getCache(player);
			TownBlockStatus status = cache.getStatus();

			/*
			 * Flag war
			 */
			if (((status == TownBlockStatus.ENEMY) && FlagWarConfig.isAllowingAttacks()) && (event.getBlock().getType() == FlagWarConfig.getFlagBaseMaterial())) {

				try {
					if (FlagWar.callAttackCellEvent(plugin, player, block, worldCoord))
						return;
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

				event.setBuild(false);
				event.setCancelled(true);

			// Event War piggy backing on flag war's EditableMaterialInWarZone 
			} else if ((status == TownBlockStatus.WARZONE && FlagWarConfig.isAllowingAttacks()) // Flag War 
					|| (TownyAPI.getInstance().isWarTime() && cache.getStatus() == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player))) { // Event War
				if (!WarZoneConfig.isEditableMaterialInWarZone(block.getType())) {
					event.setBuild(false);
					event.setCancelled(true);
					TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_warzone_cannot_edit_material", "build", block.getType().toString().toLowerCase()));
				}
				return;
			} else {
				event.setBuild(false);
				event.setCancelled(true);
			}

			/* 
			 * display any error recorded for this plot
			 */
			if ((cache.hasBlockErrMsg()) && (event.isCancelled()))
				TownyMessaging.sendErrorMsg(player, cache.getBlockErrMsg());

		} catch (NotRegisteredException e1) {
			TownyMessaging.sendErrorMsg(player, Translation.of("msg_err_not_configured"));
			event.setCancelled(true);
		}

	}

	// prevent blocks igniting if within a protected town area when fire spread is set to off.
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockBurn(BlockBurnEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		if (isBurnCancelled(event.getBlock()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.NORMAL, ignoreCancelled = true)
	public void onBlockIgnite(BlockIgniteEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;

		if (isBurnCancelled(event.getBlock()))
			event.setCancelled(true);
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPistonRetract(BlockPistonRetractEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (testBlockMove(event.getBlock(), event.getDirection()))
			event.setCancelled(true);

		List<Block> blocks = event.getBlocks();
		
		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection()))
					event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPistonExtend(BlockPistonExtendEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		if (testBlockMove(event.getBlock(), event.getDirection()))
			event.setCancelled(true);
		
		List<Block> blocks = event.getBlocks();

		if (!blocks.isEmpty()) {
			//check each block to see if it's going to pass a plot boundary
			for (Block block : blocks) {
				if (testBlockMove(block, event.getDirection()))
					event.setCancelled(true);
			}
		}
	}

	/**
	 * Decides whether blocks moved by pistons follow the rules.
	 * 
	 * @param block - block that is being moved.
	 * @param direction - direction the piston is facing.
	 * 
	 * @return true if block is able to be moved. 
	 */
	private boolean testBlockMove(Block block, BlockFace direction) {

		Block blockTo = block.getRelative(direction);
		Location loc = block.getLocation();
		Location locTo = blockTo.getLocation();
		TownBlock currentTownBlock = null, destinationTownBlock = null;

		currentTownBlock = TownyAPI.getInstance().getTownBlock(loc);
		destinationTownBlock = TownyAPI.getInstance().getTownBlock(locTo);

		if (currentTownBlock != destinationTownBlock) {
			
			// Cancel if either is not null, but other is (wild to town).
			if (((currentTownBlock == null) && (destinationTownBlock != null)) || ((currentTownBlock != null) && (destinationTownBlock == null))) {
				return true;
			}

			// If both blocks are owned by the town.
			if (!currentTownBlock.hasResident() && !destinationTownBlock.hasResident()) {
				return false;
			}

			try {
				if ((!currentTownBlock.hasResident() && destinationTownBlock.hasResident()) || (currentTownBlock.hasResident() && !destinationTownBlock.hasResident()) || (currentTownBlock.getResident() != destinationTownBlock.getResident())

				|| (currentTownBlock.getPlotPrice() != -1) || (destinationTownBlock.getPlotPrice() != -1)) {
					return true;
				}
			} catch (NotRegisteredException e) {
				// Failed to fetch a resident
				return true;
			}
		}

		return false;
	}

	private boolean isBurnCancelled(Block block) {

		Location loc = block.getLocation();
		Coord coord = Coord.parseCoord(loc);
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(block.getWorld().getName());
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(loc);
			
		/*
		 *  Something being ignited in the wilderness.
		 */
		if (townBlock == null) {
				// Give the wilderness a pass on portal ignition.
				if ((block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) && (!townyWorld.isForceFire() && !townyWorld.isFire())) {
					TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within townblock" + coord.toString() + " (wilderness.)");
					return true;
				}
		/*
		 *  Something being ignited in a town.
		 */
		} else {
			/*
			 * Figure out if this is in a warring town for Event War.
			 */
			boolean inWarringTown = false;
			if (TownyAPI.getInstance().isWarTime()) {
				if (War.isWarringTown(TownyAPI.getInstance().getTown(loc)))
					inWarringTown = true;
			}
			/*
			 * Event War & Flag War's fire control settings.
			 */
			if (townyWorld.isWarZone(coord) || inWarringTown) {
				if (WarZoneConfig.isAllowingFireInWarZone()) {                         // Allow ignition using normal fire-during-war rule.
					return false;
				} else if (inWarringTown && TownySettings.isAllowWarBlockGriefing()) { // Allow ignition using exceptionally-griefy-war rule for Event War.
					return false;
				} else {
					TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within townblock " + coord.toString() + " (war zone.)");
					return true;
				}
			}
		
			/*
			 * Finally, sort out rules for towns which are not involved in a war.
			 */
			if ((
						(block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN) && // Allowed for portal ignition inside of Towns. 
						(!TownySettings.isFireSpreadBypassMaterial(block.getType().name()))   // Allows for Netherrack/Soul_Sand/Soul_Soil ignition.
					) && 
						(!townyWorld.isForceFire() && !townBlock.getPermissions().fire) // Normal fire rules. 
				) {
				TownyMessaging.sendDebugMsg("onBlockIgnite: Canceled " + block.getType().name() + " from igniting within townblock " + coord.toString() + " (in town.)");
				return true;
			}
		}

		return false;
	}
	
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onCreateExplosion(BlockExplodeEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}
		
		TownyWorld townyWorld;
		List<Block> blocks = event.blockList();
		int count = 0;

		try {
			townyWorld = TownyUniverse.getInstance().getDataSource().getWorld(event.getBlock().getLocation().getWorld().getName());			
			if (!townyWorld.isUsingTowny())
				return; 
		} catch (NotRegisteredException e) {
			e.printStackTrace();
			return;
		}
		System.out.println("Exploded Block location " + event.getBlock().getLocation());
		
		if (townyWorld.hasBedExplosionAtBlock(event.getBlock().getLocation())) {
			event.setCancelled(true); // Doesn't actually cancel the event.
			System.out.println("stopped exploding bed"); // This is a lie. Exploding bed isn't actually stopped.
			townyWorld.removeBedExplosionAtBlock(event.getBlock().getLocation());
			return; // We would have to return here in order to stop any sort of reverting.
		}
		for (Block block : blocks) {
			count++;
			
			if (!locationCanExplode(townyWorld, block.getLocation())) {
				event.setCancelled(true);
				return;
			}
			
			if (TownyAPI.getInstance().isWilderness(block.getLocation()) && townyWorld.isUsingPlotManagementWildRevert()) {
				event.setCancelled(!TownyRegenAPI.beginProtectionRegenTask(block, count));
			}
		}
		
	}
	
	/**
	 * Test if this location has explosions enabled.
	 * 
	 * @param world - Towny-enabled World to check in
	 * @param target - Location to check
	 * @return true if allowed.
	 */
	public boolean locationCanExplode(TownyWorld world, Location target) {

		Coord coord = Coord.parseCoord(target);

		if (world.isWarZone(coord) && !WarZoneConfig.isAllowingExplosionsInWarZone()) {
			return false;
		}
		TownBlock townBlock = null;
		boolean isNeutral = false;
		townBlock = TownyAPI.getInstance().getTownBlock(target);
		if (townBlock != null && townBlock.hasTown())
			if (!War.isWarZone(townBlock.getWorldCoord()))
				isNeutral = true;

		if (TownyAPI.getInstance().isWilderness(target.getBlock().getLocation())) {
			isNeutral = !world.isExpl();
			if (!world.isExpl() && !TownyAPI.getInstance().isWarTime())
				return false;				
			if (world.isExpl() && !TownyAPI.getInstance().isWarTime())
				return true;	
		}
		
		try {			
			if (world.isUsingTowny() && !world.isForceExpl()) {
				if (TownyAPI.getInstance().isWarTime() && WarZoneConfig.explosionsBreakBlocksInWarZone() && !isNeutral){
					return true;				
				}
				if ((!townBlock.getPermissions().explosion) || (TownyAPI.getInstance().isWarTime() && WarZoneConfig.isAllowingExplosionsInWarZone() && !townBlock.getTown().hasNation() && !townBlock.getTown().isBANG()))
					return false;
			}
		} catch (NotRegisteredException e) {
			return world.isExpl();
		}
		return true;
	}

}