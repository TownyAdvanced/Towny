package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownyWorldHandler implements LoadHandler<TownyWorld> {

	@Override
	public TownyWorld loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getWorldMap().get(str);
	}

	@Override
	public TownyWorld loadSQL(LoadContext context, Object result) {
		return null;
	}
}
