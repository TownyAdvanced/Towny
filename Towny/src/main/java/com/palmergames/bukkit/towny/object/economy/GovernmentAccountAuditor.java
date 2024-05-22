package com.palmergames.bukkit.towny.object.economy;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.transaction.TransactionType;

public class GovernmentAccountAuditor implements AccountAuditor {

	private final List<BankTransaction> transactions = new ArrayList<>();
	
	@Override
	public void withdrew(Account account, double amount, String reason) {
		Towny.getPlugin().getScheduler().runLater(() -> 
			transactions.add(new BankTransaction(TransactionType.WITHDRAW, System.currentTimeMillis(), account, amount, account.getHoldingBalance(), reason)), 1L);
	}

	@Override
	public void deposited(Account account, double amount, String reason) {
		Towny.getPlugin().getScheduler().runLater(() -> 
			transactions.add(new BankTransaction(TransactionType.DEPOSIT, System.currentTimeMillis(), account, amount, account.getHoldingBalance(), reason)), 1L);
	}

	@Override
	public List<String> getAuditHistory() {
		
		List<String> history = new ArrayList<>(transactions.size());
		
		for (final BankTransaction transaction : transactions) {
			history.add(Colors.translateColorCodes(TownySettings.getBankHistoryBookFormat()
				.replace("{time}", transaction.getTime())
				.replace("{type}", transaction.getType().getName())
				.replace("{amount}", Colors.strip(TownyEconomyHandler.getFormattedBalance(transaction.getAmount())))
				.replace("{to-from}", (transaction.getType() == TransactionType.DEPOSIT ? " to " : " from "))
				.replace("{name}", transaction.getAccount().getName())
				.replace("{reason}", transaction.getReason())
				.replace("{balance}", Colors.strip(TownyEconomyHandler.getFormattedBalance(transaction.getBalance())))
			));
		}
		
		return history;
	}

	@Override
	public List<BankTransaction> getTransactions() {
		return this.transactions;
	}
}
