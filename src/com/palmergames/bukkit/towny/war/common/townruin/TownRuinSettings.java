package com.palmergames.bukkit.towny.war.common.townruin;

import com.palmergames.bukkit.towny.TownySettings;

@Deprecated
public class TownRuinSettings {

	// TODO: Delete this class next SiegeWar full release.
	public static boolean getTownRuinsEnabled() {
		return TownySettings.getTownRuinsEnabled();
	}
	
	public static int getTownRuinsMaxDurationHours() {
		return TownySettings.getTownRuinsMaxDurationHours();
	}
	
	public static int getTownRuinsMinDurationHours() {
		return TownySettings.getTownRuinsMinDurationHours();
	}

	public static boolean getTownRuinsReclaimEnabled() {
		return TownySettings.getTownRuinsReclaimEnabled();
	}

	public static double getEcoPriceReclaimTown() {
		return TownySettings.getEcoPriceReclaimTown();
	}
	
	public static boolean areRuinsMadePublic() {
		return TownySettings.areRuinsMadePublic();
	}
}
