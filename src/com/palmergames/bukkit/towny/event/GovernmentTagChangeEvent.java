package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Government;

public class GovernmentTagChangeEvent extends TagChangeEvent {
	private final Government government;
	
	public GovernmentTagChangeEvent(String newTag, Government government) {
		super(newTag);
		this.government = government;
	}

	public Government getTerritory() {
		return government;
	}
}
