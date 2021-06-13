package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.BorderUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.LightningStrikeEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.world.StructureGrowEvent;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class TownyWorldListener implements Listener {
	
	private final Towny plugin;

	public TownyWorldListener(Towny instance) {

		plugin = instance;
	}
	
	public static List<String> playersMap = new ArrayList<String>();

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldLoad(WorldLoadEvent event) {

		newWorld(event.getWorld().getName());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldInit(WorldInitEvent event) {

		newWorld(event.getWorld().getName());

	}

	private void newWorld(String worldName) {
		
		boolean dungeonWorld = false;
		
		// Don't create a new world for temporary DungeonsXL instanced worlds.
		if (Bukkit.getServer().getPluginManager().getPlugin("DungeonsXL") != null)
			if (worldName.startsWith("DXL_")) {
				dungeonWorld = true;
			}
				

		//String worldName = event.getWorld().getName();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		try {
			townyUniverse.getDataSource().newWorld(worldName);
			TownyWorld world = townyUniverse.getDataSource().getWorld(worldName);
			if (dungeonWorld)
				world.setUsingTowny(false);
			
			if (world == null)
				TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			else {
				if (!dungeonWorld)
					if (!townyUniverse.getDataSource().loadWorld(world)) {
						// First time world has been noticed
						world.save();
					}
			}
		} catch (AlreadyRegisteredException e) {
			// Allready loaded			
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			e.printStackTrace();
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onLightningStrike(LightningStrikeEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
			return;

		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getWorld().getName());

		/*
		 * Add trident-caused lightning strikes to a map temporarily. 
		 */
		if (event.getCause().equals(LightningStrikeEvent.Cause.TRIDENT)) {
			townyWorld.addTridentStrike(event.getLightning().getEntityId());
			
			final TownyWorld finalWorld = townyWorld;
			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> finalWorld.removeTridentStrike(event.getLightning().getEntityId()), 20L);
		}
	}
	
	/**
	 * Protect trees and mushroom growth transforming neighbouring plots which do not share the same owner. 
	 * @param event - StructureGrowEvent
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void onStructureGrow(StructureGrowEvent event) {
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
			return;

		// The event Location is always one spot, and although 2x2 trees technically should have 4 locations, 
		// we can trust that the saplings were all placed by one person, or group of people, who were allowed
		// to place them.
		List<BlockState> disallowed = BorderUtil.disallowedBlocks(event.getBlocks(), event.getLocation().getBlock());
		
		if (!disallowed.isEmpty())
			event.getBlocks().removeAll(disallowed);
	}

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
	public void onPortalCreate(PortalCreateEvent event) {
		if (!(event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR)) {
			return;
		}
		
		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
			return;

		if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
			return;
		}
		
		for (BlockState block : event.getBlocks()) {
			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
			if (!TownyActionEventExecutor.canBuild((Player) event.getEntity(), block.getLocation(), Material.NETHER_PORTAL)) {
				TownyMessaging.sendErrorMsg(event.getEntity(), Translation.of("msg_err_you_are_not_allowed_to_create_the_other_side_of_this_portal"));
				event.setCancelled(true);
				break;
			}
		}
	}

}
