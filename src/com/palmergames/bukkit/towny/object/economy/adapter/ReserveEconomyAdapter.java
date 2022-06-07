package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.object.economy.Account;
import net.tnemc.core.economy.EconomyAPI;
import org.bukkit.World;

import java.math.BigDecimal;

public class ReserveEconomyAdapter implements EconomyAdapter {
	
	final EconomyAPI economy;
	
	public ReserveEconomyAdapter(EconomyAPI economy) {
		this.economy = economy;
	}

	@Override
	public boolean add(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.addHoldingsDetail(accountName, bd, world.getName()).success();
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.removeHoldingsDetail(accountName, bd, world.getName()).success();
	}

	@Override
	public boolean hasAccount(String accountName) {
		return economy.hasAccountDetail(accountName).success();
	}

	@Override
	public double getBalance(String accountName, World world) {
		return economy.getHoldings(accountName, world.getName()).doubleValue();
	}

	@Override
	public void newAccount(String accountName) {
		economy.createAccountDetail(accountName).success();
	}

	@Override
	public void deleteAccount(String accountName) {
		economy.deleteAccountDetail(accountName);
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		BigDecimal bd = BigDecimal.valueOf(amount);
		return economy.setHoldingsDetail(accountName, bd, world.getName()).success();
	}

	// TODO couldn't find reserve documentation/EconomyAPI source so this will have to do for now
	@Override
	public boolean add(Account account, double amount, World world) {
		return add(account.getName(), amount, world);
	}

	@Override
	public boolean subtract(Account account, double amount, World world) {
		return subtract(account.getName(), amount, world);
	}

	@Override
	public boolean hasAccount(Account account) {
		return hasAccount(account.getName());
	}

	@Override
	public double getBalance(Account account, World world) {
		return getBalance(account.getName(), world);
	}

	@Override
	public void newAccount(Account account) {
		newAccount(account.getName());
	}

	@Override
	public void deleteAccount(Account account) {
		deleteAccount(account.getName());
	}

	@Override
	public boolean setBalance(Account account, double amount, World world) {
		return setBalance(account.getName(), amount, world);
	}

	@Override
	public String getFormattedBalance(double balance) {
		BigDecimal bd = BigDecimal.valueOf(balance);
		return economy.format(bd);
	}
}
