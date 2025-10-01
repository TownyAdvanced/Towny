package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when calculating a nation's neutrality cost.
 */
public class NationNeutralityCostCalculationEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Nation nation;
	private double neutralityCost;

	@ApiStatus.Internal
	public NationNeutralityCostCalculationEvent(Nation nation, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * Gets the nation paying the neutrality cost.
	 * 
	 * @return the nation which is paying neutrality costs
	 */
	@NotNull
	public Nation getNation() {
		return nation; 
	}

	/**
	 * Gets the cost for neutrality of the nation.
	 * 
	 * @return the neutrality cost for the nation
	 */
	public double getNeutralityCost() { 
		return neutralityCost; 
	}

	/**
	 * Sets the cost for neutrality of the nation.
	 * 
	 * @param neutralityCost the value to set the neutrality cost to
	 */
	public void setNeutralityCost(double neutralityCost) {
		this.neutralityCost = neutralityCost;
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
