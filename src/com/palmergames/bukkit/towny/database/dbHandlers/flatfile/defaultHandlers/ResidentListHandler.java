package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;

import java.util.ArrayList;
import java.util.List;

public class ResidentListHandler implements LoadHandler<List<Resident>> {
	
	@Override
	public List<Resident> loadString(LoadContext context, String str) {
		List<Resident> residents = new ArrayList<>();
		String[] residentNames = str.split(",");
		
		for (String residentName : residentNames) {
			Resident loadedResident = context.fromFileString(residentName, Resident.class);
			residents.add(loadedResident);
		}
		
		return residents;
	}

	@Override
	public List<Resident> loadSQL(Object result) {
		return null;
	}
}
