package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class TownyWorldHandler implements LoadHandler<TownyWorld> {

	@Override
	public TownyWorld loadString(LoadContext context, String str) {
		TownyMessaging.sendErrorMsg(TownyUniverse.getInstance().getWorlds().toString());
		try {
			return TownyUniverse.getInstance().getWorld(str);
		} catch (NotRegisteredException e) {
			return null;
		}
	}

	@Override
	public TownyWorld loadSQL(LoadContext context, Object result) {
		return null;
	}
}
