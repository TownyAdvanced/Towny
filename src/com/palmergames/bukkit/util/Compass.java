package com.palmergames.bukkit.util;

public class Compass {
	public enum Point {
		N,
		NE,
		E,
		SE,
		S,
		SW,
		W,
		NW
	}
	public static Compass.Point getCompassPointForDirection(double inDegrees) {
		double degrees = (inDegrees - 90) % 360 ;
		if (degrees < 0)
			degrees += 360;
		
		if (0 <= degrees && degrees < 22.5)
			return Compass.Point.N;
		else if (22.5 <= degrees && degrees < 67.5)
			return Compass.Point.NE;
		else if (67.5 <= degrees && degrees < 112.5)
			return Compass.Point.E;
		else if (112.5 <= degrees && degrees < 157.5)
			return Compass.Point.SE;
		else if (157.5 <= degrees && degrees < 202.5)
			return Compass.Point.S;
		else if (202.5 <= degrees && degrees < 247.5)
			return Compass.Point.SW;
		else if (247.5 <= degrees && degrees < 292.5)
			return Compass.Point.W;
		else if (292.5 <= degrees && degrees < 337.5)
			return Compass.Point.NW;
		else if (337.5 <= degrees && degrees < 360.0)
			return Compass.Point.N;
		else
			return null;
    }
}
