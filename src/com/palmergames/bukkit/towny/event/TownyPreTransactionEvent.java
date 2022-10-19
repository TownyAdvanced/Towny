package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Transaction;

public class TownyPreTransactionEvent extends CancellableTownyEvent {
	private final Transaction transaction;

	public TownyPreTransactionEvent(Transaction transaction) {
		this.transaction = transaction;
	}

	public Transaction getTransaction() {
		return transaction;
	}
	
	public int getNewBalance() {
		switch (transaction.getType()) {
			case ADD:
				return (int) (TownyEconomyHandler.getBalance(transaction.getPlayer().getName(),
					transaction.getPlayer().getWorld())
					+ transaction.getAmount());
			case SUBTRACT:
				return (int) (TownyEconomyHandler.getBalance(transaction.getPlayer().getName(),
					transaction.getPlayer().getWorld())
					- transaction.getAmount());
			default:
				break;
		}
		
		return 0;
	}
}
