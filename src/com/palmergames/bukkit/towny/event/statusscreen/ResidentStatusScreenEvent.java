package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.bukkit.command.CommandSender;

public class ResidentStatusScreenEvent extends StatusScreenEvent {

	private final Resident resident;
	
	public ResidentStatusScreenEvent(StatusScreen screen, CommandSender receiver, Resident resident) {
		super(screen, receiver);
		this.resident = resident;
	}
	
	public Resident getResident() {
		return resident;
	}
	
}
