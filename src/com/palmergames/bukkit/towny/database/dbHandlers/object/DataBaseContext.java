package com.palmergames.bukkit.towny.database.dbHandlers.object;

public abstract class DataBaseContext {
	SerializationHandler<?> handler;

	public DataBaseContext(SerializationHandler<?> handler) {
		this.handler = handler;
	}
}
