package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides the spawn location and access to setter for spawn locations.
 */
public interface SpawnLocation {
	/**
	 * Get the spawn location of this object.
	 * @return The spawn location
	 * 
	 * @throws TownyException If no location is found.
	 */
	@NotNull
	Location getSpawn() throws TownyException;

	/**
	 * Checks whether a spawn point exists.
	 * 
	 * @return A boolean indicating if the spawn exists.
	 */
	default boolean hasSpawn() {
		return getSpawnOrNull() != null;
	}

	/**
	 * Sets the spawn point of this object.
	 * @param spawn The Location of the new spawn.
	 *                 
	 * @throws TownyException If the spawn could not be set.
	 */
	void setSpawn(@Nullable Location spawn) throws TownyException;
	
	@Nullable
	default Location getSpawnOrNull() {
		try {
			return getSpawn();
		} catch (TownyException e) {
			return null;
		}
	}
}
