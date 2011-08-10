package com.palmergames.bukkit.towny.event;

import org.bukkit.event.world.WorldListener;
import org.bukkit.event.world.WorldLoadEvent;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownyWorldListener extends WorldListener {
	private final Towny plugin;

	public TownyWorldListener(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public void onWorldLoad(WorldLoadEvent event) {
		String worldName = event.getWorld().getName();
		try {
			plugin.getTownyUniverse().newWorld(worldName);
			TownyWorld world = plugin.getTownyUniverse().getWorld(worldName);
			if (world == null)
				plugin.sendErrorMsg("Could not create data for " + worldName);
			else
				if (!plugin.getTownyUniverse().getDataSource().loadWorld(world))
					// First time world has been noticed
					plugin.getTownyUniverse().getDataSource().saveWorld(world); 
		} catch (AlreadyRegisteredException e) {
			// Allready loaded			
		} catch (NotRegisteredException e) {
			plugin.sendErrorMsg("Could not create data for " + worldName);
			e.printStackTrace();
		}
	}
}
