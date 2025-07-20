package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
* fires when nation pay positive/negative upkeep charge
* */
public class NationUpkeepCharge extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Nation nation;
	private final double upkeep;

	public NationUpkeepCharge(Nation nation, double upkeep) {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
		this.nation = nation;
		this.upkeep = upkeep;
	}

	/**
	 * @return The nation for which pay positive/negative upkeep charge 
	 * */
	@NotNull
	public Nation getNation() { return nation; }

	/**
	 * @return upkeep charge for this nation, may be positive or negative
	 * */
	public double getUpkeep() { return upkeep; }

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
