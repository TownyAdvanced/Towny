package com.palmergames.bukkit.towny.event.economy;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;

public class TownTransactionEvent extends BankTransactionEvent {
	private final Town town;
	
	public TownTransactionEvent(Town town, Transaction transaction) {
		super(town.getAccount(), transaction);
		this.town = town;
	}

	public Town getTown() {
		return town;
	}
}
