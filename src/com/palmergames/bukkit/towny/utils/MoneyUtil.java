package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;

public class MoneyUtil {

	/**
	 * Get estimated value of town
	 * Useful when calculating the allowed debt cap for a town
	 * Covers new town costs, claimed land costs, purchased outposts costs.
	 *
	 * @param town The town to estimate a value for.
	 * @return the estimated monetary value of the town.
	 */
	public static double getEstimatedValueOfTown(Town town) {
		return TownySettings.getNewTownPrice() // New Town cost. 
				+ ((town.getTownBlocks().size() - 1) * TownySettings.getClaimPrice()) // Claimed land costs. (-1 because the homeblock comes with the NewTownPrice.) 
				+ (town.getAllOutpostSpawns().size() * (TownySettings.getOutpostCost() - TownySettings.getClaimPrice())); // Outposts costs. 
	}
}
