package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public class NationRankAddEvent extends CancellableTownyEvent {

	private final Nation nation;
	private final Resident res;
	private final String rank;

	public NationRankAddEvent(Nation nation, String rank, Resident res) {
		this.nation = nation;
		this.rank = rank;
		this.res = res;
	}

	public Nation getNation() {
		return nation;
	}

	public Resident getResident() {
		return res;
	}

	public String getRank() {
		return rank;
	}
}
