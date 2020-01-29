package com.palmergames.bukkit.towny.object;

/**
 * An interface used to show that an object is capable of participating
 * in economy specific tasks.
 * 
 * @author Suneet Tipirneni (Siris)
 */
public interface EconomyHandler {
	/**
	 * Gets the {@link EconomyAccount} associated with this object.
	 * 
	 * @return An {@link EconomyAccount} for this class.
	 */
	EconomyAccount getAccount();
}
