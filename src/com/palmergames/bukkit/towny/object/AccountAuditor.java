package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyLogger;

/**
 * A class which performs audits on ALL account transactions.
 * 
 * @author Suneet Tipirneni (Siris)
 * @see TownAccountAuditor
 * @see NationAccountAuditor
 */
public class AccountAuditor implements AccountObserver {

	@Override
	public void withdrew(Account account, double amount, String reason) {
		TownyLogger.getInstance().logMoneyTransaction(account, amount, null, reason);
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		TownyLogger.getInstance().logMoneyTransaction(account, amount, null, reason);
	}
}
