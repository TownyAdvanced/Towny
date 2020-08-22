package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;

/**
 * Allows objects to contain townblocks to be accessed/manipulated.
 * 
 * <p>All returned townblocks are unmodifiable views of the implemented 
 * collection. As a result, the conforming class must provide delegation
 * for actions which mutate the backing collection.</p>
 * 
 * @author EdgarL
 * @author Shade
 * @author Suneet Tipirneni (Siris)
 * 
 * @see Town
 * @see Resident
 * @see PlotGroup
 */
public interface TownBlockOwner {
	/**
	 * Gets the unmodifiable collection of townblocks.
	 * 
	 * @return The townblocks this object contains.
	 */
	@NotNull Collection<TownBlock> getTownBlocks();

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
	 * Adds a townblock to the collection of existing townblocks.
	 *
	 * @param townBlock The townblock to add.
	 * @return true if adding the townblock was successful, false if the element was already in the
	 * collection or any other error occurred.
	 */
	boolean addTownBlock(@NotNull TownBlock townBlock);

	/**
	 * Removes townblock from the collection of existing townblocks.
	 * 
	 * @param townBlock The townblock to remove.
	 * @return true if removing the townblock was successful, false if the element was not already in the
	 * collection or any other error occurred.
	 */
	boolean removeTownBlock(@NotNull TownBlock townBlock);
}
