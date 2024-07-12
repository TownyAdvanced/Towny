package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.BankAccount;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Called whenever a deposit or withdrawl is made to/from a Bank account
 * associated with a Town or Nation.
 */
public class BankTransactionEvent extends Event {

	private final Account account;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;

	/**
	 * Called whenever a deposit or withdrawl is made to/from a Bank account
	 * associated with a Town or Nation.
	 * @param account {@link Account} which is an economy account.
	 * @param transaction {@link Transaction} which has happened.
	 */
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

	/**
	 * @return {@link Account} which is receiving or giving money. This will be a {@link BankAccount}. 
	 */
	public Account getAccount() {
		return account;
	}

	/**
	 * @return {@link Transaction} which is occuring.
	 */
	public Transaction getTransaction() {
		return transaction;
	}
}
