package com.palmergames.bukkit.towny.regen;

import org.bukkit.Material;

import java.util.EnumSet;

public class NeedsPlaceholder {

	private static EnumSet<Material> needsPlaceholder = EnumSet.of(
			Material.SAND, Material.GRAVEL,
			Material.REDSTONE_WIRE, Material.COMPARATOR,
			Material.OAK_SAPLING, Material.SPRUCE_SAPLING,
			Material.BIRCH_SAPLING, Material.SPRUCE_SAPLING,
			Material.JUNGLE_SAPLING, Material.ACACIA_SAPLING,
			Material.DARK_OAK_SAPLING, Material.BROWN_MUSHROOM,
			Material.RED_MUSHROOM, Material.WHEAT, Material.REDSTONE_TORCH,
			Material.REDSTONE_WALL_TORCH, Material.SNOW, Material.SIGN,
			Material.WALL_SIGN);

	public static boolean contains(Material material) {

		//System.out.print("needsPlaceholder - " + needsPlaceholder.size());
		return (needsPlaceholder.contains(material));
	}

}