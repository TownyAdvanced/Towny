package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import org.jetbrains.annotations.ApiStatus;

/**
 * An adapter that is used to adapt multiple
 * economy implementations.
 */
@ApiStatus.Internal
public interface EconomyAdapter {
	String name();
	
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param account The account.
	 * @param amount The amount to add.
	 * @return A boolean indicating success.
	 */
	boolean add(Account account, double amount);

	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param account The account.
	 * @param amount The amount to add.
	 * @return A boolean indicating success.
	 */
	boolean subtract(Account account, double amount);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param account The account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(Account account);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param account The account.
	 * @return A boolean indicating success.
	 */
	double getBalance(Account account);

	/**
	 * Attempts to create an account.
	 * 
	 * @param account The name of the new account.
	 */
	void newAccount(Account account);

	/**
	 * Removes an account.
	 * 
	 * @param account The name of the account to remove.
	 */
	void deleteAccount(Account account);

	/**
	 * Renames an account.
	 * 
	 * @param account the Account to rename.
	 * @param newName the name to give the Account.
	 */
	boolean renameAccount(Account account, String newName);

	/**
	 * Sets the balance of the account.
	 * 
	 * @param account The account.
	 * @param amount The amount to add.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(Account account, double amount);

	/**
	 * Get's the proper formatting for a given balance.
	 * 
	 * @param balance The balance to format.
	 * @return A string with the balance formatted.
	 */
	String getFormattedBalance(double balance);
}
