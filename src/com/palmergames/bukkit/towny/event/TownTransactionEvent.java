package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class TownTransactionEvent extends BankTransactionEvent {
	private final Town town;
	
	public TownTransactionEvent(Town town, Transaction transaction) {
		super(town, transaction);
		this.town = town;
	}

	public Town getTown() {
		return town;
	}
}
