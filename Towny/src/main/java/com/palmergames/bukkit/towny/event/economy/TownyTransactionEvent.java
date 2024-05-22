package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event thrown when any {@link Transaction} has occured because of Towny.
 * This includes players, towns, nations and special accounts internal to Towny.
 */
public class TownyTransactionEvent extends Event {

	private final Transaction transaction;
	private static final HandlerList handlers = new HandlerList();

	/**
	 * An event thrown when any {@link Transaction} has occured because of Towny.
	 * This includes players, towns, nations and special accounts internal to Towny.
	 * 
	 * @param transaction {@link Transaction} which has occured.
	 */
	public TownyTransactionEvent(Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	/**
	 * @return {@link Transaction} which has occured.
	 */
	public Transaction getTransaction() {
		return transaction;
	}
}
