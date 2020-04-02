package com.palmergames.bukkit.towny.database.dbHandlers.sql.object;

public interface SQLSaveHandler<T> {
	SQLData save(T object);
}
