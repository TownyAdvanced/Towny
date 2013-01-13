package com.palmergames.bukkit.towny.event;

import org.bukkit.entity.Entity;
import org.bukkit.event.Cancellable;
import org.bukkit.event.HandlerList;
import org.bukkit.event.entity.EntityEvent;

public class MobRemovalEvent extends EntityEvent implements Cancellable {
	private static final HandlerList handlers = new HandlerList();

	boolean cancelled = false;
	
	public MobRemovalEvent(Entity what) {
		super(what);
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		cancelled = isCancelled;
	}

}
