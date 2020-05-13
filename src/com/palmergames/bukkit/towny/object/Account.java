package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.World;

public abstract class Account implements Nameable {
	String name;
	World world;
	
	Account(String name) {
		this.name = name;
	}
	
	Account(String name, World world) {
		this.name = name;
		this.world = world;
	}
	
	public boolean add(double amount, String reason) throws EconomyException {
		return TownyEconomyHandler.add(getName(), amount, world);
	}
	
	public boolean subtract(double amount, String reason) throws EconomyException {
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}
	
	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
		return payTo(amount, collector.getAccount(), reason);
	}

	public boolean payTo(double amount, Account collector, String reason) throws EconomyException {
		
		if (amount > getHoldingBalance()) {
			throw new EconomyException("Not enough money");
		}
		
		if (!subtract(amount, reason)) {
			return false;
		}
		return collector.add(amount, reason);
	}

	/**
	 * Fetch the current world for this object
	 *
	 * @return Bukkit world for the object
	 */
	public World getBukkitWorld() {
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
			return add(diff, reason);
		} else if (balance > amount) {
			// Subtracting from
			diff = -diff;
			return subtract(diff, reason);
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

	@Override
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
