package com.palmergames.util;

import java.text.NumberFormat;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;

public class TimeMgmt {

	public final static double ONE_SECOND_IN_MILLIS = 1000;
	public final static double ONE_MINUTE_IN_MILLIS = ONE_SECOND_IN_MILLIS * 60;
	public final static double ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;
	public final static double ONE_DAY_IN_MILLIS = ONE_HOUR_IN_MILLIS * 24;

	public final static long[][] defaultCountdownDelays = new long[][] {
			{ 10, 1 }, // <= 10s, Warn every 1s
			{ 30, 5 }, // <= 30s, Warn every 5s
			{ 60, 10 }, // <= minute, Warn every 10s
			{ 120, 15 }, // <= 2 minutes, Warn every 15s
			{ 5 * 60, 60 }, // <= 5 minutes, Warn every minute
			{ 30 * 60, 5 * 60 }, // <= 30 minutes, Warn every 5 minutes
			{ 60 * 60, 10 * 60 }, // <= 60 minutes, Warn every 10 minutes
			{ 24 * 60 * 60, 60 * 60 }, // <= day, Warn every hour
			{ Integer.MAX_VALUE, 24 * 60 * 60 } // <= max, Warn every day
	};

	public static List<Long> getCountdownDelays(int start) {

		return getCountdownDelays(start, defaultCountdownDelays);
	}

	public static List<Long> getCountdownDelays(int start, long[][] delays) {

		List<Long> out = new ArrayList<>();
		for (long[] delay : delays)
			if (delay.length != 2)
				return null;

		Integer lastDelayIndex = null;
		long nextWarningAt = Integer.MAX_VALUE;
		for (long t = start; t > 0; t--) {
			for (int d = 0; d < delays.length; d++) {
				if (t <= delays[d][0]) {
					if (lastDelayIndex == null || t <= nextWarningAt || d < lastDelayIndex) {
						lastDelayIndex = d;
						nextWarningAt = t - delays[d][1];
						out.add(t);
						break;
					}
				}
			}
		}

		return out;
	}

	public static String formatCountdownTime(long l) {

		String out = "";
		if (l >= 3600) {
			int h = (int) (l / 3600.0);
			out = h + Translation.of("msg_hours");
			l -= h * 3600;
		}
		if (l >= 60) {
			int m = (int) (l / 60.0);
			out += (out.length() > 0 ? ", " : "") + m + Translation.of("msg_minutes");
			l -= m * 60;
		}
		if (out.length() == 0 || l > 0)
			out += (out.length() > 0 ? ", " : "") + l + Translation.of("msg_seconds");
		return out;
	}
	
	public static String formatCountdownTime(Long l, Player player) {
		String out = "";
		if (l >= 3600) {
			int h = (int) (l / 3600.0);
			out = h + Translatable.of("msg_hours").forLocale(player);
			l -= h * 3600;
		}
		if (l >= 60) {
			int m = (int) (l / 60.0);
			out += (out.length() > 0 ? ", " : "") + m + Translatable.of("msg_minutes").forLocale(player);
			l -= m * 60;
		}
		if (out.length() == 0 || l > 0)
			out += (out.length() > 0 ? ", " : "") + l + Translatable.of("msg_seconds").forLocale(player);
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
        String timeUnit;
        double timeUtilCompletion;

        if(timeMillis > 0) {

            NumberFormat numberFormat = NumberFormat.getInstance();

            if (timeMillis / ONE_DAY_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = Translation.of("msg_days");
                timeUtilCompletion = timeMillis / ONE_DAY_IN_MILLIS;

            } else if (timeMillis / ONE_HOUR_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = Translation.of("msg_hours");
                timeUtilCompletion = timeMillis / ONE_HOUR_IN_MILLIS;

            } else if (timeMillis / ONE_MINUTE_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = Translation.of("msg_minutes");
                timeUtilCompletion = timeMillis / ONE_MINUTE_IN_MILLIS;

            } else {
                numberFormat.setMaximumFractionDigits(0);
                timeUnit = Translation.of("msg_seconds");
                timeUtilCompletion = timeMillis / ONE_SECOND_IN_MILLIS;
            }

            double timeRoundedUp = Math.ceil(timeUtilCompletion * 10) / 10;
            return numberFormat.format(timeRoundedUp) + timeUnit;

        } else {
            return "0" + Translation.of("msg_seconds");
        }
    }
}
