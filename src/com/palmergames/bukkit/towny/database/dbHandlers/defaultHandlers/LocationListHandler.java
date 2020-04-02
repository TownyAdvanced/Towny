package com.palmergames.bukkit.towny.database.dbHandlers.defaultHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationListHandler implements SerializationHandler<List<Location>> {

	@Override
	public List<Location> loadString(LoadContext context, String str) {
		TownyMessaging.sendErrorMsg(str);
		String[] args = str.split(";");
		List<Location> retVal = new ArrayList<>();
		for (String arg : args) {
			retVal.add(context.fromFileString(arg, Location.class));
		}
		
		return retVal;
	}

	@Override
	public List<Location> loadSQL(LoadContext context, Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, List<Location> obj) {
		StringBuilder retVal = new StringBuilder();
		for (Location location : obj) {
			retVal.append(context.toFileString(location, Location.class));
		}
		
		return retVal.toString();
	}

	@Override
	public SQLData getSQL(SaveContext context, List<Location> obj) {
		return null;
	}
}
