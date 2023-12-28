package com.palmergames.bukkit.util;

import java.util.Locale;
import java.util.function.Predicate;
import org.bukkit.World;
import org.bukkit.block.Biome;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class BiomeUtil {

	private static Predicate<Biome> isOceanPredicate = (biome) -> biome.name().contains("OCEAN");
	private static Predicate<Biome> isUnwantedBiomePredicate = (biome) -> TownySettings.getUnwantedBiomeNames().contains(biome.name().toLowerCase(Locale.ROOT));

	public static double getWorldCoordOceanBiomePercent(WorldCoord worldCoord) {
		return getWorldCoordBadBiomePercent(worldCoord, isOceanPredicate);
	}

	public static double getWorldCoordUnwantedBiomePercent(WorldCoord worldCoord) {
		return getWorldCoordBadBiomePercent(worldCoord, isUnwantedBiomePredicate);
	}

	public static double getWorldCoordBadBiomePercent(WorldCoord worldCoord, Predicate<Biome> biomePredicate) {
		World world = worldCoord.getBukkitWorld();
		int plotSize = TownySettings.getTownBlockSize();
		int worldX = worldCoord.getX() * plotSize, worldZ = worldCoord.getZ() * plotSize;

		int total = plotSize * plotSize;
		int badBiomeBlocks = 0;
		for (int z = 0; z < plotSize; z++)
			for (int x = 0; x < plotSize; x++)
				if (biomePredicate.test(world.getHighestBlockAt(worldX + x, worldZ + z).getBiome()))
					badBiomeBlocks++;

		return (double) badBiomeBlocks / total;
	}
}
