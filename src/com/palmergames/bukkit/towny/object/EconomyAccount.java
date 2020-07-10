package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.World;

import java.io.File;
import java.util.UUID;

/**
 * Economy object which provides an interface with the Economy Handler.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public class EconomyAccount implements Nameable {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();
	private World world;
	private String name;

	protected EconomyAccount(String name) {
		this.name = name;
	}
	
	protected EconomyAccount(String name, World world) {
		this(name);
		this.world = world;
	}
	
	public World getWorld() {
		return world;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	private static final class TownyServerAccount extends EconomyAccount {
		TownyServerAccount() {
			super(TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT));
		}
	}

	/**
	 * Tries to pay from the players holdings
	 *
	 * @param amount value to deduct from the player's account
	 * @param reason leger memo stating why amount is deducted
	 * @return true if successful
	 * @throws EconomyException if the transaction fails
	 */
	public boolean pay(double amount, String reason) throws EconomyException {
		TownyMessaging.sendErrorMsg("Account Name = " + getName());
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return payTo(amount, SERVER_ACCOUNT, reason);
		} else {
			boolean payed = _pay(amount);
			if (payed) {
				TownyLogger.getInstance().logMoneyTransaction(this, amount, null, reason);
			}
			return payed;
		}
	}

	boolean _pay(double amount) throws EconomyException {
		if (canPayFromHoldings(amount)) {
			if (TownyEconomyHandler.isActive())
				if (amount > 0) {
					return TownyEconomyHandler.subtract(getName(), amount, getBukkitWorld());
				} else {
					return TownyEconomyHandler.add(getName(), Math.abs(amount), getBukkitWorld());
				}
		}
		return false;
	}

	/**
	 * When collecting money add it to the Accounts bank
	 *
	 * @param amount currency to collect
	 * @param reason memo regarding transaction
	 * @return collected or pay to server account   
	 * @throws EconomyException if transaction fails
	 */
	public boolean collect(double amount, String reason) throws EconomyException {
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return SERVER_ACCOUNT.payTo(amount, this, reason);
		} else {
			boolean collected = _collect(amount);
			if (collected) {
				TownyLogger.getInstance().logMoneyTransaction(null, amount, this, reason);
			}
				
			return collected;
		}
	}

	private boolean _collect(double amount) throws EconomyException {
		return TownyEconomyHandler.add(getName(), amount, getBukkitWorld());
	}

	/**
	 * When one account is paying another account(Taxes/Plot Purchasing)
	 *
	 * @param amount currency to be collected
	 * @param collector recipient of transaction
	 * @param reason memo regarding transaction
	 * @return true if successfully payed amount to collector.
	 * @throws EconomyException if transaction fails
	 */
	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
		return payTo(amount, collector.getAccount(), reason);
	}
	
	public boolean payTo(double amount, EconomyAccount collector, String reason) throws EconomyException {
		boolean payed = _payTo(amount, collector);
		if (payed) {
			TownyLogger.getInstance().logMoneyTransaction(this, amount, collector, reason);
		}
		return payed;
	} 

	private boolean _payTo(double amount, EconomyAccount collector) throws EconomyException {
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
	 * @param amount currency to transact
	 * @param reason memo regarding transaction
	 * @return true, or pay/collect balance for given reason
	 * @throws EconomyException if transaction fails
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
			return TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getName());
		}
	}

	/**
	 * Does this object have enough in it's economy account to pay?
	 *
	 * @param amount currency to check for
	 * @return true if there is enough.
	 * @throws EconomyException if failure
	 */
	public boolean canPayFromHoldings(double amount) throws EconomyException {
		return TownyEconomyHandler.hasEnough(getName(), amount, getBukkitWorld());
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
		TownyEconomyHandler.removeAccount(getName());
	}

}
