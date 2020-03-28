package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;

public enum TownSpawnLevel {
	TOWN_RESIDENT(
			ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN,
			"msg_err_town_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_SPAWN_TOWN.getNode()),
	TOWN_RESIDENT_OUTPOST(
			ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN,
			"msg_err_town_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_SPAWN_OUTPOST.getNode()),
	PART_OF_NATION(
			ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL_NATION,
			"msg_err_town_spawn_nation_forbidden",
			"msg_err_town_spawn_nation_forbidden_war",
			"msg_err_town_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_NATION,
			PermissionNodes.TOWNY_SPAWN_NATION.getNode()),
	NATION_ALLY(
			ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL_ALLY,
			"msg_err_town_spawn_ally_forbidden",
			"msg_err_town_spawn_nation_forbidden_war",
			"msg_err_town_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_ALLY,
			PermissionNodes.TOWNY_SPAWN_ALLY.getNode()),
	UNAFFILIATED(
			ConfigNodes.GTOWN_SETTINGS_ALLOW_TOWN_SPAWN_TRAVEL,
			"msg_err_public_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC,
			PermissionNodes.TOWNY_SPAWN_PUBLIC.getNode()),
	ADMIN(
			null,
			null,
			null,
			null,
			null,
			null);

	private ConfigNodes isAllowingConfigNode, ecoPriceConfigNode;
	private String permissionNode, notAllowedLangNode, notAllowedLangNodeWar, notAllowedLangNodePeace;

	private TownSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, String notAllowedLangNodeWar, String notAllowedLangNodePeace, ConfigNodes ecoPriceConfigNode, String permissionNode) {

		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.notAllowedLangNodeWar = notAllowedLangNodeWar;
		this.notAllowedLangNodePeace = notAllowedLangNodePeace;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
	}

	public void checkIfAllowed(Towny plugin, Player player, Town town) throws TownyException {

		if (!(isAllowed(town) && hasPermissionNode(plugin, player, town))) {
			boolean war = TownyAPI.getInstance().isWarTime() || FlagWar.isUnderAttack(town);
			SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
			if(level == SpawnLevel.WAR && !war) {
				throw new TownyException(TownySettings.getLangString(notAllowedLangNodeWar));
			}
			else if(level == SpawnLevel.PEACE && war) {
				throw new TownyException(TownySettings.getLangString(notAllowedLangNodePeace));
			}
			throw new TownyException(TownySettings.getLangString(notAllowedLangNode));
		}
	}

	public boolean isAllowed(Town town) {

		return this == TownSpawnLevel.ADMIN || isAllowedTown(town);
	}

	public boolean hasPermissionNode(Towny plugin, Player player, Town town) {

		return this == TownSpawnLevel.ADMIN || (TownyUniverse.getInstance().getPermissionSource().has(player, this.permissionNode)) && (isAllowedTown(town));
	}

	private boolean isAllowedTown(Town town)
	{
		boolean war = TownyAPI.getInstance().isWarTime() || FlagWar.isUnderAttack(town);
		SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
		return level == SpawnLevel.TRUE || (level != SpawnLevel.FALSE && ((level == SpawnLevel.WAR) == war));
	}
	
	public double getCost() {

		return this == TownSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}
	
	public double getCost(Town town) {

		return this == TownSpawnLevel.ADMIN ? 0 : town.getSpawnCost();
	}
	
	public enum SpawnLevel {
		TRUE,
		FALSE,
		WAR,
		PEACE
	}
}
