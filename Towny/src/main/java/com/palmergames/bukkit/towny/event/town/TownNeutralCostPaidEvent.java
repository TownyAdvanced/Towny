package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
* fires when town pay neutral charge
*/
public class TownNeutralCostPaidEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final double neutralityCost;
	
	public TownNeutralCostPaidEvent(Town town, double neutralityCost) {
		super(!Bukkit.getServer().isPrimaryThread()); // Check if event is async
		this.town = town;
		this.neutralityCost = neutralityCost;
	}

	/**
	 * @return The town for which pay neutral charge
	 */
	@NotNull
	public Town getTown() { return town; }

	/**
	 * @return neutral charge for this town, only positive value, above 0
	 */
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
