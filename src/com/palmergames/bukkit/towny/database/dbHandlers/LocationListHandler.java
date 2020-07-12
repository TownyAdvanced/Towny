package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.List;

public class LocationListHandler implements LoadHandler<List<Location>> {

	@Override
	public List<Location> loadString(LoadContext context, String str) {
		
		if (str.equals("[]")) {
			return new ArrayList<>();
		}
		
		String[] args = str.split(";");
		List<Location> retVal = new ArrayList<>();
		for (String arg : args) {
			retVal.add(context.fromStoredString(arg, Location.class));
		}
		
		return retVal;
	}
}
