package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.bukkit.command.CommandSender;

public class TownStatusScreenEvent extends StatusScreenEvent {

	private final Town town;
	
	public TownStatusScreenEvent(StatusScreen screen, CommandSender receiver, Town town) {
		super(screen, receiver);
		this.town = town;
	}

	public Town getTown() {
		return town;
	}
	
}
