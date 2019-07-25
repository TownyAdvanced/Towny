package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyWorld;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.WorldInitEvent;
import org.bukkit.event.world.WorldLoadEvent;

public class TownyWorldListener implements Listener {

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldLoad(WorldLoadEvent event) {

		newWorld(event.getWorld().getName());
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onWorldInit(WorldInitEvent event) {

		newWorld(event.getWorld().getName());

	}

	private void newWorld(String worldName) {

		//String worldName = event.getWorld().getName();
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			townyUniverse.getDataSource().newWorld(worldName);
			TownyWorld world = townyUniverse.getDataSource().getWorld(worldName);
			if (world == null)
				TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			else {
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
}
