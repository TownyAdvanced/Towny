package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;

public class AllianceListDisplayedNumTownsCalculationEvent extends AllianceListDisplayedValueCalculationEvent {
	
	public AllianceListDisplayedNumTownsCalculationEvent(Alliance alliance, int displayedNumTowns) {
		super(alliance, displayedNumTowns);
	}
}
