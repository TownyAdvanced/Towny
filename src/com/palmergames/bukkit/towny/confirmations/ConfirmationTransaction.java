package com.palmergames.bukkit.towny.confirmations;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.economy.Account;

public class ConfirmationTransaction {
	private final double cost;
	private final Account payee;
	private final String loggedMessage;
	private final Translatable insufficientFundsMessage;

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 * <p>
	 * @param cost cost of the transaction. 
	 * @param payee Account which will have to pay.
	 * @param loggedMessage The message logged in the money.csv file.
	 * @param insufficientFundsMessage Transatable which will display the cannot pay message.
	 */
	public ConfirmationTransaction(double cost, Account payee, String loggedMessage, Translatable insufficientFundsMessage) {
		this.cost = cost;
		this.payee = payee;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = insufficientFundsMessage;
	}

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 * Uses the default no money error message.
	 * <p>
	 * @param cost cost of the transaction. 
	 * @param payee Account which will have to pay.
	 * @param loggedMessage The message logged in the money.csv file.
	 */
	public ConfirmationTransaction(double cost, Account payee, String loggedMessage) {
		this.cost = cost;
		this.payee = payee;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(cost));
	}

	public double getCost() {
		return cost;
	}

	public Account getPayee() {
		return payee;
	}

	public String getLoggedMessage() {
		return loggedMessage;
	}

	public Translatable getInsufficientFundsMessage() {
		return insufficientFundsMessage;
	}
}
