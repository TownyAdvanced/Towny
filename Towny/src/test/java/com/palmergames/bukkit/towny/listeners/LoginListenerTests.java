package com.palmergames.bukkit.towny.listeners;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.palmergames.bukkit.towny.TownySettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.palmergames.bukkit.towny.listeners.TownyLoginListener.isPossibleNPCName;
import static org.junit.jupiter.api.Assertions.*;

public class LoginListenerTests {
	@BeforeAll
	public static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@Test
	void testNpcPrefix() {
		final String npcPrefix = TownySettings.getNPCPrefix();

		assertFalse(isPossibleNPCName(npcPrefix)); // The npc prefix itself is not a possible npc name
		assertTrue(isPossibleNPCName(npcPrefix + "1"));
		assertTrue(isPossibleNPCName(npcPrefix + "99999999"));

		assertFalse(isPossibleNPCName(npcPrefix + "A"));
		assertFalse(isPossibleNPCName(npcPrefix + "1A"));
		assertFalse(isPossibleNPCName("name"));
	}
}
