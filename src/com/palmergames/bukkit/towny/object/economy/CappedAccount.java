package com.palmergames.bukkit.towny.object.economy;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import org.bukkit.World;

/**
 * A variant of an account that implements
 * a checked cap on it's balance.
 */
public class CappedAccount extends Account {
	
	double cap;
	
	public CappedAccount(String name, World world, double cap) {
		super(name, world);
		this.cap = cap;
	}
	
	public boolean canAdd(double amount) throws EconomyException {
		if (cap == 0) {
			return true;
		}
		return !(getHoldingBalance() + amount > cap);
	}
}
