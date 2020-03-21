package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadHandler;

public class NationFlatFileHandler implements LoadHandler<Nation> {
	@Override
	public Nation loadString(LoadContext context, String str) {
		return TownyUniverse.getInstance().getNationsMap().get(str);
	}
}
