package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.Account;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An Cancellable event thrown when any {@link Transaction} is about to occur
 * because of Towny. This includes players, towns, nations and special accounts
 * internal to Towny.
 */
public class TownyPreTransactionEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Transaction transaction;

	/**
	 * An Cancellable event thrown when any {@link Transaction} is about to occur
	 * because of Towny. This includes players, towns, nations and special accounts
	 * internal to Towny.
	 * 
	 * @param transaction {@link Transaction} which will be occuring.
	 */
	public TownyPreTransactionEvent(Transaction transaction) {
		this.transaction = transaction;
	}

	/**
	 * @return {@link Transaction} which will be occuring.
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * @return the future balance of the {@link Account} if this event is not
	 *         cancelled.
	 */
	public double getNewBalance() {
		return switch (transaction.getType()) {
		case ADD, DEPOSIT -> transaction.getReceivingAccount().getHoldingBalance() + transaction.getAmount();
		case SUBTRACT, WITHDRAW -> transaction.getReceivingAccount().getHoldingBalance() - transaction.getAmount();
		};
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
