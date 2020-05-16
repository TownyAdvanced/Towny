package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import org.bukkit.World;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A variant of an account that implements
 * a checked cap on it's balance.
 */
public class CappedAccount extends Account implements AccountObserver {
	
	double cap;
	
	CappedAccount(String name, World world, double cap) {
		super(name, world);
		this.cap = cap;
	}
	
	public boolean canAdd(double amount) throws EconomyException {
		if (cap == 0) {
			return true;
		}
		return !(getHoldingBalance() + amount > cap);
	}

	@Override
	public void withdrew(Account account, double amount, String reason) {
		
	}

	@Override
	public void deposited(Account account, double amount, String reason) {

	}
}
