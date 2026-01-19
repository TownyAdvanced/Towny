package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when calculating a town's neutrality cost.
 */
public class TownNeutralityCostCalculationEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private double neutralityCost;

	@ApiStatus.Internal
	public TownNeutralityCostCalculationEvent(Town town, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * Gets the town paying the neutrality cost.
	 * 
	 * @return the town which is paying neutrality costs
	 */
	@NotNull
	public Town getTown() {
		return town; 
	}

	/**
	 * Gets the cost for neutrality of the town.
	 * 
	 * @return the neutrality cost for the town
	 */
	public double getNeutralityCost() { 
		return neutralityCost; 
	}

	/**
	 * Sets the cost for neutrality of the town.
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
