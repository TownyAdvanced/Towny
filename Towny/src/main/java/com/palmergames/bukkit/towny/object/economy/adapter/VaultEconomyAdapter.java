package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;

public class VaultEconomyAdapter implements EconomyAdapter {
	
	protected final Economy economy;
	
	public VaultEconomyAdapter(Economy economy) {
		this.economy = economy;
	}
	
	@Override
	public String name() {
		return economy.getName();
	}

	@Override
	public boolean add(Account account, double amount) {
		return economy.depositPlayer(account.asOfflinePlayer(), account.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean subtract(Account account, double amount) {
		return economy.withdrawPlayer(account.asOfflinePlayer(), account.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
	}

	@Override
	public boolean hasAccount(Account account) {
		return economy.hasAccount(account.asOfflinePlayer(), account.getWorld().getName());
	}

	@Override
	public double getBalance(Account account) {
		return economy.getBalance(account.asOfflinePlayer(), account.getWorld().getName());
	}

	@Override
	public void newAccount(Account account) {
		economy.createPlayerAccount(account.asOfflinePlayer(), account.getWorld().getName());
	}

	@Override
	public void deleteAccount(Account account) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!hasAccount(account)) {
			return;
		}

		subtract(account, getBalance(account));
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
		return economy.format(balance);
	}

	@Override
	public boolean renameAccount(Account account, String newName) {
		// Unused in Vault.
		return true;
	}

	@SuppressWarnings("deprecation")
	public static class Legacy extends VaultEconomyAdapter {
		public Legacy(Economy economy) {
			super(economy);
		}

		@Override
		public boolean add(Account account, double amount) {
			return economy.depositPlayer(account.getName(), account.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
		}

		@Override
		public boolean subtract(Account account, double amount) {
			return economy.withdrawPlayer(account.getName(), account.getWorld().getName(), amount).type == EconomyResponse.ResponseType.SUCCESS;
		}

		@Override
		public boolean hasAccount(Account account) {
			return economy.hasAccount(account.getName(), account.getWorld().getName());
		}

		@Override
		public double getBalance(Account account) {
			return economy.getBalance(account.getName(), account.getWorld().getName());
		}

		@Override
		public void newAccount(Account account) {
			economy.createPlayerAccount(account.getName(), account.getWorld().getName());
		}
	}
}
