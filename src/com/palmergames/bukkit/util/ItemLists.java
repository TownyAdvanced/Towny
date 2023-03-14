package com.palmergames.bukkit.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Item lists as Strings. Useful for groups that are missing from the Spigot Tags.
 * 
 * Did not use Materials because then we would be limited to specific versions of MC as new items are added.
 * 
 * @author LlmDl
 */
public class ItemLists {
	public final EnumSet<Material> taggedMaterials;
	
	private ItemLists(@NotNull Set<String> taggedMaterials) {
		this.taggedMaterials = EnumSet.noneOf(Material.class);

		for (String mat : taggedMaterials) {
			try {
				this.taggedMaterials.add(Material.valueOf(mat.toUpperCase(Locale.ROOT)));
			} catch (IllegalArgumentException ignored) {}
		}
	}
	
	private ItemLists(EnumSet<Material> taggedMaterials) {
		this.taggedMaterials = taggedMaterials;
	}
	
	private static ItemLists of(@NotNull String... taggedMaterials) {
		return new ItemLists(Stream.of(taggedMaterials).collect(Collectors.toSet()));
	}

	/**
	 * @param matName Name of a {@link Material}.
	 * @return Whether the item list contains the specified material.
	 */
	public boolean contains(@NotNull String matName) {
		try {
			return taggedMaterials.contains(Material.valueOf(matName.toUpperCase(Locale.ROOT)));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/**
	 * @param material The bukkit material to test for.
	 * @return Whether the item list contains the specified material.
	 */
	public boolean contains(@NotNull Material material) {
		return taggedMaterials.contains(material);
	}
	
	public boolean contains(@NotNull ItemStack itemStack) {
		return contains(itemStack.getType());
	}

	/**
	 * List of Axe items.
	 */
	public static final ItemLists AXES = PredicateList.builder().endsWith("_AXE").build();

	/**
	 * List of Dye items.
	 */
	public static final ItemLists DYES = PredicateList.builder().endsWith("_DYE").endsWith("INK_SAC").build();
	
	/**
	 * List of Redstone blocks that can be interacted with.
	 */
	public static final ItemLists REDSTONE_INTERACTABLES = ItemLists.of("COMPARATOR","REPEATER","DAYLIGHT_DETECTOR","NOTE_BLOCK","REDSTONE_WIRE");

	/**
	 * List of Potted Plants.
	 */
	public static final ItemLists POTTED_PLANTS = PredicateList.builder().startsWith("POTTED_").build();

	/**
	 * List of Plants.
	 */
	public static final ItemLists PLANTS = ItemLists.of("TALL_GRASS","BROWN_MUSHROOM","RED_MUSHROOM","CACTUS","ALLIUM","AZURE_BLUET","BLUE_ORCHID","CORNFLOWER","DANDELION","LILAC","LILY_OF_THE_VALLEY","ORANGE_TULIP","OXEYE_DAISY","PEONY","PINK_TULIP","POPPY","RED_TULIP","ROSE_BUSH","SUNFLOWER","WHITE_TULIP","WITHER_ROSE","CRIMSON_FUNGUS","LARGE_FERN","PUMPKIN","VINE","TWISTING_VINE_PLANT","WEEPING_VINE_PLANT","NETHER_WART_BLOCK","COCOA","SUGAR_CANE","CRIMSON_ROOTS","WARPED_ROOTS","NETHER_SPROUTS","BIG_DRIPLEAF","SMALL_DRIPLEAF", "TORCHFLOWER");

	/**
	 * List of Ores and Valuable Raw Materials.
	 */
	public static final ItemLists ORES = PredicateList.builder().endsWith("_ORE").add("RAW_IRON_BLOCK","RAW_GOLD_BLOCK","RAW_COPPER_BLOCK","ANCIENT_DEBRIS","CLAY","GLOWSTONE","GILDED_BLACKSTONE").build();

	/**
	 * List of Saplings.
	 */
	public static final ItemLists SAPLINGS = PredicateList.builder().endsWith("_SAPLING").add("MANGROVE_PROPAGULE","CRIMSON_FUNGUS","WARPED_FUNGUS").build();

	/**
	 * List of Trees and Leaves.
	 */
	public static final ItemLists TREES = PredicateList.builder().endsWith("_WOOD").endsWith("_HYPHAE").notStartsWith("STRIPPED_").endsWith("_LEAVES").endsWith("_LOG").add("CRIMSON_STEM", "WARPED_STEM").add("BAMBOO_BLOCK").build();

	/**
	 * List of Beds.
	 */
	public static final ItemLists BEDS = PredicateList.builder().endsWith("_BED").build();

	/**
	 * List of Signs.
	 */
	public static final ItemLists SIGNS = PredicateList.builder().endsWith("_SIGN").build();

	/**
	 * List of Torches
	 */
	public static final ItemLists TORCHES = PredicateList.builder().endsWith("TORCH").build();

	/**
	 * List of Skulls and Heads
	 */
	public static final ItemLists SKULLS = PredicateList.builder().endsWith("_HEAD").endsWith("_SKULL").build();

	/**
	 * List of Boats.
	 */
	public static final ItemLists BOATS = PredicateList.builder().endsWith("_BOAT").endsWith("_RAFT").build();
	
	/**
	 * List of Minecarts.
	 */
	public static final ItemLists MINECARTS = PredicateList.builder().endsWith("MINECART").build();
 	
	/**
	 * List of Wooden Doors.
	 */
	public static final ItemLists WOOD_DOORS = PredicateList.builder().endsWith("_DOOR").not("IRON_DOOR").build();

	/**
	 * List of Doors.
	 */
	public static final ItemLists DOORS = PredicateList.builder().endsWith("_DOOR").build();
	
	/**
	 * List of Fence Gates.
	 */
	public static final ItemLists FENCE_GATES = PredicateList.builder().endsWith("_FENCE_GATE").build();

	/**
	 * List of Trap Doors.
	 */
	public static final ItemLists TRAPDOORS = PredicateList.builder().endsWith("_TRAPDOOR").build();

	/**
	 * List of Shulker Boxes.
	 */
	public static final ItemLists SHULKER_BOXES = PredicateList.builder().endsWith("SHULKER_BOX").build();

	/**
	 * List of Pressure Plates.
	 */
	public static final ItemLists PRESSURE_PLATES = PredicateList.builder().endsWith("_PRESSURE_PLATE").build();

	/**
	 * List of Buttons.
	 */
	public static final ItemLists BUTTONS = PredicateList.builder().endsWith("_BUTTON").build();

	/**
	 * List of materials that will activate redstone when triggered by a projectile.
	 */
	public static final ItemLists PROJECTILE_TRIGGERED_REDSTONE = PredicateList.builder().endsWith("_BUTTON").endsWith("_PRESSURE_PLATE").build();
	
	/**
	 * List of Buckets.
	 */
	public static final ItemLists BUCKETS = PredicateList.builder().endsWith("BUCKET").build();
	
	/**
	 * List of Copper Blocks.
	 */
	public static final ItemLists COPPER_BLOCKS = ItemLists.of("COPPER_BLOCK","COPPER_ORE","DEEPSLATE_COPPER_ORE","CUT_COPPER","CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_COPPER","EXPOSED_CUT_COPPER","EXPOSED_CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_COPPER","OXIDIZED_CUT_COPPER","OXIDIZED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_STAIRS","RAW_COPPER_BLOCK","WAXED_COPPER_BLOCK","WAXED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_EXPOSED_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_OXIDIZED_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_WEATHERED_COPPER","WAXED_WEATHERED_CUT_COPPER","WEATHERED_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_STAIRS");
	
	/** 
	 * List of Weatherable Blocks.
	 */
	public static final ItemLists WEATHERABLE_BLOCKS = ItemLists.of("COPPER_BLOCK","EXPOSED_COPPER","OXIDIZED_COPPER","WEATHERED_COPPER","CUT_COPPER","EXPOSED_CUT_COPPER","OXIDIZED_CUT_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WEATHERED_CUT_COPPER_STAIRS");
	
	/** 
	 * List of Scrapable Blocks (They can lose their wax.)
	 */
	public static final ItemLists WAXED_BLOCKS = ItemLists.of("WAXED_COPPER_BLOCK","WAXED_EXPOSED_COPPER","WAXED_WEATHERED_COPPER","WAXED_OXIDIZED_COPPER","WAXED_CUT_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_WEATHERED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_WEATHERED_CUT_COPPER_SLAB","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS");

	/** 
	 * List of Candles
	 */
	public static final ItemLists CANDLES = PredicateList.builder().endsWith("CANDLE").endsWith("_CANDLE_CAKE").build();

	/**
	 * List of Item Frames
	 */
	public static final ItemLists ITEM_FRAMES = ItemLists.of("ITEM_FRAME","GLOW_ITEM_FRAME");
	
	/** 
	 * List of Hanging entities
	 */
	public static final ItemLists HANGING_ENTITIES = ItemLists.of("ITEM_FRAME","GLOW_ITEM_FRAME","PAINTING");
	
	/**
	 * List of Campfires
	 */
	public static final ItemLists CAMPFIRES = ItemLists.of("CAMPFIRE","SOUL_CAMPFIRE");
	
	/**
	 * List of blocks that can hold books
	 */
	public static final ItemLists BOOK_CONTAINERS = ItemLists.of("CHISELED_BOOKSHELF","LECTERN");

	/**
	 * List of placeable books
	 */
	public static final ItemLists PLACEABLE_BOOKS = ItemLists.of("BOOK", "ENCHANTED_BOOK", "WRITEABLE_BOOK", "WRITTEN_BOOK");
	
	/**
	 * List of harvestable berries
	 */
	public static final ItemLists HARVESTABLE_BERRIES = ItemLists.of("CAVE_VINES_PLANT","SWEET_BERRY_BUSH");

	/*
	 * List of blocks which will be allowed to kill Minecarts.
	 */
	public static final ItemLists MINECART_KILLERS = ItemLists.of("CACTUS");
	
	/*
	 * List of unstripped wood logs.
	 */
	public static final ItemLists UNSTRIPPED_WOOD = PredicateList.builder().endsWith("_LOG").notStartsWith("STRIPPED_").add("CRIMSON_STEM", "WARPED_STEM").add("BAMBOO_BLOCK").build();
	
	/*
	 * List of cauldrons.
	 */
	public static final ItemLists CAULDRONS = ItemLists.of("CAULDRON", "WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON");
	public static final ItemLists FILLED_CAULDRONS = ItemLists.of("WATER_CAULDRON", "LAVA_CAULDRON", "POWDER_SNOW_CAULDRON");
	
	/*
	 * List of buckets that a cauldron can be filled with.
	 */
	public static final ItemLists CAULDRON_FILLABLE = ItemLists.of("WATER_BUCKET", "LAVA_BUCKET", "POWDER_SNOW_BUCKET");

	/**
	 * Config-useable material groups.
	 */
	public static final Set<String> GROUPS = Arrays.stream(ItemLists.class.getFields()).filter(field -> Modifier.isStatic(field.getModifiers())).map(Field::getName).filter(name -> !name.equals("GROUPS")).collect(Collectors.toSet());
	
	/**
	 * Returns a pre-configured list from the GROUPS.
	 * 
	 * @param groupName - String value of one of the {@link ItemLists#GROUPS}
	 * @return - Set&lt;Material&gt; grouping of materials, or an empty set if the grouping was not found.
	 */
	@NotNull
	public static Set<Material> getGrouping(String groupName) {
		if (!GROUPS.contains(groupName))
			return EnumSet.noneOf(Material.class);

		try {
			return ((ItemLists) ItemLists.class.getField(groupName).get(null)).taggedMaterials;
		} catch (Exception e) {
			return EnumSet.noneOf(Material.class);
		}
	}
	
	public static class PredicateList {
		
		// Predicates where all should match, this is used for functions that exclude certain materials. (notStartsWith, contains, etc.)
		private final Set<Predicate<String>> allMatchPredicates;
		// Predicates where only 1 has to match, this is used for functions that include new materials. (endsWith, startsWith)
		private final Set<Predicate<String>> anyMatchPredicates;

		private PredicateList(@NotNull Set<Predicate<String>> predicates, @NotNull Set<Predicate<String>> orPredicates) {
			this.allMatchPredicates = predicates;
			this.anyMatchPredicates = orPredicates;
		}
		
		private boolean contains(@NotNull Material material) {
			return allMatchPredicates.stream().allMatch(predicate -> predicate.test(material.name())) && anyMatchPredicates.stream().anyMatch(predicate -> predicate.test(material.name()));
		}
		
		private static PredicateListBuilder builder() {
			return new PredicateListBuilder().notStartsWith("LEGACY_"); // Excludes any legacy materials making it into our results.
		}
	}

	private static class PredicateListBuilder {
		private final Set<Predicate<String>> allMatchPredicates = new HashSet<>();
		private final Set<Predicate<String>> anyMatchPredicates = new HashSet<>();
		private @Nullable EnumSet<Material> exceptions;

		public ItemLists build() {
			PredicateList predicateList = new PredicateList(allMatchPredicates, anyMatchPredicates);
			EnumSet<Material> matches = EnumSet.noneOf(Material.class);

			for (Material material : Material.values())
				if (predicateList.contains(material))
					matches.add(material);
			
			if (exceptions != null)
				matches.addAll(exceptions);
			
			return new ItemLists(matches);
		}

		public PredicateListBuilder startsWith(String startingWith) {
			anyMatchPredicates.add((s) -> s.startsWith(startingWith));
			return this;
		}

		public PredicateListBuilder endsWith(@NotNull String endingWith) {
			anyMatchPredicates.add((s) -> s.endsWith(endingWith));
			return this;
		}

		public PredicateListBuilder not(@NotNull String name) {
			allMatchPredicates.add((s) -> !s.equals(name));
			return this;
		}

		public PredicateListBuilder notStartsWith(@NotNull String notStartingWith) {
			allMatchPredicates.add((s) -> !s.startsWith(notStartingWith));
			return this;
		}

		@SuppressWarnings("unused")
		public PredicateListBuilder notEndsWith(@NotNull String notEndingWith) {
			allMatchPredicates.add((s) -> !s.endsWith(notEndingWith));
			return this;
		}
		
		@SuppressWarnings("unused")
		public PredicateListBuilder contains(@NotNull String containing) {
			allMatchPredicates.add((s) -> s.contains(containing));
			return this;
		}
		
		@SuppressWarnings("unused")
		public PredicateListBuilder notContains(@NotNull String notContaining) {
			allMatchPredicates.add((s) -> !s.contains(notContaining));
			return this;
		}

		public PredicateListBuilder add(@NotNull String... names) {
			if (exceptions == null)
				exceptions = EnumSet.noneOf(Material.class);

			for (String name : names) {
				try {
					exceptions.add(Material.valueOf(name.toUpperCase(Locale.ROOT)));
				} catch (IllegalArgumentException ignored) {}
			}

			return this;
		}
	}
}
