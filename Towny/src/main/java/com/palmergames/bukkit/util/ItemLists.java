package com.palmergames.bukkit.util;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.palmergames.bukkit.towny.object.AbstractRegistryList;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import static com.palmergames.bukkit.towny.utils.MinecraftVersion.*;
import static org.bukkit.NamespacedKey.minecraft;

/**
 * Item lists as Strings. Useful for groups that are missing from the Spigot Tags.
 * <br>
 * Did not use Materials because then we would be limited to specific versions of MC as new items are added.
 * 
 * @author LlmDl
 */
@SuppressWarnings("unused")
public class ItemLists extends AbstractRegistryList<Material> {
	private ItemLists(Collection<Material> taggedMaterials) {
		super(Registry.MATERIAL, taggedMaterials);
	}
	
	public boolean contains(@NotNull ItemStack itemStack) {
		return contains(itemStack.getType());
	}

	/**
	 * List of Axe items.
	 */
	public static final ItemLists AXES = newBuilder().withTag(Tag.REGISTRY_ITEMS, minecraft("axes")).endsWith("_AXE").build();

	/**
	 * List of Dye items.
	 */
	public static final ItemLists DYES = newBuilder().endsWith("_DYE").endsWith("INK_SAC").build();
	
	/**
	 * List of Redstone blocks that can be interacted with.
	 */
	public static final ItemLists REDSTONE_INTERACTABLES = newBuilder().add("COMPARATOR","REPEATER","DAYLIGHT_DETECTOR","NOTE_BLOCK","REDSTONE_WIRE").build();

