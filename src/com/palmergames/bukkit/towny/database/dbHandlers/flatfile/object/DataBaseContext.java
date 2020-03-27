package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object;

public abstract class DataBaseContext {
	SerializationHandler<?> handler;

	public DataBaseContext(SerializationHandler<?> handler) {
		this.handler = handler;
	}
}
