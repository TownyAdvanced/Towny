package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import net.tnemc.core.economy.EconomyAPI;

import java.math.BigDecimal;

public class ReserveEconomyAdapter implements EconomyAdapter {
	
	final EconomyAPI economy;
	
	public ReserveEconomyAdapter(EconomyAPI economy) {
		this.economy = economy;
	}
	
	@Override
	public String name() {
		return economy.name();
	}

	@Override
	public boolean add(Account account, double amount) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.addHoldingsDetail(account.getUUID(), bd, account.getWorld().getName()).success();
	}

	@Override
	public boolean subtract(Account account, double amount) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.removeHoldingsDetail(account.getUUID(), bd, account.getWorld().getName()).success();
	}

	@Override
	public boolean hasAccount(Account account) {
		return economy.hasAccountDetail(account.getUUID()).success();
	}

	@Override
	public double getBalance(Account account) {
		return economy.getHoldings(account.getUUID(), account.getWorld().getName()).doubleValue();
	}

	@Override
	public void newAccount(Account account) {
		economy.createAccountDetail(account.getUUID()).success();
	}

	@Override
	public void deleteAccount(Account account) {
		economy.deleteAccountDetail(account.getUUID());
	}

	@Override
	public boolean setBalance(Account account, double amount) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.setHoldingsDetail(account.getUUID(), bd, account.getWorld().getName()).success();
	}

	@Override
	public String getFormattedBalance(double balance) {
		BigDecimal bd = BigDecimal.valueOf(balance);
		return economy.format(bd);
	}

	@Override
	public boolean renameAccount(Account account, String newName) {
		// Unused in Reserve.
		return true;
	}

	public static class Legacy extends ReserveEconomyAdapter {
		public Legacy(EconomyAPI economy) {
			super(economy);
		}

		@Override
		public boolean add(Account account, double amount) {
			BigDecimal bd = BigDecimal.valueOf(amount);
			return economy.addHoldingsDetail(account.getName(), bd, account.getWorld().getName()).success();
		}

		@Override
		public boolean subtract(Account account, double amount) {
			BigDecimal bd = BigDecimal.valueOf(amount);
			return economy.removeHoldingsDetail(account.getName(), bd, account.getWorld().getName()).success();
		}

		@Override
		public boolean hasAccount(Account account) {
			return economy.hasAccountDetail(account.getName()).success();
		}

		@Override
		public double getBalance(Account account) {
			return economy.getHoldings(account.getName(), account.getWorld().getName()).doubleValue();
		}

		@Override
		public void newAccount(Account account) {
			economy.createAccountDetail(account.getName()).success();
		}

		@Override
		public void deleteAccount(Account account) {
			economy.deleteAccountDetail(account.getName());
		}

		@Override
		public boolean setBalance(Account account, double amount) {
			BigDecimal bd = BigDecimal.valueOf(amount);
			return economy.setHoldingsDetail(account.getName(), bd, account.getWorld().getName()).success();
		}
	}
}
