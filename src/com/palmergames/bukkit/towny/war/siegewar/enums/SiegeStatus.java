package com.palmergames.bukkit.towny.war.siegewar.enums;

/**
 * This class represents the "status" of a siege
 * 
 * "In Process" means the siege is active, with the outcome not yet decided
 * Any other status means the siege has "finished", with the outcome decided.
 * 
 * @author Goosius
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
