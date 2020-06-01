package com.palmergames.bukkit.towny.object.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.World;

@SuppressWarnings("deprecation")
public class VaultEconomyAdapter implements EconomyAdapter {
	
	final Economy economy;
	
	public VaultEconomyAdapter(Economy economy) {
		this.economy = economy;
	}

	@Override
	public boolean add(String accountName, double amount, World world) {
		return economy.depositPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		return economy.withdrawPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(String accountName) {
		return economy.hasAccount(accountName);
	}

	@Override
	public double getBalance(String accountName, World world) {
		return economy.getBalance(accountName);
	}

	@Override
	public void newAccount(String accountName) {
		economy.createPlayerAccount(accountName);
	}

	@Override
	public void deleteAccount(String accountName) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!economy.hasAccount(accountName))
			economy.createPlayerAccount(accountName);
		economy.withdrawPlayer(accountName, (economy.getBalance(accountName)));
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		double currentBalance = getBalance(accountName, world);
		if (amount > currentBalance) {
			double diff = amount - currentBalance;
			return add(accountName, diff, world);
		}
		
		if (amount < currentBalance) {
			double diff = currentBalance - amount;
			return subtract(accountName, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}

	@Override
	public String getFormattedBalance(double balance) {
		return economy.format(balance);
	}
}
