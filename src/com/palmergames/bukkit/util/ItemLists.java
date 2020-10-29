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
	
	/**
	 * List of Redstone blocks that can be interacted with.
	 */
	public static List<String> REDSTONE_INTERACTABLES = new ArrayList<>(Arrays.asList("COMPARATOR","REPEATER","DAYLIGHT_DETECTOR","NOTE_BLOCK"));

	/**
	 * List of Potted Plants.
	 */
	public static List<String> POTTED_PLANTS= new ArrayList<>(Arrays.asList("POTTED_ACACIA_SAPLING","POTTED_ALLIUM","POTTED_AZURE_BLUET","POTTED_BAMBOO","POTTED_BIRCH_SAPLING","POTTED_BLUE_ORCHID","POTTED_BROWN_MUSHROOM","POTTED_CACTUS","POTTED_CORNFLOWER","POTTED_DANDELION","POTTED_DARK_OAK_SAPLING","POTTED_DEAD_BUSH","POTTED_FERN","POTTED_JUNGLE_SAPLING","POTTED_LILY_OF_THE_VALLEY","POTTED_OAK_SAPLING","POTTED_ORANGE_TULIP","POTTED_OXEYE_DAISY","POTTED_PINK_TULIP","POTTED_POPPY","POTTED_RED_MUSHROOM","POTTED_RED_TULIP","POTTED_SPRUCE_SAPLING","POTTED_WHITE_TULIP","POTTED_WITHER_ROSE"));

}
