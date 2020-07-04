package com.palmergames.bukkit.towny.war.siegewar.objects;

/**
 * This class represents a "Battle Session".
 *
 * A battle session represents a period of time,
 * in which a player's siege-fighting-time is being automatically moderated.
 *
 * A battle session starts when a player enters a siege zone, and has 2 phases
 * phase 1 - active - In this phase, the player can attend any siege
 * phase 2 - expired - In this phase the player must stay away from all sieges
 * 
 * This mechanic is useful to control the amount of time players are spending fighting during a siege.
 * This can be expected to generally reduce stress and exhaustion for players.
 * This is particularly important as sieges are generally moderately long (e.g. 3 days).
 * 
 * The feature enable switch, and duration of phases, are set in the configuration file.
 **
 * @author Goosius
 */
public class BattleSession {
	private boolean expired;
	private boolean warningGiven;
	private long expiryTime;
	private long deletionTime;
	
	public BattleSession() {
		expired = false;
		warningGiven = false;
		expiryTime = 0;
		deletionTime = 0;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public boolean isWarningGiven() {
		return warningGiven;
	}

	public void setWarningGiven(boolean warningGiven) {
		this.warningGiven = warningGiven;
	}

	public long getExpiryTime() {
		return expiryTime;
	}

	public void setExpiryTime(long expiryTime) {
		this.expiryTime = expiryTime;
	}

	public long getDeletionTime() {
		return deletionTime;
	}

	public void setDeletionTime(long deletionTime) {
		this.deletionTime = deletionTime;
	}
}
