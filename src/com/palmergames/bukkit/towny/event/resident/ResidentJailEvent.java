package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.jail.Jail;

import org.bukkit.Location;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class ResidentJailEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Resident resident;
	
	public ResidentJailEvent(Resident resident){

		this.resident = resident;
	}
	
	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Resident getResident() {
		return resident;
	}
	
	public Jail getJail() {
		return resident.getJail();
	}
	
	public int getJailCell() {
		return resident.getJailCell();
	}
	
	public int getJailHours() {
		return resident.getJailHours();
	}
	
	public Town getJailTown() {
		return getJail().getTown();
	}

	public String getJailTownName() {
		return getJailTown().getName();
	}

	public Location getJailSpawnLocation() {
		return getJail().getJailCellLocations().get(getJailCell());
	}
}
