package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

public class TownyWorld extends TownyObject {

	private HashMap<String, Town> towns = new HashMap<>();
	private boolean isClaimable = true;
	private boolean isUsingPlotManagementDelete = TownySettings.isUsingPlotManagementDelete();
	private boolean isUsingPlotManagementMayorDelete = TownySettings.isUsingPlotManagementMayorDelete();
	private boolean isUsingPlotManagementRevert = TownySettings.isUsingPlotManagementRevert();
	private boolean isUsingPlotManagementWildRevert = TownySettings.isUsingPlotManagementWildRegen();
	private long plotManagementRevertSpeed = TownySettings.getPlotManagementSpeed();
	private long plotManagementWildRevertDelay = TownySettings.getPlotManagementWildRegenDelay();
	private List<String> unclaimedZoneIgnoreBlockMaterials = null;
	private List<String> plotManagementDeleteIds = null;
	private List<String> plotManagementMayorDelete = null;
	private List<String> plotManagementIgnoreIds = null;
	private Boolean unclaimedZoneBuild = null, unclaimedZoneDestroy = null,
			unclaimedZoneSwitch = null, unclaimedZoneItemUse = null;
	private String unclaimedZoneName = null;
	private List<Coord> warZones = new ArrayList<>();
	private List<String> entityExplosionProtection = null;
	
	private boolean isUsingTowny = TownySettings.isUsingTowny();
	private boolean isWarAllowed = TownySettings.isWarAllowed();
	private boolean isPVP = TownySettings.isPvP();
	private boolean isForcePVP = TownySettings.isForcingPvP();
	private boolean isFire = TownySettings.isFire();
	private boolean isForceFire = TownySettings.isForcingFire();
	private boolean hasWorldMobs = TownySettings.isWorldMonstersOn();
	private boolean isForceTownMobs = TownySettings.isForcingMonsters();
	private boolean isExplosion = TownySettings.isExplosions();
	private boolean isForceExpl = TownySettings.isForcingExplosions();
	private boolean isEndermanProtect = TownySettings.getEndermanProtect();
	
	private boolean isDisablePlayerTrample = TownySettings.isPlayerTramplingCropsDisabled();
	private boolean isDisableCreatureTrample = TownySettings.isCreatureTramplingCropsDisabled();

	// TODO: private List<TownBlock> adminTownBlocks = new
	// ArrayList<TownBlock>();

	public TownyWorld(String name) {
		super(name);
	}

	public HashMap<String, Town> getTowns() {

		return towns;
	}

	public boolean hasTowns() {

		return !towns.isEmpty();
	}

	public boolean hasTown(String name) {

		return towns.containsKey(name);
	}

	public boolean hasTown(Town town) {

		return hasTown(town.getName());
	}

