package com.palmergames.util;

import java.text.NumberFormat;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

import java.time.Duration;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import org.bukkit.entity.Player;

public class TimeMgmt {

	public final static double ONE_SECOND_IN_MILLIS = 1000;
	public final static double ONE_MINUTE_IN_MILLIS = ONE_SECOND_IN_MILLIS * 60;
	public final static double ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;
	public final static double ONE_DAY_IN_MILLIS = ONE_HOUR_IN_MILLIS * 24;

	private static long lastTownyTimeCacheUpdate = System.currentTimeMillis();
	private static long cachedTownyTime = -1;

	public static String formatCountdownTime(long l) {
 		return formatCountdownTime(l, Translation.getDefaultLocale());
	}
	
	public static String formatCountdownTime(Long l, Locale locale) {
		String out = "";
		if (l >= 3600) {
			int h = (int) (l / 3600.0);
			out = h + Translatable.of("msg_hours").translate(locale);
			l -= h * 3600L;
		}
		if (l >= 60) {
			int m = (int) (l / 60.0);
			out += (out.length() > 0 ? ", " : "") + m + Translatable.of("msg_minutes").translate(locale);
			l -= m * 60L;
		}
		if (out.length() == 0 || l > 0)
			out += (out.length() > 0 ? ", " : "") + l + Translatable.of("msg_seconds").translate(locale);
		return out;
	}

	// Returns raw number of hours, ex: "12"
	public static String countdownTimeHoursRaw(long l) {
		return String.valueOf(Duration.ofSeconds(l).toHours());
	}
	// Returns raw number of minutes, ex: "737"
	public static String countdownTimeMinutesRaw(long l) {
		return String.valueOf(Duration.ofSeconds(l).toMinutes() % 60);
	}
	// Returns raw number of seconds, ex: "44248"
	public static String countdownTimeSecondsRaw(long l) {
		return String.valueOf(l % 60);
	}

	// Returns translation of "msg_hours" formatted, ex: "12 hours"
	public static String formatCountdownTimeHours(long l, Player player) {
		return Duration.ofSeconds(l).toHours() + Translatable.of("msg_hours").forLocale(player);
	}
	// Returns translation of "msg_minutes" formatted, ex: "737 minutes"
	public static String formatCountdownTimeMinutes(long l, Player player) {
		return Duration.ofSeconds(l).toMinutes() % 60 + Translatable.of("msg_minutes").forLocale(player);
	}
	// Returns translation of "msg_seconds" formatted, ex: "44248 seconds"
	public static String formatCountdownTimeSeconds(long l, Player player) {
		return l % 60 + Translatable.of("msg_seconds").forLocale(player);
	}

	public static String getFormattedTimeValue(double timeMillis) {
		return getFormattedTimeValue(timeMillis, Translation.getDefaultLocale());
	}

	public static String getFormattedTimeValue(double timeMillis, Locale locale) {
        String timeUnit;
        double timeUtilCompletion;

        if(timeMillis > 0) {

            NumberFormat numberFormat = NumberFormat.getInstance();

            if (timeMillis / ONE_DAY_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
				timeUnit = Translatable.of("msg_days").translate(locale);
                timeUtilCompletion = timeMillis / ONE_DAY_IN_MILLIS;

            } else if (timeMillis / ONE_HOUR_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
				timeUnit = Translatable.of("msg_hours").translate(locale);
                timeUtilCompletion = timeMillis / ONE_HOUR_IN_MILLIS;

            } else if (timeMillis / ONE_MINUTE_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
				timeUnit = Translatable.of("msg_minutes").translate(locale);
                timeUtilCompletion = timeMillis / ONE_MINUTE_IN_MILLIS;

            } else {
                numberFormat.setMaximumFractionDigits(0);
				timeUnit = Translatable.of("msg_seconds").translate(locale);
                timeUtilCompletion = timeMillis / ONE_SECOND_IN_MILLIS;
            }

            double timeRoundedUp = Math.ceil(timeUtilCompletion * 10) / 10;
            return numberFormat.format(timeRoundedUp) + timeUnit;

        } else {
			return "0" + Translatable.of("msg_seconds").translate(locale);
        }
    }

	/**
	 * Calculates the time in seconds until the next new day event.
	 * TimeZone specific, including daylight savings.
	 *
	 * @return seconds until event
	 */
	public static long townyTime() {
		return townyTime(false);
	}

	/**
	 * Calculates the time in seconds until the next new day event.
	 * TimeZone specific, including daylight savings.
	 * 
	 * @param cache - Whether to use caching or not.
	 *
	 * @return seconds until event
	 */
	public static long townyTime(boolean cache) {
		
		if (cache && lastTownyTimeCacheUpdate + 1000 > System.currentTimeMillis() && cachedTownyTime != -1)
			return cachedTownyTime;

		long secondsInDay = TownySettings.getDayInterval();

		// Get Calendar instance
		Calendar now = Calendar.getInstance();

		// Get current TimeZone
		TimeZone timeZone = now.getTimeZone();

		// Get current system time in milliseconds
		long timeMilli = System.currentTimeMillis();

		// Calculate the TimeZone specific offset (including DST)
		int timeOffset = timeZone.getOffset(timeMilli)/1000;

		final long time = Math.floorMod(secondsInDay + (TownySettings.getNewDayTime() - ((timeMilli/1000) % secondsInDay) - timeOffset), secondsInDay);
		
		if (cache) {
			cachedTownyTime = time;
			lastTownyTimeCacheUpdate = System.currentTimeMillis();
		}
		
		return time;
	}
}
