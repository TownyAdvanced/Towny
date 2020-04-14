package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationListHandler implements LoadHandler<List<Location>> {

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
}
