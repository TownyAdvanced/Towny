package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;

public class NationListDisplayedNumOnlinePlayersCalculationEvent extends NationListDisplayedValueCalculationEvent {

	public NationListDisplayedNumOnlinePlayersCalculationEvent(Nation nation, int displayedNumOnlinePlayers) {
		super(nation, displayedNumOnlinePlayers);
	}
}
