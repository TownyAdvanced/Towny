package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.economy.Account;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Transaction {
	private final Account account;
	private final TransactionType type;
	private final double amount;
	
	public Transaction(Account account, TransactionType type, double amount) {
		this.account = account;
		this.type = type;
		this.amount = amount;
	}

	@NotNull
	public Account getAccount() {
		return this.account;
	}

	@NotNull
	public TransactionType getType() {
		return type;
	}

	/**
	 * @return The player who owns the account associated with the transaction
	 */
	@Nullable
	public Player getPlayer() {
		return account instanceof EconomyAccount ? Bukkit.getServer().getPlayer(account.getUUID()) : null;
	}

	public double getAmount() {
		return amount;
	}
}
