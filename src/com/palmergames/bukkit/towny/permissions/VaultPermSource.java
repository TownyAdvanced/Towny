package com.palmergames.bukkit.towny.permissions;

import net.milkbowl.vault.chat.Chat;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;

public class VaultPermSource extends TownyPermissionSource {

	private final Chat chat;

	public VaultPermSource(Towny plugin, Chat chat) {
		this.plugin = plugin;
		this.chat = chat;
	}

	@Override
	public String getPrefixSuffix(Resident resident, String node) {
		Player player = Bukkit.getPlayerExact(resident.getName());
		if (player != null) {
			// Fetch primary group
			String primaryGroup = getPlayerGroup(player);

			String groupPrefixSuffix = "";
			String playerPrefixSuffix = "";

			// Pull prefix/suffix for both primary group and player
			if ("prefix".equalsIgnoreCase(node)) {
				if (!primaryGroup.isEmpty())
					groupPrefixSuffix = chat.getGroupPrefix(player.getWorld(), primaryGroup);
				playerPrefixSuffix = chat.getPlayerPrefix(player);
			}
			else if ("suffix".equalsIgnoreCase(node)) {
				if (!primaryGroup.isEmpty())
					groupPrefixSuffix = chat.getGroupSuffix(player.getWorld(), primaryGroup);
				playerPrefixSuffix = chat.getPlayerSuffix(player);
			}

			// Normalize
			if (groupPrefixSuffix == null)
				groupPrefixSuffix = "";
			if (playerPrefixSuffix == null)
				playerPrefixSuffix = "";

			// Combine, if different
			String prefixSuffix = playerPrefixSuffix;
			if (!playerPrefixSuffix.equals(groupPrefixSuffix))
				prefixSuffix = groupPrefixSuffix + playerPrefixSuffix;

			return TownySettings.parseSingleLineString(prefixSuffix);
		}
		return "";
	}

	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {
		Player player = Bukkit.getPlayerExact(playerName);
		if (player != null) {
			String primaryGroup = getPlayerGroup(player);

			if (!primaryGroup.isEmpty())
				return chat.getGroupInfoInteger(player.getWorld(), primaryGroup, node, -1);
		}
		return -1;
	}

	@Override
	public String getPlayerGroup(Player player) {
		String result = chat.getPrimaryGroup(player);
		return result != null ? result : "";
	}

	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {
		Player player = Bukkit.getPlayerExact(playerName);
		if (player != null) {
			return chat.getPlayerInfoString(player, node, "");
		}
		return "";
	}

}
