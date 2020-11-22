package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * Switch event thrown when a player attempts to use a block that is in the
 * Towny config's switch_use_ids list. These are typically inventory blocks, or
 * toggle-able blocks like levers, doors, buttons.
 * 
 * @author LlmDl
 */
public class TownySwitchEvent extends TownyActionEvent {

	private static final HandlerList handlers = new HandlerList();

	/**
	 * Switch event thrown when a player attempts to use a block that is in the
	 * Towny config's switch_use_ids list. These are typically inventory blocks, or
	 * toggle-able blocks like levers, doors, buttons.
	 * 
	 * This will be thrown even if Towny has already decided to cancel the event,
	 * giving other plugins (and Towny's internal war system) the chance to modify
	 * the outcome.
	 * 
	 * If you do not intend to un-cancel something already cancelled by Towny, use
	 * ignorecancelled=true in order to get only events which Towny will otherwise
	 * allow.
	 * 
	 * @param player    involved in the itemuse event.
	 * @param loc       location of the block which has an item being used on it.
	 * @param mat       material of the item being used.
	 * @param townblock - TownBlock or null if in the wilderness.
	 * @param cancelled true if Towny has already determined this will be cancelled.
	 */
	public TownySwitchEvent(Player player, Location loc, Material mat, TownBlock townblock, boolean cancelled) {
		super(player, loc, mat, townblock, cancelled);
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
