package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class NationPreTransactionEvent extends Event implements Cancellable {
	private final Nation nation;
	private static final HandlerList handlers = new HandlerList();
	private final Transaction transaction;
	private String cancelMessage = "Sorry this event was cancelled.";
	private boolean isCancelled = false;

	public NationPreTransactionEvent(Nation nation, Transaction transaction) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.transaction = transaction;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public Nation getNation() {
		return nation;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public int getNewBalance() {
		try {
			switch (transaction.getType()) {
				case DEPOSIT:
					return (int) (nation.getAccount().getHoldingBalance() + transaction.getAmount());
				case WITHDRAW:
					return (int) (nation.getAccount().getHoldingBalance() - transaction.getAmount());
				default:
					break;
			}
		} catch (EconomyException e) {
			BukkitTools.getServer().getLogger().warning(e.getMessage());
		}
		
		return 0;
	}
}
