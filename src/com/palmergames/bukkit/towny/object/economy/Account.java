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
     * Fetch the current world for this object
     *
     * @return Bukkit world for the object
     */
    World getBukkitWorld();

    /**
     * Set balance and log this action
     *
     * @param amount currency to transact
     * @param reason memo regarding transaction
     * @return true, or pay/collect balance for given reason
     * @throws EconomyException if transaction fails
     */
    boolean setBalance(double amount, String reason) throws EconomyException;

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
     * @deprecated As of 0.96.1.11, use {@link #withdraw(double, String)} instead.
     *
     * @param amount The amount to withdrawPlayer.
     * @param reason The reason for subcracting.
     * @return boolean indicating success.
     * @throws EconomyException On an economy error.
     */
    @Deprecated
    boolean pay(double amount, String reason) throws EconomyException;
}
