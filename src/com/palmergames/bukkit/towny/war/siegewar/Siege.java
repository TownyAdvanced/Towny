package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_DAY_IN_MILLIS;
import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_HOUR_IN_MILLIS;

/**
 * Created by Goosius on 07/05/2019.
 */
public class Siege {
    private Town defendingTown;
    private SiegeStatus status;
    private boolean townPlundered;
    private Nation attackerWinner;
    private long actualStartTime;
    private long scheduledEndTime;
    private long actualEndTime;
    private long nextUpkeepTime;
    private SiegeStats siegeStatsDefenders;
    private Map<Nation, SiegeStats> siegeStatsAttackers;


    public Siege(Town defendingTown) {
        this.defendingTown = defendingTown;
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

    public long getActualStartTime() {
        return actualStartTime;
    }

    public void setSiegeStatsDefenders(SiegeStats defendersSiegeStats) {
        this.siegeStatsDefenders = defendersSiegeStats;
    }

    public void setSiegeStatsAttackers(Map<Nation, SiegeStats> siegeStatsAttackers) {
        this.siegeStatsAttackers = siegeStatsAttackers;
    }

    public List<Nation> getActiveAttackers() {
        List<Nation> result = new ArrayList<>();
        for (Nation nation : new ArrayList<Nation>(siegeStatsAttackers.keySet())) {
            if (siegeStatsAttackers.get(nation) != null
                    && siegeStatsAttackers.get(nation).isActive()) {
                result.add(nation);
            }
        }
        return result;
    }

    public void setStatus(SiegeStatus status) {
        this.status = status;
    }

    public void setTownPlundered(boolean townPlundered) {
        this.townPlundered = townPlundered;
    }

    public void setAttackerWinner(Nation attackerWinner) {
        this.attackerWinner = attackerWinner;
    }

    public SiegeStatus getStatus() {
        return status;
    }

    public boolean isTownPlundered() {
        return townPlundered;
    }

    public Nation getAttackerWinner() {
        return attackerWinner;
    }

    public boolean hasAttackerWinner() {
        return attackerWinner != null;
    }

    public double getHoursUntilCompletion() {
        double timeRemainingMillis = scheduledEndTime - System.currentTimeMillis();
        double timeRemainingDays = timeRemainingMillis / ONE_HOUR_IN_MILLIS;
        return timeRemainingDays;
    }

    public String getFormattedHoursUntilCompletion() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
        double hoursRoundedUp = Math.ceil(getHoursUntilCompletion() * 10) / 10;
        return numberFormat.format(hoursRoundedUp);
    }
}