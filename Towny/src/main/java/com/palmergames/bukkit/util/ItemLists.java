package com.palmergames.bukkit.util;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;
import com.palmergames.bukkit.towny.object.AbstractRegistryList;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.ApiStatus.Internal;

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

	@Internal
	public Collection<String> getMaterialNameCollection() {
		return tagged.stream().map(Material::name).collect(Collectors.toList());
	}

	/**
	 * List of Axe items.
	 */
	public static final ItemLists AXES = newBuilder().withTag(Tag.REGISTRY_ITEMS, minecraft("axes")).endsWith("_AXE").build();

	/**
	 * List of Sword items.
	 */
	public static final ItemLists SWORDS = newBuilder().withTag(Tag.REGISTRY_ITEMS, minecraft("swords")).endsWith("_SWORD").build();

	/**
	 * List of Bow items.
	 */
	public static final ItemLists BOWS = newBuilder().add("BOW","CROSSBOW").build();

	/**
	 * List of Weapon items.
	 */
	public static final ItemLists WEAPONS = newBuilder()
			.includeList(AXES)
			.includeList(SWORDS)
			.includeList(BOWS)
			.build();

	/**
	 * List of Armour items.
	 */
	public static final ItemLists ARMOURS = newBuilder()
			.withTag(Tag.REGISTRY_ITEMS, minecraft("chest_armor"))
			.withTag(Tag.REGISTRY_ITEMS, minecraft("head_armor"))
			.withTag(Tag.REGISTRY_ITEMS, minecraft("foot_armor"))
			.withTag(Tag.REGISTRY_ITEMS, minecraft("leg_armor"))
			.endsWith("_CHESTPLATE")
			.endsWith("_HELMET")
			.endsWith("_LEGGINGS")
			.endsWith("_BOOTS")
			.build();
	
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
	 * List of Flowers.
	 */
	public static final ItemLists FLOWERS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("flowers")).build();

	/**
	 * List of Ores and Valuable Raw Materials.
	 */
	public static final ItemLists ORES = newBuilder().endsWith("_ORE").add("RAW_IRON_BLOCK","RAW_GOLD_BLOCK","RAW_COPPER_BLOCK","ANCIENT_DEBRIS","CLAY","GLOWSTONE","GILDED_BLACKSTONE").build();

	/**
	 * List of Saplings.
	 */
	public static final ItemLists SAPLINGS = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("saplings")).endsWith("_SAPLING").add("MANGROVE_PROPAGULE","CRIMSON_FUNGUS","WARPED_FUNGUS").build();

	/**
	 * List of Plantable plants.
	 */
	public static final ItemLists PLANTABLES = newBuilder().add("BAMBOO","BEETROOT_SEEDS","BROWN_MUSHROOM","CACTUS","CARROTS","CHORUS_FRUIT","COCOA_BEANS","CRIMSON_FUNGUS","KELP","MELON_SEEDS","NETHER_WART","PITCHER_POD","POTATOES","PUMPKIN_SEEDS","RED_MUSHROOM","SEA_PICKLE","SUGAR_CANE","TORCHFLOWER_SEEDS","WARPED_FUNGUS","WHEAT_SEEDS").build();

	/**
	 * List of Crops.
	 */
	public static final ItemLists CROPS = newBuilder().add("ATTACHED_MELON_STEM","ATTACHED_PUMPKIN_STEM","BEETROOTS","CARROTS","COCOA","MELON","MELON_STEM","PITCHER_CROP","PITCHER_PLANT","POTATOES","PUMPKIN","PUMPKIN_STEM","SWEET_BERRY_BUSH","WHEAT").build();

	/**
	 * List of Trees and Leaves.
	 */
	public static final ItemLists TREES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("logs")).withTag(Tag.REGISTRY_BLOCKS, minecraft("leaves")).endsWith("_WOOD").endsWith("_HYPHAE").notStartsWith("STRIPPED_").endsWith("_LEAVES").endsWith("_WART_BLOCK").endsWith("_LOG").add("CRIMSON_STEM", "WARPED_STEM").add("BAMBOO_BLOCK").build();
	
	/**
	 * List of Leaves.
	 */
	public static final ItemLists LEAVES = newBuilder().withTag(Tag.REGISTRY_BLOCKS, minecraft("leaves")).endsWith("_LEAVES").endsWith("_WART_BLOCK").build();
	
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
	 * List of Weatherable Blocks.
	 */
	public static final ItemLists WEATHERABLE_BLOCKS = newBuilder().add("COPPER_BLOCK","CUT_COPPER","CUT_COPPER_SLAB","CUT_COPPER_STAIRS", "COPPER_TRAPDOOR")
		.add("COPPER_CHEST", "COPPER_GOLEM_STATUE", "COPPER_BARS", "COPPER_CHAIN", "COPPER_LANTERN")
		.conditionally(() -> CURRENT_VERSION.isNewerThanOrEquals(MINECRAFT_1_21_9), builder -> builder.add("LIGHTNING_ROD")) // lightning rods became weatherable/waxable
		// Include the 3 other variants copper blocks have
		.startsWith("EXPOSED_")
		.startsWith("OXIDIZED_")
		.startsWith("WEATHERED_")
		.build();
	
	/** 
	 * List of Scrapable Blocks (They can lose their wax.)
	 */
	public static final ItemLists WAXED_BLOCKS = newBuilder().startsWith("WAXED_").build();

	/**
	 * List of Copper Blocks.
	 */
	public static final ItemLists COPPER_BLOCKS = concat(WEATHERABLE_BLOCKS, WAXED_BLOCKS);

	/**
	 * Copper chests - used so that not every version has to be specified in the switches config.
	 */
	public static final ItemLists COPPER_CHEST = newBuilder().endsWith("COPPER_CHEST").build();
	public static final ItemLists COPPER_GOLEM_STATUE = newBuilder().endsWith("COPPER_GOLEM_STATUE").build();

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
	public static final ItemLists HARVESTABLE_BERRIES = newBuilder().add("CAVE_VINES_PLANT","CAVE_VINES","SWEET_BERRY_BUSH").build();

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
	 * List of liquid blocks
	 */
	public static final ItemLists LIQUID_BLOCKS = newBuilder().add("WATER", "LAVA", "BUBBLE_COLUMN").build();
	
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
	 * List of Carpets.
	 */
	public static final ItemLists CARPETS = newBuilder().endsWith("_CARPET").build();

	/**
	 * List of Plants.
	 */
	public static final ItemLists PLANTS = newBuilder()
			.includeList(FLOWERS)
			.includeList(CROPS)
			.includeList(PLANTABLES)
			.add("SHORT_GRASS","TALL_GRASS","LARGE_FERN","VINE","TWISTING_VINES_PLANT","WEEPING_VINES_PLANT","NETHER_WART_BLOCK","CRIMSON_ROOTS","WARPED_ROOTS","NETHER_SPROUTS","BIG_DRIPLEAF","SMALL_DRIPLEAF").build();

	/**
	 * Minecraft uses 3 types of air
	 */
	public static final ItemLists AIR_TYPES = newBuilder()
		.add("AIR")
		.add("CAVE_AIR")
		.add("VOID_AIR").build();

	/**
	 * List of all the banners
	 */
	public static final ItemLists BANNERS = newBuilder().endsWith("_BANNER").build();

	/**
	 * List of wall coral fans, players can fall through these
	 */
	public static final ItemLists CORAL_FANS = newBuilder()
		.endsWith("_WALL_FAN")
		.build();
		
	/**
	 * List of solid blocks
	 */
	public static final ItemLists NOT_SOLID_BLOCKS = newBuilder()
			.add("TRIPWIRE", "TRIPWIRE_HOOK")
			.add("REDSTONE_WIRE","COMPARATOR","REPEATER","LEVER")
			.includeList(CORAL_FANS)
			.includeList(AIR_TYPES)
			.includeList(SIGNS)
			.includeList(BUTTONS)
			.includeList(PRESSURE_PLATES)
			.includeList(FENCE_GATES) //if open they are basically air
			.includeList(TRAPDOORS) //if open they are basically air
			.includeList(DOORS) ////if open they are basically air
			.includeList(TORCHES)
			.includeList(BANNERS)
			.includeList(CARPETS)
			.includeList(PLANTS).build();
	
	public static final ItemLists FALLING_BLOCKS = newBuilder()
			.add("SAND", "RED_SAND", "GRAVEL", "SUSPICIOUS_SAND", "SUSPICIOUS_GRAVEL")
			.endsWith("_CONCRETE_POWDER")
			.build();

	/**
	 * List of blocks which, when exploded, will not have their drops set to false, despite our asking.
	 */
	public static final ItemLists EXPLODABLE_ATTACHABLES = newBuilder()
			.add("LANTERN","SOUL_LANTERN")
			.add("REDSTONE_WIRE","COMPARATOR","REPEATER","LEVER")
			.endsWith("_CARPET")
			.endsWith("_BANNER")
			.endsWith("_BUTTON")
			.endsWith("RAIL")
			.endsWith("PUMPKIN_STEM").endsWith("MELON_STEM")
			.endsWith("_AMETHYST_BUD").add("AMETHYST_CLUSTER")
			.includeList(FLOWERS)
			.includeList(SAPLINGS)
			.includeList(PRESSURE_PLATES)
			.includeList(WOOD_DOORS)
			.build();

	public static final ItemLists INFESTED_BLOCKS = newBuilder().startsWith("INFESTED_").build();

	public static final ItemLists CHESTS = newBuilder().endsWith("CHEST").build();
	
	public static final ItemLists CHESTS_WITHOUT_ENDERCHEST = newBuilder().endsWith("CHEST").notStartsWith("ENDER_").build();

	public static final ItemLists SHELVES = newBuilder().endsWith("SHELF").build();

	/**
	 * Config-useable material groups.
	 */
	private static final Map<String, ItemLists> GROUPS = Arrays.stream(ItemLists.class.getFields())
		.filter(field -> Modifier.isStatic(field.getModifiers()))
		.filter(field -> field.getType().equals(ItemLists.class))
		.collect(Collectors.toMap(field -> field.getName().toLowerCase(Locale.ROOT), field -> {
			try {
				return (ItemLists) field.get(null);
			} catch (IllegalAccessException e) {
				throw new RuntimeException(e);
			}
		}));
	
	private static final Set<String> CUSTOM_GROUPS = new HashSet<>();
	
	/**
	 * Returns a pre-configured list from the GROUPS.
	 * 
	 * @param groupName String value of one of the {@link ItemLists#GROUPS}
	 * @return Set&lt;Material&gt; grouping of materials, or an empty set if the grouping was not found.
	 */
	@NotNull
	@Unmodifiable
	public static Set<Material> getGrouping(@NotNull String groupName) {
		final ItemLists grouping = GROUPS.get(groupName.toLowerCase(Locale.ROOT));

		return grouping != null ? ImmutableSet.copyOf(grouping.tagged) : ImmutableSet.of();
	}

	public static boolean hasGroup(@NotNull String groupName) {
		return GROUPS.containsKey(groupName.toLowerCase(Locale.ROOT));
	}
	
	public static void addGroup(@NotNull String groupName, @NotNull ItemLists group) {
		GROUPS.put(groupName.toLowerCase(Locale.ROOT), group);
		CUSTOM_GROUPS.add(groupName.toLowerCase(Locale.ROOT));
	}
	
	@ApiStatus.Internal
	public static void clearCustomGroups() {
		CUSTOM_GROUPS.forEach(GROUPS::remove);
		CUSTOM_GROUPS.clear();
	}
	
	@Internal
	public static Map<String, Collection<? extends Keyed>> allGroups() {
		return GROUPS.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().tagged()));
	}
	
	public static Builder<Material, ItemLists> newBuilder() {
		return new Builder<>(Registry.MATERIAL, Material.class, ItemLists::new);
	}
	
	private static ItemLists concat(ItemLists first, ItemLists... others) {
		final Set<Material> values = new HashSet<>(first.tagged);

		for (final ItemLists other : others) {
			values.addAll(other.tagged);
		}

		return new ItemLists(values);
	}
}
