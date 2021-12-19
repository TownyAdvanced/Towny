package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jetbrains.annotations.Nullable;

/**
 * Item lists as Strings. Useful for groups that are missing from the Spigot Tags.
 * 
 * Did not use Materials because then we would be limited to specific versions of MC as new items are added.
 * 
 * @author LlmDl
 */
public class ItemLists {

	/**
	 * List of Axe items.
	 */
	public static List<String> AXES = new ArrayList<>(Arrays.asList("WOODEN_AXE", "STONE_AXE", "IRON_AXE", "GOLD_AXE", "DIAMOND_AXE", "NETHERITE_AXE"));

	/**
	 * List of Dye items.
	 */
	public static List<String> DYES = new ArrayList<>(Arrays.asList("BLACK_DYE","BLUE_DYE","BROWN_DYE","CYAN_DYE","GRAY_DYE","GREEN_DYE","LIGHT_BLUE_DYE","LIGHT_GRAY_DYE","LIME_DYE","MAGENTA_DYE","ORANGE_DYE","PINK_DYE","PURPLE_DYE","RED_DYE","WHITE_DYE","YELLOW_DYE","INK_SAC","GLOW_INK_SAC"));
	
	/**
	 * List of Redstone blocks that can be interacted with.
	 */
	public static List<String> REDSTONE_INTERACTABLES = new ArrayList<>(Arrays.asList("COMPARATOR","REPEATER","DAYLIGHT_DETECTOR","NOTE_BLOCK","REDSTONE_WIRE"));

	/**
	 * List of Potted Plants.
	 */
	public static List<String> POTTED_PLANTS = new ArrayList<>(Arrays.asList("POTTED_ACACIA_SAPLING","POTTED_ALLIUM","POTTED_AZURE_BLUET","POTTED_BAMBOO","POTTED_BIRCH_SAPLING","POTTED_BLUE_ORCHID","POTTED_BROWN_MUSHROOM","POTTED_CACTUS","POTTED_CORNFLOWER","POTTED_DANDELION","POTTED_DARK_OAK_SAPLING","POTTED_DEAD_BUSH","POTTED_FERN","POTTED_JUNGLE_SAPLING","POTTED_LILY_OF_THE_VALLEY","POTTED_OAK_SAPLING","POTTED_ORANGE_TULIP","POTTED_OXEYE_DAISY","POTTED_PINK_TULIP","POTTED_POPPY","POTTED_RED_MUSHROOM","POTTED_RED_TULIP","POTTED_SPRUCE_SAPLING","POTTED_WHITE_TULIP","POTTED_WITHER_ROSE"));

	/**
	 * List of Boats.
	 */
	public static List<String> BOATS = new ArrayList<>(Arrays.asList("BIRCH_BOAT","ACACIA_BOAT","DARK_OAK_BOAT","JUNGLE_BOAT","OAK_BOAT","SPRUCE_BOAT"));
	
	/**
	 * List of Minecarts.
	 */
	public static List<String> MINECARTS = new ArrayList<>(Arrays.asList("MINECART","CHEST_MINECART","COMMAND_BLOCK_MINECART","TNT_MINECART","HOPPER_MINECART"));
 	
	/**
	 * List of Wooden Doors.
	 */
	public static List<String> WOOD_DOORS = new ArrayList<>(Arrays.asList("ACACIA_DOOR","BIRCH_DOOR","DARK_OAK_DOOR","JUNGLE_DOOR","OAK_DOOR","SPRUCE_DOOR","CRIMSON_DOOR","WARPED_DOOR"));

	/**
	 * List of Fence Gates.
	 */
	public static List<String> FENCE_GATES = new ArrayList<>(Arrays.asList("ACACIA_FENCE_GATE","BIRCH_FENCE_GATE","DARK_OAK_FENCE_GATE","OAK_FENCE_GATE","JUNGLE_FENCE_GATE","SPRUCE_FENCE_GATE","CRIMSON_FENCE_GATE","WARPED_FENCE_GATE"));

	/**
	 * List of Trap Doors.
	 */
	public static List<String> TRAPDOORS = new ArrayList<>(Arrays.asList("ACACIA_TRAPDOOR","BIRCH_TRAPDOOR","DARK_OAK_TRAPDOOR","JUNGLE_TRAPDOOR","OAK_TRAPDOOR","SPRUCE_TRAPDOOR","CRIMSON_TRAPDOOR","WARPED_TRAPDOOR"));

