package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.List;

public class NationStatusScreenEvent extends StatusScreenEvent {

	private Nation nation;
	
	public NationStatusScreenEvent(Nation nation, List<String> originalLines) {
		super(originalLines);
		this.nation = nation;
	}
	
	public Nation getNation() {
		return nation;
	}
	
}
