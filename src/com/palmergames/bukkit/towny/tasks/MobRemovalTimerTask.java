package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.MobRemovalEvent;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Rabbit;

import java.util.ArrayList;
import java.util.List;

public class MobRemovalTimerTask extends TownyTimerTask {

	private final Server server;
	public static List<Class<?>> classesOfWorldMobsToRemove = new ArrayList<>();
	public static List<Class<?>> classesOfWildernessMobsToRemove = new ArrayList<>();
	public static List<Class<?>> classesOfTownMobsToRemove = new ArrayList<>();
	private final boolean isRemovingKillerBunny;

	public MobRemovalTimerTask(Towny plugin, Server server) {

		super(plugin);
		this.server = server;

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
		// Build a list of mobs to be removed
		List<LivingEntity> livingEntitiesToRemove = new ArrayList<>();

		for (World world : server.getWorlds()) {
			// Filter worlds not using towny.
			if (!TownyAPI.getInstance().isTownyWorld(world))
				continue;

			TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(world.getName());
			if (townyWorld == null)
				continue;

			// Filter worlds that will always pass all checks in a world, regardless of possible conditions.
			if (townyWorld.isForceTownMobs() && townyWorld.hasWorldMobs())
				continue;

			for (LivingEntity livingEntity : world.getLivingEntities()) {
				Location livingEntityLoc = livingEntity.getLocation();
				if (!world.isChunkLoaded(livingEntityLoc.getBlockX() >> 4, livingEntityLoc.getBlockZ() >> 4))
					continue;

				// Check if entity is a Citizens NPC
				if (BukkitTools.checkCitizens(livingEntity))
					return;
				
				// Handles entities Globally.
				if (!townyWorld.hasWorldMobs() && isRemovingWorldEntity(livingEntity)) {
					livingEntitiesToRemove.add(livingEntity);
					continue;
				}
				
				// Handles entities in the wilderness.
				if (TownyAPI.getInstance().isWilderness(livingEntityLoc)) {
					if (townyWorld.hasWildernessMobs())
						continue;
					if (!isRemovingWildernessEntity(livingEntity))
						continue;
					
					livingEntitiesToRemove.add(livingEntity);
					continue;
				}

				// The entity is inside of a town.
				TownBlock townBlock = TownyAPI.getInstance().getTownBlock(livingEntityLoc);

				// Check if mobs are always allowed inside towns in this world.
				if (townyWorld.isForceTownMobs() || townBlock.getPermissions().mobs)
					continue;

				// Check that Towny is removing this type of entity inside towns.
				if (!isRemovingTownEntity(livingEntity))
					continue;

				if (TownySettings.isSkippingRemovalOfNamedMobs() && livingEntity.getCustomName() != null)
					continue;

				// Special check if it's a rabbit, for the Killer Bunny variant.
				if (livingEntity.getType().equals(EntityType.RABBIT))
					if (isRemovingKillerBunny && ((Rabbit) livingEntity).getRabbitType().equals(Rabbit.Type.THE_KILLER_BUNNY)) {
						livingEntitiesToRemove.add(livingEntity);							
						continue;						
					}
				
				livingEntitiesToRemove.add(livingEntity);
			}
		}
		MobRemovalEvent mobRemovalEvent;
		for (LivingEntity livingEntity : livingEntitiesToRemove) {
			mobRemovalEvent = new MobRemovalEvent(livingEntity);
			plugin.getServer().getPluginManager().callEvent(mobRemovalEvent);
			if (!mobRemovalEvent.isCancelled()) {
				livingEntity.remove();
			}
		}
	}
}
