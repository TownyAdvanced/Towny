package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.World;

/**
 * A variant of an account that implements
 * a checked cap on it's balance, as well as a 
 * debt system.
 */
public class BankAccount extends Account {
	
	private static final long CACHE_TIMEOUT = TownySettings.getCachedBankTimeout();
	private double balanceCap;
	private final Account debtAccount;
	private double debtCap;
	private CachedBalance cachedBalance = null; 

	/**
	 * Because of limitations in Economy API's, debt isn't
	 * supported reliably in them so we need use another account
	 * as a workaround for this problem.
	 */
	static class DebtAccount extends EconomyAccount {
		
		public static final String DEBT_PREFIX = TownySettings.getDebtAccountPrefix();

		public DebtAccount(Account account) {
			// TNE doesn't play nice with "town-" on debt accounts.
			super(DEBT_PREFIX + account.getName().replace(TownySettings.getTownAccountPrefix(),""), account.getBukkitWorld());
			
			// Check if the account already exists, if not make sure the balance is set to 0
			// as some eco configurations put a default amount in every new account.
			if (!TownyEconomyHandler.hasAccount(getName())) {
				try {
					setBalance(0, "Initial Balance");
				} catch (EconomyException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public BankAccount(String name, World world, double balanceCap) {
		super(name, world);
		this.balanceCap = balanceCap;
		if (name.startsWith(TownySettings.getTownAccountPrefix()))
			this.debtAccount = new DebtAccount(this);
		else 
			this.debtAccount = null;
		this.debtCap = 0;
		try {
			this.cachedBalance = new CachedBalance(getHoldingBalance());
		} catch (EconomyException e) {}
	}

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
			String townName = this.getName().replace(TownySettings.getTownAccountPrefix(), "");
			Town town = TownyUniverse.getInstance().getTown(townName);
			
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
	 * Sets the maximum amount of debt this account can have.
	 * 
	 * @param debtCap The new cap for debt on this account.
	 */
	public void setDebtCap(double debtCap) {
		this.debtCap = debtCap;
	}

	@Override
	protected boolean subtractMoney(double amount) {
		try {
			if (isBankrupt() && (debtAccount.getHoldingBalance() + amount > getDebtCap())) {
				return false;  //subtraction not allowed as it would exceed the debt cap
			}

			if (isBankrupt()) {
				return addDebt(amount);
			}

			if (!canPayFromHoldings(amount)) {

				// Calculate debt.
				double amountInDebt = amount - getHoldingBalance();

				if(amountInDebt <= getDebtCap()) {
					// Empty out account.
					boolean success = TownyEconomyHandler.setBalance(getName(), 0, world);
					success &= addDebt(amountInDebt);

					return success;
				} else {
					return false; //Subtraction not allowed as it would exceed the debt cap
				}
			}
		} catch (EconomyException e) {
			e.printStackTrace();
		}

		// Otherwise continue like normal.
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}

	@Override
	protected boolean addMoney(double amount) {
		try {
			
			// Check balance cap.
			if (balanceCap != 0 && (getHoldingBalance() + amount > balanceCap)) {
				return false;
			}
			
			if (isBankrupt()) {
				return removeDebt(amount);
			}
		} catch (EconomyException e) {
			e.printStackTrace();
		}

		// Otherwise continue like normal.
		return TownyEconomyHandler.add(getName(), amount, world);
	}

	/**
	 * Whether the account is in debt or not.
	 * 
	 * @return true if in debt, false otherwise.
	 * @throws EconomyException On an economy error.
	 */
	public boolean isBankrupt() throws EconomyException {
		return debtAccount != null && debtAccount.getHoldingBalance() > 0;
	}
	
	private boolean addDebt(double amount) throws EconomyException {
		return debtAccount.deposit(amount, null);
	}
	
	private boolean removeDebt(double amount) throws EconomyException {
		if (!debtAccount.canPayFromHoldings(amount)) {
			// Calculate money to go into regular account.
			double netMoney = amount - debtAccount.getHoldingBalance();
			//Clear debt account
			TownyEconomyHandler.setBalance(debtAccount.getName(), 0, world);
			//Set positive balance in regular account
			TownyEconomyHandler.setBalance(getName(), netMoney, world);
			return true;
		} else {
			return TownyEconomyHandler.subtract(debtAccount.getName(), amount,world);
		}
	}

	@Override
	public double getHoldingBalance() throws EconomyException {
		try {
			if (isBankrupt()) {
				return TownyEconomyHandler.getBalance(debtAccount.getName(), getBukkitWorld()) * -1;
			}
			return TownyEconomyHandler.getBalance(getName(), getBukkitWorld());
		} catch (NoClassDefFoundError e) {
			e.printStackTrace();
			throw new EconomyException("Economy error getting holdings for " + getName());
		}
	}

	@Override
	public String getHoldingFormattedBalance() {
		try {
			if (isBankrupt()) {
				return "-" + debtAccount.getHoldingFormattedBalance();
			}
			return TownyEconomyHandler.getFormattedBalance(getHoldingBalance());
		} catch (EconomyException e) {
			return "Error";
		}
	}

	@Override
	public void removeAccount() {
		// Make sure to remove debt account
		if (debtAccount != null)
			TownyEconomyHandler.removeAccount(debtAccount.getName());
		TownyEconomyHandler.removeAccount(getName());
	}

	private class CachedBalance {
		private double balance;
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
		if (System.currentTimeMillis() - cachedBalance.getTime() > CACHE_TIMEOUT) {			
			if (!TownySettings.isEconomyAsync()) // Some economy plugins don't handle things async, 
				try {                            // luckily we have a config option for this such case.
					cachedBalance.setBalance(getHoldingBalance());
				} catch (EconomyException e1) {
					e1.printStackTrace();
				}
			else 
				Bukkit.getScheduler().runTaskAsynchronously(Towny.getPlugin(), () -> {
					try {
						cachedBalance.setBalance(getHoldingBalance());
					} catch (EconomyException e) {
						e.printStackTrace();
					}
				});				
		}
		return cachedBalance.getBalance();
	}
}
