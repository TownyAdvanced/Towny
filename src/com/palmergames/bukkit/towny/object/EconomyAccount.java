package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import org.bukkit.World;

/**
 * Economy object which provides an interface with the Economy Handler.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public class EconomyAccount extends Account {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();
	private World world;
	
	protected EconomyAccount(String name, World world) {
		super(name);
		this.world = world;
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(getName(), amount, world);
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}

	protected EconomyAccount(String name) {
		super(name);
	}

	public World getWorld() {
		return world;
	}
	
//	/**
//	 * When one account is paying another account(Taxes/Plot Purchasing)
//	 *
//	 * @param amount currency to be collected
//	 * @param collector recipient of transaction
//	 * @param reason memo regarding transaction
//	 * @return true if successfully payed amount to collector.
//	 * @throws EconomyException if transaction fails
//	 */
//	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
//		return payTo(amount, collector.getAccount(), reason);
//	}
//	
//	public boolean payTo(double amount, EconomyAccount collector, String reason) throws EconomyException {
//		boolean payed = _payTo(amount, collector);
//		if (payed) {
//			TownyLogger.getInstance().logMoneyTransaction(this, amount, collector, reason);
//		}
//		return payed;
//	} 
//
//	private boolean _payTo(double amount, EconomyAccount collector) throws EconomyException {
//		if (_pay(amount)) {
//			if (!collector._collect(amount)) {
//				_collect(amount); //Transaction failed. Refunding amount.
//				return false;
//			} else {
//				return true;
//			}
//		} else {
//			return false;
//		}
//	}

//	/**
//	 * Fetch the current world for this object
//	 *
//	 * @return Bukkit world for the object
//	 */
//	protected World getBukkitWorld() {
//		return BukkitTools.getWorlds().get(0);
//	}
//
//	/**
//	 * Set balance and log this action
//	 *
//	 * @param amount currency to transact
//	 * @param reason memo regarding transaction
//	 * @return true, or pay/collect balance for given reason
//	 * @throws EconomyException if transaction fails
//	 */
//	public boolean setBalance(double amount, String reason) throws EconomyException {
//		double balance = getHoldingBalance();
//		double diff = amount - balance;
//		if (diff > 0) {
//			// Adding to
//			return collect(diff, reason);
//		} else if (balance > amount) {
//			// Subtracting from
//			diff = -diff;
//			return pay(diff, reason);
//		} else {
//			// Same amount, do nothing.
//			return true;
//		}
//	}
//
//	/*
//	private boolean _setBalance(double amount) {
//		return TownyEconomyHandler.setBalance(getEconomyName(), amount, getBukkitWorld());
//	}
//	*/
//
//	public double getHoldingBalance() throws EconomyException {
//		try {
//			return TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
//		} catch (NoClassDefFoundError e) {
//			e.printStackTrace();
//			throw new EconomyException("Economy error getting holdings for " + getName());
//		}
//	}
//
//	/**
//	 * Does this object have enough in it's economy account to pay?
//	 *
//	 * @param amount currency to check for
//	 * @return true if there is enough.
//	 * @throws EconomyException if failure
//	 */
//	public boolean canPayFromHoldings(double amount) throws EconomyException {
//		return TownyEconomyHandler.hasEnough(getName(), amount, getBukkitWorld());
//	}
//
//	/**
//	 * Used To Get Balance of Players holdings in String format for printing
//	 *
//	 * @return current account balance formatted in a string.
//	 */
//	public String getHoldingFormattedBalance() {
//		try {
//			return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
//		} catch (EconomyException e) {
//			return "Error Accessing Bank Account";
//		}
//	}
//
//	/**
//	 * Attempt to delete the economy account.
//	 */
//	public void removeAccount() {
//		TownyEconomyHandler.removeAccount(getName());
//	}

}
