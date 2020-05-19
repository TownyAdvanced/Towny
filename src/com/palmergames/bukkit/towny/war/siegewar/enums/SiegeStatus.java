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
    IN_PROGRESS(true), 
	ATTACKER_WIN(false), 
	DEFENDER_WIN(false), 
	ATTACKER_ABANDON(false), 
	DEFENDER_SURRENDER(false), 
	PENDING_ATTACKER_ABANDON(true), 
	PENDING_DEFENDER_SURRENDER(true), 
	UNKNOWN(false);

    private boolean active;
    
    SiegeStatus(boolean active) {
    	this.active = active;
	}
    
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
			case "PENDING_ATTACKER_ABANDON":
				return PENDING_ATTACKER_ABANDON;
			case "PENDING_DEFENDER_SURRENDER":
				return PENDING_DEFENDER_SURRENDER;
            default:
                return UNKNOWN;
        }
    }

	/**
	 * @return true if we are in the 'active' phase of the siege
	 */
	public boolean isActive() {
    	return this.active;
	}
}
