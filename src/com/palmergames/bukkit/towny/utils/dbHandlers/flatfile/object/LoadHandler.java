package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

public interface LoadHandler<T>  {
	T loadString(LoadContext context, String str);
	T loadSQL(Object result);
}
