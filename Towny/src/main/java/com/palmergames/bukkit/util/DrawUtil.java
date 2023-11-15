package com.palmergames.bukkit.util;

import org.bukkit.Location;
import org.bukkit.World;

import java.util.function.Consumer;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class DrawUtil {

	/**
	 * Run a runnable over the surface of the specified rectangular area. From
	 * the ground up.
	 * 
	 * @param world {@link World}
	 * @param x1 X-Coordinate 1 ({@link Integer})
	 * @param z1 Z-Coordinate 1 ({@link Integer})
	 * @param x2 X-Coordinate 2 ({@link Integer})
	 * @param z2 Z-Coordinate 2 ({@link Integer})
	 * @param height Y-Coordinate at call ({@link Integer})
	 * @param locationConsumer A consumer for accepting locations
	 */
	public static void runOnSurface(World world, int x1, int z1, int x2, int z2, int height, Consumer<Location> locationConsumer) {

		int _x1 = Math.min(x1, x2);
		int _x2 = Math.max(x1, x2);
		int _z1 = Math.min(z1, z2);
		int _z2 = Math.max(z1, z2);

		for (int z = _z1; z <= _z2; z++) {
			for (int x = _x1; x <= _x2; x++) {
				if (!world.isChunkLoaded(x >> 4, z >> 4)) {
					continue;
				}

				int start = world.getHighestBlockYAt(x, z);
				int end = (start + height) < world.getMaxHeight() ? (start + height - 1) : world.getMaxHeight();
				for (int y = start; y <= end; y++) {
					locationConsumer.accept(new Location(world, x, y, z));
				}
			}
		}
	}
}
