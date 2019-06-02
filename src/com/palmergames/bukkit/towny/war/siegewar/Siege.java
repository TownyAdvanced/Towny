package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_HOUR_IN_MILLIS;

/**
 * Created by Goosius on 07/05/2019.
 */
public class Siege {
    private UUID id;
    private Town defendingTown;
    private boolean siegeActive;
    private long siegeScheduledEndTime;
    private long siegeActualEndTime;
    private SiegeStats siegeStatsDefenders;
    private HashMap<Nation, SiegeStats> siegeStatsAttackers;

    public Siege(Town defendingTown) {
        id = UUID.randomUUID();
        this.defendingTown = defendingTown;
        this.siegeActive = true;
        this.siegeScheduledEndTime =
                (System.currentTimeMillis() + TownySettings.getWarSiegeMaxHoldoutTimeHours())
                * ONE_HOUR_IN_MILLIS;
        this.siegeActualEndTime = 0;
        this.siegeStatsDefenders = new SiegeStats();
        this.siegeStatsAttackers = new HashMap<Nation, SiegeStats>();
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public HashMap<Nation, SiegeStats> getSiegeStatsAttackers() {
        return siegeStatsAttackers;
    }

    public SiegeStats getSiegeStatsDefenders() {
        return siegeStatsDefenders;
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

    public boolean isComplete() {
        return complete;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setComplete(boolean complete) {
        this.complete = complete;
    }

    public String getName() {
        return getAttackingNation().getName() + "_vs_" + getDefendingTown().getName();
    }

    public void addAttacker(Nation attackingNation) {
        SiegeStats stats = new SiegeStats();
        siegeStatsAttackers.put(attackingNation, stats);
    }

    public Set<Nation> getAttackingNations() {
        return siegeStatsAttackers.keySet();
    }
}
