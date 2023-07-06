package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationTownLeaveEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final String townName;
	private final Town town;
	private final String nationName;
	private final Nation nation;
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * Event thrown when a player in charge of a town uses /n leave,
	 * to leave the nation they are joined in.
	 * 
	 * @param nation Nation being left.
	 * @param town Town leaving the nation.
	 */
	public NationTownLeaveEvent(Nation nation, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.townName = town.getName();
		this.town = town;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getTownName() {
		return townName;
	}

	public String getNationName() {
		return nationName;
	}

	public Town getTown() {
		return town;
	}

	public Nation getNation() {
		return nation;
	}
}
