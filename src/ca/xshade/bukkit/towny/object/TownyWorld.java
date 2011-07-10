package ca.xshade.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;

public class TownyWorld extends TownyObject {
	private List<Town> towns = new ArrayList<Town>();
	private boolean isClaimable = true, isPvP = true, usingDefault = true, isUsingTowny = true;
	private List<Integer> unclaimedZoneIgnoreIds = null;
	private Boolean unclaimedZoneBuild = null, unclaimedZoneDestroy = null, unclaimedZoneSwitch = null, unclaimedZoneItemUse = null;
	private String unclaimedZoneName = null;
	private Hashtable<Coord, TownBlock> townBlocks = new Hashtable<Coord, TownBlock>();

	// TODO: private List<TownBlock> adminTownBlocks = new
	// ArrayList<TownBlock>();

	public TownyWorld(String name) {
		setName(name);
	}

	public List<Town> getTowns() {
		return towns;
	}

	public boolean hasTown(String name) {
		for (Town town : towns)
			if (town.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public boolean hasTown(Town town) {
		return towns.contains(town);
	}

	public void addTown(Town town) throws AlreadyRegisteredException {
		if (hasTown(town))
			throw new AlreadyRegisteredException();
		else {
			towns.add(town);
			town.setWorld(this);
		}
	}

	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		TownBlock townBlock = townBlocks.get(coord);
		if (townBlock == null)
			throw new NotRegisteredException();
		else
			return townBlock;
	}

	public void newTownBlock(int x, int z) throws AlreadyRegisteredException {
		newTownBlock(new Coord(x, z));
	}

	public TownBlock newTownBlock(Coord key) throws AlreadyRegisteredException {
		if (hasTownBlock(key))
			throw new AlreadyRegisteredException();
		townBlocks.put(new Coord(key.getX(), key.getZ()), new TownBlock(key.getX(), key.getZ(), this));
		return townBlocks.get(new Coord(key.getX(), key.getZ()));
	}

	public boolean hasTownBlock(Coord key) {
		return townBlocks.containsKey(key);
	}

	public TownBlock getTownBlock(int x, int z) throws NotRegisteredException {
		return getTownBlock(new Coord(x, z));
	}
	
	public List<TownBlock> getTownBlocks(Town town) {
		List<TownBlock> out = new ArrayList<TownBlock>();
		for (TownBlock townBlock : town.getTownBlocks())
			if (townBlock.getWorld() == this)
				out.add(townBlock);
		return out;
	}

	public Collection<TownBlock> getTownBlocks() {
		return townBlocks.values();
	}

	public void removeTown(Town town) throws NotRegisteredException {
		if (!hasTown(town))
			throw new NotRegisteredException();
		else {
			towns.remove(town);
			try {
				town.setWorld(null);
			} catch (AlreadyRegisteredException e) {
			}
		}
	}

	public void removeTownBlock(TownBlock townBlock) {
		try {
			if (townBlock.hasResident())
				townBlock.getResident().removeTownBlock(townBlock);
		} catch (NotRegisteredException e) {
		}
		try {
			if (townBlock.hasTown())
				townBlock.getTown().removeTownBlock(townBlock);
		} catch (NotRegisteredException e) {
		}

		removeTownBlock(townBlock.getCoord());
	}
	
	public void removeTownBlocks(List<TownBlock> townBlocks) {
		for (TownBlock townBlock : new ArrayList<TownBlock>(townBlocks))
			removeTownBlock(townBlock);
	}

	public void removeTownBlock(Coord coord) {
		townBlocks.remove(coord);
	}
	
	@Override
	public List<String> getTreeString(int depth) {
		List<String> out = new ArrayList<String>();
		out.add(getTreeDepth(depth) + "World ("+getName()+")");
		out.add(getTreeDepth(depth+1) + "TownBlocks (" + getTownBlocks().size() + "): " /*+ getTownBlocks()*/);
		return out;
	}

	public void setPvP(boolean isPvP) {
		this.isPvP = isPvP;
	}

	public boolean isPvP() {
		return isPvP;
	}

	public void setClaimable(boolean isClaimable) {
		this.isClaimable = isClaimable;
	}

	public boolean isClaimable() {
		if (!isUsingTowny())
			return false;
		else
			return isClaimable;
	}

	public void setUsingDefault(boolean usingDefault) {
		this.usingDefault = usingDefault;
		if (usingDefault) {
			setUnclaimedZoneBuild(null);
			setUnclaimedZoneDestroy(null);
			setUnclaimedZoneSwitch(null);
			setUnclaimedZoneItemUse(null);
			setUnclaimedZoneIgnore(null);
			setUnclaimedZoneName(null);
		}
			
	}

	public boolean isUsingDefault() {
		return usingDefault;
	}
	
	public List<Integer> getUnclaimedZoneIgnoreIds() {
		if (unclaimedZoneIgnoreIds == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneIgnoreIds();
		else
			return unclaimedZoneIgnoreIds;
	}
	
	public boolean isUnclaimedZoneIgnoreId(int id) {
		return getUnclaimedZoneIgnoreIds().contains(id);
	}

	public void setUnclaimedZoneIgnore(List<Integer> unclaimedZoneIgnoreIds) {
		/*
		if (TownySettings.isFirstRun())
			this.unclaimedZoneIgnoreIds = TownySettings.getUnclaimedZoneIgnoreIds();
		else	
		*/	
			this.unclaimedZoneIgnoreIds = unclaimedZoneIgnoreIds;
	}

	public Boolean getUnclaimedZoneBuild() {
		if (unclaimedZoneBuild == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneBuildRights();
		else
			return unclaimedZoneBuild;
	}

	public void setUnclaimedZoneBuild(Boolean unclaimedZoneBuild) {
		this.unclaimedZoneBuild = unclaimedZoneBuild;
	}

	public Boolean getUnclaimedZoneDestroy() {
		if (unclaimedZoneDestroy == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneDestroyRights();
		else
			return unclaimedZoneDestroy;
	}

	public void setUnclaimedZoneDestroy(Boolean unclaimedZoneDestroy) {
		this.unclaimedZoneDestroy = unclaimedZoneDestroy;
	}

	public Boolean getUnclaimedZoneSwitch() {
		if (unclaimedZoneSwitch == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneSwitchRights();
		else
			return unclaimedZoneSwitch;
	}

	public void setUnclaimedZoneSwitch(Boolean unclaimedZoneSwitch) {
		this.unclaimedZoneSwitch = unclaimedZoneSwitch;
	}

	public String getUnclaimedZoneName() {
		if (unclaimedZoneName == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneName();
		else
			return unclaimedZoneName;
	}

	public void setUnclaimedZoneName(String unclaimedZoneName) {
		this.unclaimedZoneName = unclaimedZoneName;
	}

	public void setUsingTowny(boolean isUsingTowny) {
		this.isUsingTowny = isUsingTowny;
	}

	public boolean isUsingTowny() {
		return isUsingTowny;
	}

	public void setUnclaimedZoneItemUse(Boolean unclaimedZoneItemUse) {
		this.unclaimedZoneItemUse = unclaimedZoneItemUse;
	}

	public Boolean getUnclaimedZoneItemUse() {
		if (unclaimedZoneItemUse == null || isUsingDefault())
			return TownySettings.getUnclaimedZoneItemUseRights();
		else
			return unclaimedZoneItemUse;
	}
	
	/**
	 * Checks the distance from a another town's homeblock.
	 * 
	 * @param key
	 * @return the closest distance to another towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key) {
		double min = Integer.MAX_VALUE;
		for (Town town : getTowns())
			try {
				Coord townCoord = town.getHomeBlock().getCoord();
				double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
				if (dist < min)
					min = dist;
			} catch (TownyException e) {
			}
				
		return (int)Math.ceil(min);
	}
}
