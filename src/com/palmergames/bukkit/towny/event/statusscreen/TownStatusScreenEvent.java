package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Town;

public class TownStatusScreenEvent extends StatusScreenEvent {

	private Town town;
	
	public TownStatusScreenEvent(Town town) {
		this.town = town;
	}
	
	public Town getTown() {
		return town;
	}
	
}
