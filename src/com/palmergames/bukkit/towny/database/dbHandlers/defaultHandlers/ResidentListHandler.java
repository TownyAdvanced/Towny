package com.palmergames.bukkit.towny.database.dbHandlers.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;

import java.util.ArrayList;
import java.util.List;

public class ResidentListHandler implements SerializationHandler<List<Resident>> {
	
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
	public List<Resident> loadSQL(LoadContext context, Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, List<Resident> obj) {
		
		StringBuilder retVal = new StringBuilder();
		for (Resident resident : obj) {
			retVal.append(context.toFileString(resident, Resident.class)).append(",");
		}
		
		return retVal.toString();
	}

	@Override
	public SQLData getSQL(SaveContext context, List<Resident> obj) {
		return null;
	}
}