	public void addTown(Town town) throws AlreadyRegisteredException {

		if (hasTown(town))
			throw new AlreadyRegisteredException();
		else {
			towns.put(town.getName(), town);
			town.setWorld(this);
		}
	}

	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		if (!hasTownBlock(coord))
			throw new NotRegisteredException();
		return TownyUniverse.getInstance().getTownBlock(new WorldCoord(this.getName(), coord));
	}

	public boolean hasTownBlock(Coord key) {

		return TownyUniverse.getInstance().hasTownBlock(new WorldCoord(this.getName(), key));
	}

	public TownBlock getTownBlock(int x, int z) throws NotRegisteredException {

		return getTownBlock(new Coord(x, z));
	}

	public List<TownBlock> getTownBlocks(Town town) {

		List<TownBlock> out = new ArrayList<>();
		for (TownBlock townBlock : town.getTownBlocks())
			if (townBlock.getWorld() == this)
				out.add(townBlock);
		return out;
	}

	/*
	 * Used only in the getTreeString() method.
	 */
	public Collection<TownBlock> getTownBlocks() {

		List<TownBlock> townBlocks = new ArrayList<>();
		for (TownBlock townBlock : TownyUniverse.getInstance().getTownBlocks().values())
			if (townBlock.getWorld() == this)
				townBlocks.add(townBlock);
		return townBlocks;
	}

	public void removeTown(Town town) throws NotRegisteredException {

		if (!hasTown(town))
			throw new NotRegisteredException();
		else {
			towns.remove(town.getName());
			/*
			 * try {
			 * town.setWorld(null);
			 * } catch (AlreadyRegisteredException e) {
			 * }
			 */
		}
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "World (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "TownBlocks (" + getTownBlocks().size() + "): " /*
																						 * +
																						 * getTownBlocks
																						 * (
																						 * )
																						 */);
		return out;
	}

	public void setWarAllowed(boolean isWarAllowed) {

		this.isWarAllowed = isWarAllowed;
	}

	public boolean isWarAllowed() {

		return this.isWarAllowed;
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

	public void setExpl(boolean isExpl) {

		this.isExplosion = isExpl;
	}

	public boolean isExpl() {

		return isExplosion;
	}

	public void setForceExpl(boolean isExpl) {

		this.isForceExpl = isExpl;
	}

	public boolean isForceExpl() {

		return isForceExpl;
	}

	public void setFire(boolean isFire) {

		this.isFire = isFire;
	}

	public boolean isFire() {

		return isFire;
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

	public void setEndermanProtect(boolean setEnder) {

		this.isEndermanProtect = setEnder;
	}

	public boolean isEndermanProtect() {

		return isEndermanProtect;
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

	public boolean isUsingPlotManagementDelete() {

		return isUsingPlotManagementDelete;
	}

	public void setUsingPlotManagementMayorDelete(boolean using) {

		isUsingPlotManagementMayorDelete = using;
	}

	public boolean isUsingPlotManagementMayorDelete() {

		return isUsingPlotManagementMayorDelete;
	}

	public void setUsingPlotManagementRevert(boolean using) {

		isUsingPlotManagementRevert = using;
	}

	public boolean isUsingPlotManagementRevert() {

		return isUsingPlotManagementRevert;
	}

	public List<String> getPlotManagementDeleteIds() {

		if (plotManagementDeleteIds == null)
			return TownySettings.getPlotManagementDeleteIds();
		else
			return plotManagementDeleteIds;
	}

	public boolean isPlotManagementDeleteIds(String id) {

		return getPlotManagementDeleteIds().contains(id);
	}

	public void setPlotManagementDeleteIds(List<String> plotManagementDeleteIds) {

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

	public List<String> getPlotManagementIgnoreIds() {
		
		if (plotManagementIgnoreIds == null)
			return TownySettings.getPlotManagementIgnoreIds();
		else
			return plotManagementIgnoreIds;
	}

	public boolean isPlotManagementIgnoreIds(Material mat) {
		return getPlotManagementIgnoreIds().contains(mat.toString());
	}
	
	@Deprecated
	public boolean isPlotManagementIgnoreIds(String id, Byte data) {

		if (getPlotManagementIgnoreIds().contains(id + ":" + data))
			return true;
		
		return getPlotManagementIgnoreIds().contains(id);
	}

	public void setPlotManagementIgnoreIds(List<String> plotManagementIgnoreIds) {

		this.plotManagementIgnoreIds = plotManagementIgnoreIds;
	}

	/**
	 * @return the isUsingPlotManagementWildRevert
	 */
	public boolean isUsingPlotManagementWildRevert() {

		return isUsingPlotManagementWildRevert;
	}

	/**
	 * @param isUsingPlotManagementWildRevert the
	 *            isUsingPlotManagementWildRevert to set
	 */
	public void setUsingPlotManagementWildRevert(boolean isUsingPlotManagementWildRevert) {

		this.isUsingPlotManagementWildRevert = isUsingPlotManagementWildRevert;
	}

	/*
	 * No longer used - Never was used. Sadly not configurable per-world based on how the timer runs.
	 */
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
	 * @param plotManagementWildRevertDelay the plotManagementWildRevertDelay to
	 *            set
	 */
	public void setPlotManagementWildRevertDelay(long plotManagementWildRevertDelay) {

		this.plotManagementWildRevertDelay = plotManagementWildRevertDelay;
	}

	public void setPlotManagementWildRevertEntities(List<String> entities) {

		entityExplosionProtection = new ArrayList<>();

		for (String mob : entities)
			if (!mob.equals("")) {
				entityExplosionProtection.add(mob.toLowerCase());
			}

	}

	public List<String> getPlotManagementWildRevertEntities() {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return entityExplosionProtection;
	}

	public boolean isProtectingExplosionEntity(Entity entity) {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return (entityExplosionProtection.contains(entity.getType().getEntityClass().getSimpleName().toLowerCase()));

	}

	public void setUnclaimedZoneIgnore(List<String> unclaimedZoneIgnoreIds) {

		this.unclaimedZoneIgnoreBlockMaterials = unclaimedZoneIgnoreIds;
	}
	
	public List<String> getUnclaimedZoneIgnoreMaterials() {

		if (unclaimedZoneIgnoreBlockMaterials == null)
			return TownySettings.getUnclaimedZoneIgnoreMaterials();
		else
			return unclaimedZoneIgnoreBlockMaterials;
	}

	@SuppressWarnings("unlikely-arg-type")
	public boolean isUnclaimedZoneIgnoreMaterial(Material mat) {

		return getUnclaimedZoneIgnoreMaterials().contains(mat);
	}


	public boolean getUnclaimedZonePerm(ActionType type) {

		switch (type) {
		case BUILD:
			return this.getUnclaimedZoneBuild();
		case DESTROY:
			return this.getUnclaimedZoneDestroy();
		case SWITCH:
			return this.getUnclaimedZoneSwitch();
		case ITEM_USE:
			return this.getUnclaimedZoneItemUse();
		default:
			throw new UnsupportedOperationException();
		}
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
	 * @param key - Coord to check from.
	 * @return the distance to nearest towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key) {

		return getMinDistanceFromOtherTowns(key, null);

	}

	/**
	 * Checks the distance from a another town's homeblock.
	 * 
	 * @param key - Coord to check from.
	 * @param homeTown Players town
	 * @return the closest distance to another towns homeblock.
	 */
	public int getMinDistanceFromOtherTowns(Coord key, Town homeTown) {

		double min = Integer.MAX_VALUE;
		for (Town town : getTowns().values()) {
			try {
				Coord townCoord = town.getHomeBlock().getCoord();
				if (homeTown != null)
					// If the townblock either: the town is the same as homeTown OR
					// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
					if (homeTown.getHomeBlock().equals(town.getHomeBlock()) || (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNation().equals(homeTown.getNation())))
						continue;
				
				if (!town.getHomeblockWorld().equals(this)) continue;
				
				double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
				if (dist < min)
					min = dist;
			} catch (TownyException e) {
			}
		}
		return (int) Math.ceil(min);
	}

	/**
	 * Checks the distance from the closest town block.
	 * 
	 * @param key - Coord to check from.
	 * @return the distance to nearest town's townblock.
	 */
	public int getMinDistanceFromOtherTownsPlots(Coord key) {

		return getMinDistanceFromOtherTownsPlots(key, null);
	}

	/**
	 * Checks the distance from a another town's plots.
	 * 
	 * @param key - Coord to check from.
	 * @param homeTown Players town
	 * @return the closest distance to another towns nearest plot.
	 */
	public int getMinDistanceFromOtherTownsPlots(Coord key, Town homeTown) {

		double min = Integer.MAX_VALUE;
		for (Town town : getTowns().values()) {
			try {
				if (homeTown != null)
					// If the townblock either: the town is the same as homeTown OR 
					// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
					if (homeTown.getHomeBlock().equals(town.getHomeBlock()) || (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNation().equals(homeTown.getNation())))
						continue;
				for (TownBlock b : town.getTownBlocks()) {
					if (!b.getWorld().equals(this)) continue;

					Coord townCoord = b.getCoord();
					
					if (key.equals(townCoord)) continue;
					
					double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
					if (dist < min)
						min = dist;
				}
			} catch (TownyException e) {
			}
		}
		return (int) Math.ceil(min);
	}
	
	/**
	 * Returns the closest town from a given coord (key).
	 * @param key - Coord
	 * @param nearestTown - Closest town to the given coord.
	 * @return the nearestTown
	 */
	public Town getClosestTownFromCoord(Coord key, Town nearestTown) {
		
		double min = Integer.MAX_VALUE;
		for (Town town : getTowns().values()) {
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this)) continue;
				
				Coord townCoord = b.getCoord();
				double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
				if (dist < min) {
					min = dist;
					nearestTown = town;
				}						
			}		
		}		
		return (nearestTown);		
	}
	
	/**
	 * Returns the closest town with a nation from a given coord (key).
	 * 
	 * @param key - Coord.
	 * @param nearestTown - Closest town to given coord.
	 * @return the nearest town belonging to a nation.   
	 */
	public Town getClosestTownWithNationFromCoord(Coord key, Town nearestTown) {
		
		double min = Integer.MAX_VALUE;
		for (Town town : getTowns().values()) {
			if (!town.hasNation()) continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this)) continue;
				
				Coord townCoord = b.getCoord();
				double dist = Math.sqrt(Math.pow(townCoord.getX() - key.getX(), 2) + Math.pow(townCoord.getZ() - key.getZ(), 2));
				if (dist < min) {
					min = dist;
					nearestTown = town;
				}						
			}		
		}		
		return (nearestTown);		
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

	public void addMetaData(CustomDataField md) {
		super.addMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveWorld(this);
	}

	public void removeMetaData(CustomDataField md) {
		super.removeMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveWorld(this);
	}
}
