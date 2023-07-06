package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Called when Towny has finished deserializing all metadata.
 * 
 * Useful if plugins are relying on metadata in order to load plugin data 
 * or relying on custom metadata types registered by other plugins.
 * 
 * Purely an informative event.
 */
public class LoadedMetadataEvent extends Event  {
	
	private static final HandlerList handlers = new HandlerList();
	
	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
