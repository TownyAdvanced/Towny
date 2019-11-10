package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
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
    private Nation attackingNation; //Can be if nation is deleted
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

    public SiegeZone(Siege siege, Nation attackingNation) {
        this.siege = siege;
        this.attackingNation = attackingNation;
        active = false;
        siegePoints = 0;
        siegeBannerLocation = null;
        playerArrivalTimeMap = new HashMap<>();
    }

    public String getName() {
        return attackingNation.getName().toLowerCase() + "$vs$" + siege.getDefendingTown().getName().toLowerCase();
    }

    public static String generateName(String attackingNationName, String defendingTownName) {
        return attackingNationName.toLowerCase() + "$vs$" + defendingTownName.toLowerCase();
    }

    public static String generateName(Nation nation,Town town) {
        return generateName(nation.getName(), town.getName());
    }

    public static String[] generateTownAndNationName(String siegeZoneName) {
        return siegeZoneName.split("$vs$");
    }

    public Siege getSiege() {
        return siege;
    }

    public Nation getAttackingNation() {
        return attackingNation;
    }

    public Location getFlagLocation() {
        return siegeBannerLocation;
    }

    public void setFlagLocation(Location location) {
        this.siegeBannerLocation = location;
    }

    public Map<Player, Long> getPlayerArrivalTimeMap() {
        return playerArrivalTimeMap;
    }

    public Map<String, Long> getPlayerNameArrivalTimeMap() {
        Map<String, Long> result = new HashMap<>();
        for(Map.Entry<Player, Long> entry: playerArrivalTimeMap.entrySet()) {
            result.put(entry.getKey().getName().toLowerCase(),
                    entry.getValue());
        }
        return result;
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

    public void setAttackingNation(Nation attackingNation) {
        this.attackingNation = attackingNation;
    }
}
