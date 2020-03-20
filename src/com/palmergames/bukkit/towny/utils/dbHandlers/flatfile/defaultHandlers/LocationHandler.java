package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileSaveContext;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationHandler implements FlatFileDatabaseHandler<Location> {
	@Override
	public Location load(FlatFileLoadContext context, String str) {
		String[] tokens = str.split(",");
		
		World world;
		double x;
		double y;
		double z;
		float pitch;
		float yaw;
		
		try {
			world = Bukkit.getWorld(tokens[0]);
			x = Double.parseDouble(tokens[1]);
			y = Double.parseDouble(tokens[2]);
			z = Double.parseDouble(tokens[3]);
			pitch = Float.parseFloat(tokens[4]);
			yaw = Float.parseFloat(tokens[5]);
			
			return new Location(world, x, y, z, pitch, yaw);
		} catch (NumberFormatException e) {
			return null;
		}

	}

	@Override
	public String save(FlatFileSaveContext context, Location object) {
		return object.getWorld().getName() + "," + object.getX() + "," + object.getY() + object.getZ() + "," + object.getPitch() + "," + object.getYaw();
	}
}
