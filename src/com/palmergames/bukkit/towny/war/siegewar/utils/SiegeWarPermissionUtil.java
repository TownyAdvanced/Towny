package com.palmergames.bukkit.towny.war.siegewar.utils;

import java.util.List;

import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeWarPermissionNodes;

public class SiegeWarPermissionUtil {

	/**
	 * This method checks if the given nation rank, will allow the given permission node
	 *
	 * @param nationRank - A nation rank (e.g. soldier, helper)
	 * @param permissionNode - Permission node to check for
	 * @return true if the rank allows the permission node
	 */
	public static boolean doesNationRankAllowPermissionNode(String nationRank, SiegeWarPermissionNodes permissionNode) {
		List<String> allPermissionNodesAllowedByRank = TownyPerms.getNationRank(nationRank);
		String permissionNodeString = permissionNode.getNode();
		String permissionNodeWildCardString = permissionNodeString.replaceFirst("[\\w]*$", "*");
		return (allPermissionNodesAllowedByRank.contains(permissionNodeString) 
			|| allPermissionNodesAllowedByRank.contains(permissionNodeWildCardString));
	}

	/**
	 * This method checks if the given town rank, will allow the given permission node
	 *
	 * @param townRank - A town rank (e.g. guard, helper)
	 * @param permissionNode - Permission node to check for
	 * @return true if the rank allows the permission node
	 */
	public static boolean doesTownRankAllowPermissionNode(String townRank, SiegeWarPermissionNodes permissionNode) {
		List<String> allPermissionNodesAllowedByRank = TownyPerms.getTownRank(townRank);
		String permissionNodeString = permissionNode.getNode();
		String permissionNodeWildCardString = permissionNodeString.replaceFirst("[\\w]*$", "*");
		return (allPermissionNodesAllowedByRank.contains(permissionNodeString) 
			|| allPermissionNodesAllowedByRank.contains(permissionNodeWildCardString));
	}
}
