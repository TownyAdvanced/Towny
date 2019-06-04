package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_HOUR_IN_MILLIS;
import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_MINUTE_IN_MILLIS;

/**
 * Created by Goosius on 07/05/2019.
 */
public class Siege {
    private Town defendingTown;
    private boolean active;
    private long actualStartTime;
    private long scheduledEndTime;
    private long actualEndTime;
    private long nextUpkeepTime;
    private SiegeStats siegeStatsDefenders;
    private Map<Nation, SiegeStats> siegeStatsAttackers;

    public Siege(Town defendingTown) {
        this.defendingTown = defendingTown;

        //TODO ----Dontdo this stuff  here  as it is wasted during loads

        this.active = true;
        this.scheduledEndTime =
                (System.currentTimeMillis() + TownySettings.getWarSiegeMaxHoldoutTimeHours())
                * ONE_HOUR_IN_MILLIS;
        this.actualEndTime = 0;
        this.siegeStatsDefenders = new SiegeStats();
        this.siegeStatsAttackers = new HashMap<Nation, SiegeStats>();
        this.nextUpkeepTime = System.currentTimeMillis() + ONE_MINUTE_IN_MILLIS;
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public Map<Nation, SiegeStats> getSiegeStatsAttackers() {
        return siegeStatsAttackers;
    }

    public SiegeStats getSiegeStatsDefenders() {
        return siegeStatsDefenders;
    }


    public long getScheduledEndTime() {
        return scheduledEndTime;
    }

    public long getActualEndTime() {
        return actualEndTime;
    }


    public long getNextUpkeepTime() {
        return nextUpkeepTime;
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

    public void setNextUpkeepTime(long nextUpkeepTime) {
        this.nextUpkeepTime = nextUpkeepTime;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public long getActualStartTime() {
        return actualStartTime;
    }

    public void setSiegeStatsDefenders(SiegeStats defendersSiegeStats) {
        this.siegeStatsDefenders = defendersSiegeStats;
    }

    public void setSiegeStatsAttackers(Map<Nation, SiegeStats> siegeStatsAttackers) {
        this.siegeStatsAttackers = siegeStatsAttackers;
    }

    public String getSiegeHoursUntilCompletionString() {
        double hoursRemainingMillis = scheduledEndTime - System.currentTimeMillis();
        double hoursRemaining = hoursRemainingMillis / ONE_HOUR_IN_MILLIS;
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
        return numberFormat.format(hoursRemaining);
    }
}
