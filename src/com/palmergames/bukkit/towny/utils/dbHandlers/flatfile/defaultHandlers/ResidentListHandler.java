package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.SerializationHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileSaveContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.sql.object.SQLData;

import java.util.ArrayList;
import java.util.List;

public class ResidentListHandler implements LoadHandler<List<Resident>> {
	
	@Override
	public List<Resident> loadString(LoadContext context, String str) {
		List<Resident> residents = new ArrayList<>();
		String[] residentNames = str.split(",");
		
		for (String residentName : residentNames) {
			Resident loadedResident = context.
			residents.add(loadedResident);
		}
		
		return residents;
	}

	@Override
	public List<Resident> loadSQL(Object result) {
		return null;
	}

	@Override
	public String getString(FlatFileSaveContext context, List<Resident> object) {
		
		// Save residents in format: "bob, mary, sue,"
		StringBuilder saveFormat = new StringBuilder();
		for (Resident resident : object) {
			saveFormat.append(resident.getName());
		}
		
		return saveFormat.toString();
	}

	@Override
	public SQLData<List<Resident>> getSQL(List<Resident> object) {
		return null;
	}
}
