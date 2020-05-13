package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import org.bukkit.World;

public class Bank extends Account {
	
	double cap;
	
	Bank(String name, double cap) {
		super(name);
		this.cap = cap;
	}

	Bank(String name, World world, double cap) {
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
			throw new EconomyException("Cannot add above Bank Cap.");
		}
		return super.add(amount, reason);
	}
}
