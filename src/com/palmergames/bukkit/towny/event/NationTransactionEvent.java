package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class NationTransactionEvent extends Event {
	private final Nation nation;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;

	public NationTransactionEvent(Nation nation, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Nation getNation() {
		return nation;
	}

	public Transaction getTransaction() {
		return transaction;
	}
}
