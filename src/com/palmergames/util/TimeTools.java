package com.palmergames.util;

import com.palmergames.bukkit.towny.TownySettings;

import java.util.regex.Pattern;

/**
 * @author dumptruckman
 */
public class TimeTools {

	/**
	 * This will parse a time string such as 2d30m to an equivalent amount of
	 * seconds.
	 * 
	 * @param dhms The time string
	 * @return The amount of seconds
	 */
	public static long secondsFromDhms(String dhms) {

		int seconds = 0, minutes = 0, hours = 0, days = 0;
		if (dhms.contains("d")) {
			days = Integer.parseInt(dhms.split("d")[0].replaceAll(" ", ""));
			if (dhms.contains("h") || dhms.contains("m") || dhms.contains("s")) {
				dhms = dhms.split("d")[1];
			}
		}
		if (dhms.contains("h")) {
			hours = Integer.parseInt(dhms.split("h")[0].replaceAll(" ", ""));
			if (dhms.contains("m") || dhms.contains("s")) {
				dhms = dhms.split("h")[1];
			}
		}
		if (dhms.contains("m")) {
			minutes = Integer.parseInt(dhms.split("m")[0].replaceAll(" ", ""));
			if (dhms.contains("s")) {
				dhms = dhms.split("m")[1];
			}
		}
		if (dhms.contains("s")) {
			seconds = Integer.parseInt(dhms.split("s")[0].replaceAll(" ", ""));
		}
		return (days * 86400) + (hours * 3600) + (minutes * 60) + seconds;
	}

	public static long getMillis(String dhms) {

		return getSeconds(dhms) * 1000;
	}

	public static long getSeconds(String dhms) {

		if (Pattern.matches(".*[a-zA-Z].*", dhms)) {
			return (TimeTools.secondsFromDhms(dhms));
		}
		return Long.parseLong(dhms);
	}

	public static long getTicks(String dhms) {

		return convertToTicks(getSeconds(dhms));
	}

	/**
	 * Converts Seconds to Ticks
	 * 
	 * @param t - Unix time
	 * @return ticks
	 */
	public static long convertToTicks(long t) {

		return t * 20;
	}

	/**
	 * Converts Seconds to 'Short' Ticks
	 *
	 * These ticks are only relevant to the 'Short' Timer Task
	 * 
	 * Rounds half up
	 *
	 * @return ticks
	 */
	public static int convertToShortTicks(double timeSeconds) {
		return (int)((timeSeconds / TownySettings.getShortInterval()) + 0.5);
	}
}
