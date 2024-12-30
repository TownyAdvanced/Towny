package com.palmergames.bukkit.towny.object.economy.adapter;

import java.math.BigDecimal;

import com.palmergames.bukkit.towny.object.economy.Account;
import net.milkbowl.vault2.economy.Economy;
import net.milkbowl.vault2.economy.EconomyResponse;

public class VaultUnlockedEconomyAdapter implements EconomyAdapter {
	
	private static final String TOWNY = "Towny";
	protected final Economy economy;
	
	public VaultUnlockedEconomyAdapter(Economy economy) {
		this.economy = economy;
	}
	
	@Override
	public String name() {
		return economy.getName();
	}

	@Override
	public boolean add(Account account, double amount) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.deposit(TOWNY, account.getUUID(), account.getWorld().getName(), bd).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(Account account, double amount) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.withdraw(TOWNY, account.getUUID(), account.getWorld().getName(), bd).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(Account account) {
		return economy.hasAccount(account.getUUID(), account.getWorld().getName());
	}

	@Override
	public double getBalance(Account account) {
		return economy.balance(TOWNY, account.getUUID(), account.getWorld().getName()).doubleValue();
	}

	@Override
	public void newAccount(Account account) {
		economy.createAccount(account.getUUID(), account.getName(), account.getWorld().getName(), account.isPlayerAccount());
	}

	@Override
	public void deleteAccount(Account account) {
		economy.deleteAccount(TOWNY, account.getUUID());
	}

	@Override
	public boolean renameAccount(Account account, String newName) {
		return economy.renameAccount(TOWNY, account.getUUID(), newName);
	}

	@Override
	public boolean setBalance(Account account, double amount) {
		double currentBalance = getBalance(account);
		double diff = Math.abs(amount - currentBalance);

		if (amount > currentBalance) {
			return add(account, diff);
		}else if (amount < currentBalance) {
			return subtract(account, diff);
		}

		// If we get here, the balances are equal.
		return true;
	}

	@Override
	public String getFormattedBalance(double balance) {
		BigDecimal bd = BigDecimal.valueOf(balance);
		return economy.format(TOWNY, bd);
	}
}
