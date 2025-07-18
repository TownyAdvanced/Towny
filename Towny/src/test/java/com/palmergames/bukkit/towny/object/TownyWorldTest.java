package com.palmergames.bukkit.towny.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import be.seeseemelk.mockbukkit.MockBukkit;

class TownyWorldTest {

	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
		// new Towny();
	}

	@Test
	void testTownCreationWithNoOtherTown() {
		TownyWorld world = new TownyWorld("world");
		Town town = new Town("testTown");
		town.setWorld(world);

		assertTrue(world.hasTowns());
		assertTrue(world.worldCoordNotTooCloseToOtherTowns(new Coord(0, 0), town));
	}

	@ParameterizedTest
	@CsvSource({"0,0, 0,0,0,false", "0,1, 0,0,0,true", "0,1, 1,0,0,false", "0,1, 0,1,0,false", "0,1, 0,0,1,false", "0,2, 1,0,0,true",
			"0,2, 0,1,0,true", "0,2, 0,0,1,true"})
	void testTownCreationWithOneOtherTown(int x, int z, int minDistanceBetweenHomeblocks, int minPlotDistanceFromTownPlot,
			int minPlotDistanceFromOlderTownPlot, boolean expected) {
		TownySettings.getConfig().set(ConfigNodes.CLAIMING_MIN_DISTANCE_BETWEEN_HOMEBLOCKS.getRoot(), minDistanceBetweenHomeblocks);
		TownySettings.getConfig().set(ConfigNodes.CLAIMING_MIN_PLOT_DISTANCE_FROM_TOWN_PLOT.getRoot(), minPlotDistanceFromTownPlot);
		TownySettings.getConfig().set(ConfigNodes.CLAIMING_MIN_PLOT_DISTANCE_FROM_OLDER_TOWN_PLOT.getRoot(),
				minPlotDistanceFromOlderTownPlot);
		TownyWorld world = new TownyWorld("world");
		Town oldTown = new Town("oldTown");
		oldTown.setUUID(UUID.randomUUID());
		oldTown.setWorld(world);
		TownBlock townBlock = new TownBlock(0, 0, world);
		// townBlock.setTown(oldTown);
		// Upper line throws:
		// java.lang.IllegalStateException: Attempted to use getPlugin() while the plugin is null, are you shading Towny? If you do not
		// understand this message, join the Towny discord using https://discord.com/invite/gnpVs5m and ask for support.
		// at com.palmergames.bukkit.towny.Towny.getPlugin(Towny.java:764)
		// at com.palmergames.bukkit.towny.TownyUniverse.(TownyUniverse.java:100)
		// at com.palmergames.bukkit.towny.TownyUniverse.getInstance(TownyUniverse.java:106)
		// at com.palmergames.bukkit.towny.object.TownBlock.setTown(TownBlock.java:79)
		// at com.palmergames.bukkit.towny.object.TownBlock.setTown(TownBlock.java:68)

		Town newTown = new Town("newTown");
		newTown.setUUID(UUID.randomUUID());
		newTown.setWorld(world);


		assertEquals(expected, world.worldCoordNotTooCloseToOtherTowns(new Coord(x, z), newTown));
	}

}
