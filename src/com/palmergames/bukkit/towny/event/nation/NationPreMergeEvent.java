package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;

public class NationPreMergeEvent extends CancellableTownyEvent {

	private final Nation nation;
	private final Nation remainingNation;

	public NationPreMergeEvent(Nation nation, Nation remainingNation) {
		this.nation = nation;
		this.remainingNation = remainingNation;
	}

	public Nation getRemainingNation() {
		return remainingNation;
	}

	public Nation getNation() {
		return nation;
	}
}
