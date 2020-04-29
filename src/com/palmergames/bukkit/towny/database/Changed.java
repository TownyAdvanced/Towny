package com.palmergames.bukkit.towny.database;

/**
 * An interface for keeping track of changes to
 * objects, this allows the save phase to *efficiently*
 * save objects without redundancy.
 */
public interface Changed {
	/**
	 * Whether the object has been modified since it 
	 * was loaded.
	 * 
	 * @return true if changed false otherwise.
	 */
	boolean isChanged();

	/**
	 * Sets if the object is in a modified
	 * state since it's load.
	 * 
	 * @param changed The state of change.
	 */
	void setChanged(boolean changed);
}
