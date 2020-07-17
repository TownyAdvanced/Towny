package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import org.bukkit.World;

import java.io.File;
import java.util.UUID;

/**
 * Economy object which provides an interface with the Economy Handler.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public class EconomyAccount extends Account {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();

	protected EconomyAccount(String name) {
		super(name);
	}
	
	protected EconomyAccount(String name, World world) {
		super(name, world);
	}

	@Override
	protected boolean addMoney(double amount) {
		return TownyEconomyHandler.add(getName(), amount, getWorld());
	}

	@Override
	protected boolean subtractMoney(double amount) {
		return TownyEconomyHandler.subtract(getName(), amount, getWorld());
	}

}
