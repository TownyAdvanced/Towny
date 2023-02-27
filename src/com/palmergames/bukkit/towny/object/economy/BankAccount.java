package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.event.economy.TownEntersBankruptcyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

/**
 * A variant of an {@link Account} that belongs to a {@link Government}. When
 * that Government is a {@link Town}, a system for going into debt is provided.
 */
public class BankAccount extends Account {
	
	private double debtCap;
	private Government government;

	/**
	 * Constructor for a {@link Government} BankAccount. Governments can be Towns or Nations.
	 * 
	 * @param name Name of the {@link EconomyAccount} that will be used, ie: town-townname.
	 * @param world World that will be associated with this BankAccount.
	 * @param government Town or Nation that is getting a BankAccount.
	 */
	public BankAccount(String name, World world, Government government) {
		super(name, world);
		this.government = government;
	}

	/**
	 * @deprecated since 0.98.6.9, use {@link #BankAccount(String, World, Government)} instead.
	 * 
	 * @param name Name of the {@link EconomyAccount} that will be used, ie: town-townname.
	 * @param world World attacked to this BankAccount.
	 * @param balanceCap Unused as of 0.98.6.9.
	 */
	@Deprecated
	public BankAccount(String name, World world, double balanceCap) {
		super(name, world);
		if (this.getName().startsWith(TownySettings.getTownAccountPrefix()))
			this.government = TownyUniverse.getInstance().getTown(this.getName().replace(TownySettings.getTownAccountPrefix(), ""));
		else
			this.government = TownyUniverse.getInstance().getNation(this.getName().replace(TownySettings.getNationAccountPrefix(), ""));
	}

	/**
	 * No longer used.
	 * 
	 * @deprecated since 0.98.6.9, balance caps are now dynamic.
	 * @param balanceCap The max amount allowed in this account.
	 */
	@Deprecated
	public void setBalanceCap(double balanceCap) {
		return;
	}

	/**
	 * Gets the maximum amount of money this account can have.
	 *
	 * @return the max amount allowed in this account.
	 */
	public double getBalanceCap() {
		return government.getBankCap();
	}

	@Override
	protected boolean subtractMoney(double amount) {
		if (isBankrupt())
			return addDebt(amount);

		if (!canPayFromHoldings(amount) && isAllowedToEnterBankruptcy()) {

			// Calculate initial amount debt the town will take on.
			double newDebt = amount - getHoldingBalance();

			if (newDebt <= getDebtCap()) {
				// Empty out account.
				boolean success = TownyEconomyHandler.setBalance(getName(), 0, world);
				success &= addDebt(newDebt);

				// Fire an event if the Town will be allowed to take on this new debt.
				if (success)
					BukkitTools.fireEvent(new TownEntersBankruptcyEvent(getTown()));

				return success;
			} else {
				return false; //Subtraction not allowed as it would exceed the debt cap
			}
		}

		// Otherwise continue like normal.
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}

	@Override
	protected boolean addMoney(double amount) {
		// Check balance cap.
		if (getBalanceCap() != 0 && (getHoldingBalance() + amount > getBalanceCap()))
			return false;
		
		if (isBankrupt())
			return removeDebt(amount);

		// Otherwise continue like normal.
		return TownyEconomyHandler.add(getName(), amount, world);
	}

	@Override
	public double getHoldingBalance(boolean setCache) {
		double balance = isBankrupt() ? balance = getTownDebt() * -1 : TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
		if (setCache)
			this.cachedBalance.setBalance(balance);
		return balance;
	}

	@Override
	public String getHoldingFormattedBalance() {
		if (isBankrupt()) {
			return "-" + TownyEconomyHandler.getFormattedBalance(getTownDebt());
		}
		return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
	}

	@Override
	public void removeAccount() {
		TownyEconomyHandler.removeAccount(getName());
	}

	/**
	 * return true if this BankAcount is one belonging to a Town.
	 */
	private boolean isTownAccount() {
		return government instanceof Town;
	}

	/**
	 * @return town or null, if this BankAccount does not belong to a town.
	 */
	@Nullable
	private Town getTown() {
		return isTownAccount() ? (Town) government : null;
	}

	/*
	 * Bankruptcy and Debt Methods.
	 */

	/**
	 * Whether the account is in debt or not.
	 * 
	 * @return true if in debt, false otherwise.
	 */
	public boolean isBankrupt() {
		return government instanceof Town town ? town.isBankrupt() : false;
	}

	/**
	 * Adds debt to a Town debtBalance
	 * @param amount the amount to add to the debtBalance.
	 * @return true if the BankAccount is a town bank account, the debtCap allows it, and bankruptcy is enabled.
	 */
	private boolean addDebt(double amount) {
		if (isTownAccount() && TownySettings.isTownBankruptcyEnabled()) {
			double newDebtBalance = getTownDebt() + amount;
			// Subtraction not allowed as it would exceed the debt cap
			if (newDebtBalance > getDebtCap())
				return false;

			setTownDebt(newDebtBalance);
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
			double bankBalance = TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
			//Set positive balance in regular account
			TownyEconomyHandler.setBalance(getName(), bankBalance + netMoney, world);
			return true;
		} else {
			setTownDebt(getTownDebt() - amount);
			return true;
		}
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
		if (!isTownAccount()) // Nations should never be passed here.
			return Double.MAX_VALUE;

		if (TownySettings.isDebtCapDeterminedByTownLevel()) { // town_level debtCapModifier * debt_cap.override.
			return TownySettings.getTownLevel(getTown()).debtCapModifier() * TownySettings.getDebtCapOverride();
		}
		
		if (TownySettings.getDebtCapOverride() != 0.0)
			return TownySettings.getDebtCapOverride();
		
		if (TownySettings.getDebtCapMaximum() != 0.0)
			return Math.min(debtCap, TownySettings.getDebtCapMaximum());
		
		return debtCap;
	}

	/**
	 * Sets the maximum amount of debt this account can have.
	 * 
	 * @param debtCap The new cap for debt on this account.
	 */
	public void setDebtCap(double debtCap) {
		this.debtCap = debtCap;
	}

	private boolean isAllowedToEnterBankruptcy() {
		return TownySettings.isTownBankruptcyEnabled() && isTownAccount();
	}

	private double getTownDebt() {
		return getTown().getDebtBalance();
	}

	private void setTownDebt(double amount) {
		getTown().setDebtBalance(amount);
		getTown().save();
	}
}
