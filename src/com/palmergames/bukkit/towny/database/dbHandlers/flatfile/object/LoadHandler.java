package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object;

public interface LoadHandler<T>  {
	T loadString(LoadContext context, String str);
	T loadSQL(Object result);
}
