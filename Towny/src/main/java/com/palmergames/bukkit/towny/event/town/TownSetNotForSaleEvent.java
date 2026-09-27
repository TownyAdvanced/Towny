package com.palmergames.bukkit.towny.event.town;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;

public class TownSetNotForSaleEvent extends CancellableTownyEvent{
	private static final HandlerList HANDLER_LIST = new HandlerList();
	private final Town town;
	private final Player player;

	public TownSetNotForSaleEvent(Town town, Player player) {
		this.town = town;
		this.player = player;
	}

	/**
	 * Gets the town being set not for sale.
	 * @return the Town being set not for sale.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * Gets the player setting the town not for sale.
	 * @return the Player who is setting the town not for sale.
	 */
	public Player getPlayer() {
		return player;
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
