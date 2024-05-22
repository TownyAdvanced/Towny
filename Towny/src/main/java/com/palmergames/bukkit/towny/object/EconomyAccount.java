package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.BankAccount;
import org.bukkit.World;

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
	private World world;
	
	protected EconomyAccount(Resident resident, String name, World world) {
		super(resident, name);
		this.world = world;
	}

	@Override
	protected synchronized boolean addMoney(double amount) {
		
		return TownyEconomyHandler.add(this, amount, world);
		
	}

	@Override
	protected synchronized boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(this, amount, world);
	}

	public World getWorld() {
		return world;
	}

}
