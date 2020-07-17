package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class TownTransactionEvent extends Event {
	private final Town town;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;
	
	public TownTransactionEvent(Town town, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Town getTown() {
		return town;
	}

	public Transaction getTransaction() {
		return transaction;
	}
}
