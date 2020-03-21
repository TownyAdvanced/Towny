package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

public abstract class DataBaseContext {
	SerializationHandler<?> handler;

	public DataBaseContext(SerializationHandler<?> handler) {
		this.handler = handler;
	}
}
