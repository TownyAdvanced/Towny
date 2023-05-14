package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.util.JavaUtil;
import org.bukkit.Material;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyMessaging;

public class EntityTypeUtil {
	private static final List<EntityType> ExplosiveEntityTypes = Arrays.asList(
			EntityType.CREEPER,
			EntityType.DRAGON_FIREBALL, 
			EntityType.FIREBALL, 
			EntityType.SMALL_FIREBALL,
			EntityType.FIREWORK,
			EntityType.MINECART_TNT,
			EntityType.PRIMED_TNT,
			EntityType.WITHER,
			EntityType.WITHER_SKULL,
			EntityType.ENDER_CRYSTAL);
	
	private static final List<EntityType> ExplosivePVMEntityTypes = Arrays.asList(
			EntityType.CREEPER,
			EntityType.DRAGON_FIREBALL,
			EntityType.FIREBALL,
			EntityType.SMALL_FIREBALL,
			EntityType.WITHER,
			EntityType.WITHER_SKULL,
			EntityType.ENDER_CRYSTAL);

	private static final List<EntityType> ExplosivePVPEntityTypes = Arrays.asList(
			EntityType.FIREWORK,
			EntityType.MINECART_TNT,
			EntityType.PRIMED_TNT,
			EntityType.ENDER_CRYSTAL);
	
	// TODO account for older versions not having certain constants
	/**
	 * A mapping of various entity types to their corresponding material
	 */
	private static final Map<EntityType, Material> ENTITY_TYPE_MATERIAL_MAP = JavaUtil.make(() -> {
		Map<EntityType, Material> map = new HashMap<>();
		map.put(EntityType.AXOLOTL, Material.AXOLOTL_BUCKET);
		map.put(EntityType.COD, Material.COD);
		map.put(EntityType.SALMON, Material.SALMON);
		map.put(EntityType.PUFFERFISH, Material.PUFFERFISH);
		map.put(EntityType.TROPICAL_FISH, Material.TROPICAL_FISH);
		map.put(EntityType.TADPOLE, Material.TADPOLE_BUCKET);
		map.put(EntityType.ITEM_FRAME, Material.ITEM_FRAME);
		map.put(EntityType.GLOW_ITEM_FRAME, Material.GLOW_ITEM_FRAME);
		map.put(EntityType.PAINTING, Material.PAINTING);
		map.put(EntityType.ARMOR_STAND, Material.ARMOR_STAND);
		map.put(EntityType.LEASH_HITCH, Material.LEAD);
		map.put(EntityType.ENDER_CRYSTAL, Material.END_CRYSTAL);
		map.put(EntityType.MINECART, Material.MINECART);
		map.put(EntityType.MINECART_MOB_SPAWNER, Material.MINECART);
		map.put(EntityType.MINECART_CHEST, Material.CHEST_MINECART);
		map.put(EntityType.MINECART_FURNACE, Material.FURNACE_MINECART);
		map.put(EntityType.MINECART_COMMAND, Material.COMMAND_BLOCK_MINECART);
		map.put(EntityType.MINECART_HOPPER, Material.HOPPER_MINECART);
		map.put(EntityType.MINECART_TNT, Material.TNT_MINECART);
		map.put(EntityType.BOAT, Material.OAK_BOAT);
		map.put(EntityType.CHEST_BOAT, Material.OAK_CHEST_BOAT);

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

		return ExplosiveEntityTypes.contains(entityType);	
	}
	
	/**
	 * A list of PVP explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVP and will explode.
	 */
	public static boolean isPVPExplosive(EntityType entityType) {

		return ExplosivePVPEntityTypes.contains(entityType);	
	}
	
	/**
	 * A list of PVM explosion-causing entities.
	 * 
	 * @param entityType EntityType to test.
	 * @return true if the EntityType is PVM and will explode.
	 */
	public static boolean isPVMExplosive(EntityType entityType) {

		return ExplosivePVMEntityTypes.contains(entityType);	
	}
}
