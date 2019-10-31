package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Goosius on 02/06/2019.
 */
public class SiegeFront {

    private Siege siege;
    private Nation attackingNation;
    private boolean active;
    private int siegePointsTotal;
    private Location siegeBannerLocation;
    private Map<Player, Long> playerArrivalTimeMap; //player, timestamp of arrival in zone

    public SiegeFront() {
        attackingNation = null;
        siege = null;
        active = false;
        siegePointsTotal = 0;
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
