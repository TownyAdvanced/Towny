package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile;

import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.SaveContext;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.SaveHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.sql.object.SQLData;

public class TypeAdapters {
	public static final class INTEGER_ADAPTER implements SaveHandler<Integer> {

		@Override
		public String getFileString(SaveContext context, Integer obj) {
			return null;
		}

		@Override
		public SQLData<Integer> getSQL(SaveContext context, Integer obj) {
			return null;
		}
	}
}
