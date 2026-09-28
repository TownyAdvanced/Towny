package com.palmergames.bukkit.towny.db;

/**
 * A record which adds database-type-specific context to object saving.
 */
public record SerializationContext(boolean includeNameLines) {}
