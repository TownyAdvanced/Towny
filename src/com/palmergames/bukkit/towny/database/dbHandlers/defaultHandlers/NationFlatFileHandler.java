package com.palmergames.bukkit.towny.database.dbHandlers.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadHandler;

public class NationFlatFileHandler implements LoadHandler<Nation> {
	@Override
	public Nation loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getNationsMap().get(str);
	}

	@Override
	public Nation loadSQL(LoadContext context, Object result) {
		return null;
	}
}
