package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyUniverse;

public class TownAccountAuditor implements AccountObserver {
	
	String townName;
	
	public TownAccountAuditor(String townName) {
		this.townName = townName;
	}

	@Override
	public void withdrew(Account account, double amount, String reason) {
		// TODO: Audit Town Transactions.
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		// TODO: Audit Town Transactions.
	}
	
	public Town getTown() {
		return TownyUniverse.getInstance().getTownsMap().get(townName);
	}
}
