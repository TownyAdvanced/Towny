package com.palmergames.bukkit.towny.database.dbHandlers.defaultHandlers;

import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SaveContext;
import com.palmergames.bukkit.towny.database.dbHandlers.object.SerializationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

import java.util.UUID;

public class BaseTypeHandlers {
	public static final LoadHandler<Integer> INTEGER_HANDLER = new LoadHandler<Integer>() {
		@Override
		public Integer loadString(LoadContext context, String str) {
			return Integer.parseInt(str);
		}

		@Override
		public Integer loadSQL(Object result) {
			return null;
		}
	};
	
	public static final LoadHandler<String> STRING_HANDLER = new LoadHandler<String>() {
		@Override
		public String loadString(LoadContext context, String str) {
			return str;
		}

		@Override
		public String loadSQL(Object result) {
			return null;
		}
	};
	
	public static final LoadHandler<UUID> UUID_HANDLER = new LoadHandler<UUID>() {
		@Override
		public UUID loadString(LoadContext context, String str) {
			return UUID.fromString(str);
		}

		@Override
		public UUID loadSQL(Object result) {
			return null;
		}
	};
}
