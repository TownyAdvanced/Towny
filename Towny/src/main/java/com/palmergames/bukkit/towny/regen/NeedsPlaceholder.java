package com.palmergames.bukkit.towny.regen;

import com.palmergames.util.JavaUtil;
import org.bukkit.Material;

import java.util.HashSet;
import java.util.Set;

public class NeedsPlaceholder {

	private static final Set<Material> needsPlaceholder = JavaUtil.make(() -> {
		Set<Material> materials = new HashSet<>();
		materials.add(Material.SAND);
		materials.add(Material.GRAVEL);
		materials.add(Material.REDSTONE_WIRE);
		materials.add(Material.COMPARATOR);
		materials.add(Material.OAK_SAPLING);
		materials.add(Material.SPRUCE_SAPLING);
		materials.add(Material.BIRCH_SAPLING);
		materials.add(Material.JUNGLE_SAPLING);
		materials.add(Material.ACACIA_SAPLING);
		materials.add(Material.DARK_OAK_SAPLING);
		materials.add(Material.BROWN_MUSHROOM);
		materials.add(Material.RED_MUSHROOM);
		materials.add(Material.WHEAT);
		materials.add(Material.REDSTONE_TORCH);
		materials.add(Material.REDSTONE_WALL_TORCH);
		materials.add(Material.SNOW);
		materials.add(Material.OAK_WALL_SIGN);
		materials.add(Material.SPRUCE_WALL_SIGN);
		materials.add(Material.DARK_OAK_WALL_SIGN);
		materials.add(Material.BIRCH_WALL_SIGN);
		materials.add(Material.ACACIA_WALL_SIGN);
		materials.add(Material.JUNGLE_WALL_SIGN);
		
		return materials;
	});

	public static boolean contains(Material material) {

		//System.out.print("needsPlaceholder - " + needsPlaceholder.size());
		return (needsPlaceholder.contains(material));
	}

}