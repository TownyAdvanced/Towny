package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;

public class NationListDisplayedNumTownsCalculationEvent extends NationListDisplayedValueCalculationEvent {
	
	public NationListDisplayedNumTownsCalculationEvent(Nation nation, int displayedNumTowns) {
		super(nation, displayedNumTowns);
	}
}
