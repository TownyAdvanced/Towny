package com.palmergames.bukkit.towny.war.common.townruin;


import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.TimeTools;

@Deprecated
public class TownRuinUtil {
	
	// TODO: Remove this class when Dynmap-Towny is updated/Towny's next release.
	public static int getTimeSinceRuining(Town town) {
		return TimeTools.getHours(System.currentTimeMillis() - town.getRuinedTime());
	}
}
