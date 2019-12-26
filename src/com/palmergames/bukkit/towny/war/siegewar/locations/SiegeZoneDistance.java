package com.palmergames.bukkit.towny.war.siegewar.locations;

/**
 * This class represents the distance between a siege zone and some arbitrary point.
 * 
 * It is used internally to process distance validations.
 *
 * @author Goosius
 */

public class SiegeZoneDistance {
	private SiegeZone siegeZone;
	private double distance;
	
	public SiegeZoneDistance(SiegeZone siegeZone, double distance) {
		this.siegeZone = siegeZone;
		this.distance = distance;
	}

	public SiegeZone getSiegeZone() {
		return siegeZone;
	}

	public double getDistance() {
		return distance;
	}
}
