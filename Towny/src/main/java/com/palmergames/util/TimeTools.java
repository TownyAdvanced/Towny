package com.palmergames.util;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translatable;

import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * @author dumptruckman
 */
public class TimeTools {

	private static final long MILLIS_PER_SECOND = TimeUnit.SECONDS.toMillis(1);
	private static final long MILLIS_PER_MINUTE = TimeUnit.MINUTES.toMillis(1);
	private static final long MILLIS_PER_HOUR = TimeUnit.HOURS.toMillis(1);
	private static final long MILLIS_PER_DAY = TimeUnit.DAYS.toMillis(1);
	private static final long MILLIS_PER_YEAR = TimeUnit.DAYS.toMillis(365);

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
		return (days * 86400L) + (hours * 3600L) + (minutes * 60L) + (long)seconds;
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

		return t * 20L;
	}
	
	/**
	 * Converts Seconds to 'Short' Ticks
	 *
	 * These ticks are only relevant to the 'Short' Timer Task
	 * 
	 * Rounds half up
	 *
	 * @param timeSeconds number of seconds to convert.
	 * @return ticks
	 */
	public static int convertToShortTicks(double timeSeconds) {
		return (int)((timeSeconds / TownySettings.getShortInterval()) + 0.5);
	}
    
	public static int getHours(long milliSeconds) {
		return (int) ((milliSeconds /1000) / 60) /60;
	}

	public static int getDays(long milliSeconds) {
		return (int) (((milliSeconds /1000) / 60) /60) /24;
	}

	public static long getTimeInMillisXSecondsAgo(int seconds) {
		return System.currentTimeMillis() - (MILLIS_PER_SECOND * seconds); 
	}

	public static long getTimeInMillisXMinutesAgo(int minutes) {
		return System.currentTimeMillis() - (MILLIS_PER_MINUTE * minutes); 
	}

	public static long getTimeInMillisXHoursAgo(int hours) {
		return System.currentTimeMillis() - (MILLIS_PER_HOUR * hours); 
	}

	public static long getTimeInMillisXDaysAgo(int days) {
		return System.currentTimeMillis() - (MILLIS_PER_DAY * days); 
	}
	
	public static Translatable formatRelativeTime(final long unixMs) {
		final long now = System.currentTimeMillis();

		if (unixMs > now)
			return Translatable.of("time-future");

		final long diff = now - unixMs;

		if (diff < MILLIS_PER_SECOND) {
			return Translatable.of("time-just-now");
		} else if (diff < 2 * MILLIS_PER_MINUTE) {
			return Translatable.of("time-a-minute-ago");
		} else if (diff < 60 * MILLIS_PER_MINUTE) {
			return Translatable.of("time-x-minutes-ago", diff / MILLIS_PER_MINUTE);
		} else if (diff < 2 * MILLIS_PER_HOUR) {
			return Translatable.of("time-an-hour-ago");
		} else if (diff < 24 * MILLIS_PER_HOUR) {
			return Translatable.of("time-x-hours-ago", diff / MILLIS_PER_HOUR);
		} else if (diff < 48 * MILLIS_PER_HOUR) {
			return Translatable.of("time-yesterday");
		} else if (diff < MILLIS_PER_YEAR) {
			return Translatable.of("time-x-days-ago", diff / MILLIS_PER_DAY);
		} else if (diff < 2 * MILLIS_PER_YEAR) {
			return Translatable.of("time-a-year-ago");
		} else {
			return Translatable.of("time-x-years-ago", diff / MILLIS_PER_YEAR);
		}
	}
}
