package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.palmergames.bukkit.towny.TownySettings;
import org.bukkit.Material;
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
			EntityType.PRIMED_TNT);
	
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
		return switch (entityType) {
			case AXOLOTL -> Material.AXOLOTL_BUCKET;
			case COD -> Material.COD;
			case SALMON -> Material.SALMON;
			case PUFFERFISH -> Material.PUFFERFISH;
			case TROPICAL_FISH -> Material.TROPICAL_FISH;
			case ITEM_FRAME -> Material.ITEM_FRAME;
			case GLOW_ITEM_FRAME -> Material.GLOW_ITEM_FRAME;
			case PAINTING -> Material.PAINTING;
			case ARMOR_STAND -> Material.ARMOR_STAND;
			case LEASH_HITCH -> Material.LEAD;
			case ENDER_CRYSTAL -> Material.END_CRYSTAL;
			case MINECART, MINECART_MOB_SPAWNER -> Material.MINECART;
			case MINECART_CHEST -> Material.CHEST_MINECART;
			case MINECART_FURNACE -> Material.FURNACE_MINECART;
			case MINECART_COMMAND -> Material.COMMAND_BLOCK_MINECART;
			case MINECART_HOPPER -> Material.HOPPER_MINECART;
			case MINECART_TNT -> Material.TNT_MINECART;
			case BOAT -> Material.OAK_BOAT;
			default -> null;
		};
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
