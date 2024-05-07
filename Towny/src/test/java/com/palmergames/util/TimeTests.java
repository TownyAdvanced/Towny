package com.palmergames.util;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class TimeTests {
	
	@Test
	void testTimeFromDHMS() {
		long expected = TimeUnit.DAYS.toSeconds(3);
		assertEquals(expected, TimeTools.secondsFromDhms("3d"));
		
		expected += TimeUnit.HOURS.toSeconds(2);
		assertEquals(expected, TimeTools.secondsFromDhms("3d2h"));
		
		expected += TimeUnit.MINUTES.toSeconds(59);
		assertEquals(expected, TimeTools.secondsFromDhms("3d2h59m"));
		assertEquals(expected + 30, TimeTools.secondsFromDhms("3d2h59m30s"));
		
		assertEquals(100, TimeTools.secondsFromDhms("100s"));
	}
	
	@Test
	void testSecondsToTicks() {		
		assertEquals(20, TimeTools.convertToTicks(1));
		assertEquals(40, TimeTools.convertToTicks(2));
		assertEquals(60, TimeTools.convertToTicks(3));
		assertEquals(80, TimeTools.convertToTicks(4));
		assertEquals(100, TimeTools.convertToTicks(5));
	}
	
	@Test
	void testGetHours() {
		assertEquals(0, TimeTools.getHours(1));
		assertEquals(10, TimeTools.getHours(TimeUnit.HOURS.toMillis(10)));
	}
}
