package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;

public class NationPreTransactionEvent extends CancellableTownyEvent {
	private final Nation nation;
	private final Transaction transaction;

	public NationPreTransactionEvent(Nation nation, Transaction transaction) {
		this.nation = nation;
		this.transaction = transaction;
	}

	public Nation getNation() {
		return nation;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public int getNewBalance() {
		switch (transaction.getType()) {
			case DEPOSIT:
				return (int) (nation.getAccount().getHoldingBalance() + transaction.getAmount());
			case WITHDRAW:
				return (int) (nation.getAccount().getHoldingBalance() - transaction.getAmount());
			default:
				break;
		}
		
		return 0;
	}
}
