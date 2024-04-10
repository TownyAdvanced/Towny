package com.palmergames.bukkit.towny.object;

import be.seeseemelk.mockbukkit.MockBukkit;
import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.Pair;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class WorldCoordTests {
	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
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
}
