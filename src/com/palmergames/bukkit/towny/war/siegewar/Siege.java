package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

/**
 * Created by Goosius on 07/05/2019.
 */
public class Siege {
    //The following are saved in DB
    private Nation attackingNation;
    private Town defendingTown;
    private String siegeType;  //assault  or revolt   //todo - maybe enum?
    private int totalSiegePointsAttacker;
    private int totalSiegePointsDefender;
    private long actualStartTime;   //System time millis
    private long scheduledEndTime;
    private long actualEndTime;
    private boolean objectivePlunder;
    private boolean objectiveInvasion;
    private boolean objectiveDisrupt;
    private boolean objectivePoison;
    private int totalAttackersKilled;  //For report
    private int totalDefendersKilled;  //For report
    private int totalCostToAttacker;  //For report
    private long lastUpkeepTime;      //Siege upkeep occurs 1/hour

    //The following are saved in memory only, not DB
    boolean attackActionInProgress;
    long attackActionStartTime;
    boolean defenceActionInProgress;
    long defenceActionStartTime;


}
