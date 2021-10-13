package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Overriding this event you can use custom formulas to set town block claim price
 */
public class TownBlockClaimCostCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private double price;
	private final Town town;
	private final int plotAmount;
	private final boolean isBonus;

	public TownBlockClaimCostCalculationEvent(Town town, double price,int plotAmount, boolean isBonus) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.price = price;
		this.plotAmount = plotAmount;
		this.isBonus = isBonus;
	}

	/**
	 * Returns target Town.
	 * @return target Town
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * Sets the price to claim town blocks.
	 * @param value price to claim town blocks.
	 */
	public void setPrice(double value) {
		this.price = value;
	}

	/**
	 * Returns the price to claim town blocks.
	 * @return the price to claim town blocks
	 */
	public double getPrice() {
		return price;
	}
	
	/**
	 * Returns the amount of town blocks to be claimed.
	 * @return the amount of town blocks to be claimed
	 */
	public int getPlotAmount() {
		return this.plotAmount;
	}
	
	/**
	 * Returns true if claimed blocks are Bonus Blocks.
	 * @return true if claimed blocks are Bonus Blocks
	 */
	public boolean isBonus() {
		return this.isBonus;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
