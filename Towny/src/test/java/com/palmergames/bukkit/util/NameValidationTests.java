package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.command.TownCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class NameValidationTests {
	@Test
	void testBlacklistedNationCompletions() {
		for (String completion : NationCommand.nationTabCompletes)
			assertTrue(NameValidation.isBannedName(completion), completion);
	}
	
	@Test
	void testBlacklistedTownCompletions() {
		for (String completion : TownCommand.townTabCompletes)
			assertTrue(NameValidation.isBannedName(completion), completion);
	}
}
