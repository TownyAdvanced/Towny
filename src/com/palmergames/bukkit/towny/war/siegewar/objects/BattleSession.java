package com.palmergames.bukkit.towny.war.siegewar.objects;

/**
 * This class represents a "Battle Session".
 *
 * A battle session represents a period of time,
 * in which a player's siege-fighting-time is being automatically moderated.
 * 
 * A session has a number of phases
 * phase 1 - active  (default 55 mins)
 * phase 2 - active but first warning given  (default 4 mins)
 * phase 3 - active but second warning given (default 1 min)
 * phase 4 - expired (default 15 mins)
 * 
 * This mechanic is useful to control the amount of time players are spending fighting during a siege.
 * This can be expected to generally reduce stress and exhaustion for players.
 * This is particularly important as sieges are generally moderately long (e.g. 3 days).
 * 
 * The feature enable switch, and duration of the phases, are set in the configuration file.
 **
 * @author Goosius
 */
public class BattleSession {

	private long firstWarningTime;
	private boolean firstWarningGiven;
	private long secondWarningTime;
	private boolean secondWarningGiven;
	private long expiryTime;
	private boolean expired;
	private long deletionTime;
	
	public BattleSession() {
		firstWarningTime = 0;
		firstWarningGiven = false;
		secondWarningTime = 0;
		secondWarningGiven = false;
		expired = false;
		expiryTime = 0;
		deletionTime = 0;
	}

	public boolean isExpired() {
		return expired;
	}

	public void setExpired(boolean expired) {
		this.expired = expired;
	}

	public boolean isFirstWarningGiven() {
		return firstWarningGiven;
	}

	public void setFirstWarningGiven(boolean firstWarningGiven) {
		this.firstWarningGiven = firstWarningGiven;
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

	public long getFirstWarningTime() {
		return firstWarningTime;
	}

	public void setFirstWarningTime(long firstWarningTime) {
		this.firstWarningTime = firstWarningTime;
	}

	public long getSecondWarningTime() {
		return secondWarningTime;
	}

	public void setSecondWarningTime(long secondWarningTime) {
		this.secondWarningTime = secondWarningTime;
	}

	public boolean isSecondWarningGiven() {
		return secondWarningGiven;
	}

	public void setSecondWarningGiven(boolean secondWarningGiven) {
		this.secondWarningGiven = secondWarningGiven;
	}
}
