package com.palmergames.bukkit.towny.config;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class TownLevelTests {
	private Town testTown;
	
	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@BeforeEach
	void setup() {
		testTown = new Town("test", UUID.randomUUID());
	}

	@Test
	void testGetTownLevelByExactCount() {
		for (final Map.Entry<Integer, TownySettings.TownLevel> townLevelEntry : TownySettings.getConfigTownLevel().entrySet()) {
			assertEquals(townLevelEntry.getValue(), TownySettings.getTownLevel(testTown, townLevelEntry.getKey()));
		}
	}

	@Test
	void testGetTownLevelOutsideBounds() {
		assertEquals(" Ruins", TownySettings.getTownLevel(testTown, -1000).namePostfix());
		assertEquals(" (Metropolis)", TownySettings.getTownLevel(testTown, 1000).namePostfix());
	}

	@Test
	void testGetTownLevelNumberByExactCount() {
		int index = 0;

		for (final Map.Entry<Integer, TownySettings.TownLevel> townLevelEntry : TownySettings.getConfigTownLevel().entrySet()) {
			assertEquals(index, TownySettings.getTownLevelNumber(testTown, townLevelEntry.getKey()));
			index++;
		}
	}

	@Test
	void testGetMaxTownLevelNumber() {
		assertEquals(TownySettings.getTownLevelMax(), TownySettings.getTownLevelNumber(testTown, Integer.MAX_VALUE));
	}

	@RepeatedTest(10)
	void testGetTownLevelNumberAlwaysReturnsManualLevel(final RepetitionInfo info) {
		final int expectedTownLevel = Math.min(info.getCurrentRepetition(), TownySettings.getTownLevelMax());

		testTown.setManualTownLevel(info.getCurrentRepetition());

		assertEquals(expectedTownLevel, TownySettings.getTownLevelNumber(testTown, 100));
	}

	@Test
	void testGetTownLevelAlwaysRuins() {
		testTown.setRuined(true);
		
		assertEquals(0, TownySettings.getTownLevelNumber(testTown));
	}
}
