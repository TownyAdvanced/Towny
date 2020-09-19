package com.palmergames.bukkit.towny.object;

/**
 * Represents an object which controls the taxing operations of
 * an economic system.
 */
public interface TaxCollector {
	/**
	 * Gets the current taxes of the object.
	 * 
	 * @return The taxes as either a percentage or flat number.
	 */
	double getTaxes();

	/**
	 * Sets the taxes on the object.
	 * 
	 * @param taxes A flat number or percentage to set the tax to.
	 */
	void setTaxes(double taxes);
}
