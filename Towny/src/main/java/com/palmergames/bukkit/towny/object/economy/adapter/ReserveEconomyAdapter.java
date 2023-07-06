package com.palmergames.bukkit.towny.object.economy.adapter;

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

	@Override
	public String getFormattedBalance(double balance) {
		BigDecimal bd = BigDecimal.valueOf(balance);
		return economy.format(bd);
	}
}
