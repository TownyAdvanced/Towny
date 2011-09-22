package com.palmergames.util;

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

	// TODO: Throw specific exception
	// TODO: Faster loop, check if next warning is belong the delay index
	public static List<Long> getCountdownDelays(int start, long[][] delays) {
		List<Long> out = new ArrayList<Long>();
		for (int d = 0; d < delays.length; d++)
			if (delays[d].length != 2)
				return null;

		Integer lastDelayIndex = null;
		long nextWarningAt = Integer.MAX_VALUE;
		for (long t = start; t > 0; t--) {
			for (int d = 0; d < delays.length; d++) {
				if (t <= delays[d][0]) {
					if (lastDelayIndex == null || t <= nextWarningAt
							|| d < lastDelayIndex) {
						lastDelayIndex = d;
						nextWarningAt = t - delays[d][1];
						out.add(new Long(t));
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
			int h = (int) Math.floor(l / 3600);
			out = h + " hours";
			l -= h * 3600;
		}
		if (l >= 60) {
			int m = (int) Math.floor(l / 60);
			out += (out.length() > 0 ? ", " : "") + m + " minutes";
			l -= m * 60;
		}
		if (out.length() == 0 || l > 0)
			out +=  (out.length() > 0 ? ", " : "") + l + " seconds";
		return out;
	}

	public static void main(String[] args) {
		for (Long l : getCountdownDelays(36000000, defaultCountdownDelays))
			System.out.println(l + " " + formatCountdownTime(l));
	}
}
