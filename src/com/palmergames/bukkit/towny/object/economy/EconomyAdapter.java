package com.palmergames.bukkit.towny.object.economy;

import org.bukkit.World;

/**
 * An adapter that is used to adapt multiple
 * economy implementations.
 */
public interface EconomyAdapter {
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean add(String accountName, double amount, World world);

	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean subtract(String accountName, double amount, World world);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param accountName The name of the account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(String accountName);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param accountName The name of the account.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	double getBalance(String accountName, World world);

	/**
	 * Attempts to create an account.
	 * 
	 * @param accountName The name of the new account.
	 */
	void newAccount(String accountName);

	/**
	 * Removes an account.
	 * 
	 * @param accountName The name of the account to remove.
	 */
	void deleteAccount(String accountName);

	/**
	 * Sets the balance of the account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(String accountName, Double amount, World world);

	/**
	 * Get's the proper formatting for a given balance.
	 * 
	 * @param balance The balance to format.
	 * @return A string with the balance formatted.
	 */
	String getFormattedBalance(double balance);
}
