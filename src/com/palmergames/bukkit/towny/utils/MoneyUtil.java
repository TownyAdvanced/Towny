package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;

public class MoneyUtil {

	/**
	 * Get estimated value of town
	 * Useful when calculating the allowed debt cap for a town
	 *
	 * @param town  The town to consider
	 * @return the estimate value of the town
	 */
	public static double getEstimatedValueOfTown(Town town) {
		return TownySettings.getNewTownPrice() + (town.getTownBlocks().size() * TownySettings.getClaimPrice());
	}
}
