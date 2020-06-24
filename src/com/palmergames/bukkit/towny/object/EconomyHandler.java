package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.economy.AbstractAccount;
import com.palmergames.bukkit.towny.object.economy.Account;

/**
 * An interface used to show that an object is capable of participating
 * in economy specific tasks.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface EconomyHandler extends Nameable {
	/**
	 * Gets the {@link AbstractAccount} associated with this object.
	 * 
	 * @return An {@link AbstractAccount} for this class.
	 */
	Account getAccount();
}
