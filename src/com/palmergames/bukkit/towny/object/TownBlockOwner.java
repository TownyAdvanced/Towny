package com.palmergames.bukkit.towny.object;

import java.util.Collection;

/**
 * Allows objects to contain townblocks to be accessed/manipulated. 
 * 
 * @author EdgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public interface TownBlockOwner extends Permissible, Nameable {
	/**
	 * Gets the unmodifiable collection of townblocks.
	 * 
	 * @return The townblocks this object contains.
	 */
	Collection<TownBlock> getTownBlocks();

	/**
	 * Checks whether the object has the given townblock or not.
	 * 
	 * @deprecated As of version 0.96.2.11 and will be removed in a future release,
	 * use {@link TownBlockOwner#getTownBlocks()} in conjunction
	 * with {@link Collection#contains(Object)} instead.
	 *
	 * @param townBlock The townblock to check for.
	 * @return A boolean indicating if it was found or not.
	 */
	@Deprecated
	default boolean hasTownBlock(TownBlock townBlock) {
		return getTownBlocks().contains(townBlock);
	}

	/**
	 * Adds a townblock to the list of existing townblocks.
	 *
	 * @param townBlock The townblock to add.
	 * @return true if adding the townblock was successful, false if the element was already in the
	 * collection or any other error occurred.
	 */
	boolean addTownBlock(TownBlock townBlock);

	/**
	 * Removes townblock from the list of existing townblocks.
	 * 
	 * @param townBlock The townblock to remove.
	 * @return true if removing the townblock was successful, false if the element was not already in the
	 * collection or any other error occurred.
	 */
	boolean removeTownBlock(TownBlock townBlock);
}
