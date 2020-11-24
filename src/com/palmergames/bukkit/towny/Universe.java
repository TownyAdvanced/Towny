package com.palmergames.bukkit.towny;

import org.jetbrains.annotations.NotNull;

public interface Universe {

	/**
	 * Clears the object maps.
	 */
	void clearAllObjects();

	boolean registerSubUniverse(@NotNull Universe subUniverse);
	boolean deregisterSubUniverse(@NotNull Universe name);
}
