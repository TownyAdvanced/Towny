package com.palmergames.bukkit.towny.war.siegewar.objects;

/**
 * This class represents the distance between a siege and some arbitrary point.
 * 
 * It is used internally to process distance validations.
 *
 * @author Goosius
 */

public class SiegeDistance {
	private Siege siege;
	private double distance;
	
	public SiegeDistance(Siege siege, double distance) {
		this.siege = siege;
		this.distance = distance;
	}

	public Siege getSiege() {
		return siege;
	}

	public double getDistance() {
		return distance;
	}
}
