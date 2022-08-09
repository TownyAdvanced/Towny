package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import org.bukkit.World;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * Economy object which provides an interface with the Economy Handler.
 *
 * @author ElgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public class EconomyAccount extends Account {
	public static final TownyServerAccount SERVER_ACCOUNT = new TownyServerAccount();
	
	public EconomyAccount(String name, UUID uuid, Supplier<World> worldSupplier) {
		super(name, uuid, worldSupplier);
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
