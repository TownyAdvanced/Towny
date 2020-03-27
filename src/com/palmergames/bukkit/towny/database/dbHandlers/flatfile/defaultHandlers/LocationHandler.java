package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationHandler implements SerializationHandler<Location> {
	@Override
	public Location loadString(LoadContext context, String str) {
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
	public Location loadSQL(Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, Location obj) {
		return null;
	}

	@Override
	public SQLData<Location> getSQL(SaveContext context, Location obj) {
		return null;
	}
}
