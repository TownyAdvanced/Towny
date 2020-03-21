package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadHandler;

public class ResidentHandler implements LoadHandler<Resident> {

	@Override
	public Resident loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getResidentMap().get(str);
	}

	@Override
	public Resident loadSQL(Object result) {
		return null;
	}
}
