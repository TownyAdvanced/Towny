package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyUniverse;

public class NationAccountAuditor implements AccountObserver {
	
	String nationName;
	
	public NationAccountAuditor(String nationName) {
		this.nationName = nationName;
	}

	@Override
	public void withdrew(Account account, double amount, String reason) {
		// TODO: Audit Nation Transactions. 
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		// TODO: Audit Nation Transactions. 
	}
	
	private Nation getNation() {
		return TownyUniverse.getInstance().getNationsMap().get(nationName);
	}
}
