package com.palmergames.bukkit.towny.event.resident;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
	
	public int getJailSpawn() {
		return resident.getJailSpawn();
	}
	
	public String getJailTownName() {
		return resident.getJailTown();
	}
	
	@Nullable
	public Town getJailTown() {
		return TownyUniverse.getInstance().getTown(getJailTownName());
	}
	
	public int getJailDays() {
		return resident.getJailDays();
	}
}
