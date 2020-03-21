package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.defaultHandlers;

import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.SerializationHandler;
import com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object.LoadContext;

public class BaseTypeHandlers {
	public static final SerializationHandler<Integer> INTEGER_HANDLER = new SerializationHandler<Integer>() {
		@Override
		public Integer loadString(LoadContext context, String str) {
			return Integer.parseInt(str);
		}

		@Override
		public String save(Integer object) {
			return null;
		}
	};
}
