package com.palmergames.bukkit.towny.config;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyWorld;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class EntityTypeConversionTests {
	
	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@Test
	void testRenamedFieldsConversions() {
		TownyWorld world = new TownyWorld("test", UUID.randomUUID());
		List<String> renamed = Arrays.asList("FISHING_HOOK", "MINECART_CHEST", "ENDER_CRYSTAL", "MINECART_FURNACE", "MINECART_HOPPER", "MINECART_COMMAND", "THROWN_EXP_BOTTLE", "ENDER_SIGNAL", "MINECART_TNT", "MUSHROOM_COW", "SPLASH_POTION", "PRIMED_TNT", "LEASH_HITCH", "LIGHTNING", "DROPPED_ITEM", "MINECART_MOB_SPAWNER", "SNOWMAN", "FIREWORK");
		world.setPlotManagementWildRevertEntities(renamed);
		
		assertEquals(renamed.size(), world.getPlotManagementWildRevertEntities().size());
	}
	
	@Test
	void testDefaultConfigAllNamesAreValid() {
		TownyWorld world = new TownyWorld("test", UUID.randomUUID());
		world.setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());
		
		assertEquals(world.getPlotManagementWildRevertEntities().size(), TownySettings.getWildExplosionProtectionEntities().size());
	}
}
