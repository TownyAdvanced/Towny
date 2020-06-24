package com.palmergames.bukkit.towny.object.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;

import java.util.Optional;

@SuppressWarnings("deprecation")
public class VaultEconomyAdapter implements EconomyAdapter {
	
	private final Economy economy;
	
	public VaultEconomyAdapter(Economy economy) {
		this.economy = economy;
	}

	@Override
	public boolean depositPlayer(String accountName, double amount, World world) {
		String worldName = world != null ? world.getName() : null;
		return isSuccessful(economy.depositPlayer(accountName, worldName, amount));
	}

	@Override
	public boolean withdrawPlayer(String accountName, double amount, World world) {
		String worldName = world != null ? world.getName() : null;
		return isSuccessful(economy.withdrawPlayer(accountName, worldName, amount));
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
	public void newPlayerAccount(String accountName) {
		economy.createPlayerAccount(accountName);
	}

	@Override
	public void deletePlayerAccount(String accountName) {
		// Attempt to zero the account as Vault provides no delete method.
		if (!economy.hasAccount(accountName)) {
			return;
		}
		
		economy.withdrawPlayer(accountName, (economy.getBalance(accountName)));
	}

	@Override
	public boolean setBalance(String accountName, double amount, World world) {
		double currentBalance = getBalance(accountName, world);
		double diff = Math.abs(amount - currentBalance);
		
		if (amount > currentBalance) {
			return depositPlayer(accountName, diff, world);
		} else if (amount < currentBalance) {
			return withdrawPlayer(accountName, diff, world);
		}
		
		// If we get here, the balances are equal.
		return true;
	}

	@Override
	public String getFormattedBalance(double balance) {
		return economy.format(balance);
	}

	@Override
	public boolean hasBankSupport() {
		return economy.hasBankSupport();
	}

	@Override
	public boolean newBank(String name, OfflinePlayer player) {
		return isSuccessful(economy.createBank(name, player));
	}

	@Override
	public boolean deleteBank(String name) {
		return isSuccessful(economy.deleteBank(name));
	}

	@Override
	public double getBankBalance(String name) {
		EconomyResponse response = economy.bankBalance(name);
		if (isSuccessful(response)) {
			return response.amount;
		}
		
		throw new UnsupportedOperationException("Bank: " + name + " could not access balance.");
	}

	@Override
	public boolean bankHas(String name, double amount) {
		return isSuccessful(economy.bankHas(name, amount));
	}

	@Override
	public boolean bankWithdraw(String name, double amount) {
		return isSuccessful(economy.bankWithdraw(name, amount));
	}

	@Override
	public boolean bankDeposit(String name, double amount) {
		return isSuccessful(economy.bankDeposit(name, amount));
	}
	
	private boolean isSuccessful(EconomyResponse response) {
		return response.type == EconomyResponse.ResponseType.SUCCESS;
	}
	
	@Override
	public boolean setBankBalance(String bankName, double amount) {
		double currentBalance = getBankBalance(bankName);
		double diff = Math.abs(amount - currentBalance);

		if (amount > currentBalance) {
			return bankDeposit(bankName, diff);
		} else if (amount < currentBalance) {
			return bankWithdraw(bankName, diff);
		}

		// If we get here, the balances are equal.
		return true;
	}
}
