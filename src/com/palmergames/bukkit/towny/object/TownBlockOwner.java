package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;

import java.util.ArrayList;
import java.util.List;

public class TownBlockOwner extends TownyObject {

	protected List<TownBlock> townBlocks = new ArrayList<>();
	protected TownyPermission permissions = new TownyPermission();
	
	protected TownBlockOwner(String name) {
		super(name);
	}
	
	public void setTownblocks(List<TownBlock> townblocks) {

		this.townBlocks = townblocks;
	}

	public List<TownBlock> getTownBlocks() {

		return townBlocks;
	}

	public boolean hasTownBlock(TownBlock townBlock) {

		return townBlocks.contains(townBlock);
	}

	public void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException {

		if (hasTownBlock(townBlock))
			throw new AlreadyRegisteredException();
		else
			townBlocks.add(townBlock);
	}

	public void removeTownBlock(TownBlock townBlock) throws NotRegisteredException {

		if (!hasTownBlock(townBlock))
			throw new NotRegisteredException();
		else
			townBlocks.remove(townBlock);
	}

	public void setPermissions(String line) {

		//permissions.reset(); not needed, already done in permissions.load()
		permissions.load(line);
	}

	public TownyPermission getPermissions() {

		return permissions;
	}
}
