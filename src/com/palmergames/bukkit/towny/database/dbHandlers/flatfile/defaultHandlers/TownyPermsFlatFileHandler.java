package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;

public class TownyPermsFlatFileHandler implements LoadHandler<TownyPermission> {
	@Override
	public TownyPermission loadString(LoadContext context, String str) {
		TownyPermission townyPermission = new TownyPermission();
		townyPermission.load(str);

		return townyPermission;
	}

	@Override
	public TownyPermission loadSQL(Object result) {
		return null;
	}
}
