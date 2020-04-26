package com.palmergames.util;

import java.text.NumberFormat;
import com.palmergames.bukkit.towny.TownySettings;

import java.util.ArrayList;
import java.util.List;

public class TimeMgmt {

	public final static double ONE_SECOND_IN_MILLIS = 1000;
	public final static double ONE_MINUTE_IN_MILLIS = ONE_SECOND_IN_MILLIS * 60;
	public final static double ONE_HOUR_IN_MILLIS = ONE_MINUTE_IN_MILLIS * 60;
	public final static double ONE_DAY_IN_MILLIS = ONE_HOUR_IN_MILLIS * 24;

	public final static long[][] defaultCountdownDelays = new long[][] {
			{ 10, 1 }, // <= 10s, Warn every 1s
			{ 30, 5 }, // <= 30s, Warn every 5s
			{ 60, 10 }, // <= minute, Warn every 10s
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
			out = h + TownySettings.getLangString("msg_hours");
			l -= h * 3600;
		}
		if (l >= 60) {
			int m = (int) (l / 60.0);
			out += (out.length() > 0 ? ", " : "") + m + TownySettings.getLangString("msg_minutes");
			l -= m * 60;
		}
		if (out.length() == 0 || l > 0)
			out += (out.length() > 0 ? ", " : "") + l + TownySettings.getLangString("msg_seconds");
		return out;
	}

	public static String getFormattedTimeValue(double timeMillis) {
        String timeUnit;
        double timeUtilCompletion;

        if(timeMillis> 0) {

            NumberFormat numberFormat = NumberFormat.getInstance();

            if (timeMillis / ONE_DAY_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = TownySettings.getLangString("msg_days");
                timeUtilCompletion = timeMillis / ONE_DAY_IN_MILLIS;

            } else if (timeMillis / ONE_HOUR_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = TownySettings.getLangString("msg_hours");
                timeUtilCompletion = timeMillis / ONE_HOUR_IN_MILLIS;

            } else if (timeMillis / ONE_MINUTE_IN_MILLIS > 1) {
                numberFormat.setMaximumFractionDigits(1);
                timeUnit = TownySettings.getLangString("msg_minutes");
                timeUtilCompletion = timeMillis / ONE_MINUTE_IN_MILLIS;

            } else {
                numberFormat.setMaximumFractionDigits(0);
                timeUnit = TownySettings.getLangString("msg_seconds");
                timeUtilCompletion = timeMillis / ONE_SECOND_IN_MILLIS;
            }

            double timeRoundedUp = Math.ceil(timeUtilCompletion * 10) / 10;
            return numberFormat.format(timeRoundedUp) + timeUnit;

        } else {
            return "0" + TownySettings.getLangString("msg_seconds");
        }
    }
}
