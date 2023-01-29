package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * Container event thrown when a player attempts to use a block that is in the
 * Towny config's containers_ids list. These are typically inventory blocks
 * 
 * @author Grooble
 */
public class TownyContainerEvent extends TownyActionEvent {
    private static final HandlerList handlers = new HandlerList();
	private final Block block;

	/**
	 * Container event thrown when a player attempts to use a block that is in the
	 * Towny config's containers_ids list. These are typically inventory blocks
	 * 
	 * This will be thrown even if Towny has already decided to cancel the event,
	 * giving other plugins (and Towny's internal war system) the chance to modify
	 * the outcome.
	 * 
	 * If you do not intend to un-cancel something already cancelled by Towny, use
	 * ignorecancelled=true in order to get only events which Towny will otherwise
	 * allow.
	 * 
	 * @param player    involved in the container event.
	 * @param mat       material of the block being used.
	 * @param block     block being used.
	 * @param townBlock - TownBlock or null if in the wilderness.
	 * @param cancelled true if Towny has already determined this will be cancelled.
	 */
	public TownyContainerEvent(Player player, Material mat, Block block, TownBlock townBlock, boolean cancelled) {
		super(player, block.getLocation(), mat, townBlock, cancelled);
		this.block = block;
	}

	public Block getBlock() {
		return block;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
