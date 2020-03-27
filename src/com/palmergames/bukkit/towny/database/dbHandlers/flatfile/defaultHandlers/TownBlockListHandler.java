package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.FlatFileSaveContext;

import java.util.List;

public class TownBlockListHandler implements SerializationHandler<List<TownBlock>> {
	@Override
	public List<TownBlock> loadString(LoadContext context, String str) {
		
		return null;
	}

	@Override
	public String getString(FlatFileSaveContext context, List<TownBlock> object) {
		return null;
	}
}
