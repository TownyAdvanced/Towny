package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class TownyWorldListener implements Listener {
	
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
						townyUniverse.getDataSource().saveWorld(world);
					}
			}
		} catch (AlreadyRegisteredException e) {
			// Allready loaded			
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			e.printStackTrace();
		}
	}
	
// Below is an attempt at blocking portals being made by people who could not build the 2nd side of the portal.
// It fails to place the player into the hashmap fast enough that the PlayerPortalEvent can be cancelled.
// Players who do have their CreateEvent cancelled end up suffocating in the dirt of their destination world.
//	
//	@EventHandler(priority = EventPriority.LOWEST)
//	public void onPortalCreate(PortalCreateEvent event) {
//		
//		TownyUniverse townyUniverse = TownyUniverse.getInstance();
//		Player player = null;
//		try {
//			if (!townyUniverse.getDataSource().getWorld(event.getWorld().getName()).isUsingTowny())
//				return;
//		} catch (NotRegisteredException ignored) {
//		}
//		
//		CreateReason reason = event.getReason();
//		if (!reason.equals(CreateReason.NETHER_PAIR))
//			return;
//		
//		if (event.getEntity().getType().equals(EntityType.PLAYER)) {
//			player = (Player) event.getEntity();
//		} else 
//			return;
//		
//		
//
//		Location loc = null;
//		boolean bBuild = false;
//		for (BlockState blocks : event.getBlocks()) {
//			loc = blocks.getBlock().getLocation();
//
//			bBuild = PlayerCacheUtil.getCachePermission(player, loc, Material.OBSIDIAN, ActionType.BUILD);
//			if (!bBuild) {
//				playersMap.add(player.getName());
//				event.setCancelled(true);				
//				System.out.println("Build Test for portal Failed.");
//				break;
//			}
//		}
//	}
//	
//	@EventHandler(priority = EventPriority.HIGHEST) 
//	public void onPlayerPortal(PlayerPortalEvent event) {
//		
//		if (!event.getCause().equals(TeleportCause.NETHER_PORTAL))
//			return;
//		
//		System.out.println("portalMap empty" + playersMap.isEmpty() );
//		if (playersMap.contains(event.getPlayer().getName())) {
//			event.setCancelled(true);
//			playersMap.remove(event.getPlayer().getName());
//			System.out.println("event canceled");
//		}
//	}
}
