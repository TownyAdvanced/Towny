package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Used to facilitate transactions regarding money, 
 * and the storage of funds.
 * 
 * @author Suneet Tipirneni (Siris)
 * @see Bank
 * @see EconomyAccount
 */
public abstract class AbstractAccount implements Account {
	private static final AccountObserver GLOBAL_OBSERVER = new GlobalAccountObserver();
	private final List<AccountObserver> observers = new ArrayList<>();
	private AccountAuditor auditor;
	
	String name;
	World world;
	
	public AbstractAccount(String name) {
		this.name = name;
		observers.add(GLOBAL_OBSERVER);
	}
	
	public AbstractAccount(String name, World world) {
		this.name = name;
		this.world = world;
		
		// ALL account transactions will route auditing data through this
		// central auditor.
		observers.add(GLOBAL_OBSERVER);
	}
	
	// Template methods
	protected abstract boolean addMoney(double amount);
	protected abstract boolean subtractMoney(double amount);

	@Override
	public boolean deposit(double amount, String reason) throws EconomyException {
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return payFromServer(amount, reason);
		}
		if (addMoney(amount)) {
			notifyObserversDeposit(this, amount, reason);
			return true;
		}
		
		return false;
	}

	@Override
	public boolean withdraw(double amount, String reason) throws EconomyException {
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return payToServer(amount, reason);
		}
		if (subtractMoney(amount)) {
			notifyObserversWithdraw(this, amount, reason);
			return true;
		}
		
		return false;
	}

	@Override
	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
		return payTo(amount, collector.getAccount(), reason);
	}
	
	protected boolean payToServer(double amount, String reason) throws EconomyException {
		// Take money out.
		withdraw(amount, reason);
		
		// Put it back into the server.
		return TownyEconomyHandler.addToServer(amount, getBukkitWorld());
	}
	
	protected boolean payFromServer(double amount, String reason) throws EconomyException {
		// Put money in.
		deposit(amount, reason);
		
		// Remove it from the server economy.
		return TownyEconomyHandler.subtractFromServer(amount, getBukkitWorld());
	}

	@Override
	public boolean payTo(double amount, Account collector, String reason) throws EconomyException {
		
		if (amount > getHoldingBalance()) {
			return false;
		}

		return withdraw(amount, reason) && collector.deposit(amount, reason);
	}

	@Override
	public World getBukkitWorld() {
		return BukkitTools.getWorlds().get(0);
	}

	@Override
	public boolean setBalance(double amount, String reason) throws EconomyException {
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

	@Override
	public double getHoldingBalance() throws EconomyException {
		try {
			return TownyEconomyHandler.getPlayerBalance(getName(), getBukkitWorld());
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getName());
		}
	}

	@Override
	public boolean canPayFromHoldings(double amount) throws EconomyException {
		return TownyEconomyHandler.playerHasEnough(getName(), amount, getBukkitWorld());
	}

	@Override
	public String getHoldingFormattedBalance() {
		try {
			return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
		} catch (EconomyException e) {
			return "Error Accessing Bank AbstractAccount";
		}
	}

	@Override
	public void removeAccount() {
		TownyEconomyHandler.removeAccount(getName());
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
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
	 * @param observer The observer to depositPlayer.
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
	
	// Legacy Compatibility Methods.
	
	@Override
	@Deprecated
	public boolean collect(double amount, String reason) throws EconomyException {
		return deposit(amount, reason);
	}
	
	@Override
	@Deprecated
	public boolean pay(double amount, String reason) throws EconomyException {
		return withdraw(amount, reason);
	}
}
