package com.palmergames.util;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translatable;

public class MathUtil {

	/**
	 * Compute the square of a number.
	 * 
	 * NOTE: This function does not perform any safe-checks against overflow / underflow.
	 * @param a Number to square
	 * @return Square of the parameter passed in.
	 */
	public static double sqr(double a) {
		return a * a;
	}

	/**
	 * Compute the pythagorean square distance based on the formula a^2 + b^2 = c^2.
	 * 
	 * @param a Length of an axis 
	 * @param b Length of an axis
	 * @return The distance squared between two axis (which is c^2)
	 */
	public static double distanceSquared(double a, double b) {
		return sqr(a) + sqr(b);
	}

	/**
	 * Compute the shortest distance between two perpendicular lines.
	 * 
	 * @param a Length of a line
	 * @param b Length of another line
	 * @return the shortest distance between two lines
	 */
	public static double distance(double a, double b) {
		return Math.sqrt(distanceSquared(a, b));
	}

	/**
	 * Compute the shortest distance between two points in a 2d grid.
	 * 
	 * Uses pythagorean formula.
	 * 
	 * @param x1 X-coord of point 1
	 * @param x2 X-coord of point 2
	 * @param y1 Y-coord of point 1
	 * @param y2 Y-coord of point 2
	 * @return shortest distance between two points.
	 */
	public static double distance(double x1, double x2, double y1, double y2) {
		return distance(x1 - x2, y1 - y2);
	}
	
	public static double distance(Coord coord1, Coord coord2) {
		return distance(coord1.getX(), coord2.getX(), coord1.getZ(), coord2.getZ());
	}

	public static double getDoubleOrThrow(String input) throws TownyException {
		double d;
		try {
			d = Double.parseDouble(input);
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
		return d;
	}
	
	public static int getIntOrThrow(String input) throws TownyException {
		input = parseAbbreviations(input);
		int i;
		try {
			i = Integer.parseInt(input);
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_int"));
		}
		return i;
	}
	
	private static String parseAbbreviations(String input) {
		if (input.endsWith("k") || input.endsWith("K"))
			return parseNumbers(input) + "000";
		if (input.endsWith("m") || input.endsWith("M"))
			return parseNumbers(input) + "000000";
		if (input.endsWith("b") || input.endsWith("B"))
			return parseNumbers(input) + "000000000";
		return input;
	}

	private static String parseNumbers(String input) {
		String output = "";
		for (int i = 0; i < input.length(); i++)
			if (Character.isDigit(input.charAt(i)))
				output += input.charAt(i);
		return output;
	}

	public static double getPositiveDoubleOrThrow(String input) throws TownyException {
		double i = getDoubleOrThrow(input);
		if (i < 0)
			throw new TownyException(Translatable.of("msg_err_negative"));
		return i;
	}

	public static int getPositiveIntOrThrow(String input) throws TownyException {
		int i = getIntOrThrow(input);
		if (i < 0)
			throw new TownyException(Translatable.of("msg_err_negative"));
		return i;
	}
	
	public static int clamp(int value, int min, int max) {
		// replace me with Math.clamp after java 21 becomes the minimum version
		if (min > max) {
			throw new IllegalArgumentException(min + " > " + max);
		}

		return Math.min(max, Math.max(value, min));
	}
}
