package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;

public class NullPermSource extends TownyPermissionSource {

	public NullPermSource(Towny towny) {

		this.plugin = towny;
	}

	@Override
	public String getPrefixSuffix(Resident resident, String node) {

		// using no permissions provider
		return "";
	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return -1 = can't find
	 */
	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {

		// // using no permissions provider
		return -1;
	}
	
	@Override
	public int getPlayerPermissionIntNode(String playerName, String node) {

		// // using no permissions provider
		return -1;
	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return empty = can't find
	 */
	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {

		// // using no permissions provider
		return "";
	}

	/**
	 * Returns the players Group name.
	 * 
	 * @param player
	 * @return empty as we are using no permissions
	 */
	@Override
	public String getPlayerGroup(Player player) {

		// using no permissions provider
		return "";
	}

}
