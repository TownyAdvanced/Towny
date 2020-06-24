package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Used for objects that are identified by a UUID object.
 */
public interface Identifiable {
	/**
	 * Gets the unique identifier for this object.
	 * 
	 * @return The unique ID as a UUID.
	 */
	@NotNull
	UUID getUniqueIdentifier();
}
