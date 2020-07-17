package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyTransactionEvent extends Event {
	
	private final Transaction transaction;
	private static final HandlerList handlers = new HandlerList();
	
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

	public Transaction getTransaction() {
		return transaction;
	}
}
