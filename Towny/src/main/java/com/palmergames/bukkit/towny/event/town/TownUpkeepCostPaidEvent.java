package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a nation has paid upkeep costs.
 */
public class TownUpkeepCostPaidEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final double upkeep;
	private final double upkeepPenalty;

	@ApiStatus.Internal
	public TownUpkeepCostPaidEvent(Town town, double upkeep, double upkeepPenalty) {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
		this.town = town;
		this.upkeep = upkeep;
		this.upkeepPenalty = upkeepPenalty;
	}
	
	/**
	 * {@return The town which is paying the upkeep cost}
	 */
	@NotNull
	public Town getTown() { 
		return town; 
	}

	/**
	 * {@return the upkeep cost for this nation, may be positive or negative}
	 */
	public double getUpkeep() { 
		return upkeep; 
	}
	
	/**
	 * {@return the upkeep penalty that was charged for this town, may be 0 or above}
	 */
	public double getUpkeepPenalty() { 
		return upkeepPenalty; 
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
