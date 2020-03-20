package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadHandler;

public class ResidentFlatFileHandler implements FlatFileLoadHandler<Resident> {
	@Override
	public Resident load(FlatFileLoadContext context, String str) {
		return TownyUniverse.getInstance().getResidentMap().get(str);
	}
}
