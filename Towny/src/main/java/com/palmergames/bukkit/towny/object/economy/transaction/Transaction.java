package com.palmergames.bukkit.towny.object.economy.transaction;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.object.Resident;
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

	/**
	 * In the case of a Transaction where a player deposits money into a town or
	 * nation bank, this will return the player, or null if it is not a transaction
	 * where a player sent money.
	 * 
	 * @return the Player that sent money or null.
	 */
	@Nullable
	public Player getSendingPlayer() {
		if (hasSenderAccount() && getSendingAccount().getEconomyHandler() instanceof Resident resident)
			return resident.getPlayer();
		return null;
	}

	public double getAmount() {
		return amount;
	}
}
