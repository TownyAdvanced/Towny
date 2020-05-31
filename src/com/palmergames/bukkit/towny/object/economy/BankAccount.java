package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import org.bukkit.World;

/**
 * A variant of an account that implements
 * a checked cap on it's balance, as well as a 
 * debt system.
 */
public class BankAccount extends Account {
	
	double balanceCap;
	Account debtAccount = new DebtAccount(this);
	// TODO: Debt Cap

	/**
	 * Because of limitations in Economy API's, debt isn't
	 * supported reliably in them so we need use another account
	 * as a workaround for this problem.
	 */
	static class DebtAccount extends Account {
		
		public static final String DEBT_PREFIX = TownySettings.getDebtAccountPrefix();

		public DebtAccount(Account account) {
			super(account.getName() + DEBT_PREFIX, account.getBukkitWorld());
		}
	}
	
	public BankAccount(String name, World world, double balanceCap) {
		super(name, world);
		this.balanceCap = balanceCap;
	}
	
	public boolean canAdd(double amount) throws EconomyException {
		if (balanceCap == 0) {
			return true;
		}
		return !(getHoldingBalance() + amount > balanceCap);
	}

	@Override
	public boolean subtractMoney(double amount) {
		try {
			if (!canPayFromHoldings(amount)) {
				
				// Calculate debt.
				double amountInDebt = amount - getHoldingBalance();

				// Empty out account.
				boolean success = TownyEconomyHandler.setBalance(getName(), 0, world);
				
				success &= addDebt(amountInDebt);
				
				return success;
			}
		} catch (EconomyException e) {
			e.printStackTrace();
		}
		
		// Otherwise continue like normal.
		return TownyEconomyHandler.subtract(getName(), amount, world);
	}

	@Override
	public boolean addMoney(double amount) {
		try {
			if (isBankrupt()) {
				return removeDebt(amount);
			}
		} catch (EconomyException e) {
			e.printStackTrace();
		}

		// Otherwise continue like normal.
		return TownyEconomyHandler.add(getName(), amount, world);
	}

	public boolean isBankrupt() throws EconomyException {
		return debtAccount.getHoldingBalance() > 0;
	}
	
	public boolean addDebt(double amount) throws EconomyException {
		return debtAccount.deposit(amount, null);
	}
	
	public boolean removeDebt(double amount) throws EconomyException {
		if (!debtAccount.canPayFromHoldings(amount)) {
			
			// Calculate money being added.
			double netMoney = amount - debtAccount.getHoldingBalance();
			
			// Zero out balance
			TownyEconomyHandler.setBalance(debtAccount.getName(), 0, world);
			
			return deposit(netMoney, null);
		}
		
		return TownyEconomyHandler.subtract(debtAccount.getName(), amount, getBukkitWorld());
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
}
