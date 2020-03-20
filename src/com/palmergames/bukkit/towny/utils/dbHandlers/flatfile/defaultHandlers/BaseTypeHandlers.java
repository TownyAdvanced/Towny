package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileDatabaseHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.FlatFileLoadContext;

public class BaseTypeHandlers {
	public static final FlatFileDatabaseHandler<Integer> INTEGER_HANDLER = new FlatFileDatabaseHandler<Integer>() {
		@Override
		public Integer load(FlatFileLoadContext context, String str) {
			return Integer.parseInt(str);
		}

		@Override
		public String save(Integer object) {
			return null;
		}
	};
}
