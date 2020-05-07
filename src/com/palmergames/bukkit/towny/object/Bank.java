package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.TownyException;

/**
 * Allows objects to manage a self contained bank.
 *
 * These methods are more of convenience or utility methods.
 */
public interface Bank extends EconomyHandler {
	/**
	 * Takes money from object bank account and gives it to a resident.
	 * 
	 * @param resident The resident to pay to.
	 * @param amount The amount to pay.
	 * @throws EconomyException When there's a payment error.
	 * @throws TownyException When an economy is not available.
	 */
	default void withdrawFromBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));
		}
		if (!getAccount().payTo(amount, resident, getName() + " - " + " Withdraw")) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_money"));
		}
	}

	/**
	 * Takes money from the resident and puts it into the object's bank.
	 * 
	 * @param resident The resident making the deposit.
	 * @param amount The amount to deposit into a bank.
	 * @throws EconomyException When there's a payment error.
	 * @throws TownyException When an economy is not available.
	 */
	default void depositToBank(Resident resident, int amount) throws EconomyException, TownyException {
		if (!TownySettings.isUsingEconomy()) {
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));
		}
		if (!resident.getAccount().payTo(amount, getAccount(), "Territory - " + getName() + " deposit")) {
			throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));
		}
	}
}
