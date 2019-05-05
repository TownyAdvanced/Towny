package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.MobRemovalEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;

import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;

import java.util.ArrayList;
import java.util.List;

public class MobRemovalTimerTask extends TownyTimerTask {

	private Server server;
	public static List<Class<?>> classesOfWorldMobsToRemove = new ArrayList<Class<?>>();
	public static List<Class<?>> classesOfTownMobsToRemove = new ArrayList<Class<?>>();

	public MobRemovalTimerTask(Towny plugin, Server server) {

		super(plugin);
		this.server = server;

		classesOfWorldMobsToRemove = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getWorldMobRemovalEntities(), "WorldMob: ");
		classesOfTownMobsToRemove = EntityTypeUtil.parseLivingEntityClassNames(TownySettings.getTownMobRemovalEntities(), "TownMob: ");
	}

	public static boolean isRemovingWorldEntity(LivingEntity livingEntity) {
		return EntityTypeUtil.isInstanceOfAny(classesOfWorldMobsToRemove, livingEntity);
	}

	public static boolean isRemovingTownEntity(LivingEntity livingEntity) {
		return EntityTypeUtil.isInstanceOfAny(classesOfTownMobsToRemove, livingEntity);
	}

	@Override
	public void run() {
		// Build a list of mobs to be removed
		List<LivingEntity> livingEntitiesToRemove = new ArrayList<LivingEntity>();

		for (World world : server.getWorlds()) {
			TownyWorld townyWorld;

			// Filter worlds not registered
			try {
				townyWorld = TownyUniverse.getDataSource().getWorld(world.getName());
			} catch (NotRegisteredException e) {
				// World was not registered by Towny, so we skip all mobs in it.
				continue;
			} catch (NullPointerException ex) {
				// Spigot has unloaded this world.
				continue;
			}

			// Filter worlds not using towny.
			if (!townyWorld.isUsingTowny())
				continue;

			// Filter worlds that will always pass all checks in a world, regardless of possible conditions.
			if (townyWorld.isForceTownMobs() && townyWorld.hasWorldMobs())
				continue;

			//
			for (LivingEntity livingEntity : world.getLivingEntities()) {
				Location livingEntityLoc = livingEntity.getLocation();

				Coord coord = Coord.parseCoord(livingEntityLoc);
				

				if (!townyWorld.hasTownBlock(coord))
					continue;

				try {
					TownBlock townBlock = townyWorld.getTownBlock(coord);

					// The entity is inside a registered plot.

					// Check if mobs are always allowed inside towns in this world.
					if (townyWorld.isForceTownMobs())
						continue;

					// Check if plot allows mobs.
					if (townBlock.getPermissions().mobs)
						continue;

					// Check if the plot is registered to a town.
					Town town = townBlock.getTown();

					// Check if the town this plot is registered to allows mobs.
					if (town.hasMobs())
						continue;

					// Check that Towny is removing this type of entity inside towns.
					if (!isRemovingTownEntity(livingEntity))
						continue;

				} catch (NotRegisteredException x) {
					// It will fall through to here if the mob is:
					// - In an unregistered plot in this world.
					// - If the plot isn't registered to a town.

					// Check if we're allowing mobs in unregistered plots in this world.
					if (townyWorld.hasWorldMobs())
						continue;

					// Check that Towny is removing this type of entity in unregistered plots.
					if (!isRemovingWorldEntity(livingEntity))
						continue;
				}

				// Check if entity is a Citizens NPC
				if (plugin.isCitizens2()) {
					if (CitizensAPI.getNPCRegistry().isNPC(livingEntity))
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
				TownyMessaging.sendDebugMsg("MobRemoval Removed: " + livingEntity.toString());
				livingEntity.remove();
			}
		}
	}
}
