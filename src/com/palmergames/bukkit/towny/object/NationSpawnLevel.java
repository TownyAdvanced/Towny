package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public enum NationSpawnLevel {
	PART_OF_NATION(
			ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN,
			"msg_err_nation_spawn_forbidden",
			"msg_err_nation_spawn_forbidden_war",
			"msg_err_nation_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_NATION_SPAWN_NATION.getNode()),	
	NATION_ALLY(
			ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN_TRAVEL_ALLY,
			"msg_err_nation_spawn_ally_forbidden",
			"msg_err_nation_spawn_nation_forbidden_war",
			"msg_err_nation_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_ALLY,
			PermissionNodes.TOWNY_NATION_SPAWN_ALLY.getNode()),
	UNAFFILIATED(
			ConfigNodes.GNATION_SETTINGS_ALLOW_NATION_SPAWN_TRAVEL,
			"msg_err_public_nation_spawn_forbidden",
			"msg_err_public_nation_spawn_forbidden_war",
			"msg_err_public_nation_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC,
			PermissionNodes.TOWNY_NATION_SPAWN_PUBLIC.getNode()),
	ADMIN(
			null,
			null,
			null,
			null,
			null,
			null);

	private ConfigNodes isAllowingConfigNode, ecoPriceConfigNode;
	private String permissionNode, notAllowedLangNode, notAllowedLangNodeWar, notAllowedLangNodePeace;

	NationSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, String notAllowedLangNodeWar, String notAllowedLangNodePeace, ConfigNodes ecoPriceConfigNode, String permissionNode) {

		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.notAllowedLangNodeWar = notAllowedLangNodeWar;
		this.notAllowedLangNodePeace = notAllowedLangNodePeace;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
	}

	public void checkIfAllowed(Towny plugin, Player player, Nation nation) throws TownyException {

		if (!(isAllowed(nation) && hasPermissionNode(plugin, player, nation))) {
			boolean war = TownyAPI.getInstance().isWarTime();
			NSpawnLevel level = TownySettings.getNSpawnLevel(this.isAllowingConfigNode);
			if(level == NSpawnLevel.WAR && !war) {
				throw new TownyException(TownySettings.getLangString(notAllowedLangNodeWar));
			}
			else if(level == NSpawnLevel.PEACE && war) {
				throw new TownyException(TownySettings.getLangString(notAllowedLangNodePeace));
			}
			throw new TownyException(TownySettings.getLangString(notAllowedLangNode));
		}
	}

	public boolean isAllowed(Nation nation) {
		return this == NationSpawnLevel.ADMIN || isAllowedNation(nation);
	}

	public boolean hasPermissionNode(Towny plugin, Player player, Nation nation) {

		return this == NationSpawnLevel.ADMIN || (TownyUniverse.getInstance().getPermissionSource().has(player, this.permissionNode)) && (isAllowedNation(nation));
	}
	
	private boolean isAllowedNation(Nation nation) {
		boolean war = TownyAPI.getInstance().isWarTime();
		NSpawnLevel level = TownySettings.getNSpawnLevel(this.isAllowingConfigNode);
		return level == NSpawnLevel.TRUE || (level != NSpawnLevel.FALSE && ((level == NSpawnLevel.WAR) == war));
	}

	public double getCost() {

		return this == NationSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}

	public double getCost(Nation nation) {

		return this == NationSpawnLevel.ADMIN ? 0 : nation.getSpawnCost();
	}
	
	public enum NSpawnLevel {
		TRUE,
		FALSE,
		WAR,
		PEACE
	}
}
