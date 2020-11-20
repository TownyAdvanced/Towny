package com.palmergames.bukkit.towny.event.actions;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyBurnEvents are thrown when a block is either burned or ignited.
 * 
 * @param block - Block being burned.
 * @param location - Location of the block.
 * @param cancelled - Whether the event is cancelled yet.
 * @author LlmDl
 */
public class TownyBurnEvent extends Event implements Cancellable{

	private static final HandlerList handlers = new HandlerList();
	private final Block block;
	private final Location location;
	private boolean cancelled;
	
	/**
	 * Event thrown when a block will be burned or ignited.
	 * 
	 * @param block - Block being burned.
	 * @param location - Location of the block.
	 * @param cancelled - Whether the event is cancelled yet.
	 */
	public TownyBurnEvent(Block block, Location location, boolean cancelled) {
		this.block = block;
		this.location = location;
		this.cancelled = cancelled;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
		
	}
	
	public Block getBlock() {
		return block;
	}

	public Location getLocation() {
		return location;
	}
}
