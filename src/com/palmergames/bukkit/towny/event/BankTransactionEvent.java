package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called whenever a deposit is made to any object with
 * an associated bank.
 * 
 * Deprecated as of 0.96.3.4 use {@link com.palmergames.bukkit.towny.event.economy.BankTransactionEvent} instead.
 */
@Deprecated
public class BankTransactionEvent extends Event {

	private final Account account;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;
	
	public BankTransactionEvent(Account account, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.transaction = transaction;
		this.account = account;
	}
	
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}

	public Account getAccount() {
		return account;
	}

	public Transaction getTransaction() {
		return transaction;
	}
}
