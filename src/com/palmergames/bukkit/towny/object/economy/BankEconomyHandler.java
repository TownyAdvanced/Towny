package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.object.EconomyHandler;

/**
 * Defines methods necessary for the operation of a bank.
 */
public interface BankEconomyHandler extends EconomyHandler {
	/**
	 * Gets the max amount of money that can be in the bank.
	 * 
	 * @return The max amount of money.
	 */
	double getBankCap();

	/**
	 * The prefix to be used for the bank account.
	 * 
	 * @return A string providing the prefix.
	 */
	String getEconomyPrefix();

	@Override
	Bank getAccount();
}
