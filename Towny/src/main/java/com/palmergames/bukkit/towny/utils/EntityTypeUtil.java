package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.util.JavaUtil;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyMessaging;

public class EntityTypeUtil {
	
	/**
	 * Used for debugging whether all entity types were mapped, should always equal the map size on the latest version.
	 */
	private static int attempted = 0;

	/**
	 * A mapping of various entity types to their corresponding material
	 */
	private static final Map<EntityType, Material> ENTITY_TYPE_MATERIAL_MAP = JavaUtil.make(() -> {
		Map<EntityType, Material> map = new HashMap<>();
		register(map, "axolotl", "axolotl_bucket");
		register(map, "cod", "cod");
		register(map, "salmon", "salmon");
		register(map, "pufferfish", "pufferfish");
		register(map, "tropical_fish", "tropical_fish");
		register(map, "tadpole", "tadpole_bucket");
		register(map, "parrot", "parrot_spawn_egg");
		register(map, "item_frame", "item_frame");
		register(map, "glow_item_frame", "glow_item_frame");
		register(map, "painting", "painting");
		register(map, "armor_stand", "armor_stand");
		register(map, "leash_knot", "lead");
		register(map, "end_crystal", "end_crystal");
		register(map, "minecart", "minecart");
		register(map, "spawner_minecart", "minecart");
		register(map, "chest_minecart", "chest_minecart");
		register(map, "furnace_minecart", "furnace_minecart");
		register(map, "command_block_minecart", "command_block_minecart");
		register(map, "hopper_minecart", "hopper_minecart");
		register(map, "tnt_minecart", "tnt_minecart");
		register(map, "boat", "oak_boat");
		register(map, "chest_boat", "oak_chest_boat");
		register(map, "cow", "cow_spawn_egg");
		register(map, "goat", "goat_spawn_egg");
		register(map, "mooshroom", "mooshroom_spawn_egg");
		register(map, "ender_pearl", "ender_pearl");
		register(map, "wind_charge", "wind_charge");
		
		TownyMessaging.sendDebugMsg("[EntityTypeUtil] Attempted: " + attempted + " | Registered: " + map.size());

		return map;
	});
	
	public static boolean isInstanceOfAny(List<Class<?>> classes, Object obj) {

		for (Class<?> c : classes)
			if (c.isInstance(obj))
				return true;
		return false;
	}
	
	public static boolean isProtectedEntity(Entity entity) {
		return isInstanceOfAny(TownySettings.getProtectedEntityTypes(), entity);
	}

	public static List<Class<?>> parseLivingEntityClassNames(List<String> mobClassNames, String errorPrefix) {

		List<Class<?>> livingEntityClasses = new ArrayList<>();
		for (String mobClassName : mobClassNames) {
			if (mobClassName.isEmpty())
				continue;

			try {
				Class<?> c = Class.forName("org.bukkit.entity." + mobClassName);
				livingEntityClasses.add(c);
			} catch (ClassNotFoundException e) {
				TownyMessaging.sendErrorMsg(String.format("%s%s is not an acceptable class.", errorPrefix, mobClassName));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(String.format("%s%s is not an acceptable living entity.", errorPrefix, mobClassName));
			}
		}
		return livingEntityClasses;
	}
	
	/**
	 * Helper method to get a Material from an Entity.
	 * Used with protection tests in plots.
	 * 
	 * @param entityType EntityType to gain a Material value for.
	 * @return null or a suitable Material.
	 */
	@Nullable
	public static Material parseEntityToMaterial(EntityType entityType) {
		Material lookup = ENTITY_TYPE_MATERIAL_MAP.get(entityType);
		if (lookup != null)
			return lookup;

		// Attempt to find the spawn egg
		final NamespacedKey spawnEggKey = NamespacedKey.fromString(entityType.getKey() + "_spawn_egg");
		final Material spawnEgg = spawnEggKey != null ? Registry.MATERIAL.get(spawnEggKey) : null;
		if (spawnEgg != null) {
			return spawnEgg;
		}
		
		// Attempt to lookup a material with the same name, if it doesn't exist it's null.
		return Registry.MATERIAL.get(entityType.getKey());
	}

	/**
	 * Helper method for parsing an entity to a material, or a default material if none is found.
	 * @param entityType Entity type to parse
	 * @param defaultValue Material to use if none could be found.
	 * @return The parsed material, or the fallback value.
	 */
	@NotNull
	public static Material parseEntityToMaterial(EntityType entityType, @NotNull Material defaultValue) {
		Material material = parseEntityToMaterial(entityType);
		return material == null ? defaultValue : material;
	}
	
	/**
	 * A list of explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType will explode.
	 */
	public static boolean isExplosive(EntityType entityType) {

		return EntityLists.EXPLOSIVE.contains(entityType);
	}
	
	/**
	 * A list of PVP explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVP and will explode.
	 */
	public static boolean isPVPExplosive(EntityType entityType) {

		return EntityLists.PVP_EXPLOSIVE.contains(entityType);
	}
	
	/**
	 * A list of PVM explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVM and will explode.
	 */
	public static boolean isPVMExplosive(EntityType entityType) {

		return EntityLists.PVE_EXPLOSIVE.contains(entityType);
	}
	
	private static void register(Map<EntityType, Material> map, String name, String mat) {
		attempted++;
		EntityType type = Registry.ENTITY_TYPE.get(NamespacedKey.minecraft(name));
		Material material = Registry.MATERIAL.get(NamespacedKey.minecraft(mat));

		if (type == null || material == null) {
			TownyMessaging.sendDebugMsg("[EntityTypeUtil] Could not map " + name + " to " + mat);
			return;
		}

		map.put(type, material);
	}
}
