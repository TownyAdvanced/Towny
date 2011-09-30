package com.palmergames.bukkit.towny.object;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;

public enum TownSpawnLevel {
	TOWN_RESIDENT(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN, "msg_err_town_spawn_forbidden", ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL, "towny.town.spawn.town"),
	PART_OF_NATION(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL_NATION, "msg_err_town_spawn_nation_forbidden", ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_NATION, "towny.town.spawn.nation"),
	NATION_ALLY(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL_ALLY, "msg_err_town_spawn_ally_forbidden", ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_ALLY, "towny.town.spawn.ally"),
	UNAFFILIATED(ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL, "msg_err_public_spawn_forbidden", ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC, "towny.town.spawn.public"),
	ADMIN(null, null, null, null);
	
	private ConfigNodes isAllowingConfigNode, ecoPriceConfigNode;
	private String permissionNode, notAllowedLangNode;
	
	private TownSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, ConfigNodes ecoPriceConfigNode, String permissionNode) {
		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
	}
	
	public void checkIfAllowed(Towny plugin, Player player) throws TownyException {
		if (!(isAllowed() && hasPermissionNode(plugin, player)))
			throw new TownyException(TownySettings.getLangString(notAllowedLangNode));
	}
	
	public boolean isAllowed() {
		return this == TownSpawnLevel.ADMIN ? true : TownySettings.getBoolean(this.isAllowingConfigNode);
	}
	
	public boolean hasPermissionNode(Towny plugin, Player player) {
		return this == TownSpawnLevel.ADMIN ? true : plugin.hasPermission(player, this.permissionNode);
	}
	
	public double getCost() {
		return this == TownSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}
}
