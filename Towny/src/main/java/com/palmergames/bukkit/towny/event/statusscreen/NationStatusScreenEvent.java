package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

public class NationStatusScreenEvent extends StatusScreenEvent {

	private Nation nation;
	
	public NationStatusScreenEvent(StatusScreen screen, Nation nation) {
		super(screen);
		this.nation = nation;
	}
	
	public Nation getNation() {
		return nation;
	}
	
}
