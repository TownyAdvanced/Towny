package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a nation has paid upkeep costs.
 */
public class NationUpkeepCostPaidEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Nation nation;
	private final double upkeep;

	@ApiStatus.Internal
	public NationUpkeepCostPaidEvent(Nation nation, double upkeep) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.upkeep = upkeep;
	}

	/**
	 * {@return the nation which is paying the upkeep cost}
	 */
	@NotNull
	public Nation getNation() { 
		return nation; 
	}

	/**
	 * {@return the upkeep cost for this nation, may be positive or negative}
	 */
	public double getUpkeep() {
		return upkeep; 
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
