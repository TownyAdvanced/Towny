package com.palmergames.bukkit.towny.war.siegewar.locations;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.util.TimeMgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
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

	public List<SiegeZone> getActiveSiegeZones() {
		List<SiegeZone> result = new ArrayList<>();
		for(SiegeZone siegeZone: siegeZones.values()) {
			if(siegeZone.isActive())
				result.add(siegeZone);
		}
		return result;
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

    public void setSiegeZones(Map<Nation, SiegeZone> siegeZones) {
        this.siegeZones = siegeZones;
    }

    public List<Nation> getActiveAttackers() {
        List<Nation> result = new ArrayList<>();
        for (Nation nation : new ArrayList<Nation>(siegeZones.keySet())) {
            if (siegeZones.get(nation) != null
                    && siegeZones.get(nation).isActive()) {
                result.add(nation);
            }
        }
        return result;
    }

    public List<Nation> getAllAttackers() {
        return new ArrayList<Nation>(siegeZones.keySet());
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

    public String getWinnerName() {
        switch(status) {
            case ATTACKER_WIN:
            case DEFENDER_SURRENDER:
                return TownyFormatter.getFormattedNationName(attackerWinner);
            case DEFENDER_WIN:
            case ATTACKER_ABANDON:
                if(defendingTown.hasNation()) {
                    try {
                        return TownyFormatter.getFormattedNationName(defendingTown.getNation());
                    } catch (NotRegisteredException e) {
                        e.printStackTrace();
                    }
                } else {
                    return TownyFormatter.getFormattedTownName(defendingTown);
                }
            case IN_PROGRESS:
                return "n/a";
            default:
                return "Unknown siege status";
        }
    }

    public boolean getTownPlundered() {
        return townPlundered;
    }

    public boolean getTownInvaded() {
        return townInvaded;
    }

    public void setAllSiegeZonesToInactive() {
        for(SiegeZone siegeZone: siegeZones.values()) {
            siegeZone.setActive(false);
        }
    }
}