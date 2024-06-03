package com.palmergames.bukkit.towny.object.economy.transaction;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.economy.Account;

public class Transaction {
	private final TransactionType type;
	private final Account receivingAccount;
	private final Account sendingAccount;
	private final double amount;

	public Transaction(TransactionBuilder builder) {
		this.type = builder.type; 
		this.receivingAccount = builder.receivingAccount;
		this.sendingAccount = builder.sendingAccount;
		this.amount = builder.amount;
	}

	public static TransactionBuilder add(double amount) {
		return new TransactionBuilder(amount, TransactionType.ADD);
	}

	public static TransactionBuilder subtract(double amount) {
		return new TransactionBuilder(amount, TransactionType.SUBTRACT);
	}

	public static TransactionBuilder deposit(double amount) {
		return new TransactionBuilder(amount, TransactionType.DEPOSIT);
	}

	public static TransactionBuilder withdraw(double amount) {
		return new TransactionBuilder(amount, TransactionType.WITHDRAW);
	}

	public TransactionType getType() {
		return type;
	}

	@Nullable
	public Account getReceivingAccount() {
		return receivingAccount;
	}

	public boolean hasReceiverAccount() {
		return receivingAccount != null;
	}

	@Nullable
	public Account getSendingAccount() {
		return sendingAccount;
	}

	public boolean hasSenderAccount() {
		return sendingAccount != null;
	}

	@Nullable
	public Player getPlayer() {
		return getSendingAccount() != null ? Bukkit.getServer().getPlayerExact(getSendingAccount().getName()) : null;
	}

	public double getAmount() {
		return amount;
	}
}
