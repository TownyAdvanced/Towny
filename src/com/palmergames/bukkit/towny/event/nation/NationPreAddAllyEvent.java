package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;

public class NationPreAddAllyEvent extends CancellableTownyEvent {

	private final String allyName;
	private final Nation ally;
	private final String nationName;
	private final Nation nation;

	public NationPreAddAllyEvent(Nation nation, Nation ally) {
		this.allyName = ally.getName();
		this.ally = ally;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getAllyName() {
		return allyName;
	}

	public String getNationName() {
		return nationName;
	}

	public Nation getAlly() {
		return ally;
	}

	public Nation getNation() {
		return nation;
	}
}
