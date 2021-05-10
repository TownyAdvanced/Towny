package com.palmergames.bukkit.towny.regen;

import org.bukkit.Material;

import java.util.EnumSet;

public class NeedsPlaceholder {

	private static EnumSet<Material> needsPlaceholder = EnumSet.of(Material.SAND, Material.GRAVEL, Material.REDSTONE_WIRE, Material.DIODE_BLOCK_OFF, Material.DIODE_BLOCK_ON, Material.SAPLING, Material.BROWN_MUSHROOM, Material.RED_MUSHROOM, Material.CROPS, Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.SNOW, Material.WALL_SIGN);

	public static boolean contains(Material material) {

		//System.out.print("needsPlaceholder - " + needsPlaceholder.size());
		return (needsPlaceholder.contains(material));
	}

}