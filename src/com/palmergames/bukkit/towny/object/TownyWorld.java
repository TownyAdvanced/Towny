package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;

public class TownyWorld extends TownyObject {
	private List<Town> towns = new ArrayList<Town>();
	private boolean isClaimable = true, isPVP, isForcePVP, isForceExpl, isForceFire, isForceTownMobs, hasWorldMobs, isDisablePlayerTrample,
			isDisableCreatureTrample, isUsingTowny = true,
			isUsingPlotManagementDelete = true,
			isUsingPlotManagementMayorDelete = true,
			isUsingPlotManagementRevert = true,
			isUsingPlotManagementWildRevert = true;
	private Long plotManagementRevertSpeed, plotManagementWildRevertDelay;
	private List<Integer> unclaimedZoneIgnoreIds = null;
	private List<Integer> plotManagementDeleteIds = null;
	private List<String> plotManagementMayorDelete = null;
	private List<Integer> plotManagementIgnoreIds = null;
	private Boolean unclaimedZoneBuild = null, unclaimedZoneDestroy = null, unclaimedZoneSwitch = null, unclaimedZoneItemUse = null;
	private String unclaimedZoneName = null;
	private Hashtable<Coord, TownBlock> townBlocks = new Hashtable<Coord, TownBlock>();
	private List<Coord> warZones = new ArrayList<Coord>();
	
	// TODO: private List<TownBlock> adminTownBlocks = new
	// ArrayList<TownBlock>();

	public TownyWorld(String name) {
		setName(name);
		
		isPVP =  true;
		isForcePVP = TownySettings.isForcingPvP();
		isForceFire = TownySettings.isForcingFire();
		isForceTownMobs = TownySettings.isForcingMonsters();
		isDisablePlayerTrample = TownySettings.isPlayerTramplingCropsDisabled();
		isDisableCreatureTrample = TownySettings.isCreatureTramplingCropsDisabled();
		hasWorldMobs = true;
        //hasWorldMobs = TownySettings.isWorldMonstersOn(); ??
		isForceExpl = TownySettings.isForcingExplosions();
		setUsingPlotManagementDelete(TownySettings.isUsingPlotManagementDelete());
		setUsingPlotManagementRevert(TownySettings.isUsingPlotManagementRevert());
		setPlotManagementRevertSpeed(TownySettings.getPlotManagementSpeed());
		setUsingPlotManagementWildRevert(TownySettings.isUsingPlotManagementWildRegen());
		setPlotManagementWildRevertDelay(TownySettings.getPlotManagementWildRegenDelay());	
	}

	public List<Town> getTowns() {
		return towns;
	}
	
