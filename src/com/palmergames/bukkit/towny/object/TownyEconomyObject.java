package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.World;

/**
 * Economy object which provides an interface with the Economy Handler.
 * 
 * @author ElgarL, Shade
 * 
 */
public class TownyEconomyObject extends TownyObject {

	private static final class TownyServerAccount extends TownyEconomyObject {

		@Override
		public String getName() {

			return TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
		}
	}

	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();

	/**
	 * Tries to pay from the players holdings
	 * 
	 * @param amount
	 * @param reason
	 * @return true if successfull
	 * @throws EconomyException
	 */
	public boolean pay(double amount, String reason) throws EconomyException {

		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return payTo(amount, SERVER_ACCOUNT, reason);
		} else {
			boolean payed = _pay(amount);
			if (payed)
				TownyLogger.logMoneyTransaction(this, amount, null, reason);
			return payed;
		}
	}

	private boolean _pay(double amount) throws EconomyException {

		if (canPayFromHoldings(amount)) {
			if (TownyEconomyHandler.isActive())
				if (amount > 0) {
					return TownyEconomyHandler.subtract(getEconomyName(), amount, getBukkitWorld());
				} else {
					return TownyEconomyHandler.add(getEconomyName(), Math.abs(amount), getBukkitWorld());
				}
		}
		return false;
	}

	/**
	 * When collecting money add it to the Accounts bank
	 * 
	 * @param amount
	 * @param reason
	 * @throws EconomyException
	 */
	public boolean collect(double amount, String reason) throws EconomyException {

		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return SERVER_ACCOUNT.payTo(amount, this, reason);
		} else {
			boolean collected = _collect(amount);
			if (collected)
				TownyLogger.logMoneyTransaction(null, amount, this, reason);
			return collected;
		}
	}

	private boolean _collect(double amount) throws EconomyException {

		return TownyEconomyHandler.add(getEconomyName(), amount, getBukkitWorld());
	}

	/**
	 * When one account is paying another account(Taxes/Plot Purchasing)
	 * 
	 * @param amount
	 * @param collector
	 * @param reason
	 * @return true if successfully payed amount to collector.
	 * @throws EconomyException
	 */
	public boolean payTo(double amount, TownyEconomyObject collector, String reason) throws EconomyException {

		boolean payed = _payTo(amount, collector);
		if (payed)
			TownyLogger.logMoneyTransaction(this, amount, collector, reason);
		return payed;
	}

	private boolean _payTo(double amount, TownyEconomyObject collector) throws EconomyException {

		if (_pay(amount)) {
			if (!collector._collect(amount)) {
				_collect(amount); //Transaction failed. Refunding amount.
				return false;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	/**
	 * Get a valid economy account name for this object.
	 * 
	 * @return account name
	 */
	public String getEconomyName() {

		return getName();
	}

	/**
	 * Fetch the current world for this object
	 * 
	 * @return Bukkit world for the object
	 */
	protected World getBukkitWorld() {

		return BukkitTools.getWorlds().get(0);
	}

	/**
	 * Set balance and log this action
	 * 
	 * @param amount
	 * @param reason
	 */
	public boolean setBalance(double amount, String reason) throws EconomyException {

		double balance = getHoldingBalance();
		double diff = amount - balance;
		if (diff > 0) {
			// Adding to
			return collect(diff, reason);
		} else if (balance > amount) {
			// Subtracting from
			diff = -diff;
			return pay(diff, reason);
		} else {
			// Same amount, do nothing.
			return true;
		}
	}

	/*
	private boolean _setBalance(double amount) {
		return TownyEconomyHandler.setBalance(getEconomyName(), amount, getBukkitWorld());
	}
	*/

	public double getHoldingBalance() throws EconomyException {

		try {
			return TownyEconomyHandler.getBalance(getEconomyName(), getBukkitWorld());
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getEconomyName());
		}
	}

	/**
	 * Does this object have enough in it's economy account to pay?
	 * 
	 * @param amount
	 * @return true if there is enough.
	 * @throws EconomyException
	 */
	public boolean canPayFromHoldings(double amount) throws EconomyException {

		return TownyEconomyHandler.hasEnough(getEconomyName(), amount, getBukkitWorld());
	}

	/**
	 * Used To Get Balance of Players holdings in String format for printing
	 * 
	 * @return current account balance formatted in a string.
	 */
	public String getHoldingFormattedBalance() {

		try {
			return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
		} catch (EconomyException e) {
			return "Error Accessing Bank Account";
		}
	}

	/**
	 * Attempt to delete the economy account.
	 */
	public void removeAccount() {

		TownyEconomyHandler.removeAccount(getEconomyName());

	}

}
