package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;

public class EconomyManager {
	public static final EconomyManager.TownyServerAccount SERVER_ACCOUNT = new TownyEconomyObject.TownyServerAccount();

	private static final class TownyServerAccount extends TownyEconomyObject {
		TownyServerAccount() {
			super(TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT));
		}
	}
}
