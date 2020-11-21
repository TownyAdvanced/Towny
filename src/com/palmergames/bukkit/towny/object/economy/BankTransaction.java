package com.palmergames.bukkit.towny.object.economy;

import java.text.SimpleDateFormat;

import com.palmergames.bukkit.towny.object.TransactionType;

public class BankTransaction {
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d ''yy '@' HH:mm:ss");
	private final TransactionType type;
	private final Long time;
	private final Account account;
	private final double amount;
	private final double balance;
	private final String reason;
	
	public BankTransaction(TransactionType type, Long time, Account account, double amount, double balance, String reason) {
		this.type = type;
		this.time = time;
		this.account = account;
		this.amount = amount;
		this.reason = reason;
		this.balance = balance;
	}

	public TransactionType getType() {
		return type;
	}
	
	public String getTime() {
		return dateFormat.format(time);
	}

	public Account getAccount() {
		return account;
	}
	
	public double getBalance() {
		return balance;
	}

	public double getAmount() {
		return amount;
	}
	
	public String getReason() {
		return reason;
	}
}
