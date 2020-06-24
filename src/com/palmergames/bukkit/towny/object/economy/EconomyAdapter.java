package com.palmergames.bukkit.towny.object.economy;

import org.bukkit.OfflinePlayer;
import org.bukkit.World;

/**
 * An adapter that is used to adapt multiple
 * economy implementations.
 */
public interface EconomyAdapter {
	/**
	 * Attempts to deposit money to an account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to depositPlayer.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean depositPlayer(String accountName, double amount, World world);

	/**
	 * Attempts to withdraw money from an account.
	 *
	 * @param accountName The name of the account.
	 * @param amount The amount to depositPlayer.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean withdrawPlayer(String accountName, double amount, World world);

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
	void newPlayerAccount(String accountName);

	/**
	 * Removes an account.
	 * 
	 * @param accountName The name of the account to remove.
	 */
	void deletePlayerAccount(String accountName);

	/**
	 * Sets the balance of the account.
	 * 
	 * @param accountName The name of the account.
	 * @param amount The amount to depositPlayer.
	 * @param world The world this account is in.
	 * @return A boolean indicating success.
	 */
	boolean setBalance(String accountName, double amount, World world);

	/**
	 * Get's the proper formatting for a given balance.
	 * 
	 * @param balance The balance to format.
	 * @return A string with the balance formatted.
	 */
	String getFormattedBalance(double balance);

	/**
	 * Whether the economy has support for banks or not.
	 * 
	 * @return true for bank support, false otherwise.
	 */
	boolean hasBankSupport();

	/**
	 * Creates a bank account with the specified name and the player as the owner
	 * 
	 * @param name of account
	 * @param player the account should be linked to
	 * @return A boolean indicating success.
	 */
	default boolean newBank(String name, OfflinePlayer player) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Deletes a bank account with the specified name.
	 * @param name of the back to delete
	 *                
	 * @return A boolean indicating success.
	 */
	default boolean deleteBank(String name) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Returns the amount the bank has.
	 * 
	 * @param name of the account
	 * @return A boolean indicating success.
	 */
	default double getBankBalance(String name) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Sets the balance of the given bank.
	 * 
	 * @param bankName The name of the bank.
	 * @param amount The amount to set the balance at.
	 * @return A boolean indicating success.
	 */
	default boolean setBankBalance(String bankName, double amount) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Returns true or false whether the bank has the amount specified - DO NOT USE NEGATIVE AMOUNTS
	 *
	 * @param name of the account
	 * @param amount to check for
	 * @return A boolean indicating success.
	 */
	default boolean bankHas(String name, double amount) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Withdraw an amount from a bank account - DO NOT USE NEGATIVE AMOUNTS
	 *
	 * @param name of the account
	 * @param amount to withdraw
	 * @return A boolean indicating success.
	 */
	default boolean bankWithdraw(String name, double amount) {
		throw new UnsupportedOperationException("Not yet implemented");
	}

	/**
	 * Deposit an amount into a bank account - DO NOT USE NEGATIVE AMOUNTS
	 *
	 * @param name of the account
	 * @param amount to deposit
	 * @return A boolean indicating success.
	 */
	default boolean bankDeposit(String name, double amount) {
		throw new UnsupportedOperationException("Not yet implemented");
	}
}
