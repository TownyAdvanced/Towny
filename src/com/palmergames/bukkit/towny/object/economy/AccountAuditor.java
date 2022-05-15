package com.palmergames.bukkit.towny.object.economy;

import java.util.List;

public interface AccountAuditor extends AccountObserver {
	/**
	 * Gets the formatted transactions associated with this account. This should
	 * return the history in chronological order.
	 * 
	 * @return The formatted transaction history from this account.
	 */
	List<String> getAuditHistory();

	/**
	 * Gets the transactions associated with this account. This should
	 * return the history in chronological order.
	 *
	 * @return The transaction history from this account.
	 */
	List<BankTransaction> getTransactions();
}
