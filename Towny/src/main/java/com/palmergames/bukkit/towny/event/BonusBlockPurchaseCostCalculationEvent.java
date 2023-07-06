package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Overriding this event you can use custom formulas to set bonus block purchase price
 */
public class BonusBlockPurchaseCostCalculationEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private double price;
	private final Town town;
	private final int plotAmount;

	public BonusBlockPurchaseCostCalculationEvent(Town town, double price,int plotAmount) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.price = price;
		this.plotAmount = plotAmount;
	}

	/**
	 * Returns the target Town.
	 * @return town Town
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
	 * Returns the price to purchase bonus blocks.
	 * @return price to purchase bonus blocks
	 */
	public double getPrice() {
		return price;
	}
	
	/**
	 * Returns the amount of bonus blocks to be purchased.
	 * @return plotAmount The amount of bonus blocks to be purchased
	 */
	public int getAmountOfPurchasingBlocksRequest() {
		return plotAmount;
	}
	
	/**
	 * Returns the amount of bonus blocks the town has already purchased.
	 * @return amount of blocks already bought by the town, prior to this event.
	 */ 
	public int getAlreadyPurchasedBlocksAmount() { 
		return town.getPurchasedBlocks(); 
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
