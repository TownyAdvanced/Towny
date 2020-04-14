package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.sql.JDBCType;
import java.util.Arrays;

public class LocationHandler implements SerializationHandler<Location> {
	@Override
	public Location loadString(LoadContext context, String str) {
		TownyMessaging.sendErrorMsg(str);
		String[] tokens = str.split(",");
		
		TownyMessaging.sendErrorMsg(Arrays.toString(tokens));
		
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
	public Location loadSQL(LoadContext context, Object result) {
		String data = (String)result;
		return context.fromFileString(data, Location.class);
	}

	@Override
	public String getFileString(SaveContext context, Location obj) {
		World world = obj.getWorld();
		double x = obj.getX();
		double y = obj.getY();
		double z = obj.getZ();
		double pitch = obj.getPitch();
		double yaw = obj.getYaw();
		
		assert world != null;
		
		return world.getName() + "," + x + "," + y + "," + z + "," + pitch + "," + yaw;
	}

	@Override
	public SQLData getSQL(SaveContext context, Location obj) {
		String saveData = context.toFileString(obj, Location.class);
		return new SQLData(saveData, JDBCType.VARCHAR);
	}
}
