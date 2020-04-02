package com.palmergames.bukkit.towny.database.dbHandlers.object;

import com.palmergames.bukkit.towny.database.dbHandlers.sql.object.SQLData;

public interface SaveHandler<T> {
	String getFileString(SaveContext context,T obj);
	SQLData getSQL(SaveContext context, T obj);
}
