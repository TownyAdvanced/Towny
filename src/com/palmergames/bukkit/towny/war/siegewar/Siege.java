package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

/**
 * Created by Goosius on 07/05/2019.
 */
public class Siege {
    private Nation attackingNation;
    private Town defendingTown;
    private SiegeType siegeType;
    private int totalSiegePointsAttacker;
    private int totalSiegePointsDefender;
    private long actualStartTime;   //System time millis
    private long scheduledEndTime;
    private long actualEndTime;
    private String[] objectives;
    private int totalAttackersKilled;  //For report
    private int totalDefendersKilled;  //For report
    private double totalCostToAttacker;  //For report
    private long lastUpkeepTime;      //Siege upkeep occurs 1/hour
    private boolean active;

    public Siege(Nation attackingNation,
                 Town defendingTown) {
        this.attackingNation = attackingNation;
        this.defendingTown = defendingTown;
        this.siegeType = null;
        this.totalSiegePointsAttacker = 0;
        this.totalSiegePointsDefender = 0;
        this.actualStartTime = 0;
        this.scheduledEndTime = 0;
        this.actualEndTime = 0;
        this.totalAttackersKilled = 0;
        this.totalDefendersKilled = 0;
        this.totalCostToAttacker = 0;
        this.lastUpkeepTime = 0;
        this.active = false;
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public void setSiegeType(SiegeType siegeType) {
        this.siegeType = siegeType;
    }

    public void setObjectives(String[] objectives) {
        this.objectives = objectives;
    }

    public Nation getAttackingNation() {
        return attackingNation;
    }

    public SiegeType getSiegeType() {
        return siegeType;
    }

    public int getTotalSiegePointsAttacker() {
        return totalSiegePointsAttacker;
    }

    public int getTotalSiegePointsDefender() {
        return totalSiegePointsDefender;
    }

    public long getActualStartTime() {
        return actualStartTime;
    }

    public long getScheduledEndTime() {
        return scheduledEndTime;
    }

    public long getActualEndTime() {
        return actualEndTime;
    }

    public int getTotalAttackersKilled() {
        return totalAttackersKilled;
    }

    public int getTotalDefendersKilled() {
        return totalDefendersKilled;
    }

    public double getTotalCostToAttacker() {
        return totalCostToAttacker;
    }

    public long getLastUpkeepTime() {
        return lastUpkeepTime;
    }

    public void setTotalSiegePointsAttacker(int totalSiegePointsAttacker) {
        this.totalSiegePointsAttacker = totalSiegePointsAttacker;
    }

    public void setTotalSiegePointsDefender(int totalSiegePointsDefender) {
        this.totalSiegePointsDefender = totalSiegePointsDefender;
    }

    public void setActualStartTime(long actualStartTime) {
        this.actualStartTime = actualStartTime;
    }

    public void setScheduledEndTime(long scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public void setActualEndTime(long actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public void setTotalAttackersKilled(int totalAttackersKilled) {
        this.totalAttackersKilled = totalAttackersKilled;
    }

    public void setTotalDefendersKilled(int totalDefendersKilled) {
        this.totalDefendersKilled = totalDefendersKilled;
    }

    public void setTotalCostToAttacker(double totalCostToAttacker) {
        this.totalCostToAttacker = totalCostToAttacker;
    }

    public void setLastUpkeepTime(long lastUpkeepTime) {
        this.lastUpkeepTime = lastUpkeepTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
