package com.palmergames.bukkit.towny.war.siegewar;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Goosius on 02/06/2019.
 */
public class CombatantData {

    private boolean active;
    private int siegePointsTotal;
    private Location siegeBannerLocation;
    private Map<Player, Long> combatantTimestampMap; //player, timestamp of arrival in zone

    public CombatantData() {
        active = false;
        siegePointsTotal = 0;
        siegeBannerLocation = null;
        combatantTimestampMap = new HashMap<>();
    }

    public Location getSiegeBannerLocation() {
        return siegeBannerLocation;
    }

    public void setSiegeBannerLocation(Location location) {
        this.siegeBannerLocation = location;
    }

    public Map<Player, Long> getCombatantTimestampMap() {
        return combatantTimestampMap;
    }

    public Integer getSiegePointsTotal() {
        return siegePointsTotal;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setSiegePointsTotal(int siegePointsTotal) {
        this.siegePointsTotal = siegePointsTotal;
    }

    public void addSiegePoints(int siegePointsForAttackingPlayer) {
        siegePointsTotal += siegePointsForAttackingPlayer;
    }
}
