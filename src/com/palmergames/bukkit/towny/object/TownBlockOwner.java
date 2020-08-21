package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Allows objects to contain townblocks to be accessed/manipulated. 
 * 
 * @author EdgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 */
public interface TownBlockOwner {
	/**
	 * Gets the unmodifiable collection of townblocks.
	 * 
	 * @return The townblocks this object contains.
	 */
	@NotNull
	Collection<TownBlock> getTownBlocks();

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
