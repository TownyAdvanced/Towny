package com.palmergames.bukkit.towny.object.economy.transaction;

import com.palmergames.bukkit.towny.event.economy.TownyTransactionEvent;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;

public class TransactionBuilder {
	TransactionType type;
	Account receivingAccount;
	Account sendingAccount;
	double amount;

	public TransactionBuilder(double amount, TransactionType type) {
		this.amount = amount;
		this.type = type;
	}

	public TransactionBuilder paidTo(Account account) {
		this.receivingAccount = account;
		return this;
	}

	public TransactionBuilder paidTo(EconomyHandler handler) {
		this.receivingAccount = handler.getAccount();
		return this;
	}

	public TransactionBuilder paidToServer() {
		this.receivingAccount = TownyServerAccount.ACCOUNT;
		return this;
	}

	public TransactionBuilder paidBy(Account account) {
		this.sendingAccount = account;
		return this;
	}

	public TransactionBuilder paidBy(EconomyHandler handler) {
		this.sendingAccount = handler.getAccount();
		return this;
	}

	public TransactionBuilder paidByServer() {
		this.sendingAccount = TownyServerAccount.ACCOUNT;
		return this;
	}

	public Transaction build() {
		return new Transaction(this);
	}

	public TownyTransactionEvent asTownyTransactionEvent() {
		return new TownyTransactionEvent(new Transaction(this));
	}
}
