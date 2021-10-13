package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Overriding this event you can use custom formulas to set bonus block claim price
 */
public class BonusBlockClaimCostCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private double price;
	private final Town town;
	private final int plotAmount;

	public BonusBlockClaimCostCalculationEvent(Town town, double price,int plotAmount) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.price = price;
		this.plotAmount = plotAmount;
	}

	/**
	 * Returns target Town.
	 * @return target Town
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * Sets the price to claim bonus blocks.
	 * @param value price to claim bonus blocks.
	 */
	public void setPrice(double value) {
		this.price = value;
	}

	/**
	 * Returns the price to claim bonus blocks.
	 * @return the price to claim bonus blocks
	 */
	public double getPrice() {
		return price;
	}
	
	/**
	 * Returns the amount of bonus blocks to be claimed.
	 * @return the amount of bonus blocks to be claimed
	 */
	public int getPlotAmount() {
		return this.plotAmount;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
