package com.palmergames.bukkit.towny.event.executors;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.actions.TownyActionEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.event.damage.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.ArraySort;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ItemLists;

/**
 * An executor class used to check the internal Towny PlayerCache and then
 * launch the ActionType-based Towny Events. Displays feedback to the player
 * when an action is cancelled.
 *
 * @author LlmDl
 */
public class TownyActionEventExecutor {

	/**
	 * First checks the Player's cache using the PlayerCacheUtil, and sets the
	 * cancellation of the Action-Type Event. Then fires the ActionType-based Event,
	 * with which Towny will decide if Event War or Flag War change anything, or any
	 * other plugin wanting to affect the outcome of an action-based decision.
	 * 
	 * Displays feedback to the player when an action is cancelled.
	 * 
	 * @param player     - Player involved in the event.
	 * @param loc   - Location of the event.
	 * @param mat   - Material being involved in the event.
	 * @param action - The ActionType of the event. ex: BUILD
	 * @param event - One of the four ActionType-based events.
	 * @return true if not cancelled by the cache or the event results.
	 */
	private static boolean isAllowedAction(Player player, Location loc, Material mat, ActionType action, TownyActionEvent event) {

		/*
		 * Use the PlayerCache to decide what Towny will do,
		 * then set the error message if there is one.
		 */
		if (!PlayerCacheUtil.getCachePermission(player, loc, mat, action)) {
			event.setCancelled(true);
			PlayerCache cache = PlayerCacheUtil.getCache(player);
			if (cache.hasBlockErrMsg())
				event.setMessage(cache.getBlockErrMsg());
		}

		/*
		 * Fire the event to let other plugins/Towny's internal war make changes.
		 */
		BukkitTools.getPluginManager().callEvent((Event) event);

		/*
		 * Send any feedback when the action is denied.
		 */
		if (event.isCancelled() && event.getMessage() != null)
			TownyMessaging.sendErrorMsg(player, event.getMessage());

		return !event.isCancelled();
	}
	
	/**
	 * Towny's primary internal test to determine if something can explode
	 * at the given location, based on Towny's plot permissions.
	 * 
	 * @param loc - Location being tested.
	 * @return true if the explosion is allowed.
	 */
	private static boolean isAllowedExplosion(Location loc) {
		boolean canExplode = false;
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(loc.getWorld().getName());
		if (world == null)
			canExplode = false;
		else {
		
			if (TownyAPI.getInstance().isWilderness(loc)) {
				/*
				 * Handle occasions in the wilderness first.
				 */
				if (world.isForceExpl() || world.isExpl())
					canExplode = true;
				else if (!world.isExpl())
					canExplode = false;			
			} else {
				/*
				 * Must be inside of a town.
				 */
				canExplode = world.isForceExpl() || TownyAPI.getInstance().getTownBlock(loc).getPermissions().explosion;			
			}
		}

		return canExplode;
	}
	
	private static List<Block> filterExplodingBlockList(List<Block> blocks) {

		List<Block> approvedBlocks = new ArrayList<Block>();
		
		for (Block block : blocks) {
			if (isAllowedExplosion(block.getLocation()))
				approvedBlocks.add(block);
		}
		return approvedBlocks;
	}
	
	private static Block getBlock(Location loc) {
		return Bukkit.getWorld(loc.getWorld().getName()).getBlockAt(loc);
	}

	/**
	 * Towny's primary internal test to determine if something can burn
	 * at the given location, based on Towny's plot permissions.
	 * 
	 * @param block - Block being tested.
	 * @return true if the burn is allowed.
	 */
	private static boolean isAllowedBurn(Block block) {
		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(block.getWorld().getName());
			
		/*
		 *  Something being ignited in the wilderness.
		 */
		if (TownyAPI.getInstance().isWilderness(block)) {
			if (isNotPortal(block) && isNotCandle(block) && (!townyWorld.isForceFire() && !townyWorld.isFire()))
				// Disallow because it is not above obsidian and neither Fire option is true.
				return false;

		/*
		 *  Something being ignited in a town.
		 */
		} else {
			if ((isNotPortal(block) && isNotCandle(block) && isNotFireSpreadBypassMat(block))          // Allows for NetherPortal/Netherrack/Soul_Sand/Soul_Soil ignition.
			&& (!townyWorld.isForceFire() && !TownyAPI.getInstance().getTownBlock(block.getLocation()).getPermissions().fire)) // Normal fire rules. 
				// Disallow because it is not above obsidian or on a FireSpreadBypassMat, and neither Fire option is true.
				return false;
		}

		// Fire is allowed here.
		return true;
	}
	
	private static boolean isNotPortal(Block block) {
		return block.getRelative(BlockFace.DOWN).getType() != Material.OBSIDIAN;
	}
	
	private static boolean isNotCandle(Block block) {
		return !ItemLists.CANDLES.contains(block.getType().name());
	}
	
	private static boolean isNotFireSpreadBypassMat(Block block) {
		switch (block.getType()) {
			case CAMPFIRE:
				break;
			default:
				block = block.getRelative(BlockFace.DOWN);
		}
		return !TownySettings.isFireSpreadBypassMaterial(block.getType().name());
	}
	
