package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.TownyEconomyHandler;

import java.util.UUID;

/**
 * For internal use only.
 */
public class TownyServerAccount extends Account {
	public TownyServerAccount() {
		super("Server_Account", UUID.fromString("98a37f1a-e431-4eb2-9415-5db53b566436"));
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(this, amount, getWorld());
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(this, amount, getWorld());
	}
}
