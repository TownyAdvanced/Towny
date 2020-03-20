package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

public interface FlatFileSaveHandler<T> extends Handler<T> {
	String save(FlatFileSaveContext context, T object);
}