	/**
	 * Can the player build this material at this location?
	 * 
	 * @param player     - Player involved in the event.
	 * @param loc   - Location of the event.
	 * @param mat   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canBuild(Player player, Location loc, Material mat) {
		TownyBuildEvent event = new TownyBuildEvent(player, loc, mat, getBlock(loc), TownyAPI.getInstance().getTownBlock(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.BUILD, event);
	}

	/**
	 * Can the player destroy this material at this location?
	 * 
	 * @param player     - Player involved in the event.
	 * @param loc   - Location of the event.
	 * @param mat   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canDestroy(Player player, Location loc, Material mat) {
		TownyDestroyEvent event = new TownyDestroyEvent(player, loc, mat, getBlock(loc), TownyAPI.getInstance().getTownBlock(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.DESTROY, event);
	}

	/**
	 * Can the player use switches of this material at this location?
	 * 
	 * @param player     - Player involved in the event.
	 * @param loc   - Location of the event.
	 * @param mat   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canSwitch(Player player, Location loc, Material mat) {
		TownySwitchEvent event = new TownySwitchEvent(player, loc, mat, getBlock(loc), TownyAPI.getInstance().getTownBlock(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.SWITCH, event);
	}

	/**
	 * Can the player use items of this material at this location?
	 * 
	 * @param player     - Player involved in the event.
	 * @param loc   - Location of the event.
	 * @param mat   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canItemuse(Player player, Location loc, Material mat) {
		TownyItemuseEvent event = new TownyItemuseEvent(player, loc, mat, TownyAPI.getInstance().getTownBlock(loc), false);
		return isAllowedAction(player, loc, mat, ActionType.ITEM_USE, event);
	}
	
	/**
	 * Filters out blocks which should not be exploded, from a list
	 * of blocks which have been caused to explode.
	 * 
	 * First uses Towny's plot permissions to decide whether a block should be filtered out.
	 * Then, fires a TownyExplodingBlockEvent which allows Towny's internal war systems,
	 * as well as outside plugins to alter the block list that is eventually used.
	 * 
	 * @param blockList - List of Blocks which might be exploded.
	 * @param mat - Material which caused a block explosion.
	 * @param entity - Entity which caused a entity explosion.
	 * @param bukkitExplodeEvent - The Bukkit Explosion Event that caused this explosion.
	 * @return filteredBlocks - List of Blocks which are going to be allowed to explode.
	 */
	public static List<Block> filterExplodableBlocks(List<Block> blockList, Material mat, Entity entity, Event bukkitExplodeEvent) {
		/* 
		 * Sort blocks into lowest Y to highest Y in order to preserve
		 * blocks affected by gravity or tile entities requiring a base. 
		 */
		blockList.sort(ArraySort.getInstance());

		/*
		 * Filter out any blocks which are not allowed to explode based 
		 * on Towny's plot permissions settings.
		 */		
		List<Block> filteredBlocks = filterExplodingBlockList(blockList);

		/*
		 * Fire a TownyExplodingBlockEvent to let Towny's war systems 
		 * and other plugins have a say in the results.
		 */
		TownyExplodingBlocksEvent event = new TownyExplodingBlocksEvent(blockList, filteredBlocks, mat, entity, bukkitExplodeEvent);
		BukkitTools.getPluginManager().callEvent(event);

		/*
		 * Finally, return the results of the TownyExplodingBlockEvent
		 * if the event modified the original blockList. Otherwise, return
		 * the filteredBlocks block list.
		 */
		if (event.isChanged())
			filteredBlocks = event.getBlockList();

		return filteredBlocks;
	}
	
	/**
	 * Test if explosions can hurt entities here.
	 * 
	 * First uses Towny's internal plot permissions and then 
	 * fires a TownyExplosionDamagesEntityEvent to let Towny's 
	 * war systems and other plugins decide how to proceed.
	 * 
	 * @param loc - Location to check
	 * @param harmedEntity - Entity which will be damaged.
	 * @param cause - DamageCause which has caused the damage.
	 * @return true if allowed.
	 */
	public static boolean canExplosionDamageEntities(Location loc, Entity harmedEntity, DamageCause cause) {
		/*
		 *  isAllowedExplosion() will get Towny's normal response as to 
		 *  whether an explosion is allowed in the given location.
		 */		
		boolean cancelled = !isAllowedExplosion(loc);

		/*
		 * Fire a TownyExplosionDamagesEntityEvent to let Towny's war systems 
		 * and other plugins have a say in the results.
		 */
		TownyExplosionDamagesEntityEvent event = new TownyExplosionDamagesEntityEvent(loc, harmedEntity, cause, TownyAPI.getInstance().getTownBlock(loc), cancelled);
		BukkitTools.getPluginManager().callEvent(event);

		/*
		 * Finally return the results after Towny lets its own 
		 * war systems and other plugins have a say.
		 */
		return !event.isCancelled();
	}

	/**
	 * Test is fire can burn this block.
	 * 
	 * First uses Towny's internal plot permission and then
	 * fires a TownyBurnEvent to let Towny's war systems 
	 * and other plugins decide how to proceed.
	 * 
	 * @param block - Block to check.
	 * @return true is allowed to burn.
	 */
	public static boolean canBurn(Block block) {
		/*
		 * isAllowedBurn will get Towny's normal response as 
		 * to whether the given block is allowed to burn.
		 */
		boolean cancelled = !isAllowedBurn(block);
		
		/*
		 * Fire a TownyBurnEvent to let Towny's war system
		 * and other plugins have a say in the results.
		 */
		TownyBurnEvent event = new TownyBurnEvent(block, block.getLocation(), TownyAPI.getInstance().getTownBlock(block.getLocation()), cancelled);
		BukkitTools.getPluginManager().callEvent(event);
		
		/*
		 * Finally return the results after Towny lets its own 
		 * war systems and other plugins have a say.
		 */
		return !event.isCancelled();
	}
}
