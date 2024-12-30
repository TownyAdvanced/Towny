package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.BankAccount;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * An Account object representing a Player's account. In contrast to the
 * {@link BankAccount} that represents Towns' and Nations' Accounts.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 * @author LlmDl
 */
public class EconomyAccount extends Account {
	protected EconomyAccount(Resident resident, String name, UUID uuid, Supplier<TownyWorld> worldSupplier) {
		super(resident, name, uuid, worldSupplier, true);
	}

	@Override
	protected synchronized boolean addMoney(double amount) {
		return TownyEconomyHandler.add(this, amount);
		
	}

	@Override
	protected synchronized boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(this, amount);
	}
}
