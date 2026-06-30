package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * An event fired when the /town reslist command is used, or when the [Residents] button is generated for the /town status screen.
 * The purpose of the event is to allow other plugins to modify the list of residents to be displayed
 */
public class TownDisplayReslistEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private List<Resident> residents;
	
	public TownDisplayReslistEvent(Town town, List<Resident> residents) {
		this.town = town;
		this.residents = new ArrayList<>(residents);
	}

	public Town getTown() {
		return town;
	}

	public List<Resident> getResidents() {
		return residents;
	}

	public void setResidents(List<Resident> residents) {
		this.residents = residents;
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
