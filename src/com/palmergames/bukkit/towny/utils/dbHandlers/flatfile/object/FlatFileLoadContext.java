package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

import java.lang.reflect.Type;
import java.util.HashMap;

public class FlatFileLoadContext extends FlatFileContext {
	public FlatFileLoadContext(HashMap<Type, Handler<?>> handlers) {
		super(handlers);
	}

	public <T> T load(String str, Class<T> type) {
		FlatFileLoadHandler<T> typeHandler = getHandlerForType(type);
		return typeHandler.load(this, str);
	}
}
