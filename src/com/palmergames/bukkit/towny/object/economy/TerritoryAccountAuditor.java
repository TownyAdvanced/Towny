package com.palmergames.bukkit.towny.object.economy;

import java.util.List;

public class TerritoryAccountAuditor implements AccountAuditor {

	@Override
	public void withdrew(Account account, double amount, String reason) {
		// TODO: Implement Audit Storage
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		// TODO: Implement Audit Storage.
	}

	@Override
	public List<Audit> getAuditHistory() {
		throw new UnsupportedOperationException("This feature is not implemented yet.");
	}
}
