package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Transaction;
import org.bukkit.Warning;

/**
 * This event is no longer called.
 * @deprecated since 0.98.4.9 use com.palmergames.bukkit.towny.event.economy.TownTransactionEvent instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.economy package.")
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
