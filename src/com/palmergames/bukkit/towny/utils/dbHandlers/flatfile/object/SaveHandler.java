package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

import com.palmergames.bukkit.towny.utils.dbHandlers.sql.object.SQLData;

public interface SaveHandler<T> {
	String getFileString(SaveContext context,T obj);
	SQLData<T> getSQL(SaveContext context, T obj);
}
