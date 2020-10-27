package com.palmergames.bukkit.towny.event.internal;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.TownyItemuseEvent;
import com.palmergames.bukkit.towny.object.PlayerCache;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * An internal event used to launch the TownyItemuseEvent.
 * Displays feedback to the player when an action is cancelled.
 *
 * @author LlmDl
 */
public class TownyInternalItemusePermissionEvent {
	
	private boolean cancelled;

	/**
	 * First checks the Player's cache using the PlayerCacheUtil, and 
	 * sets the cancellation of the Internal Event. Then fires a 
	 * TownyItemuseEvent, with which Towny will decide if Event War or
	 * Flag War change anything, or any other plugin wanting to affect
	 * the outcome of a Itemuse decision.
	 * 
	 * Displays feedback to the player when an action is cancelled.
	 * 
	 * @param Player - Player involved in the event.
	 * @param Location - Location of the event.
	 * @param Material - Material being involved in the event.
	 */
	public TownyInternalItemusePermissionEvent(Player player, Location loc, Material mat) {
		cancelled = !PlayerCacheUtil.getCachePermission(player, loc, mat, ActionType.ITEM_USE);
		TownyMessaging.sendDebugMsg("TownyInternalItemusePermissionEvent - PRE - " + player.getName() + " - loc:" + loc + " - mat:" + mat.name() + " - cancelled:" + cancelled);

		TownyItemuseEvent event = new TownyItemuseEvent(player, loc, mat, cancelled);
		if (cancelled) {
			PlayerCache cache = PlayerCacheUtil.getCache(player);
			if (cache.hasBlockErrMsg())
				event.setMessage(cache.getBlockErrMsg());
		}
		BukkitTools.getPluginManager().callEvent(event);
		cancelled = event.isCancelled();
		if (cancelled && event.getMessage() != null)
			TownyMessaging.sendErrorMsg(player, event.getMessage());

		TownyMessaging.sendDebugMsg("TownyInternalItemusePermissionEvent - POST - " + player.getName() + " - loc:" + loc + " - mat:" + mat.name() + " - cancelled:" + cancelled);
	}
	
	public boolean isCancelled() {
		return cancelled;
	}
}
