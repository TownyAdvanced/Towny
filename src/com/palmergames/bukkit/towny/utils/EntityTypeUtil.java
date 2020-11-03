package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.util.JavaUtil;

public class EntityTypeUtil {
	private static List<EntityType> ExplosiveEntityTypes = new ArrayList<>(Arrays.asList(
			EntityType.CREEPER,
			EntityType.DRAGON_FIREBALL, 
			EntityType.FIREBALL, 
			EntityType.SMALL_FIREBALL,
			EntityType.FIREWORK, 
			EntityType.MINECART_TNT, 
			EntityType.PRIMED_TNT, 
			EntityType.WITHER, 
			EntityType.WITHER_SKULL,
			EntityType.ENDER_CRYSTAL));

	public static boolean isInstanceOfAny(List<Class<?>> classesOfWorldMobsToRemove2, Object obj) {

		for (Class<?> c : classesOfWorldMobsToRemove2)
			if (c.isInstance(obj))
				return true;
		return false;
	}

	public static List<Class<?>> parseLivingEntityClassNames(List<String> mobClassNames, String errorPrefix) {

		List<Class<?>> livingEntityClasses = new ArrayList<Class<?>>();
		for (String mobClassName : mobClassNames) {
			if (mobClassName.isEmpty())
				continue;

			try {
				Class<?> c = Class.forName("org.bukkit.entity." + mobClassName);
				if (JavaUtil.isSubInterface(LivingEntity.class, c))
					livingEntityClasses.add(c);
				else
					throw new Exception();
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
		Material material = null;
		
		switch(entityType) {
		case ITEM_FRAME:
			material = Material.ITEM_FRAME;
			break;

		case PAINTING:
			material = Material.PAINTING;
			break;
			
		case ARMOR_STAND:
			material = Material.ARMOR_STAND;
			break;
			
		case LEASH_HITCH:
			material = Material.LEAD;
			break;

		case ENDER_CRYSTAL:
			material = Material.END_CRYSTAL;
			break;

		case MINECART:
		case MINECART_MOB_SPAWNER:
			material = Material.MINECART;
			break;
			
		case MINECART_CHEST:
			material = Material.CHEST_MINECART;
			break;
		
		case MINECART_FURNACE:
			material = Material.FURNACE_MINECART;
			break;

		case MINECART_COMMAND:
			material = Material.COMMAND_BLOCK_MINECART;
			break;

		case MINECART_HOPPER:
			material = Material.HOPPER_MINECART;
			break;
			
		case MINECART_TNT:
			material = Material.TNT_MINECART;
			break;
		}
					
		return material;
	}
	
	/**
	 * A list of explosion-causing entities.
	 * 
	 * @param entityType - EntityType to test.
	 * @return true if the EntityType will explode.
	 */
	public static boolean isExplosive(EntityType entityType) {

		return ExplosiveEntityTypes.contains(entityType);	
	}
}
