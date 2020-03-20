package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadHandler;

public class NationFlatFileHandler implements FlatFileLoadHandler<Nation> {
	@Override
	public Nation load(FlatFileLoadContext context, String str) {
		return TownyUniverse.getInstance().getNationsMap().get(str);
	}
}
