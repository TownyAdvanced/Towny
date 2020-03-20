package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

public interface FlatFileLoadHandler<T> extends Handler<T> {
	T load(FlatFileLoadContext context, String str);
}
