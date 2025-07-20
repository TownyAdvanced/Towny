package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
* fires when nation pay neutral charge
* */
public class NationNeutralCostCharge extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Nation nation;
	private final double neutralityCost;
	
	public NationNeutralCostCharge(Nation nation, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
		this.nation = nation;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * @return The nation for which pay neutral charge
	 * */
	@NotNull
	public Nation getTown() { return nation; }

	/**
	 * @return neutral charge for this town, only positive value, above 0
	 * */
	public double getNeutralityCost() { return neutralityCost; }

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
