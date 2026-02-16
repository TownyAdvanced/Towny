package com.palmergames.bukkit.towny.config;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class NationLevelTests {
	private Nation testNation;

	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}

	@BeforeEach
	void setup() {
		testNation = new Nation("test", UUID.randomUUID());
	}

	@Test
	void testGetTownLevelByExactCount() {
		for (final Map.Entry<Integer, TownySettings.NationLevel> nationLevelEntry : TownySettings.getConfigNationLevel().entrySet()) {
			assertEquals(nationLevelEntry.getValue(), TownySettings.getNationLevel(testNation, nationLevelEntry.getKey()));
		}
	}

	@Test
	void testGetTownLevelOutsideBounds() {
		assertEquals("Land of ", TownySettings.getNationLevel(testNation, -1000).namePrefix());
		assertEquals(" Realm", TownySettings.getNationLevel(testNation, 1000).namePostfix());
	}

	@Test
	void testGetTownLevelNumberByExactCount() {
		int index = 0;

		for (final Map.Entry<Integer, TownySettings.NationLevel> nationLevelEntry : TownySettings.getConfigNationLevel().entrySet()) {
			assertEquals(index, TownySettings.getNationLevelNumber(testNation, nationLevelEntry.getKey()));
			index++;
		}
	}
}
