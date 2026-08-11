package com.palmergames.bukkit.towny.config;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.test.TownyConfigExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(TownyConfigExtension.class)
public class TownRuinConfigTests {
	@BeforeEach
	void reset() {
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWN_PERMISSIONS_CHANGE_ON_RUIN.getRoot(), true);
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWN_PLOTS_PERMISSIONS_OPEN_UP_PROGRESSIVELY.getRoot(), false);
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWNS_BECOME_OPEN.getRoot(), false);
	}

	@Test
	void testRuinPermissionsChangeByDefault() {
		assertTrue(TownySettings.doRuinsPermissionsChange());
	}

	@Test
	void testProgressivePermissionChangesAreDisabledWhenRuinPermissionsArePreserved() {
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWN_PERMISSIONS_CHANGE_ON_RUIN.getRoot(), false);
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWN_PLOTS_PERMISSIONS_OPEN_UP_PROGRESSIVELY.getRoot(), true);

		assertFalse(TownySettings.doRuinsPlotPermissionsProgressivelyAllowAll());
	}

	@Test
	void testRuinsBecomeOpenIndependentlyOfPermissionChanges() {
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWN_PERMISSIONS_CHANGE_ON_RUIN.getRoot(), false);
		TownySettings.getConfig().set(ConfigNodes.TOWN_RUINING_TOWNS_BECOME_OPEN.getRoot(), true);

		assertTrue(TownySettings.areRuinsMadeOpen());
		assertFalse(TownySettings.doRuinsPermissionsChange());
	}
}
