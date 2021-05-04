package com.palmergames.bukkit.towny.object.economy;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.TransactionType;

public class GovernmentAccountAuditor implements AccountAuditor {

	private List<BankTransaction> transactions = new ArrayList<BankTransaction>();
	
	@Override
	public void withdrew(Account account, double amount, String reason) {
		transactions.add(new BankTransaction(TransactionType.WITHDRAW, System.currentTimeMillis(), account, amount, account.getHoldingBalance(), reason));
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		transactions.add(new BankTransaction(TransactionType.DEPOSIT, System.currentTimeMillis(), account, amount, account.getHoldingBalance(), reason));
	}

	@Override
	public List<String> getAuditHistory() {
		
		List<String> history = new ArrayList<>(transactions.size());
		String line;
		
		for (BankTransaction transaction : transactions) {
			line = transaction.getTime() + "\n\n";
			line += transaction.getType().getName() + " of " + ChatColor.stripColor(TownyEconomyHandler.getFormattedBalance(transaction.getAmount()));
			line += (transaction.getType() == TransactionType.DEPOSIT ? " to " : " from ") + transaction.getAccount().getName() + "\n\n";
			line += "Reason: " + transaction.getReason() + "\n\n";
			line += "Balance: " + ChatColor.stripColor(TownyEconomyHandler.getFormattedBalance(transaction.getBalance()));
			history.add(line);
		}
		
		return history;
	}
}
