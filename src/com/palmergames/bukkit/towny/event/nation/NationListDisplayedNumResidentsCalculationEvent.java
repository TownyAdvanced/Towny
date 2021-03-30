package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;

public class NationListDisplayedNumResidentsCalculationEvent extends NationListDisplayedValueCalculationEvent {

	public NationListDisplayedNumResidentsCalculationEvent(Nation nation, int displayedNumResidents) {
		super(nation, displayedNumResidents);
	}
}
