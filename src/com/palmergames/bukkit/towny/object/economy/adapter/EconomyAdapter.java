package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import org.bukkit.World;

/**
 * An adapter that is used to adapt multiple
 * economy implementations.
 */
public interface EconomyAdapter {
	/**
	 * Attempts to add money to an account.
	 * 
	 * @param account The account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean add(Account account, double amount, World world);
	
	@Deprecated
	boolean add(String accountName, double amount, World world);

	/**
	 * Attempts to subtract money from an account.
	 *
	 * @param account The account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean subtract(Account account, double amount, World world);
	
	boolean subtract(String accountName, double amount, World world);

	/**
	 * Checks whether the given account exists.
	 * 
	 * @param account The account.
	 * @return A boolean indicating success.
	 */
	boolean hasAccount(Account account);
	
	boolean hasAccount(String accountName);

	/**
	 * Gets the balance of the account.
	 * 
	 * @param account The account.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	double getBalance(Account account, World world);
	
	@Deprecated
	double getBalance(String accountName, World world);

	/**
	 * Attempts to create an account.
	 * 
	 * @param account The account.
	 */
	void newAccount(Account account);
	
	@Deprecated
	void newAccount(String accountName);

	/**
	 * Removes an account.
	 * 
	 * @param account The account to remove.
	 */
	void deleteAccount(Account account);
	
	@Deprecated
	void deleteAccount(String accountName);

	/**
	 * Sets the balance of the account.
	 * 
	 * @param account The account.
	 * @param amount The amount to add.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(Account account, double amount, World world);
	
	@Deprecated
	boolean setBalance(String accountName, double amount, World world);

	/**
	 * Get's the proper formatting for a given balance.
	 * 
	 * @param balance The balance to format.
	 * @return A string with the balance formatted.
	 */
	String getFormattedBalance(double balance);
}
