package com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object;

/**
 * Used on for namespacing.
 * @param <T> The type parameter.
 */
public interface SerializationHandler<T> extends LoadHandler<T>, SaveHandler<T> {}
