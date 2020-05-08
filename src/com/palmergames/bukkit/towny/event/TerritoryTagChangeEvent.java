package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Territory;

public class TerritoryTagChangeEvent extends TagChangeEvent {
	private final Territory territory;
	
	public TerritoryTagChangeEvent(String newTag, Territory territory) {
		super(newTag);
		this.territory = territory;
	}

	public Territory getTerritory() {
		return territory;
	}
}
