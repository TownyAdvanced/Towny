package com.palmergames.bukkit.towny.database.dbHandlers.object;

public interface LoadHandler<T>  {
	T loadString(LoadContext context, String str);
	T loadSQL(LoadContext context, Object result);
}
