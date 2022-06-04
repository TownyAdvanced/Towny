package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;

public class AllianceListDisplayedNumResidentsCalculationEvent extends AllianceListDisplayedValueCalculationEvent {

	public AllianceListDisplayedNumResidentsCalculationEvent(Alliance alliance, int displayedNumResidents) {
		super(alliance, displayedNumResidents);
	}
}
