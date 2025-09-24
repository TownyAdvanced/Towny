package com.palmergames.bukkit.util;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.block.Biome;

public class BiomeUtil {

	private static final Set<NamespacedKey> UNWANTED_BIOME_KEYS = new HashSet<>();

	private static final Predicate<NamespacedKey> IS_OCEAN = (biome) -> biome.getKey().contains("ocean");
	private static final Predicate<NamespacedKey> IS_UNWANTED_BIOME = UNWANTED_BIOME_KEYS::contains;
	
	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:unwanted_chunks"), () -> {
			UNWANTED_BIOME_KEYS.clear();
			
			for (final String unwantedBiomeName : TownySettings.getUnwantedBiomeNames()) {
				NamespacedKey key = NamespacedKey.fromString(unwantedBiomeName.toLowerCase(Locale.ROOT));
				if (key == null) {
					Towny.getPlugin().getLogger().warning("Unwanted biome name is not a valid key: " + unwantedBiomeName);
					continue;
				}
				
				UNWANTED_BIOME_KEYS.add(key);
			}
		});
	}

	public static double getWorldCoordOceanBiomePercent(WorldCoord worldCoord) {
		return getWorldCoordBiomePercent(worldCoord, IS_OCEAN);
	}

	public static double getWorldCoordUnwantedBiomePercent(WorldCoord worldCoord) {
		return getWorldCoordBiomePercent(worldCoord, IS_UNWANTED_BIOME);
	}

	public static double getWorldCoordBiomePercent(WorldCoord worldCoord, Predicate<NamespacedKey> biomePredicate) {
		World world = worldCoord.getBukkitWorld();
		if (world == null)
			return 0;
		
		int plotSize = TownySettings.getTownBlockSize();
		int worldX = worldCoord.getX() * plotSize, worldZ = worldCoord.getZ() * plotSize;

		int total = plotSize * plotSize;
		int badBiomeBlocks = 0;
		for (int z = 0; z < plotSize; z++)
			for (int x = 0; x < plotSize; x++)
				if (biomePredicate.test(getBiomeKey(world, worldX, world.getHighestBlockYAt(worldX + x, worldZ + z), worldZ)))
					badBiomeBlocks++;

		return (double) badBiomeBlocks / total;
	}
	
	@SuppressWarnings({"deprecation", "removal"})
	public static NamespacedKey getBiomeKey(final World world, final int x, final int y, final int z) {
		if (Biome.class.isEnum()) {
			// pre 1.21
			return Bukkit.getUnsafe().getBiomeKey(world, x, y, z);
		}

		return world.getBiome(x, y, z).getKey();
	}
}
