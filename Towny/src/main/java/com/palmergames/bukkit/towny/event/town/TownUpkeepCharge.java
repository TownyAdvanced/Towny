package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
* fires when town pay positive/negative upkeep charge
* */
public class TownUpkeepCharge extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final double upkeep;
	private final double upkeepPentlty;

	public TownUpkeepCharge(Town town, double upkeep, double upkeepPentlty) {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
		this.town = town;
		this.upkeep = upkeep;
		this.upkeepPentlty = upkeepPentlty;
	}
	
	/**
	* @return The town for which pay positive/negative upkeep charge 
	* */
	@NotNull
	public Town getTown() { return town; }

	/**
	* @return upkeep charge for this town, may be positive or negative
	* */
	public double getUpkeep() { return upkeep; }
	
	/**
	* @return upkeepPenalty for this town, may be 0 or above
	* */
	public double getUpkeepPentlty() { return upkeepPentlty; }

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
