package com.palmergames.bukkit.towny.utils;

import java.util.List;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;

public class TownUtil {

	/**
	 * Makes a list of residents who haven't logged in in the given days.
	 * NPCs and Mayors are not included.
	 * 
	 * @param list List of Residents from which to test for inactivity.
	 * @param days Number of days after which players are considered inactive.
	 * @since 0.97.0.7
	 */
	public static List<Resident> gatherInactiveResidents(List<Resident> resList, int days) {
		return resList.stream()
				.filter(res -> !res.isNPC() && !res.isMayor() && !BukkitTools.isOnline(res.getName()) && (System.currentTimeMillis() - res.getLastOnline() > TimeTools.getMillis(days + "d")))
				.collect(Collectors.toList());
	}

}
