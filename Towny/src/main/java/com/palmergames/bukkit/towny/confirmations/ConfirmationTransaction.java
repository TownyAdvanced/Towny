package com.palmergames.bukkit.towny.confirmations;

import java.util.function.Supplier;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.economy.Account;

public class ConfirmationTransaction {
	private final Supplier<Double> costSupplier;
	private double cost;
	private final Account payee;
	private final String loggedMessage;
	private final Translatable insufficientFundsMessage;

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 *
	 * @param costSupplier cost of the transaction. 
	 * @param townyObject EconomyHandler which will have to pay, this will be a Resident, Town or Nation.
	 * @param loggedMessage The message logged in the money.csv file.
	 * @param insufficientFundsMessage Transatable which will display the cannot pay message.
	 */
	public ConfirmationTransaction(Supplier<Double> costSupplier, EconomyHandler townyObject, String loggedMessage, Translatable insufficientFundsMessage) {
		this.costSupplier = costSupplier;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = insufficientFundsMessage;
		this.payee = !TownyEconomyHandler.isActive() ? null : townyObject.getAccount();
	}

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 * Uses the default no money error message.
	 *
	 * @param costSupplier cost of the transaction. 
	 * @param townyObject EconomyHandler which will have to pay, this will be a Resident, Town or Nation.
	 * @param loggedMessage The message logged in the money.csv file.
	 */
	public ConfirmationTransaction(Supplier<Double> costSupplier, EconomyHandler townyObject, String loggedMessage) {
		this.costSupplier = costSupplier;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = null;
		this.payee = !TownyEconomyHandler.isActive() ? null : townyObject.getAccount();
	}

	public void supplyCost() {
		this.cost = costSupplier.get();
	}

	public double getCost() {
		return cost;
	}

	@Nullable
	public Account getPayee() {
		return payee;
	}

	public String getLoggedMessage() {
		return loggedMessage;
	}

	public Translatable getInsufficientFundsMessage() {
		return insufficientFundsMessage != null ? insufficientFundsMessage : Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(getCost()));
	}

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 *
	 * @deprecated since 0.100.0.10 use {@link #ConfirmationTransaction(Supplier, EconomyHandler, String, Translatable)} instead.
	 * @param costSupplier cost of the transaction. 
	 * @param payee Account which will have to pay.
	 * @param loggedMessage The message logged in the money.csv file.
	 * @param insufficientFundsMessage Transatable which will display the cannot pay message.
	 */
	@Deprecated
	public ConfirmationTransaction(Supplier<Double> costSupplier, Account payee, String loggedMessage, Translatable insufficientFundsMessage) {
		this.costSupplier = costSupplier;
		this.payee = payee;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = insufficientFundsMessage;
	}

	/**
	 * A transaction which must succeed for a Confirmation to complete.
	 * Uses the default no money error message.
	 *
	 * @deprecated since 0.100.0.10 use {@link #ConfirmationTransaction(Supplier, EconomyHandler, String)} instead.
	 * @param costSupplier cost of the transaction. 
	 * @param payee Account which will have to pay.
	 * @param loggedMessage The message logged in the money.csv file.
	 */
	@Deprecated
	public ConfirmationTransaction(Supplier<Double> costSupplier, Account payee, String loggedMessage) {
		this.costSupplier = costSupplier;
		this.payee = payee;
		this.loggedMessage = loggedMessage;
		this.insufficientFundsMessage = null;
	}
}
