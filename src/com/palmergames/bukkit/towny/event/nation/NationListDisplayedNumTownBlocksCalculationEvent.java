package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;

public class NationListDisplayedNumTownBlocksCalculationEvent extends NationListDisplayedValueCalculationEvent {

	public NationListDisplayedNumTownBlocksCalculationEvent(Nation nation, int displayedNumTowns) {
		super(nation, displayedNumTowns);
	}
}
