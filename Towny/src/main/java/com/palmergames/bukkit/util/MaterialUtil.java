package com.palmergames.bukkit.util;

import org.bukkit.Material;
import org.jetbrains.annotations.Nullable;

public class MaterialUtil {

	@Nullable
	public static Material getItemFromMaterial(Material material) {
		return switch (material) {
		case CARROTS -> Material.CARROT;
		case POTATOES -> Material.POTATO;
		case BEETROOTS -> Material.BEETROOT;
		case SWEET_BERRY_BUSH -> Material.SWEET_BERRIES;
		case PUMPKIN_STEM, ATTACHED_PUMPKIN_STEM -> Material.PUMPKIN;
		case MELON_STEM, ATTACHED_MELON_STEM -> Material.MELON;
		default -> null;
		};
	}
}
