package com.palmergames.bukkit.towny.object.economy;

import java.util.List;

public interface AccountAuditor extends AccountObserver {
	/**
	 * Gets the transactions associated with this account. This should
	 * return the history in chronological order.
	 * 
	 * @return The transaction history from this account.
	 */
	List<String> getAuditHistory();
}
