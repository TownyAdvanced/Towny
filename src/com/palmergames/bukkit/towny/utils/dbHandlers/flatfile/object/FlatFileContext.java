package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

import java.lang.reflect.Type;
import java.util.HashMap;

public abstract class FlatFileContext {
	private HashMap<Type, Handler<?>> handlers;

	public FlatFileContext(HashMap<Type, Handler<?>> handlers) {
		this.handlers = handlers;
	}

	<T> FlatFileLoadHandler<T> getHandlerForType(Type type) {
		
		Handler<?> handler = handlers.get(type);
		
		if (!(handler instanceof FlatFileLoadHandler)) {
			throw new UnsupportedOperationException("Handler for type: " + type + " does not implement FlatFileSaveHandler.");
		}
		
		return (FlatFileLoadHandler<T>) handler;
	}
	
}
