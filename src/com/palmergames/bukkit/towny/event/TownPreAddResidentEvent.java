package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownPreAddResidentEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final String townName;
	private final Town town;
	private final Resident resident;
	
	public TownPreAddResidentEvent(Town town, Resident resident) {
		this.town = town;
		this.townName = town.getName();
		this.resident = resident;
	}

	public String getTownName() {
		return townName;
	}

	public Town getTown() {
		return town;
	}

	public Resident getResident() {
		return resident;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
