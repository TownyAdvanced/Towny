package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Goosius on 02/06/2019.
 */
public class SiegeZone {

    private Siege siege;
    private Location siegeBannerLocation;
    private Nation attackingNation;
    private boolean active;
    private int siegePoints;
    private Map<Player, Long> playerArrivalTimeMap; //player, timestamp of arrival in zone

    public SiegeZone() {
        attackingNation = null;
        siege = null;
        active = false;
        siegePoints = 0;
        siegeBannerLocation = null;
        playerArrivalTimeMap = new HashMap<>();
    }

    public Siege getSiege() {
        return siege;
    }

    public Nation getAttackingNation() {
        return attackingNation;
    }

    public Location getSiegeBannerLocation() {
        return siegeBannerLocation;
    }

    public void setSiegeBannerLocation(Location location) {
        this.siegeBannerLocation = location;
    }

    public Map<Player, Long> getPlayerArrivalTimeMap() {
        return playerArrivalTimeMap;
    }

    public Integer getSiegePoints() {
        return siegePoints;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isActive() {
        return active;
    }

    public void setSiegePoints(int siegePoints) {
        this.siegePoints = siegePoints;
    }

    public void addSiegePoints(int siegePointsForAttackingPlayer) {
        siegePoints += siegePointsForAttackingPlayer;
    }

    public String getSiegeBannerLocationForSerialization() {
        return siegeBannerLocation.getBlockX()
                + "," + siegeBannerLocation.getBlockY()
                + "," + siegeBannerLocation.getBlockZ();
    }

    public String getPlayerArrivalTimeMapForSerialization() {
       StringBuilder result = new StringBuilder();
       boolean firstEntry = true;

       for(Map.Entry<Player, Long> entry: playerArrivalTimeMap.entrySet()) {
           if(firstEntry){
              firstEntry = false;
           } else {
               result.append(", ");
           }
           result.append(entry.getKey().getName());
           result.append("@");
           result.append(entry.getValue());
       }
       return result.toString();
    }

    public void setSiege(Siege siege) {
        this.siege = siege;
    }
}
