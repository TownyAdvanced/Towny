package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

import java.util.ArrayList;
import java.util.List;

public interface TownBlockOwner extends Permissible {
	
	void setTownblocks(List<TownBlock> townBlocks);

	List<TownBlock> getTownBlocks();

	boolean hasTownBlock(TownBlock townBlock);

	void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException;

	void removeTownBlock(TownBlock townBlock) throws NotRegisteredException;
}
