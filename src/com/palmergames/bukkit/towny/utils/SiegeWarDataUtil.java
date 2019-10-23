package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.CombatantData;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by Goosius on 03/06/2019.
 */
public class SiegeWarDataUtil {

    private static final String SIEGE_STATS_BLOB_ENTRY_SEPARATOR = ",";
    private static final String SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR = ":";
    private static final String SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR = "&";
    private static final String SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR = "@";


    public static String generateSiegeStatsBlob(CombatantData siegeStats) {
        List<String> values = new ArrayList<>();
        values.add("active" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + Boolean.toString(siegeStats.isActive()));
        values.add("siegePointsTotal" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + siegeStats.getSiegePointsTotal().toString());
        return StringMgmt.join(values,SIEGE_STATS_BLOB_ENTRY_SEPARATOR);
    }

    public static String generateNationSiegeStatsMapBlob(Map<Nation, CombatantData> siegeStatAttackers) {
        List<String> entries = new ArrayList<>();
        for(Nation nation: siegeStatAttackers.keySet()) {
            entries.add(
                    nation.getName() +
                            SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR +
                            generateSiegeStatsBlob(siegeStatAttackers.get(nation)));
        }
        return StringMgmt.join(entries,SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR);
    }


    public static Map<Nation, CombatantData> unpackSiegeStatsAttackersMapBlob(String blob) throws NotRegisteredException {
        return unpackNationSiegeStatsMap(blob,
                SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR,
                SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR);
    }


    public static CombatantData unpackSiegeStatsBlob(String blob) {

        CombatantData siegeStats = new CombatantData();
        Map<String, String> siegeStatsMap = new HashMap<String,String>();
        String[] keysValuesStringArray = blob.split(SIEGE_STATS_BLOB_ENTRY_SEPARATOR);
        String[] keyValueArray;
        String line;

        for(String keyValueString: keysValuesStringArray) {
            keyValueArray = keyValueString.split(SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR);
            if(keyValueArray.length == 2) {
                siegeStatsMap.put(keyValueArray[0], keyValueArray[1]);
            } else {
                siegeStatsMap.put(keyValueArray[0], "");
            }
        }

        try {
            line = siegeStatsMap.get("active");
            siegeStats.setActive(Boolean.parseBoolean(line));
        } catch (Exception e) {
            siegeStats.setActive(false);
        }

        try {
            line = siegeStatsMap.get("siegePointsTotal");
            siegeStats.setSiegePointsTotal(Integer.parseInt(line));
        } catch (Exception e) {
            siegeStats.setSiegePointsTotal(0);
        }

        return siegeStats;
    }

    private static Map<Nation,CombatantData> unpackNationSiegeStatsMap(
            String givenString,
            String entrySeparator,
            String keyValueSeparator) throws NotRegisteredException {

        Map<Nation, CombatantData> result = new HashMap<Nation, CombatantData>();

        if(givenString.length() != 0) {
            String[] allEntriesArray = givenString.split(entrySeparator);

            Nation nation;
            CombatantData siegeStats;
            String[] oneEntryArray;

            for (String oneEntryString : allEntriesArray) {
                oneEntryArray = oneEntryString.split(keyValueSeparator);
                nation = TownyUniverse.getDataSource().getNation(oneEntryArray[0]);
                siegeStats = unpackSiegeStatsBlob(oneEntryArray[1]);
                result.put(nation, siegeStats);
            }
        }
        return result;
    }

}
