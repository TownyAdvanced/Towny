package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.Nameable;
import org.bukkit.World;

public interface Account extends Nameable {
    /**
     * Attempts to depositPlayer money to the account, 
     * and notifies account observers of any changes.
     * 
     * @param amount The amount to depositPlayer.
     * @param reason The reason for adding.
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    boolean deposit(double amount, String reason) throws EconomyException;

    /**
     * Attempts to withdraw money from the account, 
     * and notifies account observers of any changes.
     *
     * @param amount The amount to withdraw.
     * @param reason The reason for subtracting.
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    boolean withdraw(double amount, String reason) throws EconomyException;

    /**
     * Pays another account the specified funds.
     *
     * @param amount The amount to pay.
     * @param collector The account to pay.
     * @param reason The reason for the pay. 
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    boolean payTo(double amount, EconomyHandler collector, String reason) throws EconomyException;

    /**
     * Pays another account the specified funds.
     *
     * @param amount The amount to pay.
     * @param collector The account to pay.
     * @param reason The reason for the pay.
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    boolean payTo(double amount, Account collector, String reason) throws EconomyException;

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
	
	protected boolean payToServer(double amount, String reason) throws EconomyException {
		// Take money out.
		TownyEconomyHandler.subtract(getName(), amount, getBukkitWorld());
		
		// Put it back into the server.
		return TownyEconomyHandler.addToServer(amount, getBukkitWorld());
	}
	
	protected boolean payFromServer(double amount, String reason) throws EconomyException {
		// Put money in.
		TownyEconomyHandler.add(getName(), amount, getBukkitWorld());
		
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
	 * @throws EconomyException On an economy error.
	 */
	public boolean payTo(double amount, Account collector, String reason) throws EconomyException {
		
		if (amount > getHoldingBalance()) {
			return false;
		}

    /**
     * Gets the current balance of this account.
     * 
     * @return The amount in this account.
     * @throws EconomyException On an economy error.
     */
    double getHoldingBalance() throws EconomyException;

    /**
     * Does this object have enough in it's economy account to pay?
     *
     * @param amount currency to check for
     * @return true if there is enough.
     * @throws EconomyException if failure
     */
    boolean canPayFromHoldings(double amount) throws EconomyException;

    /**
     * Used To Get Balance of Players holdings in String format for printing
     *
     * @return current account balance formatted in a string.
     */
    String getHoldingFormattedBalance();

    /**
     * Attempt to delete the economy account.
     */
    void removeAccount();

    void setName(String name);

    /**
     * @deprecated As of 0.96.1.11, use {@link #deposit(double, String)} instead.
     * 
     * @param amount The amount to depositPlayer.
     * @param reason The reason for adding.
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    @Deprecated
    boolean collect(double amount, String reason) throws EconomyException;

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
	
	// Legacy Compatibility Methods.
	
	/**
	 * @deprecated As of 0.96.1.11, use {@link #deposit(double, String)} instead.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean collect(double amount, String reason) throws EconomyException {
		return deposit(amount, reason);
	}
	
	/**
	 * @deprecated As of 0.96.1.11, use {@link #withdraw(double, String)} instead.
	 *
	 * @param amount The amount to subtract.
	 * @param reason The reason for subcracting.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean pay(double amount, String reason) throws EconomyException {
		return withdraw(amount, reason);
	}
	
	// Legacy Compatibility Methods.

	/**
	 * @deprecated As of 0.96.1.11, use {@link #deposit(double, String)} instead.
	 * 
	 * @param amount The amount to add.
	 * @param reason The reason for adding.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean collect(double amount, String reason) throws EconomyException {
		return deposit(amount, reason);
	}
	
	/**
	 * @deprecated As of 0.96.1.11, use {@link #withdraw(double, String)} instead.
	 *
	 * @param amount The amount to subtract.
	 * @param reason The reason for subcracting.
	 * @return boolean indicating success.
	 * @throws EconomyException On an economy error.
	 */
	@Deprecated
	public boolean pay(double amount, String reason) throws EconomyException {
		return withdraw(amount, reason);
	}

}
