package com.palmergames.bukkit.towny.object.economy;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.Colors;
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
		
		for (final BankTransaction transaction : transactions) {
			history.add(Colors.translateColorCodes(TownySettings.getAuditHistoryFormat()
				.replace("{time}", transaction.getTime())
				.replace("{type}", transaction.getType().getName())
				.replace("{amount}", ChatColor.stripColor(TownyEconomyHandler.getFormattedBalance(transaction.getAmount())))
				.replace("{to-from}", (transaction.getType() == TransactionType.DEPOSIT ? " to " : " from "))
				.replace("{name}", transaction.getAccount().getName())
				.replace("{reason}", transaction.getReason())
				.replace("{amount}", ChatColor.stripColor(TownyEconomyHandler.getFormattedBalance(transaction.getBalance())))
			));
		}
		
		return history;
	}
}
