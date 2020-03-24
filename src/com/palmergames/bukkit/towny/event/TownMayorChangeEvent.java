package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownMayorChangeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Town town;
	private Resident mayor;
	
	public TownMayorChangeEvent(Town town, Resident newMayor) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.mayor = newMayor;
	}


	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public Resident getNewMayor() {
		return mayor;
	}
}
