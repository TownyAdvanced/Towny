package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by Goosius on 02/06/2019.
 */
public class SiegeStats {

    private UUID id;
    boolean active;
    int siegePointsTotal;
    int siegePointsPrincipal;
    HashMap<Nation, Integer> siegePointsAllies;

    public SiegeStats() {
        id = UUID.randomUUID();
        active = true;
        siegePointsTotal = 0;
        siegePointsPrincipal = 0;
        siegePointsAllies = new HashMap<Nation, Integer>();
    }

    public int getSiegePointsTotal() {
        return siegePointsTotal;
    }


    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }
}
