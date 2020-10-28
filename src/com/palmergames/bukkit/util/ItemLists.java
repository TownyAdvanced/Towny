package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Item lists as Strings. Useful for groups that are missing from the Spigot Tags.
 * 
 * Did not use Materials because then we would be limited to specific versions of MC as new items are added.
 * 
 * @author LlmDl
 */
public interface ItemLists {

	/**
	 * List of Axe items.
	 */
	public static List<String> AXES = new ArrayList<>(Arrays.asList("WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLD_AXE", "DIAMOND_AXE", "NETHERITE_AXE"));

	/**
	 * List of Dye items.
	 */
	public static List<String> DYES = new ArrayList<>(Arrays.asList("BLACK_DYE","BLUE_DYE","BROWN_DYE","CYAN_DYE","GRAY_DYE","GREEN_DYE","LIGHT_BLUE_DYE","LIGHT_GRAY_DYE","LIME_DYE","MAGENTA_DYE","ORANGE_DYE","PINK_DYE","PURPLE_DYE","RED_DYE","WHITE_DYE","YELLOW_DYE"));

}
