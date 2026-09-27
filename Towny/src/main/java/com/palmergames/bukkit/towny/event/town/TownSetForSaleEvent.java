package com.palmergames.bukkit.towny.event.town;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;

public class TownSetForSaleEvent extends CancellableTownyEvent{
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final Player player;
	private final double price;

	public TownSetForSaleEvent(Town town, Player player, double price) {
		this.town = town;
		this.player = player;
		this.price = price;
	}

	/**
	 * Gets the town being set for sale.
	 * @return the Town being set for sale.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * Gets the player setting the town for sale.
	 * @return the Player who is setting the town for sale.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * Gets the price for which the player has set the town for sale.
	 * @return the price of the town.
	 */
	public double getPrice() {
		return price;
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
