package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlayerTransactionEvent extends Event {
	
	private Transaction transaction;
	private static final HandlerList handlers = new HandlerList();
	
	public PlayerTransactionEvent(Transaction transaction) {
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
