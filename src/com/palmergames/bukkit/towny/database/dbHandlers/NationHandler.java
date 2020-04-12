package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;

public class NationHandler implements LoadHandler<Nation> {
	@Override
	public Nation loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getNationsMap().get(str);
	}

	@Override
	public Nation loadSQL(LoadContext context, Object result) {
		return null;
	}
}
