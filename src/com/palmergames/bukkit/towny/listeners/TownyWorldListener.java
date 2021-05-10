package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.BlockState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
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

//	@EventHandler(priority = EventPriority.NORMAL)
//	public void onLightningStrike(LightningStrikeEvent event) {
//
//		if (plugin.isError()) {
//			event.setCancelled(true);
//			return;
//		}
//
//		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
//			return;
//
//		TownyWorld townyWorld = TownyAPI.getInstance().getTownyWorld(event.getWorld().getName());
//
//		/*
//		 * Add trident-caused lightning strikes to a map temporarily. 
//		 */
//		if (event.getCause().equals(LightningStrikeEvent.Cause.TRIDENT)) {
//			townyWorld.addTridentStrike(event.getLightning().getEntityId());
//			
//			final TownyWorld finalWorld = townyWorld;
//			Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> finalWorld.removeTridentStrike(event.getLightning().getEntityId()), 20L);
//		}
//	}
	
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

		TownBlock townBlock = null;
		TownBlock otherTownBlock = null;
		Town town = null;
		Town otherTown = null;
		Resident resident = null;
		List<BlockState> removed = new ArrayList<>();
		// The event Location is always one spot, and although 2x2 trees technically should have 4 locations, 
		// we can trust that the saplings were all placed by one person, or group of people, who were allowed
		// to place them.
		Coord coord = Coord.parseCoord(event.getLocation());
		for (BlockState blockState : event.getBlocks()) {
			Location blockLocation = blockState.getLocation();
			Coord blockCoord = Coord.parseCoord(blockLocation);

			// Wilderness so continue.
			if (TownyAPI.getInstance().isWilderness(blockLocation)) {
				continue;
			}

			// Same townblock as event location, continue;
			if (coord.equals(blockCoord)) {
				continue;
			}
			
			townBlock = TownyAPI.getInstance().getTownBlock(blockLocation);

			// Resident Owned Location
			if (townBlock.hasResident()) {
				try {
					resident = townBlock.getResident();
				} catch (NotRegisteredException e) {
				}
				otherTownBlock = TownyAPI.getInstance().getTownBlock(blockLocation);
				try {
					// if residents don't match.
					if (otherTownBlock.hasResident() && otherTownBlock.getResident() != resident) {
						removed.add(blockState);
						continue;
					// if plot doesn't have a resident.
					} else if (!otherTownBlock.hasResident()) {
						removed.add(blockState);
						continue;
					// if both townblock have same owner. 
					} else if (resident == otherTownBlock.getResident()) {
						continue;
					}
				} catch (NotRegisteredException e) {
				}
			// Town Owned Location
			} else {
				try {
					town = townBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				try {
					otherTownBlock = TownyAPI.getInstance().getTownBlock(blockLocation);
					otherTown = otherTownBlock.getTown();
				} catch (NotRegisteredException e) {
				}
				// If towns don't match.
				if (town != otherTown) {						
					removed.add(blockState);
					continue;
				// If town-owned is growing into a resident-owned plot.
				} else if (otherTownBlock.hasResident()) {
					removed.add(blockState);
					continue;
				// If towns match.
				} else if (town == otherTown) {
					continue;
				}
			}
		}
		if (!removed.isEmpty())
			event.getBlocks().removeAll(removed);
	}

//	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled=true)
//	public void onPortalCreate(PortalCreateEvent event) {
//		if (!(event.getReason() == PortalCreateEvent.CreateReason.NETHER_PAIR)) {
//			return;
//		}
//		
//		if (!TownyAPI.getInstance().isTownyWorld(event.getWorld()))
//			return;
//
//		if (!event.getEntity().getType().equals(EntityType.PLAYER)) {
//			return;
//		}
//		
//		for (BlockState block : event.getBlocks()) {
//			//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
//			if (!TownyActionEventExecutor.canBuild((Player) event.getEntity(), block.getLocation(), Material.NETHER_PORTAL)) {
//				TownyMessaging.sendErrorMsg(event.getEntity(), Translation.of("msg_err_you_are_not_allowed_to_create_the_other_side_of_this_portal"));
//				event.setCancelled(true);
//				break;
//			}
//		}
//	}

}
