package com.palmergames.bukkit.towny.event;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;
import org.bukkit.event.world.WorldInitEvent;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownyWorldListener extends WorldListener {
	private final Towny plugin;

	public TownyWorldListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onWorldLoad(WorldLoadEvent event) {
		newWorld(event.getWorld().getName());
	}
	
	@Override
	public void onWorldInit(WorldInitEvent event) {
		newWorld(event.getWorld().getName());
		
	}

	private void newWorld(String worldName) {
		
		//String worldName = event.getWorld().getName();
		try {
			plugin.getTownyUniverse().newWorld(worldName);
			TownyWorld world = TownyUniverse.getWorld(worldName);
			if (world == null)
				TownyMessaging.sendErrorMsg("Could not create data for " + worldName);
			else {
				if (!TownyUniverse.getDataSource().loadWorld(world)) {
					// First time world has been noticed
					TownyUniverse.getDataSource().saveWorld(world);
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
