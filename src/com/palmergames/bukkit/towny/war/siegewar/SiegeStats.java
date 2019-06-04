package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Created by Goosius on 02/06/2019.
 */
public class SiegeStats {

    boolean active;
    int siegePointsTotal;
    int siegePointsPrincipal;
    Map<Nation, Integer> siegePointsAllies;

    public SiegeStats() {
        active = false;
        siegePointsTotal = 0;
        siegePointsPrincipal = 0;
        siegePointsAllies = new HashMap<Nation, Integer>();
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

    public Integer getSiegePointsPrincipal() {
        return siegePointsPrincipal;
    }

    public Map<Nation, Integer> getSiegePointsAllies() {
        return siegePointsAllies;
    }

    public void setSiegePointsTotal(int siegePointsTotal) {
        this.siegePointsTotal = siegePointsTotal;
    }

    public void setSiegePointsPrincipal(int siegePointsPrincipal) {
        this.siegePointsPrincipal = siegePointsPrincipal;
    }

    public void setSiegePointsAllies(Map<Nation, Integer> siegePointsAllies) {
        this.siegePointsAllies = siegePointsAllies;
    }
}
