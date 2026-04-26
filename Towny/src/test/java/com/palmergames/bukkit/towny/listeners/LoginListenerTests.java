package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.test.TownyConfigExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static com.palmergames.bukkit.towny.listeners.TownyLoginListener.isPossibleNPCName;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TownyConfigExtension.class)
public class LoginListenerTests {
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
