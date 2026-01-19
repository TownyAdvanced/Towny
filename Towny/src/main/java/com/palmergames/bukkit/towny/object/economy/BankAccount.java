package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.event.economy.TownEntersBankruptcyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;

import org.jetbrains.annotations.Nullable;

/**
 * A variant of an {@link Account} that belongs to a {@link Government}. When
 * that Government is a {@link Town}, a system for going into debt is provided.
 */
public class BankAccount extends Account {
	
	private double debtCap;
	private final Government government;

	/**
	 * Constructor for a {@link Government} BankAccount. Governments can be Towns or Nations.
	 * 
	 * @param name Name of the {@link EconomyAccount} that will be used, ie: town-townname.
	 * @param government Town or Nation that is getting a BankAccount.
	 */
	public BankAccount(String name, Government government) {
		super(government, name, TownyEconomyHandler.modifyNPCUUID(government.getUUID()), () -> TownyAPI.getInstance().getTownyWorld(government.getWorld()), false);
		this.government = government;
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
	protected synchronized boolean subtractMoney(double amount) {
		if (isBankrupt())
			return addDebt(amount);

		if (!canPayFromHoldings(amount) && isAllowedToEnterBankruptcy()) {

			// Calculate initial amount debt the town will take on.
			double newDebt = amount - getHoldingBalance();

			if (newDebt <= getDebtCap()) {
				// Empty out account.
				boolean success = TownyEconomyHandler.setBalance(this, 0);
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
		return TownyEconomyHandler.subtract(this, amount);
	}

	@Override
	protected synchronized boolean addMoney(double amount) {
		// Check balance cap.
		if (getBalanceCap() != 0 && (getHoldingBalance() + amount > getBalanceCap()))
			return false;
		
		if (isBankrupt())
			return removeDebt(amount);

		// Otherwise continue like normal.
		return TownyEconomyHandler.add(this, amount);
	}

	@Override
	public synchronized boolean canPayFromHoldings(double amount) {
		if (isBankrupt())
			return getTownDebt() + amount <= getDebtCap();
		else
			return super.canPayFromHoldings(amount);
	}

	@Override
	public synchronized double getHoldingBalance(boolean setCache) {
		double balance = isBankrupt() ? getTownDebt() * -1 : TownyEconomyHandler.getBalance(this);
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
	 * Town and Nation Bank Account removal methods.
	 */

	/**
	 * Attempt to delete the economy account of a Town or Nation.
	 */
	@Override
	public void removeAccount() {
		if (TownySettings.isDeletedObjectBalancePaidToOwner()) {
			final Resident owner = getGovernmentOwner();
			
			if (owner != null && !owner.isNPC()) {
				double balance = getHoldingBalance();
				if (balance > 0) {
					TownyMessaging.sendMsg(owner, Translatable.of("msg_recieved_refund_for_deleted_object", TownyEconomyHandler.getFormattedBalance(balance)));
					payTo(balance, owner, "Deleted " + (isTownAccount() ? "Town" : "Nation") + " bank balance refund.");
				}
			}
		}

		Towny.getPlugin().getScheduler().runLater(() -> super.removeAccount(), 10L);
	}

	@Nullable
	private Resident getGovernmentOwner() {
		return government instanceof Town town ? town.getMayor() : government instanceof Nation nation && nation.hasCapital() ? nation.getKing() : null;
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
		return government instanceof Town town && town.isBankrupt();
	}

	/**
	 * Adds debt to a Town debtBalance
	 * @param amount the amount to add to the debtBalance.
	 * @return true if the BankAccount is a town bank account, the debtCap allows it, and bankruptcy is enabled.
	 */
	private synchronized boolean addDebt(double amount) {
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
	private synchronized boolean removeDebt(double amount) {
		if (getTownDebt() < amount) {
			// Calculate money to go into regular account.
			double netMoney = amount - getTownDebt();
			//Clear debt account
			setTownDebt(0.0);
			//Set positive balance in regular account
			TownyEconomyHandler.setBalance(this, getHoldingBalance() + netMoney);
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
	public synchronized double getDebtCap() {
		if (!isTownAccount()) // Nations should never be passed here.
			return Double.MAX_VALUE;

		if (TownySettings.isDebtCapDeterminedByTownLevel()) { // town_level debtCapModifier * debt_cap.override.
			return getTown().getTownLevel().debtCapModifier() * TownySettings.getDebtCapOverride();
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
