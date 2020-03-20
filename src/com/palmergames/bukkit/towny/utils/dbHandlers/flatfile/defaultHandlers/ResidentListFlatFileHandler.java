package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileSaveContext;

import java.util.ArrayList;
import java.util.List;

public class ResidentListFlatFileHandler implements FlatFileDatabaseHandler<List<Resident>> {
	@Override
	public List<Resident> load(FlatFileLoadContext context, String str) {
		List<Resident> residents = new ArrayList<>();
		String[] residentNames = str.split(",");
		
		for (String residentName : residentNames) {
			Resident loadedResident = context.load(residentName, Resident.class);
			residents.add(loadedResident);
		}
		
		return residents;
	}

	@Override
	public String save(FlatFileSaveContext context, List<Resident> object) {
		
		// Save residents in format: "bob, mary, sue,"
		StringBuilder saveFormat = new StringBuilder();
		for (Resident resident : object) {
			saveFormat.append(resident.getName());
		}
		
		return saveFormat.toString();
	}
}
