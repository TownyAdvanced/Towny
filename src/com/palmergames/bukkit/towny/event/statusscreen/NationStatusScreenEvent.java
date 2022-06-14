package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.bukkit.command.CommandSender;

public class NationStatusScreenEvent extends StatusScreenEvent {

	private final Nation nation;
	
	public NationStatusScreenEvent(StatusScreen screen, CommandSender receiver, Nation nation) {
		super(screen, receiver);
		this.nation = nation;
	}
	
	public Nation getNation() {
		return nation;
	}
	
}
