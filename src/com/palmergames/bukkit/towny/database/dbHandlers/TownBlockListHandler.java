package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.LoadContext;

import java.util.List;

public class TownBlockListHandler implements SerializationHandler<List<TownBlock>> {
	@Override
	public List<TownBlock> loadString(LoadContext context, String str) {
		return null;
	}

	@Override
	public List<TownBlock> loadSQL(LoadContext context, Object result) {
		return null;
	}

	@Override
	public String getFileString(SaveContext context, List<TownBlock> obj) {
		return null;
	}

	@Override
	public SQLData getSQL(SaveContext context, List<TownBlock> obj) {
		return null;
	}
}
