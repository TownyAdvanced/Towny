package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

public class TypeAdapters {
	public static final class INTEGER_ADAPTER implements SaveHandler<Integer> {

		@Override
		public String getFileString(SaveContext context, Integer obj) {
			return null;
		}

		@Override
		public SQLData getSQL(SaveContext context, Integer obj) {
			return null;
		}
	}
}