	public boolean hasTowns() {
		return !towns.isEmpty();
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
			/*
			try {
				town.setWorld(null);
			} catch (AlreadyRegisteredException e) {
			}
			*/
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

	public void setPVP(boolean isPVP) {
		this.isPVP = isPVP;
	}

	public boolean isPVP() {
		return this.isPVP;
	}
	
	public void setForcePVP(boolean isPVP) {
		this.isForcePVP = isPVP;
	}

	public boolean isForcePVP() {
		return this.isForcePVP;
	}
	
	public void setForceExpl(boolean isExpl) {
		this.isForceExpl = isExpl;
	}

	public boolean isForceExpl() {
		return isForceExpl;
	}
	
	public void setForceFire(boolean isFire) {
		this.isForceFire = isFire;
	}

	public boolean isForceFire() {
		return isForceFire;
	}
	
	public void setDisablePlayerTrample(boolean isDisablePlayerTrample) {
		this.isDisablePlayerTrample = isDisablePlayerTrample;
	}

	public boolean isDisablePlayerTrample() {
		return isDisablePlayerTrample;
	}
	
	public void setDisableCreatureTrample(boolean isDisableCreatureTrample) {
		this.isDisableCreatureTrample = isDisableCreatureTrample;
	}

	public boolean isDisableCreatureTrample() {
		return isDisableCreatureTrample;
	}
	
	public void setWorldMobs(boolean hasMobs) {
		this.hasWorldMobs = hasMobs;
	}

	public boolean hasWorldMobs() {
		return this.hasWorldMobs;
	}
	
	public void setForceTownMobs(boolean setMobs) {
		this.isForceTownMobs = setMobs;
	}

	public boolean isForceTownMobs() {
		return isForceTownMobs;
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

	public void setUsingDefault() {
		setUnclaimedZoneBuild(null);
		setUnclaimedZoneDestroy(null);
		setUnclaimedZoneSwitch(null);
		setUnclaimedZoneItemUse(null);
		setUnclaimedZoneIgnore(null);
		setUnclaimedZoneName(null);
	}
	
	public void setUsingPlotManagementDelete(boolean using) {
		isUsingPlotManagementDelete = using;
	}
	
	public boolean isUsingPlotManagementDelete( ) {
		return isUsingPlotManagementDelete;
	}
	
	public void setUsingPlotManagementMayorDelete(boolean using) {
		isUsingPlotManagementMayorDelete = using;
	}
	
	public boolean isUsingPlotManagementMayorDelete( ) {
		return isUsingPlotManagementMayorDelete;
	}
	
	public void setUsingPlotManagementRevert(boolean using) {
		isUsingPlotManagementRevert = using;
	}
	
	public boolean isUsingPlotManagementRevert( ) {
		return isUsingPlotManagementRevert;
	}
	
	public List<Integer> getPlotManagementDeleteIds() {
		if (plotManagementDeleteIds == null)
			return TownySettings.getPlotManagementDeleteIds();
		else
			return plotManagementDeleteIds;
	}
	
	public boolean isPlotManagementDeleteIds(int id) {
		return getPlotManagementDeleteIds().contains(id);
	}

	public void setPlotManagementDeleteIds(List<Integer> plotManagementDeleteIds) {
			this.plotManagementDeleteIds = plotManagementDeleteIds;
	}
	
	public List<String> getPlotManagementMayorDelete() {
		if (plotManagementMayorDelete == null)
			return TownySettings.getPlotManagementMayorDelete();
		else
			return plotManagementMayorDelete;
	}
	
	public boolean isPlotManagementMayorDelete(String material) {
		return getPlotManagementMayorDelete().contains(material.toUpperCase());
	}

	public void setPlotManagementMayorDelete(List<String> plotManagementMayorDelete) {
			this.plotManagementMayorDelete = plotManagementMayorDelete;
	}
	
	public List<Integer> getPlotManagementIgnoreIds() {
		if (plotManagementIgnoreIds == null)
			return TownySettings.getPlotManagementIgnoreIds();
		else
			return plotManagementIgnoreIds;
	}
	
	public boolean isPlotManagementIgnoreIds(int id) {
		return getPlotManagementIgnoreIds().contains(id);
	}

	public void setPlotManagementIgnoreIds(List<Integer> plotManagementIgnoreIds) {
			this.plotManagementIgnoreIds = plotManagementIgnoreIds;
	}
	
	/**
	 * @return the isUsingPlotManagementWildRevert
	 */
	public boolean isUsingPlotManagementWildRevert() {
		return isUsingPlotManagementWildRevert;
	}

	/**
	 * @param isUsingPlotManagementWildRevert the isUsingPlotManagementWildRevert to set
	 */
	public void setUsingPlotManagementWildRevert(boolean isUsingPlotManagementWildRevert) {
		this.isUsingPlotManagementWildRevert = isUsingPlotManagementWildRevert;
	}

	/**
	 * @return the plotManagementRevertSpeed
	 */
	public long getPlotManagementRevertSpeed() {
			return plotManagementRevertSpeed;
	}

	/**
	 * @param plotManagementRevertSpeed the plotManagementRevertSpeed to set
	 */
	public void setPlotManagementRevertSpeed(long plotManagementRevertSpeed) {
		this.plotManagementRevertSpeed = plotManagementRevertSpeed;
	}

	/**
	 * @return the plotManagementWildRevertDelay
	 */
	public long getPlotManagementWildRevertDelay() {
		return plotManagementWildRevertDelay;
	}

	/**
	 * @param plotManagementWildRevertDelay the plotManagementWildRevertDelay to set
	 */
	public void setPlotManagementWildRevertDelay(long plotManagementWildRevertDelay) {
		this.plotManagementWildRevertDelay = plotManagementWildRevertDelay;
	}

	public List<Integer> getUnclaimedZoneIgnoreIds() {
		if (unclaimedZoneIgnoreIds == null)
			return TownySettings.getUnclaimedZoneIgnoreIds();
		else
			return unclaimedZoneIgnoreIds;
	}
	
	public boolean isUnclaimedZoneIgnoreId(int id) {
		return getUnclaimedZoneIgnoreIds().contains(id);
	}

	public void setUnclaimedZoneIgnore(List<Integer> unclaimedZoneIgnoreIds) {
			this.unclaimedZoneIgnoreIds = unclaimedZoneIgnoreIds;
	}

	public Boolean getUnclaimedZoneBuild() {
		if (unclaimedZoneBuild == null)
			return TownySettings.getUnclaimedZoneBuildRights();
		else
			return unclaimedZoneBuild;
	}

	public void setUnclaimedZoneBuild(Boolean unclaimedZoneBuild) {
		this.unclaimedZoneBuild = unclaimedZoneBuild;
	}

	public Boolean getUnclaimedZoneDestroy() {
		if (unclaimedZoneDestroy == null)
			return TownySettings.getUnclaimedZoneDestroyRights();
		else
			return unclaimedZoneDestroy;
	}

	public void setUnclaimedZoneDestroy(Boolean unclaimedZoneDestroy) {
		this.unclaimedZoneDestroy = unclaimedZoneDestroy;
	}

	public Boolean getUnclaimedZoneSwitch() {
		if (unclaimedZoneSwitch == null)
			return TownySettings.getUnclaimedZoneSwitchRights();
		else
			return unclaimedZoneSwitch;
	}

	public void setUnclaimedZoneSwitch(Boolean unclaimedZoneSwitch) {
		this.unclaimedZoneSwitch = unclaimedZoneSwitch;
	}

	public String getUnclaimedZoneName() {
		if (unclaimedZoneName == null)
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
		if (unclaimedZoneItemUse == null)
			return TownySettings.getUnclaimedZoneItemUseRights();
		else
			return unclaimedZoneItemUse;
	}
	
	/**
	 * Checks the distance from the closest homeblock.
	 * 
	 * @param key
	 * @return the distance to nearest towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key) {
		
		return getMinDistanceFromOtherTowns(key, null);
		
	}
	
	/**
	 * Checks the distance from a another town's homeblock.
	 * 
	 * @param key
	 * @param town Players town
	 * @return the closest distance to another towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key, Town homeTown) {
		double min = Integer.MAX_VALUE;
		for (Town town : getTowns())
			try {
				Coord townCoord = town.getHomeBlock().getCoord();
				if (homeTown != null)
					if (homeTown.getHomeBlock().equals(town.getHomeBlock()))
						continue;
				double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
				if (dist < min)
					min = dist;
			} catch (TownyException e) {
			}
				
		return (int)Math.ceil(min);
	}

	public void addWarZone(Coord coord) {
    	if (!isWarZone(coord))
    		warZones.add(coord);
    }
    
    public void removeWarZone(Coord coord) {
    	warZones.remove(coord);
    }
    
    public boolean isWarZone(Coord coord) {
    	return warZones.contains(coord);
    }

}
