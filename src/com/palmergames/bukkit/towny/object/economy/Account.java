package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.Nameable;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Bukkit;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Used to facilitate transactions regarding money, 
 * and the storage of funds.
 * 
 * @author Suneet Tipirneni (Siris)
 * @see BankAccount
 * @see EconomyAccount
 */
public abstract class Account implements Nameable {
	private static final long CACHE_TIMEOUT = TownySettings.getCachedBankTimeout();
	private static final AccountObserver GLOBAL_OBSERVER = new GlobalAccountObserver();
	private final List<AccountObserver> observers = new ArrayList<>();
	private AccountAuditor auditor;
	protected CachedBalance cachedBalance = null;
	
	String name;
	World world;
	
	public Account(String name) {
		this.name = name;
		observers.add(GLOBAL_OBSERVER);
		this.cachedBalance = new CachedBalance(getHoldingBalance(false));
	}
	
	public Account(String name, World world) {
		this.name = name;
		this.world = world;
		
		// ALL account transactions will route auditing data through this
		// central auditor.
		observers.add(GLOBAL_OBSERVER);
		this.cachedBalance = new CachedBalance(getHoldingBalance(false));
	}
	
	// Template methods
	protected abstract boolean addMoney(double amount);
	protected abstract boolean subtractMoney(double amount);

	/**
	 * Attempts to add money to the account, 
	 * and notifies account observers of any changes.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 */
	public boolean deposit(double amount, String reason) {
		if (addMoney(amount)) {
			notifyObserversDeposit(this, amount, reason);
			if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED))
				return payFromServer(amount, reason);

			return true;
		}
		
		return false;
	}

	/**
	 * Attempts to withdraw money from the account, 
	 * and notifies account observers of any changes.
	 *
	 * @param amount The amount to withdraw.
	 * @param reason The reason for subtracting.
	 * @return boolean indicating success.
	 */
	public boolean withdraw(double amount, String reason) {
		if (subtractMoney(amount)) {
			notifyObserversWithdraw(this, amount, reason);
			if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED))
				return payToServer(amount, reason);

			return true;
		}
		
		return false;
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay. 
	 * @return boolean indicating success.
	 */
	public boolean payTo(double amount, EconomyHandler collector, String reason) {
		return payTo(amount, collector.getAccount(), reason);
	}
	
	protected boolean payToServer(double amount, String reason) {
		
		// Put it back into the server.
		return TownyEconomyHandler.addToServer(amount, getBukkitWorld());
	}
	
	protected boolean payFromServer(double amount, String reason) {
		
		// Remove it from the server economy.
		return TownyEconomyHandler.subtractFromServer(amount, getBukkitWorld());
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay.
	 * @return boolean indicating success.
	 */
	public boolean payTo(double amount, Account collector, String reason) {
		
		if (amount > getHoldingBalance()) {
			return false;
		}

		return withdraw(amount, reason) && collector.deposit(amount, reason);
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
	 */
	public boolean setBalance(double amount, String reason) {
		double balance = getHoldingBalance();
		double diff = amount - balance;
		if (diff > 0) {
			// Adding to
			return deposit(diff, reason);
		} else if (balance > amount) {
			// Subtracting from
			diff = -diff;
			return withdraw(diff, reason);
		} else {
			// Same amount, do nothing.
			return true;
		}
	}

	public double getHoldingBalance() {
		return getHoldingBalance(true);
	}
	
	/**
	 * Gets the current balance of this account.
	 * 
	 * @param setCache when True the account will have its cachedbalance set.
	 * @return The amount in this account.
	 */
	public double getHoldingBalance(boolean setCache) {
		double balance = TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
		if (setCache)
			cachedBalance.setBalance(balance);
		return balance;
	}

	/**
	 * Does this object have enough in it's economy account to pay?
	 *
	 * @param amount currency to check for
	 * @return true if there is enough.
	 */
	public boolean canPayFromHoldings(double amount) {
		return TownyEconomyHandler.hasEnough(getName(), amount, getBukkitWorld());
	}

	/**
	 * Used To Get Balance of Players holdings in String format for printing
	 *
	 * @return current account balance formatted in a string.
	 */
	public String getHoldingFormattedBalance() {
		return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
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

	/**
	 * Gets the observers of this account.
	 * 
	 * @return A list of account observers.
	 */
	public List<AccountObserver> getObservers() {
		return Collections.unmodifiableList(observers);
	}
	
	private void notifyObserversDeposit(Account account, double amount, String reason) {
		for (AccountObserver observer : getObservers()) {
			observer.deposited(account, amount, reason);
		}
	}

	private void notifyObserversWithdraw(Account account, double amount, String reason) {
		for (AccountObserver observer : getObservers()) {
			observer.withdrew(account, amount, reason);
		}
	}

	/**
	 * Adds an account observer that listens to account changes.
	 * 
	 * @param observer The observer to add.
	 */
	public final void addObserver(AccountObserver observer) {
		observers.add(observer);
	}

	/**
	 * Removes an account observer that listens to account changes.
	 *
	 * @param observer The observer to remove.
	 */
	public final void removeObserver(AccountObserver observer) {
		observers.remove(observer);
	}

	/**
	 * Gets the auditor that audits this account.
	 * 
	 * @return The auditor tracking this account.
	 */
	public final AccountAuditor getAuditor() {
		return auditor;
	}

	/**
	 * Sets the auditor that audits this account, and
	 * adds it as an observer.
	 *
	 * @param auditor The auditor to track this account.
	 */
	public final void setAuditor(AccountAuditor auditor) {
		this.auditor = auditor;
		
		// Add the auditor to the observer list.
		addObserver(auditor);
	}
	
	class CachedBalance {
		private double balance = 0;
		private long time;

		CachedBalance(double _balance) {
			balance = _balance;
			time = System.currentTimeMillis();
		}

		double getBalance() {
			return balance;
		}
		
		boolean isStale() {
			return System.currentTimeMillis() - time > CACHE_TIMEOUT;
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
	 * Returns a cached balance of an {@link Account}, the value of which can be
	 * brand new or up to 10 minutes old (time configurable in the config,) based on
	 * whether the cache has been checked recently.
	 *
	 * @return balance {@link Double} which is from a {@link CachedBalance#balance}.
	 */
	public double getCachedBalance() {
		if (cachedBalance.isStale())
			cachedBalance.updateCache();

		return cachedBalance.getBalance();
	}
}
