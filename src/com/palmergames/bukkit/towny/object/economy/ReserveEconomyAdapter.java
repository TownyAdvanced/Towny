package com.palmergames.bukkit.towny.object.economy;

import net.tnemc.core.economy.EconomyAPI;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.math.BigDecimal;

public class ReserveEconomyAdapter implements EconomyAdapter {
	
	final EconomyAPI economy;
	
	public ReserveEconomyAdapter(EconomyAPI economy) {
		this.economy = economy;
	}

	@Override
	public boolean depositPlayer(String accountName, double amount, World world) {
		return economy.addHoldingsDetail(accountName, new BigDecimal(amount), world.getName()).success();
	}

	@Override
	public boolean withdrawPlayer(String accountName, double amount, World world) {
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
	public void newPlayerAccount(String accountName) {
		economy.createAccountDetail(accountName).success();
	}

	@Override
	public void deletePlayerAccount(String accountName) {
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

	@Override
	public boolean hasBankSupport() {
		return false;
	}
}
