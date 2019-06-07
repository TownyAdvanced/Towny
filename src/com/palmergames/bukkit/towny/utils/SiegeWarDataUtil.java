package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.war.siegewar.SiegeStats;
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
    private static final String SIEGE_STATS_BLOB_ALLY_MAP_ENTRY_SEPARATOR = "£";
    private static final String SIEGE_STATS_BLOB_ALLY_MAP_KEYVALUE_SEPARATOR = "%";
    private static final String SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR = "&";
    private static final String SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR = "@";


    public static String generateSiegeStatsBlob(SiegeStats siegeStats) {
        List<String> values = new ArrayList<>();
        values.add("active" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + Boolean.toString(siegeStats.isActive()));
        values.add("siegePointsTotal" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + siegeStats.getSiegePointsTotal().toString());
        values.add("SiegePointsPrincipal" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + siegeStats.getSiegePointsPrincipal().toString());
        values.add("SiegePointsAllies" + SIEGE_STATS_BLOB_KEY_VALUE_SEPARATOR + StringMgmt.join(siegeStats.getSiegePointsAllies(),
                SIEGE_STATS_BLOB_ALLY_MAP_ENTRY_SEPARATOR,
                SIEGE_STATS_BLOB_ALLY_MAP_KEYVALUE_SEPARATOR));
        return StringMgmt.join(values,SIEGE_STATS_BLOB_ENTRY_SEPARATOR);
    }

    public static String generateNationSiegeStatsMapBlob(Map<Nation, SiegeStats> siegeStatAttackers) {
        List<String> entries = new ArrayList<>();
        for(Nation nation: siegeStatAttackers.keySet()) {
            entries.add(
                    nation.getName() +
                            SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR +
                            generateSiegeStatsBlob(siegeStatAttackers.get(nation)));
        }
        return StringMgmt.join(entries,SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR);
    }


    public static Map<Nation, SiegeStats> unpackSiegeStatsAttackersMapBlob(String blob) throws NotRegisteredException {
        return unpackNationSiegeStatsMap(blob,
                SIEGE_STATS_ATTACKERS_MAP_BLOB_ENTRY_SEPARATOR,
                SIEGE_STATS_ATTACKERS_MAP_BLOB_KEYVALUE_SEPARATOR);
    }


    public static SiegeStats unpackSiegeStatsBlob(String blob) {

        SiegeStats siegeStats = new SiegeStats();
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

        try {
            line = siegeStatsMap.get("siegePointsPrincipal");
            siegeStats.setSiegePointsPrincipal(Integer.parseInt(line));
        } catch (Exception e) {
            siegeStats.setSiegePointsPrincipal(0);
        }

        try {
            line = siegeStatsMap.get("siegePointsAllies");
            siegeStats.setSiegePointsAllies(unpackNationIntegerMap(line,
                    SIEGE_STATS_BLOB_ALLY_MAP_ENTRY_SEPARATOR,
                    SIEGE_STATS_BLOB_ALLY_MAP_KEYVALUE_SEPARATOR));
        } catch (Exception e) {
            siegeStats.setSiegePointsAllies(new HashMap<Nation, Integer>());
        }

        return siegeStats;
    }



    private static Map<Nation,Integer> unpackNationIntegerMap(
            String givenString,
            String entrySeparator,
            String keyValueSeparator) throws NotRegisteredException {
        Map<Nation, Integer> result = new HashMap<Nation, Integer>();

        if(givenString.length() != 0) {
            Nation nation;
            Integer integerValue;
            String[] oneEntryArray;
            String[] allEntriesArray = givenString.split(entrySeparator);
            for (String oneEntryString : allEntriesArray) {
                oneEntryArray = oneEntryString.split(keyValueSeparator);
                nation = TownyUniverse.getDataSource().getNation(oneEntryArray[0]);
                integerValue = Integer.parseInt(oneEntryArray[1]);
                result.put(nation, integerValue);
            }
        }
        return result;
    }

    private static Map<Nation,SiegeStats> unpackNationSiegeStatsMap(
            String givenString,
            String entrySeparator,
            String keyValueSeparator) throws NotRegisteredException {

        Map<Nation, SiegeStats> result = new HashMap<Nation, SiegeStats>();

        if(givenString.length() != 0) {
            String[] allEntriesArray = givenString.split(entrySeparator);

            Nation nation;
            SiegeStats siegeStats;
            String[] oneEntryArray;

            TownyMessaging.sendErrorMsg("Given String:" + givenString);
            TownyMessaging.sendErrorMsg("Entry Separator:" + entrySeparator);

            for (String oneEntryString : allEntriesArray) {

                TownyMessaging.sendErrorMsg("One entry String:" + oneEntryString);
                TownyMessaging.sendErrorMsg("KV separator" + keyValueSeparator);

                oneEntryArray = oneEntryString.split(keyValueSeparator);
                TownyMessaging.sendErrorMsg("1:" + oneEntryArray[0]);
                TownyMessaging.sendErrorMsg("2:" + oneEntryArray[1]);
                nation = TownyUniverse.getDataSource().getNation(oneEntryArray[0]);
                TownyMessaging.sendErrorMsg("Nation:" + nation);
                TownyMessaging.sendErrorMsg("Nation:" + nation.getName());

                siegeStats = unpackSiegeStatsBlob(oneEntryArray[1]);
                TownyMessaging.sendErrorMsg("Siege stats got");

                result.put(nation, siegeStats);
            }
        }
        return result;
    }




}
