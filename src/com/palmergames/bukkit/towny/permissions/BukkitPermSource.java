package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;
import org.bukkit.permissions.PermissionAttachmentInfo;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;

public class BukkitPermSource extends TownyPermissionSource {

	public BukkitPermSource(Towny towny) {

		this.plugin = towny;
	}

	@Override
	public String getPrefixSuffix(Resident resident, String node) {

		/*
		 * Bukkit doesn't support prefix/suffix
		 * so treat the same as bPerms
		 */

		Player player = resident.getPlayer();
		if (player == null)
			return "";

		for (PermissionAttachmentInfo test : player.getEffectivePermissions()) {
			if (test.getPermission().startsWith(node + ".")) {
				String[] split = test.getPermission().split("\\.");
				return split[split.length - 1];
			}
		}
		
		return "";
	}

	/**
	 * 
	 * @param playerName - Player name.
	 * @param node - Node to be checked.
	 * @return -1 = can't find
	 */
	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {

		return getEffectivePermIntNode(playerName, node);
	}
	
	@Override
	public int getPlayerPermissionIntNode(String playerName, String node) {
		
		return getEffectivePermIntNode(playerName, node);
	}

	/**
	 * 
	 * @param playerName - Player name.
	 * @param node - Node to be checked.
	 * @return empty = can't find
	 */
	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {

		/*
		 * Bukkit doesn't support non boolean nodes
		 * so treat the same as bPerms
		 */

		Player player = BukkitTools.getPlayerExact(playerName);
		if (player == null)
			return "";

		for (PermissionAttachmentInfo test : player.getEffectivePermissions()) {
			if (test.getPermission().startsWith(node + ".")) {
				String[] split = test.getPermission().split("\\.");
				return split[split.length - 1];

			}
		}

		return "";
	}

	/**
	 * Returns the players Group name.
	 * 
	 * @param player - Player.
	 * @return Empty string as bukkit doesn't support groups
	 */
	@Override
	public String getPlayerGroup(Player player) {

		//BukkitPermissions doesn't support groups.
		return "";

	}

}
