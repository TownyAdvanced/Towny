package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

/**
 * An event thrown when a {@link Town} {@link BankAccount} either receives or
 * pays money.
 */
public class TownTransactionEvent extends BankTransactionEvent {
	private final Town town;

	/**
	 * An event thrown when a {@link Town} {@link BankAccount} either receives or
	 * pays money.
	 * 
	 * @param town        {@link Town} whose account which is paying or receiving
	 *                    money.
	 * @param transaction {@link Transaction} which has occured.
	 */
	public TownTransactionEvent(Town town, Transaction transaction) {
		super(town.getAccount(), transaction);
		this.town = town;
	}

	/**
	 * @return {@link town}
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return {@link BankAccount} belonging to the town.
	 */
	public BankAccount getTownBankAccount() {
		return town.getAccount();
	}
}
