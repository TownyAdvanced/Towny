package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;

public class AllianceListDisplayedNumNationsCalculationEvent extends AllianceListDisplayedValueCalculationEvent {
	
	public AllianceListDisplayedNumNationsCalculationEvent(Alliance alliance, int displayedNumNations) {
		super(alliance, displayedNumNations);
	}
}
