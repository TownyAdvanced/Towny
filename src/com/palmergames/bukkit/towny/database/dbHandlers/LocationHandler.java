package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

public class LocationHandler implements SerializationHandler<Location> {
	@Override
	public Location loadString(String str) {

		String elements = StringUtils.substringBetween(str, "{", "}");
		String[] tokens = elements.split(";");
		
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
			e.printStackTrace();
			return null;
		}

	}

	@Override
	public String toStoredString(Location obj) {
		World world = obj.getWorld();
		double x = obj.getX();
		double y = obj.getY();
		double z = obj.getZ();
		double pitch = obj.getPitch();
		double yaw = obj.getYaw();
		
		assert world != null;
		
		return "{" + world.getName() + ";" + x + ";" + y + ";" + z + ";" + pitch + ";" + yaw + "}";
	}
}
