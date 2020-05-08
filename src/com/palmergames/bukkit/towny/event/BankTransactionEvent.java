package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Bank;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called whenever a deposit is made to any object with
 * an associated bank.
 */
public class BankTransactionEvent extends Event {

	private final Bank bank;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;
	
	public BankTransactionEvent(Bank bank, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.transaction = transaction;
		this.bank = bank;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Bank getBank() {
		return bank;
	}

	public Transaction getTransaction() {
		return transaction;
	}
}
