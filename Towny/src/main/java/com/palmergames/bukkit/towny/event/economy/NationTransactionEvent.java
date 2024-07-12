package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

/**
 * An event thrown when a {@link Nation} {@link BankAccount} either receives or
 * pays money.
 */
public class NationTransactionEvent extends BankTransactionEvent {

	final Nation nation;

	/**
	 * An event thrown when a {@link Nation} {@link BankAccount} either receives or
	 * pays money.
	 * 
	 * @param nation      {@link Nation} whose account which is paying or receiving
	 *                    money.
	 * @param transaction {@link Transaction} which has occured.
	 */
	public NationTransactionEvent(Nation nation, Transaction transaction) {
		super(nation.getAccount(), transaction);
		this.nation = nation;
	}

	/**
	 * @return {@link Nation}
	 */
	public Nation getNation() {
		return nation;
	}

	/**
	 * @return {@link BankAccount} belonging to the nation.
	 */
	public BankAccount getNationBankAccount() {
		return nation.getAccount();
	}
}
