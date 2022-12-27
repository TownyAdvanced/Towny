package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Warning;

/**
 * This event is no longer called.
 * @deprecated since 0.98.4.9 use com.palmergames.bukkit.towny.event.economy.NationTransactionEvent instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.economy package.")
public class NationTransactionEvent extends BankTransactionEvent {
	
	final Nation nation;
	
	public NationTransactionEvent(Nation nation, Transaction transaction) {
		super(nation.getAccount(), transaction);
		this.nation = nation;
	}

	public Nation getNation() {
		return nation;
	}
}
