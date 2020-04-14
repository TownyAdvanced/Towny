package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.SaveContext;
import com.palmergames.bukkit.towny.database.handler.SQLData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.database.handler.SerializationHandler;
import com.palmergames.bukkit.towny.database.handler.LoadContext;

import java.util.ArrayList;
import java.util.List;

public class TownBlockListHandler implements LoadHandler<List<TownBlock>> {
	@Override
	public List<TownBlock> loadString(LoadContext context, String str) {
		String strCopy;
		strCopy =  str.replace("]", "");
		strCopy = str.replace("[", "");
		String[] townBlockStrs = strCopy.split(",");
		
		List<TownBlock> townBlocks = new ArrayList<>();
		
		for (String townblock : townBlockStrs) {
			
			TownBlock tb = context.fromFileString(townblock, TownBlock.class);
			
			if (tb == null) {
				continue;
			}
			
			townBlocks.add(tb);
		}
		
		return townBlocks;
	}

	@Override
	public List<TownBlock> loadSQL(LoadContext context, Object result) {
		return null;
	}
}
