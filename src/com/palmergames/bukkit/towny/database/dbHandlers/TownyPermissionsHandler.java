package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.object.TownyPermission;

public class TownyPermissionsHandler implements LoadHandler<TownyPermission> {
	
	@Override
	public TownyPermission loadString(String str) {
		TownyPermission townyPermission = new TownyPermission();
		townyPermission.load(str);
		return townyPermission;
	}
}
