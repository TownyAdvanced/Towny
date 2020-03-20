package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileSaveContext;

import java.util.List;

public class TownBlockListHandler implements FlatFileDatabaseHandler<List<TownBlock>> {
	@Override
	public List<TownBlock> load(FlatFileLoadContext context, String str) {
		
		return null;
	}

	@Override
	public String save(FlatFileSaveContext context, List<TownBlock> object) {
		return null;
	}
}
