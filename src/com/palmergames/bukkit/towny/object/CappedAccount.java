package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import org.bukkit.World;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A variant of an account that implements
 * a checked cap on it's balance.
 */
public class CappedAccount extends Account {
	
	double cap;
	Map<Date, Transaction> audits = new HashMap<>();
	
	CappedAccount(String name, World world, double cap) {
		super(name, world);
		this.cap = cap;
	}
	
	private boolean canAdd(double amount) throws EconomyException {
		if (cap == 0) {
			return true;
		}
		return !(getHoldingBalance() + amount > cap);
	} 

	@Override
	public boolean add(double amount, String reason) throws EconomyException {
		if (!canAdd(amount)) {
			// TODO: Lang String
			throw new EconomyException("Cannot add above Account Cap.");
		}
		return super.add(amount, reason);
	}
}
