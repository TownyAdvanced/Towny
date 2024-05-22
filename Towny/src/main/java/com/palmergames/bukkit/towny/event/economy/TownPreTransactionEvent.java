package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An Cancellable event thrown when a {@link Town} {@link BankAccount} is about
 * to either receive or pay money.
 */
public class TownPreTransactionEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final Transaction transaction;

	/**
	 * An Cancellable event thrown when a {@link Town} {@link BankAccount} is about
	 * to either receive or pay money.
	 * 
	 * @param town        {@link Town} whose account which is paying or receiving
	 *                    money.
	 * @param transaction {@link Transaction} which will be occuring.
	 */
	public TownPreTransactionEvent(Town town, Transaction transaction) {
		this.town = town;
		this.transaction = transaction;
	}

	/**
	 * @return {@link Town}
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return {@link Transaction} which will be occuring.
	 */
	public Transaction getTransaction() {
		return transaction;
	}

	/**
	 * @return the future balance of the Town {@link BankAccount} if this event is
	 *         not cancelled.
	 */
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
