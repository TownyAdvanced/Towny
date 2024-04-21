package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyLogger;

/**
 * A class which performs audits on ALL account transactions.
 * 
 * @author Suneet Tipirneni (Siris)
 * @see AccountAuditor
 */
public final class GlobalAccountObserver implements AccountObserver {

	@Override
	public void withdrew(Account account, double amount, String reason) {
		TownyLogger.logMoneyTransaction(account, amount, null, reason);
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		TownyLogger.logMoneyTransaction(account, amount, null, reason);
	}
}
