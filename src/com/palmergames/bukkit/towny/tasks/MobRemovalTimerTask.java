package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.MobRemovalEvent;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Rabbit;

import java.util.ArrayList;
import java.util.List;

public class MobRemovalTimerTask extends TownyTimerTask {

	public static List<Class<?>> classesOfWorldMobsToRemove = new ArrayList<>();
	public static List<Class<?>> classesOfWildernessMobsToRemove = new ArrayList<>();
	public static List<Class<?>> classesOfTownMobsToRemove = new ArrayList<>();
	private final boolean isRemovingKillerBunny;

	public MobRemovalTimerTask(Towny plugin) {
		super(plugin);

		classesOfWorldMobsToRemove = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getWorldMobRemovalEntities(), "WorldMob: ");
		classesOfWildernessMobsToRemove = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getWildernessMobRemovalEntities(),"WildernessMob: ");
		classesOfTownMobsToRemove = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getTownMobRemovalEntities(), "TownMob: ");
		isRemovingKillerBunny = TownySettings.isRemovingKillerBunny();
	}

	public static boolean isRemovingWorldEntity(LivingEntity livingEntity) {
		return EntityTypeUtil.isInstanceOfAny(classesOfWorldMobsToRemove, livingEntity);
	}
	
	public static boolean isRemovingWildernessEntity(LivingEntity livingEntity) {
		return  EntityTypeUtil.isInstanceOfAny(classesOfWildernessMobsToRemove, livingEntity);
	}

	public static boolean isRemovingTownEntity(LivingEntity livingEntity) {
		return EntityTypeUtil.isInstanceOfAny(classesOfTownMobsToRemove, livingEntity);
	}

	@Override
	public void run() {
		final boolean skipRemovalEvent = MobRemovalEvent.getHandlerList().getRegisteredListeners().length == 0;

		for (World world : Bukkit.getWorlds()) {
			// Filter worlds not using towny.
			TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world);
			if (townyWorld == null || !townyWorld.isUsingTowny())
				continue;

			// Filter worlds that will always pass all checks in a world, regardless of possible conditions.
			if (townyWorld.isForceTownMobs() && townyWorld.hasWorldMobs())
				continue;

			final List<LivingEntity> entities = world.getLivingEntities();
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				// Build a list of mobs to be removed
				List<LivingEntity> entitiesToRemove = new ArrayList<>();

				for (LivingEntity entity : entities) {
					Location livingEntityLoc = entity.getLocation();

					// Check if entity is a Citizens NPC
					if (PluginIntegrations.getInstance().checkCitizens(entity))
						continue;

					// Handles entities Globally.
					if (!townyWorld.hasWorldMobs() && isRemovingWorldEntity(entity)) {
						entitiesToRemove.add(entity);
						continue;
					}

					// Handles entities in the wilderness.
					if (TownyAPI.getInstance().isWilderness(livingEntityLoc)) {
						if (townyWorld.hasWildernessMobs() || !isRemovingWildernessEntity(entity))
							continue;
					} else {
						// The entity is inside of a town.
						TownBlock townBlock = TownyAPI.getInstance().getTownBlock(livingEntityLoc);

						// Check if mobs are always allowed inside towns in this world.
						if (townyWorld.isForceTownMobs() || townBlock.getPermissions().mobs)
							continue;

						// Check that Towny is removing this type of entity inside towns.
						if (!isRemovingTownEntity(entity))
							continue;
					}

					// Check if this is an EliteMob before we do any skipping-removal-of-named-mobs.
					if (PluginIntegrations.getInstance().checkHostileEliteMobs(entity)) {
						entitiesToRemove.add(entity);
						continue;
					}

					if (TownySettings.isSkippingRemovalOfNamedMobs() && entity.getCustomName() != null)
						continue;

					// Special check if it's a rabbit, for the Killer Bunny variant.
					if (entity instanceof Rabbit rabbit) {
						if (isRemovingKillerBunny && rabbit.getRabbitType() == Rabbit.Type.THE_KILLER_BUNNY) {
							entitiesToRemove.add(entity);
							continue;
						}
					}

					// Ensure the entity hasn't been removed since
					if (entity.isDead())
						continue;

					if (!skipRemovalEvent && BukkitTools.isEventCancelled(new MobRemovalEvent(entity)))
						continue;

					entitiesToRemove.add(entity);
				}
				
				Bukkit.getScheduler().runTask(plugin, () -> {
					for (LivingEntity entity : entitiesToRemove)
						entity.remove();
				});
			});
		}
	}
}
