package com.palmergames.bukkit.towny.config;

import org.mockbukkit.mockbukkit.MockBukkit;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.config.migration.RunnableMigrations;
import com.palmergames.bukkit.towny.TownySettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

public class ConfigMigrationTests {
	
	static RunnableMigrations runnableMigrations = new RunnableMigrations();
	
	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@Test
	void testEntityClassMigration() {
		TownySettings.getConfig().set(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_ENTITY_REVERT_LIST.getRoot(), "Creeper,EnderCrystal,EnderDragon,Fireball,SmallFireball,LargeFireball,TNTPrimed,ExplosiveMinecart,Wither,WitherSkull");
		
		Consumer<CommentedConfiguration> migration = runnableMigrations.getByName("convert_entity_class_names");
		assertNotNull(migration);

		migration.accept(TownySettings.getConfig());
		
		// The same as our current default but with SMALL_FIREBALL and FIREBALL switched around.
		String expected = "CREEPER,END_CRYSTAL,ENDER_DRAGON,SMALL_FIREBALL,FIREBALL,TNT,TNT_MINECART,WITHER,WITHER_SKULL".toLowerCase(Locale.ROOT);
		assertEquals(expected, TownySettings.getConfig().get(ConfigNodes.NWS_PLOT_MANAGEMENT_WILD_ENTITY_REVERT_LIST.getRoot()));
	}
}
