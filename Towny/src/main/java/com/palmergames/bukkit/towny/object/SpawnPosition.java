package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.Nullable;

public interface SpawnPosition {
	/**
	 * Sets the spawn position of this object.
	 * @param position The position of the new spawn.
	 */
	void spawnPosition(@Nullable Position position);

	/**
	 * Gets the spawn location of this object.
	 * @return The spawn location or {@code null} if none has been set.
	 */
	@Nullable
	Position spawnPosition();
}