	/**
	 * List of Potted Plants.
	 */
	public static final ItemLists POTTED_PLANTS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("potted_plants")).startsWith("POTTED_").build();

	/**
	 * List of Plants.
	 */
	public static final ItemLists PLANTS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("flowers")).add("TALL_GRASS","BROWN_MUSHROOM","RED_MUSHROOM","CACTUS","ALLIUM","AZURE_BLUET","BLUE_ORCHID","CORNFLOWER","DANDELION","LILAC","LILY_OF_THE_VALLEY","ORANGE_TULIP","OXEYE_DAISY","PEONY","PINK_TULIP","POPPY","RED_TULIP","ROSE_BUSH","SUNFLOWER","WHITE_TULIP","WITHER_ROSE","CRIMSON_FUNGUS","LARGE_FERN","PUMPKIN","VINE","TWISTING_VINES_PLANT","WEEPING_VINES_PLANT","NETHER_WART_BLOCK","COCOA","SUGAR_CANE","CRIMSON_ROOTS","WARPED_ROOTS","NETHER_SPROUTS","BIG_DRIPLEAF","SMALL_DRIPLEAF", "TORCHFLOWER").build();

	/**
	 * List of Ores and Valuable Raw Materials.
	 */
	public static final ItemLists ORES = newBuilder().endsWith("_ORE").add("RAW_IRON_BLOCK","RAW_GOLD_BLOCK","RAW_COPPER_BLOCK","ANCIENT_DEBRIS","CLAY","GLOWSTONE","GILDED_BLACKSTONE").build();

	/**
	 * List of Saplings.
	 */
	public static final ItemLists SAPLINGS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("saplings")).endsWith("_SAPLING").add("MANGROVE_PROPAGULE","CRIMSON_FUNGUS","WARPED_FUNGUS").build();

	/**
	 * List of Trees and Leaves.
	 */
	public static final ItemLists TREES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("logs")).withTag(Tag.REGISTRY_BLOCKS, minecraft("leaves")).endsWith("_WOOD").endsWith("_HYPHAE").notStartsWith("STRIPPED_").endsWith("_LEAVES").endsWith("_LOG").add("CRIMSON_STEM", "WARPED_STEM").add("BAMBOO_BLOCK").build();

	/**
	 * List of Beds.
	 */
	public static final ItemLists BEDS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("beds")).endsWith("_BED").build();

	/**
	 * List of Signs.
	 */
	public static final ItemLists SIGNS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("signs")).withTag(Tag.REGISTRY_BLOCKS, minecraft("all_signs")).endsWith("_SIGN").build();

	/**
	 * List of Torches
	 */
	public static final ItemLists TORCHES = newBuilder().endsWith("TORCH").build();

	/**
	 * List of Skulls and Heads
	 */
	public static final ItemLists SKULLS = newBuilder().endsWith("_HEAD").endsWith("_SKULL").build();

	/**
	 * List of Boats.
	 */
	public static final ItemLists BOATS = newBuilder().withTag(Tag.REGISTRY_ITEMS, minecraft("boats")).endsWith("_BOAT").endsWith("_RAFT").build();
	
	/**
	 * List of Minecarts.
	 */
	public static final ItemLists MINECARTS = newBuilder().endsWith("MINECART").build();
 	
	/**
	 * List of Wooden Doors.
	 */
	public static final ItemLists WOOD_DOORS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("wooden_doors")).endsWith("_DOOR").not("IRON_DOOR").build();

	/**
	 * List of Doors.
	 */
	public static final ItemLists DOORS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("doors")).endsWith("_DOOR").build();
	
	/**
	 * List of Fence Gates.
	 */
	public static final ItemLists FENCE_GATES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("fence_gates")).endsWith("_FENCE_GATE").build();

	/**
	 * List of Trap Doors.
	 */
	public static final ItemLists TRAPDOORS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("trapdoors")).endsWith("_TRAPDOOR").build();

	/**
	 * List of Shulker Boxes.
	 */
	public static final ItemLists SHULKER_BOXES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("shulker_boxes")).endsWith("SHULKER_BOX").build();

	/**
	 * List of Pressure Plates.
	 */
	public static final ItemLists PRESSURE_PLATES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("pressure_plates")).endsWith("_PRESSURE_PLATE").build();

	/**
	 * List of Non-Wooden Pressure Plates.
	 */
	public static final ItemLists NON_WOODEN_PRESSURE_PLATES = newBuilder()
		.withTag(Tag.REGISTRY_BLOCKS, minecraft("pressure_plates")).excludeTag(Tag.REGISTRY_BLOCKS, minecraft("wooden_pressure_plates")).build();

	/**
	 * List of Buttons.
	 */
	public static final ItemLists BUTTONS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("buttons")).endsWith("_BUTTON").build();

	/**
	 * List of materials that will activate redstone when triggered by a projectile.
	 */
	public static final ItemLists PROJECTILE_TRIGGERED_REDSTONE = newBuilder()
		.withTag(Tag.REGISTRY_BLOCKS, minecraft("wooden_buttons"))
		.endsWith("_BUTTON").not("STONE_BUTTON")
		.withTag(Tag.REGISTRY_BLOCKS, minecraft("wooden_pressure_plates"))
		.endsWith("_PRESSURE_PLATE").not("STONE_PRESSURE_PLATE").build();
	
	/**
	 * List of Buckets.
	 */
	public static final ItemLists BUCKETS = newBuilder().endsWith("BUCKET").build();
	
	/**
	 * List of Copper Blocks.
	 */
	public static final ItemLists COPPER_BLOCKS = newBuilder().add("COPPER_BLOCK","COPPER_ORE","DEEPSLATE_COPPER_ORE","CUT_COPPER","CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_COPPER","EXPOSED_CUT_COPPER","EXPOSED_CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_COPPER","OXIDIZED_CUT_COPPER","OXIDIZED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_STAIRS","RAW_COPPER_BLOCK","WAXED_COPPER_BLOCK","WAXED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_EXPOSED_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_OXIDIZED_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_WEATHERED_COPPER","WAXED_WEATHERED_CUT_COPPER","WEATHERED_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_STAIRS").build();
	
	/** 
	 * List of Weatherable Blocks.
	 */
	public static final ItemLists WEATHERABLE_BLOCKS = newBuilder().add("COPPER_BLOCK","EXPOSED_COPPER","OXIDIZED_COPPER","WEATHERED_COPPER","CUT_COPPER","EXPOSED_CUT_COPPER","OXIDIZED_CUT_COPPER","WEATHERED_CUT_COPPER","CUT_COPPER_SLAB","EXPOSED_CUT_COPPER_SLAB","OXIDIZED_CUT_COPPER_SLAB","WEATHERED_CUT_COPPER_SLAB","CUT_COPPER_STAIRS","EXPOSED_CUT_COPPER_STAIRS","OXIDIZED_CUT_COPPER_STAIRS","WEATHERED_CUT_COPPER_STAIRS").build();
	
	/** 
	 * List of Scrapable Blocks (They can lose their wax.)
	 */
	public static final ItemLists WAXED_BLOCKS = newBuilder().add("WAXED_COPPER_BLOCK","WAXED_EXPOSED_COPPER","WAXED_WEATHERED_COPPER","WAXED_OXIDIZED_COPPER","WAXED_CUT_COPPER","WAXED_EXPOSED_CUT_COPPER","WAXED_WEATHERED_CUT_COPPER","WAXED_OXIDIZED_CUT_COPPER","WAXED_CUT_COPPER_SLAB","WAXED_EXPOSED_CUT_COPPER_SLAB","WAXED_WEATHERED_CUT_COPPER_SLAB","WAXED_OXIDIZED_CUT_COPPER_SLAB","WAXED_CUT_COPPER_STAIRS","WAXED_EXPOSED_CUT_COPPER_STAIRS","WAXED_WEATHERED_CUT_COPPER_STAIRS","WAXED_OXIDIZED_CUT_COPPER_STAIRS").build();

	/** 
	 * List of Candles
	 */
	public static final ItemLists CANDLES = newBuilder()
		.withTag(Tag.REGISTRY_BLOCKS, minecraft("candles"))
		.withTag(Tag.REGISTRY_BLOCKS, minecraft("candle_cakes"))
		.endsWith("CANDLE").endsWith("_CANDLE_CAKE").build();

	/**
	 * List of Item Frames
	 */
	public static final ItemLists ITEM_FRAMES = newBuilder().add("ITEM_FRAME","GLOW_ITEM_FRAME").build();
	
	/** 
	 * List of Hanging entities
	 */
	public static final ItemLists HANGING_ENTITIES = newBuilder().add("ITEM_FRAME","GLOW_ITEM_FRAME","PAINTING").build();
	
	/**
	 * List of Campfires
	 */
	public static final ItemLists CAMPFIRES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("campfires")).add("CAMPFIRE","SOUL_CAMPFIRE").build();
	
	/**
	 * List of blocks that can hold books
	 */
	public static final ItemLists BOOK_CONTAINERS = newBuilder().add("CHISELED_BOOKSHELF","LECTERN").build();

	/**
	 * List of placeable books
	 */
	public static final ItemLists PLACEABLE_BOOKS = newBuilder().add("BOOK", "ENCHANTED_BOOK", "WRITABLE_BOOK", "WRITTEN_BOOK").build();
	
	/**
	 * List of harvestable berries
	 */
	public static final ItemLists HARVESTABLE_BERRIES = newBuilder().add("CAVE_VINES_PLANT","SWEET_BERRY_BUSH").build();

	/*
	 * List of blocks which will be allowed to kill Minecarts.
	 */
	public static final ItemLists MINECART_KILLERS = newBuilder().add("CACTUS").build();
	
	/*
	 * List of unstripped wood logs.
	 */
	public static final ItemLists UNSTRIPPED_WOOD = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("logs")).endsWith("_LOG").notStartsWith("STRIPPED_").add("CRIMSON_STEM", "WARPED_STEM").add("BAMBOO_BLOCK").build();
	
	/*
	 * List of cauldrons.
	 */
	public static final ItemLists CAULDRONS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("cauldrons")).endsWith("CAULDRON").build();
	public static final ItemLists FILLED_CAULDRONS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("cauldrons")).endsWith("CAULDRON").not("CAULDRON").build();
	
	/*
	 * List of buckets that a cauldron can be filled with.
	 */
	public static final ItemLists CAULDRON_FILLABLE = newBuilder().add("WATER_BUCKET", "LAVA_BUCKET", "POWDER_SNOW_BUCKET").build();

	/**
	 * List of hoes
	 */
	public static final ItemLists HOES = newBuilder().withTag(Tag.REGISTRY_ITEMS, minecraft("hoes")).endsWith("_hoe").build();

	public static final ItemLists BRUSHABLE_BLOCKS = newBuilder().add("SUSPICIOUS_SAND", "SUSPICIOUS_GRAVEL").build();
	
	public static final ItemLists PROJECTILE_BREAKABLE_BLOCKS = newBuilder()
		.add("CHORUS_FLOWER", "POINTED_DRIPSTONE")
		.conditionally(() -> CURRENT_VERSION.isNewerThanOrEquals(MINECRAFT_1_20_3), builder -> builder.add("DECORATED_POT"))
		.build();

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
	@Unmodifiable
	public static Set<Material> getGrouping(String groupName) {
		if (!GROUPS.contains(groupName))
			return ImmutableSet.of();

		try {
			return ImmutableSet.copyOf(((ItemLists) ItemLists.class.getField(groupName).get(null)).tagged);
		} catch (Exception e) {
			return ImmutableSet.of();
		}
	}
	
	public static Builder<Material, ItemLists> newBuilder() {
		return new Builder<>(Registry.MATERIAL, Material.class, ItemLists::new).notStartsWith("LEGACY_");
	}
}
