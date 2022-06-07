package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.World;

@SuppressWarnings("deprecation")
public class VaultEconomyAdapter implements EconomyAdapter {
	
	private final Economy economy;
	
	public VaultEconomyAdapter(Economy economy) {
		this.economy = economy;
	}

	@Override
	public boolean add(Account account, double amount, World world) {
		return economy.depositPlayer(account.asOfflinePlayer(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean add(String accountName, double amount, World world) {
		return economy.depositPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(Account account, double amount, World world) {
		return economy.withdrawPlayer(account.asOfflinePlayer(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		return economy.withdrawPlayer(accountName, amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(Account account) {
		return economy.hasAccount(account.asOfflinePlayer());
	}

	@Override
	public boolean hasAccount(String accountName) {
		return economy.hasAccount(accountName);
	}

	@Override
	public double getBalance(Account account, World world) {
		return economy.getBalance(account.asOfflinePlayer());
	}

	@Override
	public double getBalance(String accountName, World world) {
		return economy.getBalance(accountName);
	}

	@Override
	public void newAccount(Account account) {
		economy.createPlayerAccount(account.asOfflinePlayer());
	}

	@Override
	public void newAccount(String accountName) {
		economy.createPlayerAccount(accountName);
	}

	@Override
	public void deleteAccount(Account account) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!economy.hasAccount(account.asOfflinePlayer())) {
			return;
		}
		
		economy.withdrawPlayer(account.asOfflinePlayer(), economy.getBalance(account.asOfflinePlayer()));
	}

	@Override
	public void deleteAccount(String accountName) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!economy.hasAccount(accountName)) {
			return;
		}

		economy.withdrawPlayer(accountName, economy.getBalance(accountName));
	}

	@Override
	public boolean setBalance(Account account, double amount, World world) {
		double currentBalance = getBalance(account, world);
		double diff = Math.abs(amount - currentBalance);
		
		if (amount > currentBalance) {
			return add(account, diff, world);
		}else if (amount < currentBalance) {
			return subtract(account, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		double currentBalance = getBalance(accountName, world);
		double diff = Math.abs(amount - currentBalance);

		if (amount > currentBalance) {
			return add(accountName, diff, world);
		}else if (amount < currentBalance) {
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
