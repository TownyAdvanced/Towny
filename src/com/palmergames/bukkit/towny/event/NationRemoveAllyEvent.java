package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;

public class NationRemoveAllyEvent extends CancellableTownyEvent {

	private final Nation nation;
	private final Nation removedNation;

	public NationRemoveAllyEvent(Nation nation, Nation removedNation) {
		this.nation = nation;
		this.removedNation = removedNation;
	}

	public Nation getRemovedNation() {
		return removedNation;
	}

	public Nation getNation() {
		return nation;
	}
}
