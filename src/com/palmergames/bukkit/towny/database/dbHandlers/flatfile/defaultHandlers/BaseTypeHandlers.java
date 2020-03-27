package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

public class BaseTypeHandlers {
	public static final SerializationHandler<Integer> INTEGER_HANDLER = new SerializationHandler<Integer>() {
		@Override
		public Integer loadString(LoadContext context, String str) {
			return null;
		}

		@Override
		public Integer loadSQL(Object result) {
			return null;
		}

		@Override
		public String getFileString(SaveContext context, Integer obj) {
			return null;
		}

		@Override
		public SQLData<Integer> getSQL(SaveContext context, Integer obj) {
			return null;
		}
	};
}
