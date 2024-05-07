package com.palmergames.bukkit.towny.object.spawnlevel;

import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public enum TownSpawnLevel {
	TOWN_RESIDENT(
			ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN,
			"msg_err_town_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_SPAWN_TOWN.getNode(),
			ConfigNodes.SPAWNING_TOWN_SPAWN_COOLDOWN_TIMER),
	TOWN_RESIDENT_OUTPOST(
			ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN,
			"msg_err_town_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_SPAWN_OUTPOST.getNode(),
			ConfigNodes.SPAWNING_TOWN_SPAWN_OUTPOST_COOLDOWN_TIMER),
	PART_OF_NATION(
			ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL_NATION,
			"msg_err_town_spawn_nation_forbidden",
			"msg_err_town_spawn_nation_forbidden_war",
			"msg_err_town_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_NATION,
			PermissionNodes.TOWNY_SPAWN_NATION.getNode(),
			ConfigNodes.SPAWNING_TOWN_SPAWN_NATION_MEMBER_COOLDOWN_TIMER),
	NATION_ALLY(
			ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL_ALLY,
			"msg_err_town_spawn_ally_forbidden",
			"msg_err_town_spawn_nation_forbidden_war",
			"msg_err_town_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_ALLY,
			PermissionNodes.TOWNY_SPAWN_ALLY.getNode(),
			ConfigNodes.SPAWNING_TOWN_SPAWN_NATION_ALLY_COOLDOWN_TIMER),
	UNAFFILIATED(
			ConfigNodes.SPAWNING_ALLOW_TOWN_SPAWN_TRAVEL,
			"msg_err_public_spawn_forbidden",
			"msg_err_town_spawn_forbidden_war",
			"msg_err_town_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC,
			PermissionNodes.TOWNY_SPAWN_PUBLIC.getNode(),
			ConfigNodes.SPAWNING_TOWN_SPAWN_UNAFFILIATED_COOLDOWN_TIMER),
	ADMIN(
			null,
			null,
			null,
			null,
			null,
			null,
			null);

	private ConfigNodes isAllowingConfigNode, ecoPriceConfigNode;
	private String permissionNode, notAllowedLangNode, notAllowedLangNodeWar, notAllowedLangNodePeace;
	private int cooldown;

	TownSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, String notAllowedLangNodeWar, String notAllowedLangNodePeace, ConfigNodes ecoPriceConfigNode, String permissionNode, ConfigNodes cooldownConfigNode) {

		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.notAllowedLangNodeWar = notAllowedLangNodeWar;
		this.notAllowedLangNodePeace = notAllowedLangNodePeace;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
		this.cooldown = cooldownConfigNode == null ? 0 : TownySettings.getInt(cooldownConfigNode);
	}

	public void checkIfAllowed(Player player, Town town) throws TownyException {

		if (!isAllowed(player, town)) {
			boolean war = town.hasActiveWar();
			SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
			if(level == SpawnLevel.WAR && !war) {
				throw new TownyException(Translation.of(notAllowedLangNodeWar));
			}
			else if(level == SpawnLevel.PEACE && war) {
				throw new TownyException(Translation.of(notAllowedLangNodePeace));
			}
			throw new TownyException(Translation.of(notAllowedLangNode));
		}
	}

	private boolean isAllowed(Player player, Town town) {

		return this == TownSpawnLevel.ADMIN || (TownyUniverse.getInstance().getPermissionSource().testPermission(player, this.permissionNode)) && (isAllowedTown(town));
	}

	private boolean isAllowedTown(Town town) {
		boolean war = town.hasActiveWar();
		SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
		return level == SpawnLevel.TRUE || (level != SpawnLevel.FALSE && ((level == SpawnLevel.WAR) == war));
	}
	
	public double getCost() {

		return this == TownSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}
	
	public double getCost(Town town) {

		return this == TownSpawnLevel.ADMIN ? 0 : town.getSpawnCost();
	}

	/**
	 * @return the cooldown
	 */
	public int getCooldown() {
		return cooldown;
	}
}
