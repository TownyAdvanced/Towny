package com.palmergames.bukkit.towny.object;

import java.util.UUID;

/**
 * All classes that implement this interface
 * are uniquely identifiable by a UUID.
 */
public interface Identifiable {
	
	UUID getUUID();

	/**
	 * This should only be used by internal loading methods!
	 * @param uuid the UUID to set.
	 */
	void setUUID(UUID uuid);
	
}
