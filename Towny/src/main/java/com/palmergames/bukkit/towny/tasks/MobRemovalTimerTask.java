package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.MobRemovalEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.BukkitTools;

import com.palmergames.util.JavaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Rabbit;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MobRemovalTimerTask extends TownyTimerTask {

	public static Set<EntityType> typesOfWorldMobsToRemove = new HashSet<>();
	public static Set<EntityType> typesOfWildernessMobsToRemove = new HashSet<>();
	public static Set<EntityType> typesOfTownMobsToRemove = new HashSet<>();
	private static final Set<String> ignoredSpawnReasons = new HashSet<>();
	private static boolean isRemovingKillerBunny;
	
	static {
		populateFields();
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:mob-removal-task"), config -> populateFields());
	}
	
	// https://jd.papermc.io/paper/1.20/org/bukkit/entity/Entity.html#getEntitySpawnReason()
	private static final MethodHandle GET_SPAWN_REASON = JavaUtil.getMethodHandle(Entity.class, "getEntitySpawnReason");

	public MobRemovalTimerTask(Towny plugin) {
		super(plugin);

		populateFields();
	}

	public static boolean isRemovingWorldEntity(LivingEntity livingEntity) {
		return typesOfWorldMobsToRemove.contains(livingEntity.getType());
	}
	
	public static boolean isRemovingWildernessEntity(LivingEntity livingEntity) {
		return typesOfWildernessMobsToRemove.contains(livingEntity.getType());
	}

	public static boolean isRemovingTownEntity(LivingEntity livingEntity) {
		return typesOfTownMobsToRemove.contains(livingEntity.getType());
	}
	
	public static boolean isSpawnReasonIgnored(@NotNull Entity entity) {
		return isSpawnReasonIgnored(entity, null);
	}

	public static boolean isSpawnReasonIgnored(@NotNull Entity entity, @Nullable CreatureSpawnEvent.SpawnReason spawnReason) {
		if (spawnReason != null && ignoredSpawnReasons.contains(spawnReason.name()))
			return true;

		if (GET_SPAWN_REASON == null || ignoredSpawnReasons.isEmpty())
			return false;

		try {
			final Enum<?> reason = (Enum<?>) GET_SPAWN_REASON.invoke(entity);

			return ignoredSpawnReasons.contains(reason.name());
		} catch (Throwable throwable) {
			return false;
		}
	}

	@Override
	public void run() {
		for (final World world : Bukkit.getWorlds()) {
			// Filter worlds not using towny.
			final TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world);
			if (!isRemovingEntities(townyWorld))
				continue;

			for (final LivingEntity entity : world.getLivingEntities()) {
				checkEntity(plugin, townyWorld, entity);
			}
		}
	}

	/**
	 * @param world The world to check
	 * @return Whether entities have a chance of being removed in this world
	 */
	public static boolean isRemovingEntities(final @Nullable TownyWorld world) {
		if (world == null || !world.isUsingTowny())
			return false;

		// Filter worlds that will always pass all checks in a world, regardless of possible conditions.
		if (world.isForceTownMobs() && world.hasWorldMobs())
			return false;

		return true;
	}

	/**
	 * Checks and removes entities if necessary. Can be called from any thread.
	 * @param plugin Towny's plugin instance
	 * @param townyWorld The world the entity is in
	 * @param ent The entity to check
	 */
	public static void checkEntity(final @NotNull Towny plugin, final @NotNull TownyWorld townyWorld, final @NotNull Entity ent) {
		if (!(ent instanceof LivingEntity entity))
			return;

		if (entity instanceof Player || PluginIntegrations.getInstance().isNPC(entity))
			return;

		// Handles entities Globally.
		if (!townyWorld.hasWorldMobs() && isRemovingWorldEntity(entity)) {
			removeEntity(plugin, entity);
			return;
		}

		final Runnable runnable = () -> {
			final Location livingEntityLoc = entity.getLocation();
			final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(livingEntityLoc);

			// Handles entities in the wilderness.
			if (townBlock == null) {
				if (townyWorld.hasWildernessMobs() || !isRemovingWildernessEntity(entity))
					return;
			} else {
				// The entity is inside of a town.

				// Check if mobs are always allowed inside towns in this world, if the townblock allows it, or if the town has mobs forced on.
				if (townyWorld.isForceTownMobs() || townBlock.getPermissions().mobs || (townBlock.getTownOrNull() != null && townBlock.getTownOrNull().isAdminEnabledMobs()))
					return;

				// Check that Towny is removing this type of entity inside towns.
				if (!isRemovingTownEntity(entity))
					return;
			}

			// Check if this is an EliteMob before we do any skipping-removal-of-named-mobs.
			if (PluginIntegrations.getInstance().checkHostileEliteMobs(entity)) {
				removeEntity(plugin, entity);
				return;
			}

			// Special check if it's a rabbit, for the Killer Bunny variant.
			if (entity instanceof Rabbit rabbit && isRemovingKillerBunny && rabbit.getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY) {
				removeEntity(plugin, entity);
				return;
			}

			if (TownySettings.isSkippingRemovalOfNamedMobs() && entity.getCustomName() != null)
				return;

			// Don't remove if the entity's spawn reason is considered ignored by the config
			if (isSpawnReasonIgnored(entity))
				return;

			removeEntity(plugin, entity);
		};
		
		if (plugin.getScheduler().isEntityThread(entity))
			runnable.run();
		else 
			plugin.getScheduler().run(entity, runnable);
	}
	
	private static void removeEntity(final @NotNull Towny plugin, final @NotNull Entity entity) {
		if (MobRemovalEvent.getHandlerList().getRegisteredListeners().length > 0 && BukkitTools.isEventCancelled(new MobRemovalEvent(entity)))
			return;

		if (!plugin.getScheduler().isEntityThread(entity))
			plugin.getScheduler().run(entity, entity::remove);
		else
			entity.remove();
	}
	
	private static void populateFields() {
		typesOfWorldMobsToRemove = entityClassesToTypes(EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getWorldMobRemovalEntities(), "WorldMob: "));
		typesOfWildernessMobsToRemove = entityClassesToTypes(EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getWildernessMobRemovalEntities(),"WildernessMob: "));
		typesOfTownMobsToRemove = entityClassesToTypes(EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getTownMobRemovalEntities(), "TownMob: "));
		isRemovingKillerBunny = TownySettings.isRemovingKillerBunny();
		
		ignoredSpawnReasons.clear();
		for (final String cause : TownySettings.getStrArr(ConfigNodes.PROT_MOB_REMOVE_IGNORED_SPAWN_CAUSES))
			ignoredSpawnReasons.add(cause.toUpperCase(Locale.ROOT));
	}
	
	private static Set<EntityType> entityClassesToTypes(List<Class<?>> classes) {
		final Set<EntityType> types = new HashSet<>();

		for (final EntityType entityType : Registry.ENTITY_TYPE) {
			if (entityType.getEntityClass() == null) {
				continue;
			}
			
			for (final Class<?> clazz : classes) {
				if (clazz.isAssignableFrom(entityType.getEntityClass())) {
					types.add(entityType);
					break;
				}
			}
		}

		return types;
	}
}
