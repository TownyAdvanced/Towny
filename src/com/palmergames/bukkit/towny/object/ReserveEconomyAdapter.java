package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.economy.EconomyAdapter;
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
		return economy.addHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();
	}

	@Override
	public boolean subtract(String accountName, double amount, World world) {
		return economy.removeHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();
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
		return economy.setHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();
	}

	@Override
	public String getFormattedBalance(double balance) {
		return economy.format(new BigDecimal(balance));
	}
}
