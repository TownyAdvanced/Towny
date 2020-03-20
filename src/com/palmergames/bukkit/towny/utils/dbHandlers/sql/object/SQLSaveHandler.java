package com.palmergames.bukkit.towny.utils.dbHandlers.sql.object;

public interface SQLSaveHandler<T> {
	SQLData<T> save(T object);
}
