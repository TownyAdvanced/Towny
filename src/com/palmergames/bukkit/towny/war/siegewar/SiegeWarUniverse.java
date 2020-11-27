package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SiegeWarUniverse {
    private static SiegeWarUniverse instance;
    private final Map<String, Siege> sieges = new ConcurrentHashMap<>();

    private SiegeWarUniverse() {
    }

    public static SiegeWarUniverse getInstance() {
        if (instance == null) {
            instance = new SiegeWarUniverse();
        }
        return instance;
    }

    /**
     * Clear the siege object map.
     */
    public void clearAllObjects() {
        sieges.clear();
    }

    public Map<String, Siege> getSiegesMap() {
        return sieges;
    }

    public Set<Player> getPlayersInBannerControlSessions() {
        Set<Player> result = new HashSet<>();
        for (Siege siege : sieges.values()) {
            result.addAll(siege.getBannerControlSessions().keySet());
        }
        return result;
    }
}
