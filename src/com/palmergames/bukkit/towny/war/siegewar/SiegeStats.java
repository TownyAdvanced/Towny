package com.palmergames.bukkit.towny.war.siegewar;

/**
 * Created by Goosius on 02/06/2019.
 */
public class SiegeStats {

    private boolean active;
    private int siegePointsTotal;

    public SiegeStats() {
        active = false;
        siegePointsTotal = 0;
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
}
