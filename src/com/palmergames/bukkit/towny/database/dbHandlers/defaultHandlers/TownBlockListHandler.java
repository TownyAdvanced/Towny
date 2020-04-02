package com.palmergames.bukkit.towny.database.dbHandlers.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;

import java.util.List;

public class TownBlockListHandler implements SerializationHandler<List<TownBlock>> {
	@Override
	public List<TownBlock> loadString(LoadContext context, String str) {
		return null;
	}

	@Override
	public List<TownBlock> loadSQL(Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, List<TownBlock> obj) {
		return null;
	}

	@Override
	public SQLData<List<TownBlock>> getSQL(SaveContext context, List<TownBlock> obj) {
		return null;
	}
}
