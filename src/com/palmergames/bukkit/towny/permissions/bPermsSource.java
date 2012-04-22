package com.palmergames.bukkit.towny.permissions;

import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.util.CalculableType;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;

public class bPermsSource extends TownyPermissionSource {

	public bPermsSource(Towny towny, Plugin test) {

		//this.bPermissions = (Permissions)test;
		this.plugin = towny;
	}

	/**
	 * getPermissionNode
	 * 
	 * returns the specified prefix/suffix nodes from permissionsEX
	 * 
	 * @param resident
	 * @param node
	 * @return String of the prefix/suffix
	 */
	@Override
	public String getPrefixSuffix(Resident resident, String node) {

		String group = "", user = "";
		Player player = plugin.getServer().getPlayer(resident.getName());

		group = ApiLayer.getValue(player.getWorld().getName(), CalculableType.GROUP, getPlayerGroup(player), node);
		user = ApiLayer.getValue(player.getWorld().getName(), CalculableType.USER, player.getName(), node);

		if (group == null)
			group = "";
		if (user == null)
			user = "";

		if (!group.equals(user))
			user = group + user;
		user = TownySettings.parseSingleLineString(user);

		return user;

	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return -1 = can't find
	 */
	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {

		Player player = plugin.getServer().getPlayer(playerName);

		String result = ApiLayer.getValue(player.getWorld().getName(), CalculableType.GROUP, getPlayerGroup(player), node);

		try {
			return Integer.parseInt(result);
		} catch (NumberFormatException e) {
			return -1;
		}

	}

	/**
	 * 
	 * @param playerName
	 * @param node
	 * @return empty = can't find
	 */
	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {

		Player player = plugin.getServer().getPlayer(playerName);

		String result = ApiLayer.getValue(player.getWorld().getName(), CalculableType.USER, player.getName(), node);

		if (result == null)
			return "";

		return result;

	}

	/**
	 * Returns the players Group name.
	 * 
	 * @param player
	 * @return String name of this players group.
	 */
	@Override
	public String getPlayerGroup(Player player) {

		return ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, player.getName())[0];
	}

}