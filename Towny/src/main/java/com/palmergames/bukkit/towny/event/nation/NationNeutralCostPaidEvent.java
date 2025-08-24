package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a nation has paid the neutrality cost.
 */
public class NationNeutralCostPaidEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Nation nation;
	private final double neutralityCost;

	@ApiStatus.Internal
	public NationNeutralCostPaidEvent(Nation nation, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * {@return the nation which is paying neutrality costs}
	 */
	@NotNull
	public Nation getNation() {
		return nation; 
	}

	/**
	 * {@return the neutrality cost for this nation, which is always positive}
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
