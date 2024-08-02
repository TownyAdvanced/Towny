package com.palmergames.bukkit.towny.object;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.TownyCommand;
import be.seeseemelk.mockbukkit.MockBukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.bukkit.Location;

class TownyWorldTest {

	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}

	@BeforeEach
	void reset() {
		// TownySettings.getConfig().set(ConfigNodes.X.getRoot(), 0);
	}

	@Test
	void testTownCreationWithNoOtherTown() {
		TownyWorld world = new TownyWorld("world");
		Town town = new Town("testTown");
		town.setWorld(world);

		// Player testTownOwnerPlayer = MockBukkit.getOrCreateMock().addPlayer("testTownOwner");
		// Resident testTownOwner = new Resident(testTownOwnerPlayer.getName());
		// town.setMayor(testTownOwner);
		// add a claim to the town
		// town.addClaim(new TownBlock(town, new WorldCoord(town.getWorld(), 0, 0)));
		// Towny plugin = MockBukkit.load(Towny.class);
		// Player player = null;
		// new TownClaim(plugin, player, town, List.of(new WorldCoord("world", 0, 0)), false, true, false, false);
		assertTrue(world.hasTowns());
		assertTrue(world.worldCoordNotTooCloseToOtherTowns(new Coord(0, 0), town));
		// TownyCommand.onCommand(testTownOwnerPlayer, @NotNull Command cmd, @NotNull String label, String[] args);
		// Run command to claim a townblock
		// testTownOwnerPlayer.teleport(new Location(MockBukkit.getOrCreateMock().getWorld("world"), 1, 0, 1));
		// testTownOwnerPlayer.performCommand("town claim");
	}
}
