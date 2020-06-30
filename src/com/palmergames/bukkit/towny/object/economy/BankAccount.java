package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import org.bukkit.World;

/**
 * A variant of an account that implements
 * a checked cap on it's balance, as well as a 
 * debt system.
 */
public class BankAccount extends Account {
	
	private double balanceCap;
	private final Account debtAccount;
	private double debtCap;

	/**
	 * Because of limitations in Economy API's, debt isn't
	 * supported reliably in them so we need use another account
	 * as a workaround for this problem.
	 */
	static class DebtAccount extends EconomyAccount {
		
		public static final String DEBT_PREFIX = TownySettings.getTownDebtAccountPrefix();

		public DebtAccount(Account account) {
			// TNE doesn't play nice with "town-" on debt accounts.
			super(DEBT_PREFIX + account.getName().replace("town-",""), account.getBukkitWorld());
		}
	}
	
	public BankAccount(String name, World world, double balanceCap) {
		super(name, world);
		this.balanceCap = balanceCap;
		this.debtAccount = new DebtAccount(this);
		this.debtCap = 0;
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
	 * @return The max amount of debt for this account.
	 */
	public double getDebtCap() {
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
			if (balanceCap != 0 && !(getHoldingBalance() + amount > balanceCap)) {
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
		return debtAccount.getHoldingBalance() > 0;
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
		TownyEconomyHandler.removeAccount(debtAccount.getName());
		TownyEconomyHandler.removeAccount(getName());
	}
}
