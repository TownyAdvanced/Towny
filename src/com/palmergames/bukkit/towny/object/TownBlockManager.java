package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

public interface TownBlockManager extends TownBlockHolder, Permissible {
	/**
	 * Adds a townblock to the list of existing townblocks.
	 *
	 * @param townBlock The townblock to add.
	 * @throws AlreadyRegisteredException When the townblock is already in the list.
	 */
	void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException;

	/**
	 * Removes townblock from the list of existing townblocks.
	 *
	 * @param townBlock The townblock to remove.
	 * @throws NotRegisteredException Thrown when the townblock given is not in the list.
	 */
	void removeTownBlock(TownBlock townBlock) throws NotRegisteredException;
}
