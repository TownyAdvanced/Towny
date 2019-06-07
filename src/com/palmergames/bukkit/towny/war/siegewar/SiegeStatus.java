package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.exceptions.TownyException;

/**
 * Created by Goosius on 06/06/2019.
 */
public enum SiegeStatus {
    IN_PROGRESS, ATTACKER_WIN, DEFENDER_WIN, ATTACKER_ABANDON, DEFENDER_SURRENDER, UNKNOWN;

    public static SiegeStatus parseString(String line) {
        switch (line) {
            case "IN_PROGRESS":
                return IN_PROGRESS;
            case "ATTACKER_WIN":
                return ATTACKER_WIN;
            case "DEFENDER_WIN":
                return DEFENDER_WIN;
            case "ATTACKER_ABANDON":
                return ATTACKER_ABANDON;
            case "DEFENDER_SURRENDER":
                return DEFENDER_SURRENDER;
            default:
                return UNKNOWN;
        }
    }
}
