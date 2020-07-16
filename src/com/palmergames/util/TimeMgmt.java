package com.palmergames.util;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translation;

import java.util.ArrayList;
import java.util.List;

public class TimeMgmt {

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
}
