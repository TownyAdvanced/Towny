package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.object.TransactionType;

import java.util.ArrayList;
import java.util.List;

public class TerritoryAccountAuditor implements AccountAuditor {
	
	String townName;
	List<Audit> audits = new ArrayList<>();
	
	public TerritoryAccountAuditor(String townName) {
		this.townName = townName;
	}

	@Override
	public void withdrew(Account account, double amount, String reason) {
		Audit audit = new Audit(TransactionType.WITHDRAW, null, (int)amount, reason);
		audits.add(audit);
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		Audit audit = new Audit(TransactionType.DEPOSIT, null, (int)amount, reason);
		audits.add(audit);
	}

	@Override
	public List<Audit> getAuditHistory() {
		return audits; 
	}
}
