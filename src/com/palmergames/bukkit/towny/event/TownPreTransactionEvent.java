package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;

public class TownPreTransactionEvent extends CancellableTownyEvent {
	private final Town town;
	private final Transaction transaction;

	public TownPreTransactionEvent(Town town, Transaction transaction) {
		this.town = town;
		this.transaction = transaction;
	}

	public Town getTown() {
		return town;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public int getNewBalance() {
		switch (transaction.getType()) {
			case DEPOSIT:
				return (int) (town.getAccount().getHoldingBalance() + transaction.getAmount());
			case WITHDRAW:
				return (int) (town.getAccount().getHoldingBalance() - transaction.getAmount());
			default:
				break;
		}

		return 0;
	}
}
