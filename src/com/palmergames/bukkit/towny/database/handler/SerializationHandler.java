package com.palmergames.bukkit.towny.database.handler;

/**
 * Used on for namespacing.
 * @param <T> The type parameter.
 */
public interface SerializationHandler<T> extends LoadHandler<T>, SaveHandler<T> {}