	/**
	 * List of Shulker Boxes.
	 */
	public static List<String> SHULKER_BOXES = new ArrayList<>(Arrays.asList("SHULKER_BOX","WHITE_SHULKER_BOX","ORANGE_SHULKER_BOX","MAGENTA_SHULKER_BOX","LIGHT_BLUE_SHULKER_BOX","LIGHT_GRAY_SHULKER_BOX","YELLOW_SHULKER_BOX","LIME_SHULKER_BOX","PINK_SHULKER_BOX","GRAY_SHULKER_BOX","CYAN_SHULKER_BOX","PURPLE_SHULKER_BOX","BLUE_SHULKER_BOX","BROWN_SHULKER_BOX","GREEN_SHULKER_BOX","RED_SHULKER_BOX","BLACK_SHULKER_BOX"));

	/**
	 * List of Pressure Plates.
	 */
	public static List<String> PRESSURE_PLATES = new ArrayList<>(Arrays.asList("STONE_PRESSURE_PLATE","ACACIA_PRESSURE_PLATE","BIRCH_PRESSURE_PLATE","DARK_OAK_PRESSURE_PLATE","JUNGLE_PRESSURE_PLATE","OAK_PRESSURE_PLATE","SPRUCE_PRESSURE_PLATE","HEAVY_WEIGHTED_PRESSURE_PLATE","LIGHT_WEIGHTED_PRESSURE_PLATE","CRIMSON_PRESSURE_PLATE","WARPED_PRESSURE_PLATE","POLISHED_BLACKSTONE_PRESSURE_PLATE"));

	/**
	 * List of Buttons.
	 */
	public static List<String> BUTTONS = new ArrayList<>(Arrays.asList("STONE_BUTTON","ACACIA_BUTTON","BIRCH_BUTTON","DARK_OAK_BUTTON","JUNGLE_BUTTON","OAK_BUTTON","SPRUCE_BUTTON","CRIMSON_BUTTON","WARPED_BUTTON","POLISHED_BLACKSTONE_BUTTON"));

	/**
	 * List of materials that will activate redstone when triggered by a projectile.
	 */
	public static List<String> PROJECTILE_TRIGGERED_REDSTONE = new ArrayList<>(Arrays.asList("ACACIA_BUTTON","BIRCH_BUTTON","DARK_OAK_BUTTON","JUNGLE_BUTTON","OAK_BUTTON","SPRUCE_BUTTON","CRIMSON_BUTTON","WARPED_BUTTON","ACACIA_PRESSURE_PLATE","BIRCH_PRESSURE_PLATE","DARK_OAK_PRESSURE_PLATE","JUNGLE_PRESSURE_PLATE","OAK_PRESSURE_PLATE","SPRUCE_PRESSURE_PLATE","HEAVY_WEIGHTED_PRESSURE_PLATE","LIGHT_WEIGHTED_PRESSURE_PLATE","CRIMSON_PRESSURE_PLATE","WARPED_PRESSURE_PLATE"));
	
	/**
	 * Config-useable material groups.
	 */
	public static List<String> GROUPS = new ArrayList<>(Arrays.asList("BOATS","MINECARTS","WOOD_DOORS","PRESSURE_PLATES","FENCE_GATES","TRAPDOORS","SHULKER_BOXES","BUTTONS","CANDLES"));
	
	/**
	 * List of Buckets.
	 */
	public static List<String> BUCKETS = new ArrayList<>(Arrays.asList("WATER_BUCKET","LAVA_BUCKET", "BUCKET"));
	
	/**
	 * List of Copper Blocks.
	 */
	public static List<String> COPPER_BLOCKS = new ArrayList<>(Arrays.asList("COPPER_BLOCK","COPPER_ORE","DEEPSLATE_COPPER_ORE","CUT_COPPER","CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_COPPER","EXPOSED_CUT_COPPER","EXPOSED_CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_COPPER","OXIDIZED_CUT_COPPER","OXIDIZED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_STAIRS","RAW_COPPER_BLOCK","WAXED_COPPER_BLOCK","WAXED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_EXPOSED_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_OXIDIZED_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_WEATHERED_COPPER","WAXED_WEATHERED_CUT_COPPER","WEATHERED_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_STAIRS"));
	
