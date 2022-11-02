package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownyPreTransactionEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
