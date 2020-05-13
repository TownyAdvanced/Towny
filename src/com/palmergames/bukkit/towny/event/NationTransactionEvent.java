package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;

public class NationTransactionEvent extends BankTransactionEvent {
	
	Nation nation;
	
	public NationTransactionEvent(Nation nation, Transaction transaction) {
		super(nation.getAccount(), transaction);
		this.nation = nation;
	}

	public Nation getNation() {
		return nation;
	}
}