	/** 
	 * List of Weatherable Blocks.
	 */
	public static List<String> WEATHERABLE_BLOCKS = new ArrayList<>(Arrays.asList("COPPER_BLOCK","EXPOSED_COPPER","OXIDIZED_COPPER","WEATHERED_COPPER","CUT_COPPER","EXPOSED_CUT_COPPER","OXIDIZED_CUT_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WEATHERED_CUT_COPPER_STAIRS"));
	
	/** 
	 * List of Scrapable Blocks (They can lose their wax.)
	 */
	public static List<String> WAXED_BLOCKS = new ArrayList<>(Arrays.asList("WAXED_COPPER_BLOCK","WAXED_EXPOSED_COPPER","WAXED_WEATHERED_COPPER","WAXED_OXIDIZED_COPPER","WAXED_CUT_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_WEATHERED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_WEATHERED_CUT_COPPER_SLAB","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS"));

	/** 
	 * List of Candles
	 */
	public static List<String> CANDLES = new ArrayList<>(Arrays.asList("CANDLE","WHITE_CANDLE","ORANGE_CANDLE","MAGENTA_CANDLE","LIGHT_BLUE_CANDLE","YELLOW_CANDLE","LIME_CANDLE","PINK_CANDLE","GRAY_CANDLE","LIGHT_GRAY_CANDLE","CYAN_CANDLE","PURPLE_CANDLE","BLUE_CANDLE","BROWN_CANDLE","GREEN_CANDLE","RED_CANDLE","BLACK_CANDLE","CANDLE_CAKE","WHITE_CANDLE_CAKE","ORANGE_CANDLE_CAKE","MAGENTA_CANDLE_CAKE","LIGHT_BLUE_CANDLE_CAKE","YELLOW_CANDLE_CAKE","LIME_CANDLE_CAKE","PINK_CANDLE_CAKE","GRAY_CANDLE_CAKE","LIGHT_GRAY_CANDLE_CAKE","CYAN_CANDLE_CAKE","PURPLE_CANDLE_CAKE","BLUE_CANDLE_CAKE","BROWN_CANDLE_CAKE","GREEN_CANDLE_CAKE","RED_CANDLE_CAKE","BLACK_CANDLE_CAKE"));

	/**
	 * List of Item Frames
	 */
	public static List<String> ITEM_FRAMES = new ArrayList<>(Arrays.asList("ITEM_FRAME","GLOW_ITEM_FRAME"));
	
	/** 
	 * List of Hanging entities
	 */
	public static List<String> HANGING_ENTITIES = new ArrayList<>(Arrays.asList("ITEM_FRAME","GLOW_ITEM_FRAME","PAINTING"));
	
	/**
	 * List of Campfires
	 */
	public static List<String> CAMPFIRES = new ArrayList<>(Arrays.asList("CAMPFIRE","SOUL_CAMPFIRE"));
	
	/**
	 * List of harvestable berries
	 */
	public static List<String> HARVESTABLE_BERRIES = new ArrayList<>(Arrays.asList("CAVE_VINES_PLANT","SWEET_BERRY_BUSH"));

	/*
	 * List of blocks which will be allowed to kill Minecarts.
	 */
	public static List<String> MINECART_KILLERS = new ArrayList<>(Arrays.asList("CACTUS","LAVA_CAULDRON"));
	
	/**
	 * Returns a pre-configured list from the GROUPS.
	 * 
	 * @param groupName - String value of one of the {@link ItemLists#GROUPS}
	 * @return - List&lt;String&gt; grouping of materials.
	 */
	@Nullable
	public static List<String> getGrouping(String groupName) {
		return switch (groupName) {
			case "BOATS" -> BOATS;
			case "MINECARTS" -> MINECARTS;
			case "WOOD_DOORS" -> WOOD_DOORS;
			case "PRESSURE_PLATES" -> PRESSURE_PLATES;
			case "FENCE_GATES" -> FENCE_GATES;
			case "TRAPDOORS" -> TRAPDOORS;
			case "SHULKER_BOXES" -> SHULKER_BOXES;
			case "BUTTONS" -> BUTTONS;
			case "CANDLES" -> CANDLES;
			default -> null;
		};
	}
}
