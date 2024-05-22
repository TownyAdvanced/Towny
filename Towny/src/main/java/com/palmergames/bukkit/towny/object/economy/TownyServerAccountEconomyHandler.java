package com.palmergames.bukkit.towny.object.economy;

import org.bukkit.World;

import com.palmergames.bukkit.towny.object.EconomyHandler;

/**
 * Defines methods necessary for the TownyServerAccount, used mainly in the
 * closed economy feature.
 */
public interface TownyServerAccountEconomyHandler extends EconomyHandler {
	/**
	 * Gets the account associated with the TownyServerAccount
	 *
	 * @return The TownyServerAccount
	 */
	@Override
	Account getAccount(); // Covariant return type of Account from superinterface

	public boolean addToServer(Account account, double amount, World world);

	public boolean subtractFromServer(Account account, double amount, World world);
}
