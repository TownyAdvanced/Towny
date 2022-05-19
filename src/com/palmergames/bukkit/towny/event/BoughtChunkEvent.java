package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class BoughtChunkEvent extends Event {
//
	private static final HandlerList handlers = new HandlerList();

	private Town town;
	private Double d;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public BoughtChunkEvent(Town t, Double d) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = t;
		this.d = d;
	}

	public Town getTown() {
		return town;
	}

	public Double getD() {
		return d;
	}

}
