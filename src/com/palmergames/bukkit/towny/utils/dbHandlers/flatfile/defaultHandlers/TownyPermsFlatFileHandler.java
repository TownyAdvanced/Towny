package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadHandler;

public class TownyPermsFlatFileHandler implements FlatFileLoadHandler<TownyPermission> {
	@Override
	public TownyPermission load(FlatFileLoadContext context, String str) {
		TownyPermission townyPermission = new TownyPermission();
		townyPermission.load(str);

		return townyPermission;
	}
}
