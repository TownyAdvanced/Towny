package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An Cancellable event thrown when a {@link Nation} {@link BankAccount} is
 * about to either receive or pay money.
 */
public class NationPreTransactionEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private final Transaction transaction;

	/**
	 * An Cancellable event thrown when a {@link Nation} {@link BankAccount} is
	 * about to either receive or pay money.
	 * 
	 * @param nation      {@link Nation} whose account which is paying or receiving
	 *                    money.
	 * @param transaction {@link Transaction} which will be occuring.
	 */
	public NationPreTransactionEvent(Nation nation, Transaction transaction) {
		this.nation = nation;
		this.transaction = transaction;
	}

	/**
	 * @return {@link Nation}
	 */
	public Nation getNation() {
		return nation;
	}

	/**
	 * @return {@link Transaction} which will be occuring.
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * @return the future balance of the Nation {@link BankAccount} if this event is
	 *         not cancelled.
	 */
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
