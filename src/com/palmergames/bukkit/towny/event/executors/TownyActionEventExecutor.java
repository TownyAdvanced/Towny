package com.palmergames.bukkit.towny.event.executors;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.TownyActionEvent;
import com.palmergames.bukkit.towny.event.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.TownySwitchEvent;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;

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
	 * @param Player     - Player involved in the event.
	 * @param Location   - Location of the event.
	 * @param Material   - Material being involved in the event.
	 * @param ActionType - The ActionType of the event. ex: BUILD
	 * @param TownyActionEvent - One of the four ActionType-based events.
	 * @return true if allowed by the cache and the event.
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

		return event.isCancelled();
	}

	/**
	 * Can the player build this material at this location?
	 * 
	 * @param Player     - Player involved in the event.
	 * @param Location   - Location of the event.
	 * @param Material   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canBuild(Player player, Location loc, Material mat) {
		TownyBuildEvent event = new TownyBuildEvent(player, loc, mat, false);
		return isAllowedAction(player, loc, mat, ActionType.BUILD, event);
	}

	/**
	 * Can the player destroy this material at this location?
	 * 
	 * @param Player     - Player involved in the event.
	 * @param Location   - Location of the event.
	 * @param Material   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canDestroy(Player player, Location loc, Material mat) {
		TownyDestroyEvent event = new TownyDestroyEvent(player, loc, mat, false);
		return isAllowedAction(player, loc, mat, ActionType.DESTROY, event);
	}

	/**
	 * Can the player use switches of this material at this location?
	 * 
	 * @param Player     - Player involved in the event.
	 * @param Location   - Location of the event.
	 * @param Material   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canSwitch(Player player, Location loc, Material mat) {
		TownySwitchEvent event = new TownySwitchEvent(player, loc, mat, false);
		return isAllowedAction(player, loc, mat, ActionType.SWITCH, event);
	}

	/**
	 * Can the player use items of this material at this location?
	 * 
	 * @param Player     - Player involved in the event.
	 * @param Location   - Location of the event.
	 * @param Material   - Material being involved in the event.
	 * @return true if allowed.
	 */
	public static boolean canItemuse(Player player, Location loc, Material mat) {
		TownyItemuseEvent event = new TownyItemuseEvent(player, loc, mat, false);
		return isAllowedAction(player, loc, mat, ActionType.ITEM_USE, event);
	}
}
