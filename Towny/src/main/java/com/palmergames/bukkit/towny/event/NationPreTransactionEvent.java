package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Warning;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * This event is no longer called.
 * @deprecated since 0.98.4.9 use com.palmergames.bukkit.towny.event.economy.NationPreTransactionEvent instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.economy package.")
public class NationPreTransactionEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
