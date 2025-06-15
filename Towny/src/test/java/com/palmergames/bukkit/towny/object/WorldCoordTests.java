package com.palmergames.bukkit.towny.object;

import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;
import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import static org.junit.jupiter.api.Assertions.*;

public class WorldCoordTests {
	static ServerMock server;
	
	@BeforeAll
	static void init() {
		server = MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
	}
	
	@Test
	void testChunkPositions() {
		final int length = 10;
		
		for (int x = -length / 2; x < length / 2; x++) {
			for (int z = -length / 2; z < length / 2; z++) {
				WorldCoord coord = new WorldCoord("a", x, z);

				Collection<Pair<Integer, Integer>> chunkPositions = coord.getChunkPositions(16);
				assertEquals(1, chunkPositions.size());
				
				Pair<Integer, Integer> pos = Iterables.getFirst(chunkPositions, null);
				assertEquals(x, pos.left());
				assertEquals(z, pos.right());
			}
		}
	}
	
	@Test
	void testLargerCellSizeChunkPositions() {
		WorldCoord coord = new WorldCoord("a", 0, 0);
		
		assertEquals(4, coord.getChunkPositions(32).size());
		assertEquals(9, coord.getChunkPositions(48).size());
	}
	
	@RepeatedTest(16)
	void testSmallerCellSizes(RepetitionInfo info) {
		assertEquals(1, new WorldCoord("a", 0, 0).getChunkPositions(info.getCurrentRepetition()).size());
	}
	
	@Test
	void testCorners() {
		int chunkX = -10;
		int chunkZ = -10;
		
		server.addSimpleWorld("world");
		WorldCoord coord = new WorldCoord("world", chunkX, chunkZ);
		assertEquals(chunkX, coord.getUpperMostCornerLocation().getChunk().getX());
		assertEquals(chunkZ, coord.getUpperMostCornerLocation().getChunk().getZ());

		assertEquals(chunkX, coord.getLowerMostCornerLocation().getChunk().getX());
		assertEquals(chunkZ, coord.getLowerMostCornerLocation().getChunk().getZ());
	}
}
