package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

public class TownStatusScreenEvent extends StatusScreenEvent {

	private Town town;
	
	public TownStatusScreenEvent(StatusScreen screen, Town town) {
		super(screen);
		this.town = town;
	}

	public Town getTown() {
		return town;
	}
	
}
