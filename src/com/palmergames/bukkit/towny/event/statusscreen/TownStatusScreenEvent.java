package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Town;

import java.util.List;

public class TownStatusScreenEvent extends StatusScreenEvent {

	private Town town;
	
	public TownStatusScreenEvent(Town town, List<String> originalLines) {
		super(originalLines);
		this.town = town;
	}
	
	public Town getTown() {
		return town;
	}
	
}
