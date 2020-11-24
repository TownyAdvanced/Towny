package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.Universe;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SiegeWarUniverse implements Universe {
	private static SiegeWarUniverse instance;
	private final Set<Universe> subuniverses = new HashSet<>();
	private final Map<String, Siege> sieges = new ConcurrentHashMap<>();

	private SiegeWarUniverse() {
		TownyUniverse.getInstance().registerSubUniverse(this);
	}

	public static SiegeWarUniverse getInstance() {
		if (instance == null) {
			instance = new SiegeWarUniverse();
		}
		return instance;
	}
	public Map<String, Siege> getSiegesMap() {
		return sieges;
	}

	/**
	 * Clears the object maps.
	 */
	public void clearAllObjects() {
		sieges.clear();
		for (Universe subuniverse : subuniverses) {
			subuniverse.clearAllObjects();
		}
	}
	@Override
	public boolean registerSubUniverse(@NotNull Universe subuniverse) {
		return subuniverses.add(subuniverse);
	}

	@Override
	public boolean deregisterSubUniverse(@NotNull Universe subuniverse) {
		return subuniverses.remove(subuniverse);
	}

	public Set<Player> getPlayersInBannerControlSessions() {
		Set<Player> result = new HashSet<>();
		for (Siege siege : sieges.values()) {
			result.addAll(siege.getBannerControlSessions().keySet());
		}
		return result;
	}
}
