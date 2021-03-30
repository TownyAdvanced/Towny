package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Nation;

public class NationStatusScreenEvent extends StatusScreenEvent {

	private Nation nation;
	
	public NationStatusScreenEvent(Nation nation) {
		this.nation = nation;
	}
	
	public Nation getNation() {
		return nation;
	}
	
}
