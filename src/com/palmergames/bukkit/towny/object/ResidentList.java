package com.palmergames.bukkit.towny.object;

import java.util.Iterator;
import java.util.List;

public interface ResidentList extends Iterable<TownyObject> {
	/**
	 * Returns an unmodifiable list of residents.
	 * 
	 * @return The list of residents in this object.
	 */
	List<Resident> getResidents();

	/**
	 * Whether a given object contains a resident.
	 * 
	 * @param name The name of the resident.
	 * @return {@code true} if found, {@code false} otherwise.
	 */
	boolean hasResident(String name);

	/**
	 * Gets the unmodifiable list of outlaws.
	 * 
	 * @return The list of outlaws in this object.
	 */
	List<Resident> getOutlaws();
}
