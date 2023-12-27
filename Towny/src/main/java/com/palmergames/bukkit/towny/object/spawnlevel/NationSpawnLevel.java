package com.palmergames.bukkit.towny.object.spawnlevel;

import com.palmergames.bukkit.towny.TownyUniverse;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public enum NationSpawnLevel {
	PART_OF_NATION(
			ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN,
			"msg_err_nation_spawn_forbidden",
			"msg_err_nation_spawn_forbidden_war",
			"msg_err_nation_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL,
			PermissionNodes.TOWNY_NATION_SPAWN_NATION.getNode(),
			ConfigNodes.SPAWNING_NATION_SPAWN_NATION_MEMBER_COOLDOWN_TIMER),
	NATION_ALLY(
			ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN_TRAVEL_ALLY,
			"msg_err_nation_spawn_ally_forbidden",
			"msg_err_nation_spawn_nation_forbidden_war",
			"msg_err_nation_spawn_nation_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_ALLY,
			PermissionNodes.TOWNY_NATION_SPAWN_ALLY.getNode(),
			ConfigNodes.SPAWNING_NATION_SPAWN_NATION_ALLY_COOLDOWN_TIMER),
	UNAFFILIATED(
			ConfigNodes.SPAWNING_ALLOW_NATION_SPAWN_TRAVEL,
			"msg_err_public_nation_spawn_forbidden",
			"msg_err_public_nation_spawn_forbidden_war",
			"msg_err_public_nation_spawn_forbidden_peace",
			ConfigNodes.ECO_PRICE_TOWN_SPAWN_TRAVEL_PUBLIC,
			PermissionNodes.TOWNY_NATION_SPAWN_PUBLIC.getNode(),
			ConfigNodes.SPAWNING_NATION_SPAWN_NATION_UNAFFILIATED_COOLDOWN_TIMER),
	ADMIN(
			null,
			null,
			null,
			null,
			null,
			null,
			null);

	private final ConfigNodes isAllowingConfigNode;
	private final ConfigNodes ecoPriceConfigNode;
	private final String permissionNode;
	private final String notAllowedLangNode;
	private final String notAllowedLangNodeWar;
	private final String notAllowedLangNodePeace;
	private final int cooldown;

	NationSpawnLevel(ConfigNodes isAllowingConfigNode, String notAllowedLangNode, String notAllowedLangNodeWar, String notAllowedLangNodePeace, ConfigNodes ecoPriceConfigNode, String permissionNode, ConfigNodes cooldownConfigNode) {

		this.isAllowingConfigNode = isAllowingConfigNode;
		this.notAllowedLangNode = notAllowedLangNode;
		this.notAllowedLangNodeWar = notAllowedLangNodeWar;
		this.notAllowedLangNodePeace = notAllowedLangNodePeace;
		this.ecoPriceConfigNode = ecoPriceConfigNode;
		this.permissionNode = permissionNode;
		this.cooldown = cooldownConfigNode == null ? 0 : TownySettings.getInt(cooldownConfigNode);
	}

	public void checkIfAllowed(Player player, Nation nation) throws TownyException {

		if (!isAllowed(player, nation)) {
			boolean war = nation.hasActiveWar();
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

	private boolean isAllowed(Player player, Nation nation) {

		return this == NationSpawnLevel.ADMIN || (TownyUniverse.getInstance().getPermissionSource().testPermission(player, this.permissionNode)) && (isAllowedNation(nation));
	}
	
	private boolean isAllowedNation(Nation nation) {
		boolean war = nation.hasActiveWar();
		SpawnLevel level = TownySettings.getSpawnLevel(this.isAllowingConfigNode);
		return level == SpawnLevel.TRUE || (level != SpawnLevel.FALSE && ((level == SpawnLevel.WAR) == war));
	}

	public double getCost() {

		return this == NationSpawnLevel.ADMIN ? 0 : TownySettings.getDouble(ecoPriceConfigNode);
	}

	public double getCost(Nation nation) {

		return this == NationSpawnLevel.ADMIN ? 0 : nation.getSpawnCost();
	}

	/**
	 * @return the cooldown
	 */
	public int getCooldown() {
		return cooldown;
	}
}
