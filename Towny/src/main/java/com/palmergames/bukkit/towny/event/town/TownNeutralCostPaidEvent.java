package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when town has paid neutrality costs.
 */
public class TownNeutralCostPaidEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final double neutralityCost;

	@ApiStatus.Internal
	public TownNeutralCostPaidEvent(Town town, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * {@return the town which is paying neutrality costs}
	 */
	@NotNull
	public Town getTown() {
		return town; 
	}

	/**
	 * {@return the neutrality cost for this town, which is always positive}
	 */
	public double getNeutralityCost() { 
		return neutralityCost; 
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
