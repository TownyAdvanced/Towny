package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Town;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * A variant of an account that implements
 * a checked cap on it's balance, as well as a 
 * debt system.
 */
public class BankAccount extends Account {
	
	private static final long CACHE_TIMEOUT = TownySettings.getCachedBankTimeout();
	private double balanceCap;
	private double debtCap;
	private CachedBalance cachedBalance = null;
	private Government government;

	public BankAccount(UUID uuid, World world, double balanceCap, Government government) {
		super(uuid, world);
		this.balanceCap = balanceCap;
		this.government = government;
		this.cachedBalance = new CachedBalance(getHoldingBalance());

	}


	@Override
	public String getName() {
		return government.getName();
	}
	
	public UUID getUUID() {
		return government.getUUID();
	}
	
	public World getBukkitWorld() {
		return world;
	}

	/*
	 * BankAccount Balance Methods.
	 */
	
	@Override
	protected boolean subtractMoney(double amount) {
		if (isBankrupt() && (getTownDebt() + amount > getDebtCap()))
			return false;  //subtraction not allowed as it would exceed the debt cap

		if (isBankrupt())
			return addDebt(amount);

		if (!canPayFromHoldings(amount)) {

			// Calculate debt.
			double amountInDebt = amount - getHoldingBalance();

			if(amountInDebt <= getDebtCap()) {
				// Empty out account.
				boolean success = TownyEconomyHandler.setBalance(government, 0, world);
				success &= addDebt(amountInDebt);

				return success;
			} else {
				return false; //Subtraction not allowed as it would exceed the debt cap
			}
		}

		// Otherwise continue like normal.
		return TownyEconomyHandler.subtract(government, amount, world);
	}

	@Override
	protected boolean addMoney(double amount) {
		// Check balance cap.
		if (balanceCap != 0 && (getHoldingBalance() + amount > balanceCap))
			return false;
		
		if (isBankrupt())
			return removeDebt(amount);

		// Otherwise continue like normal.
		return TownyEconomyHandler.add(government, amount, world);
	}

	@Override
	public double getHoldingBalance() {
		if (isBankrupt()) {
			return getTownDebt() * -1;
		}
		return TownyEconomyHandler.getBalance(government, world);
	}

	@Override
	public String getHoldingFormattedBalance() {
		if (isBankrupt()) {
			return "-" + TownyEconomyHandler.getFormattedBalance(getTownDebt());
		}
		return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
	}
	
	@Override
	public boolean canPayFromHoldings(double amount) {
		return TownyEconomyHandler.hasEnough(government, amount, world);
	}

	@Override
	protected boolean payToServer(double amount, String reason) {
		// Take money out.
		TownyEconomyHandler.subtract(government, amount, world);
		
		// Put it back into the server.
		return TownyEconomyHandler.addToServer(amount, world);
	}
	
	@Override
	protected boolean payFromServer(double amount, String reason) {
		// Put money in.
		TownyEconomyHandler.add(government, amount, world);
		
		// Remove it from the server economy.
		return TownyEconomyHandler.subtractFromServer(amount, world);
	}
	
	@Override
	public void removeAccount() {
		TownyEconomyHandler.removeAccount(government);
	}

	/*
	 * Town BankAccount Debt Methods.
	 */

	/**
	 * Whether the account is in debt or not.
	 * 
	 * @return true if in debt, false otherwise.
	 */
	public boolean isBankrupt() {
		if (isTownAccount())
			return getTown().isBankrupt();
		return false;
	}

	/**
	 * The maximum amount of debt this account can have.
	 * 
	 * Can be overriden by the config debt_cap override.
	 * Can be overriden by the config debt_cap maximum. 
	 * 
	 * @return The max amount of debt for this account.
	 */
	public double getDebtCap() {
		if (TownySettings.isDebtCapDeterminedByTownLevel()) { // town_level debtCapModifier * debt_cap.override.
			String townName = TownyUniverse.getInstance().getTown(getUUID()).getName();
			Town town = getTown();
			
			// For whatever reason, this just errors and continues
			if (town == null) {
				TownyMessaging.sendErrorMsg(String.format("Error fetching debt cap for town %s because town is not registered!", townName));
			}
			
			return Double.parseDouble(TownySettings.getTownLevel(town).get(TownySettings.TownLevel.DEBT_CAP_MODIFIER).toString()) * TownySettings.getDebtCapOverride();
		}
		
		if (TownySettings.getDebtCapOverride() != 0.0)
			return TownySettings.getDebtCapOverride();
		
		if (TownySettings.getDebtCapMaximum() != 0.0)
			return Math.min(debtCap, TownySettings.getDebtCapMaximum());
		
		return debtCap;
	}
	
