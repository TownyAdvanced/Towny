package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile.object;

/**
 * Used on for namespacing.
 * @param <T> The type parameter.
 */
public interface SerializationHandler<T> extends LoadHandler<T>, SaveHandler<T> {}
