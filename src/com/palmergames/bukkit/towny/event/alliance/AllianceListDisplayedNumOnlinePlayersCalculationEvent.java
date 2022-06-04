package com.palmergames.bukkit.towny.event.alliance;

import com.palmergames.bukkit.towny.object.Alliance;

public class AllianceListDisplayedNumOnlinePlayersCalculationEvent extends AllianceListDisplayedValueCalculationEvent {

	public AllianceListDisplayedNumOnlinePlayersCalculationEvent(Alliance alliance, int displayedNumOnlinePlayers) {
		super(alliance, displayedNumOnlinePlayers);
	}
}
