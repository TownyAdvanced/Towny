package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;

public class NationFlatFileHandler implements LoadHandler<Nation> {
	@Override
	public Nation loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getNationsMap().get(str);
	}

	@Override
	public Nation loadSQL(Object result) {
		return null;
	}
}
