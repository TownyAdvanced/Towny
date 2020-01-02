package com.palmergames.bukkit.towny.war.siegewar.locations;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.util.TimeMgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.palmergames.util.TimeMgmt.ONE_HOUR_IN_MILLIS;

/**
 * This class represents a "Siege".
 * 
 * A siege is an attack by one or more nations on a particular town.
 * 
 * A siege is initiated by a nation leader with appropriate permissions,
 * It typically lasts for a moderate duration (e.g. hours or days),
 * and can be ended n a number of ways, including abandon, surrender, or points victory.
 * 
 * After a siege ends, it enters an aftermath phase where the status is no longer "In Progress",
 * During this phase, the town cannot be attacked again,
 * and if an attacker has won, they have the options of "plunder" or "invade".
 *
 * @author Goosius
 */
public class Siege {
    private Town defendingTown;
    private SiegeStatus status;
    private boolean townPlundered;
    private boolean townInvaded;
    private Nation attackerWinner;
    private long startTime;           //Start of siege
    private long scheduledEndTime;    //Scheduled end of siege
    private long actualEndTime;       //Actual end time of siege
    private Map<Nation, SiegeZone> siegeZones;

    public Siege(Town defendingTown) {
        this.defendingTown = defendingTown;
        status = SiegeStatus.IN_PROGRESS;
        this.attackerWinner = null;
        siegeZones =new HashMap<>();
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public Map<Nation, SiegeZone> getSiegeZones() {
        return siegeZones;
    }
    
    public long getScheduledEndTime() {
        return scheduledEndTime;
    }

    public long getActualEndTime() {
        return actualEndTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setScheduledEndTime(long scheduledEndTime) {
        this.scheduledEndTime = scheduledEndTime;
    }

    public void setActualEndTime(long actualEndTime) {
        this.actualEndTime = actualEndTime;
    }

    public long getStartTime() {
        return startTime;
    }
    
	public List<String> getSiegeZoneNames() {
    	List<String> names = new ArrayList<>();
    	for(SiegeZone siegeZone: siegeZones.values()) {
    		names.add(siegeZone.getName());
		}
    	return names;
	}
	
	public void setStatus(SiegeStatus status) {
        this.status = status;
    }

    public void setTownPlundered(boolean townPlundered) {
        this.townPlundered = townPlundered;
    }

    public void setTownInvaded(boolean townInvaded) {
        this.townInvaded = townInvaded;
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

    public boolean isTownInvaded() {
        return townInvaded;
    }
    public Nation getAttackerWinner() {
        return attackerWinner;
    }

    public boolean hasAttackerWinner() {
        return attackerWinner != null;
    }

    public double getTimeUntilCompletionMillis() {
        return scheduledEndTime - System.currentTimeMillis();
    }

    public String getFormattedHoursUntilScheduledCompletion() {
        if(status == SiegeStatus.IN_PROGRESS) {
            double timeUntilCompletionMillis = getTimeUntilCompletionMillis();
            return TimeMgmt.getFormattedTimeValue(timeUntilCompletionMillis);
        } else {
            return "0";
        }
    }

    public boolean getTownPlundered() {
        return townPlundered;
    }

    public boolean getTownInvaded() {
        return townInvaded;
    }

	public long getDurationMillis() {
		return System.currentTimeMillis() - startTime;
	}

	public long getTimeUntilSurrenderIsAllowedMillis() {
		return (long)((TownySettings.getWarSiegeMinSiegeDurationBeforeSurrenderHours() * ONE_HOUR_IN_MILLIS) - getDurationMillis());
	}

	public long getTimeUntilAbandonIsAllowedMillis() {
		return (long)((TownySettings.getWarSiegeMinSiegeDurationBeforeAbandonHours() * ONE_HOUR_IN_MILLIS) - getDurationMillis());
	}
}