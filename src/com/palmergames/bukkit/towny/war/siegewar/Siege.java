package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.palmergames.bukkit.towny.utils.SiegeWarUtil.ONE_DAY_IN_MILLIS;

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

    public double getDaysUntilCompletion() {
        double timeRemainingMillis = scheduledEndTime - System.currentTimeMillis();
        double timeRemainingDays = timeRemainingMillis / ONE_DAY_IN_MILLIS;
        return timeRemainingDays;
    }

    public String getFormattedDaysUntilCompletion() {
        NumberFormat numberFormat = NumberFormat.getInstance();
        numberFormat.setMaximumFractionDigits(1);
        return numberFormat.format(getDaysUntilCompletion());
    }

    public int getNumberOfActiveAttackers() {
        int count = 0;
        for (Nation nation : siegeStatsAttackers.keySet()) {
            if (siegeStatsAttackers.get(nation).active) {
                count++;
            }
        }
        return count;
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

    public String getResultString() {
        if (status == null) {
            return "In Progress. No result yet";

        } else if (status == SiegeStatus.ATTACKER_WIN || status == SiegeStatus.DEFENDER_SURRENDER) {
            if (townPlundered) {
                return "Town captured & plundered by " + TownyFormatter.getFormattedNationName(attackerWinner);
            } else {
                return "Town captured by " + TownyFormatter.getFormattedNationName(attackerWinner);
            }

        } else if (status == SiegeStatus.DEFENDER_WIN) {
            return "Attackers driven away";

        } else if (status == SiegeStatus.ATTACKER_ABANDON) {
            return "Attack abandoned.";

        } else {
            TownyMessaging.sendErrorMsg("Unknown siege result");
            return "???";
        }
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
}