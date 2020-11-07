package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.ExplosionUtil;
import com.palmergames.bukkit.towny.war.common.WarZoneConfig;
import com.palmergames.bukkit.towny.war.eventwar.War;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
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

import java.util.ArrayList;
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

		Block block = event.getBlock();		
		if (!TownyAPI.getInstance().isTownyWorld(block.getWorld()))
			return;

		//Cancel based on whether this is allowed using the PlayerCache and then a cancellable event.
		event.setCancelled(!TownyActionEventExecutor.canDestroy(event.getPlayer(), block.getLocation(), block.getType()));
	}

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onBlockPlace(BlockPlaceEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		Block block = event.getBlock();
		if (!TownyAPI.getInstance().isTownyWorld(block.getWorld()))
			return;

		/*
		 * Allow portals to be made.
		 */
		if (block.getType() == Material.FIRE && block.getRelative(BlockFace.DOWN).getType() == Material.OBSIDIAN)
			return;

		//Cancel based on whether this is allowed using the PlayerCache and then a cancellable event.
		if (!TownyActionEventExecutor.canBuild(event.getPlayer(), block.getLocation(), block.getType())) {
			event.setBuild(true);
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

		if (!TownyAPI.getInstance().isTownyWorld(event.getBlock().getWorld()))
			return;
		
		TownyWorld townyWorld;
		List<Block> blocks = event.blockList();
		int count = 0;

		try {
			townyWorld = TownyUniverse.getInstance().getDataSource().getWorld(event.getBlock().getLocation().getWorld().getName());			
		} catch (NotRegisteredException e) {
			e.printStackTrace();
			return;
		}

		Material material = event.getBlock().getType();
		boolean revertingThisMaterial = false;
		
		/*
		 * event.getBlock() doesn't return the bed when the bed is the cause of the explosion, so we use this workaround.
		 */
		if (townyWorld.hasBedExplosionAtBlock(event.getBlock().getLocation()))
			material = townyWorld.getBedExplosionMaterial(event.getBlock().getLocation());
		
		/*
		 * Don't regenerate block explosions unless they are on the list of blocks whose explosions regenerate.
		 */
		if (townyWorld.isUsingPlotManagementWildBlockRevert() && townyWorld.isProtectingExplosionBlock(material))
			revertingThisMaterial = true;
		
		// Blocks that will be allowed to explode.
		List<Block> toKeep = new ArrayList<Block>();
		
		for (Block block : blocks) {
			count++;
			
			if (!ExplosionUtil.locationCanExplode(block.getLocation())) {
				continue;
			} else {
				toKeep.add(block);
			}
			
			if (TownyAPI.getInstance().isWilderness(block.getLocation()) && revertingThisMaterial) {
				event.setCancelled(!TownyRegenAPI.beginProtectionRegenTask(block, count, townyWorld));
			}
		}
		
		event.blockList().clear();
		event.blockList().addAll(toKeep);
	}
}