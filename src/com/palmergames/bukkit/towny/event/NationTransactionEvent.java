package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.Bukkit;

public class NationTransactionEvent extends BankTransactionEvent {
	
	Nation nation;
	
	public NationTransactionEvent(Nation nation, Transaction transaction) {
		super(nation, transaction);
		this.nation = nation;
	}

	public Nation getNation() {
		return nation;
	}
}
