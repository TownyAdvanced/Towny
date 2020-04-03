package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;

public class TownyPermsFlatFileHandler implements LoadHandler<TownyPermission> {
	@Override
	public TownyPermission loadString(LoadContext context, String str) {
		TownyPermission townyPermission = new TownyPermission();
		townyPermission.load(str);

		return townyPermission;
	}

	@Override
	public TownyPermission loadSQL(LoadContext context, Object result) {
		return null;
	}
}