	/**
	 * return true if this BankAcount is one belonging to a Town.
	 */
	private boolean isTownAccount() {
		return TownyUniverse.getInstance().getTown(getUUID()) != null;
	}
	
	/**
	 * @return town or null, if this BankAccount does not belong to a town.
	 */
	@Nullable
	private Town getTown() {
		Town town = null;
		if (isTownAccount()) 
			town = TownyUniverse.getInstance().getTown(getUUID());
		return town;
	}
	
	private double getTownDebt() {
		return getTown().getDebtBalance();
	}
	
	private void setTownDebt(double amount) {
		getTown().setDebtBalance(amount);
		getTown().save();
	}

	/**
	 * Adds debt to a Town debtBalance
	 * @param amount the amount to add to the debtBalance.
	 * @return true if the BankAccount is a town bank account.
	 */
	private boolean addDebt(double amount) {
		if (isTownAccount()) {
			setTownDebt(getTownDebt() + amount);
			return true;
		}
		return false;
	}
	
	/**
	 * @param amount the amount to remove from the debtBalance.
	 * @return true always.
	 */
	private boolean removeDebt(double amount) {
		if (getTownDebt() < amount) {
			// Calculate money to go into regular account.
			double netMoney = amount - getTownDebt();
			//Clear debt account
			setTownDebt(0.0);
			// Sometimes there's money in the bank account 
			// (from a player manually putting money in via
			// eco plugin, maybe.)
			double bankBalance = TownyEconomyHandler.getBalance(government, world);
			//Set positive balance in regular account
			TownyEconomyHandler.setBalance(government, bankBalance + netMoney, world);
			return true;
		} else {
			setTownDebt(getTownDebt() - amount);
			return true;
		}
	}

	/**
	 * Sets the maximum amount of debt this account can have.
	 * 
	 * @param debtCap The new cap for debt on this account.
	 */
	public void setDebtCap(double debtCap) {
		this.debtCap = debtCap;
	}

	
	/*
	 * Caching System
	 */
	
	private class CachedBalance {
		private double balance = 0;
		private long time;
		
		private CachedBalance(double _balance) {
			balance = _balance;
			time = System.currentTimeMillis();
		}
		
		double getBalance() {
			return balance;
		}
		private long getTime() {
			return time;
		}
		
		void setBalance(double _balance) {
			balance = _balance;
			time = System.currentTimeMillis();
		}
		
		void updateCache() {
			if (!TownySettings.isEconomyAsync()) // Some economy plugins don't handle things async, luckily we have a config option for this such case. 
				setBalance(getHoldingBalance());
			else 
				Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), () -> cachedBalance.setBalance(getHoldingBalance()));			
		}
	}
	
	/**
	 * Returns a cached balance of a town or nation bank account,
	 * the value of which can be brand new or up to 10 minutes old 
	 * (time configurable in the config,) based on whether the 
	 * cache has been checked recently.
	 * 
	 * @return a cached balance of a town or nation bank account.
	 */
	public double getCachedBalance() {
		if (System.currentTimeMillis() - cachedBalance.getTime() > CACHE_TIMEOUT)
			cachedBalance.updateCache();

		return cachedBalance.getBalance();
	}


	/*
	 * Balance Cap Methods
	 */
	
	/**
	 * Sets the max amount of money allowed in this account.
	 * 
	 * @param balanceCap The max amount allowed in this account.
	 */
	public void setBalanceCap(double balanceCap) {
		this.balanceCap = balanceCap;
	}

	/**
	 * Sets the maximum amount of money this account can have.
	 *
	 * @return the max amount allowed in this account.
	 */
	public double getBalanceCap() {
		return balanceCap;
	}
}
