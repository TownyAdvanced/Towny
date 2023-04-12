package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

abstract class TagChangeEvent extends Event {
	String newTag;
	private static final HandlerList handlers = new HandlerList();
	
	public TagChangeEvent(String newTag) {
		this.newTag = newTag;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public String getNewTag() {
		return newTag;
	}
}
