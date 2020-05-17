package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
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
 * @see CappedAccount
 * @see EconomyAccount
 */
public abstract class Account implements Nameable {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();
	private static final AccountObserver ACCOUNT_OBSERVER = new AccountAuditor();
	private final List<AccountObserver> auditors = new ArrayList<>();
	String name;
	World world;
	
	
	Account(String name) {
		this.name = name;
	}
	
	Account(String name, World world) {
		this.name = name;
		this.world = world;
		
		// ALL account transactions will route auditing data through this
		// central auditor.
		auditors.add(ACCOUNT_OBSERVER);
	}
	
	boolean canAdd(double amount) throws EconomyException {
		return true;
	}
	
	boolean canSubtract(double amount) throws EconomyException {
		return !(amount > getHoldingBalance());
	}
	
	private boolean addMoney(double amount) throws EconomyException {
		return TownyEconomyHandler.add(getName(), amount, world);
	}
	
	private boolean subtractMoney(double amount) throws EconomyException {
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}

	/**
	 * Attempts to add money to the account.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public final boolean add(double amount, String reason) throws EconomyException {
		if (canAdd(amount) && addMoney(amount)) {
			notifyAuditorsDeposit(this, amount, reason);
			return true;
		}
		
		return false;
	}

	/**
	 * Attempts to subtract money from the account.
	 *
	 * @param amount The amount to subtract.
	 * @param reason The reason for subtracting.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public final boolean subtract(double amount, String reason) throws EconomyException {
		if (TownySettings.getBoolean(ConfigNodes.ECO_CLOSED_ECONOMY_ENABLED)) {
			return payTo(amount, SERVER_ACCOUNT, reason);
		} else {
			if (canSubtract(amount) && subtractMoney(amount)) {
				notifyAuditorsWithdraw(this, amount, reason);
				return true;
			}

			return false;
		}
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay. 
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	public boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException {
		return payTo(amount, collector.getAccount(), reason);
	}

	/**
	 * Pays another account the specified funds.
	 *
	 * @param amount The amount to pay.
	 * @param collector The account to pay.
	 * @param reason The reason for the pay.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
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

	/**
	 * Gets the current balance of this account.
	 * 
	 * @return The amount in this account.
	 * @throws EconomyException On an economy error.
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

	public List<AccountObserver> getAuditors() {
		return Collections.unmodifiableList(auditors);
	}
	
	private final void notifyAuditorsDeposit(Account account, double amount, String reason) {
		for (AccountObserver observer : getAuditors()) {
			observer.deposited(account, amount, reason);
		}
	}

	private final void notifyAuditorsWithdraw(Account account, double amount, String reason) {
		for (AccountObserver observer : getAuditors()) {
			observer.withdrew(account, amount, reason);
		}
	}
	
	public final void addAuditor(AccountObserver auditor) {
		auditors.add(auditor);
	}
	
	public final void removeAuditor(AccountObserver auditor) {
		auditors.remove(auditor);
	}

	private static final class TownyServerAccount extends Account {
		TownyServerAccount() {
			super(TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT));
		}
	}
}
