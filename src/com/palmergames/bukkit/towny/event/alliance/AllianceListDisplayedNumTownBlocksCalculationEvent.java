package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;

public class AllianceListDisplayedNumTownBlocksCalculationEvent extends AllianceListDisplayedValueCalculationEvent {

	public AllianceListDisplayedNumTownBlocksCalculationEvent(Alliance alliance, int displayedNumTowns) {
		super(alliance, displayedNumTowns);
	}
}
