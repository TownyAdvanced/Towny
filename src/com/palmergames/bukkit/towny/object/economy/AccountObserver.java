package com.palmergames.bukkit.towny.object.economy;

/**
 * A class which facilitates the observance
 * of transactions within economy accounts.
 */
public interface AccountObserver {
	/**
	 * Called whenever an account money is withdrawn from an account.
	 * 
	 * @param account The account withdrew from.
	 * @param amount The amount withdrew.
	 * @param reason The reason for withdrawing.
	 */
	void withdrew(Account account, double amount, String reason);

	/**
	 * Called whenever an account money is deposited to an account.
	 *
	 * @param account The account deposited to.
	 * @param amount The amount deposited.
	 * @param reason The reason for depositing.
	 */
	void deposited(Account account, double amount, String reason);
}
