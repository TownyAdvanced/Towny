package com.palmergames.bukkit.towny.object;

import com.google.common.collect.Iterables;
import com.palmergames.bukkit.towny.test.BukkitMockExtension;
import com.palmergames.util.Pair;
import org.bukkit.World;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.RepetitionInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(BukkitMockExtension.class)
public class WorldCoordTests {	
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
		
		final World mockWorld = mock(World.class);
		when(mockWorld.getMaxHeight()).thenReturn(320);
		when(mockWorld.getMinHeight()).thenReturn(-64);

		final WorldCoord coord = spy(new WorldCoord("world", chunkX, chunkZ));
		when(coord.getBukkitWorld()).thenReturn(mockWorld);

		assertEquals(chunkX, coord.getUpperMostCornerLocation().getBlockX() >> 4);
		assertEquals(chunkZ, coord.getUpperMostCornerLocation().getBlockZ() >> 4);

		assertEquals(chunkX, coord.getLowerMostCornerLocation().getBlockX() >> 4);
		assertEquals(chunkZ, coord.getLowerMostCornerLocation().getBlockZ() >> 4);
	}
}
