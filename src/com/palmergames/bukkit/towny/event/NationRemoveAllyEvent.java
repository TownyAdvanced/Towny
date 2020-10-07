package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationRemoveAllyEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Nation nation;
	private final Nation removedNation;

	public NationRemoveAllyEvent(Nation nation, Nation removedNation) {
		this.nation = nation;
		this.removedNation = nation;

	}

	public Nation getRemovedNation() {
		return removedNation;
	}

	public Nation getNation() {
		return nation;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	
}
