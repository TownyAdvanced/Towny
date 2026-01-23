package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.ObjectSaveException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.metadata.MetadataLoader;
import com.palmergames.util.MathUtil;

import com.palmergames.util.StringMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.UnaryOperator;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TownyWorld extends TownyObject {
	private UUID uuid;

	private final HashMap<UUID, Town> towns = new HashMap<>();

	private boolean isDeletingEntitiesOnUnclaim = TownySettings.isDeletingEntitiesOnUnclaim();
	private Set<EntityType> unclaimDeleteEntityTypes = null;
	
	private boolean isUsingPlotManagementDelete = TownySettings.isUsingPlotManagementDelete();
	private Set<Material> plotManagementDeleteIds = null;
	
	private boolean isUsingPlotManagementMayorDelete = TownySettings.isUsingPlotManagementMayorDelete();
	private Set<Material> plotManagementMayorDelete = null;
	
	private boolean isUsingPlotManagementRevert = TownySettings.isUsingPlotManagementRevert();
	private Set<Material> plotManagementIgnoreIds = null;
	private Set<Material> revertOnUnclaimWhitelistMaterials = null;

	private boolean isUsingPlotManagementWildEntityRevert = TownySettings.isUsingPlotManagementWildEntityRegen();	
	private long plotManagementWildRevertDelay = TownySettings.getPlotManagementWildRegenDelay();
	private Set<EntityType> entityExplosionProtection = null;
	private Set<Material> plotManagementWildRevertBlockWhitelist = null;
	private Set<Material> wildRevertMaterialsToNotOverwrite = null;
	
	private boolean isUsingPlotManagementWildBlockRevert = TownySettings.isUsingPlotManagementWildBlockRegen();
	private Set<Material> blockExplosionProtection = null;
	
	private Set<Material> unclaimedZoneIgnoreBlockMaterials = null;
	private Boolean unclaimedZoneBuild = null, unclaimedZoneDestroy = null,
			unclaimedZoneSwitch = null, unclaimedZoneItemUse = null;

	private String unclaimedZoneName = null;

	private boolean isUsingTowny = TownySettings.isUsingTowny();
	private boolean isClaimable = TownySettings.isNewWorldClaimable();
	private boolean isWarAllowed = TownySettings.isWarAllowed();
	private boolean isPVP = TownySettings.isPvP();
	private boolean isForcePVP = TownySettings.isForcingPvP();
	private boolean isFriendlyFire = TownySettings.isFriendlyFireEnabled();
	private boolean isFire = TownySettings.isFire();
	private boolean isForceFire = TownySettings.isForcingFire();
	private boolean hasWorldMobs = TownySettings.isWorldMonstersOn();
	private boolean hasWildernessMonsters = TownySettings.isWildernessMonstersOn();
	private boolean isForceTownMobs = TownySettings.isForcingMonsters();
	private boolean isExplosion = TownySettings.isExplosions();
	private boolean isForceExpl = TownySettings.isForcingExplosions();
	private boolean isEndermanProtect = TownySettings.getEndermanProtect();
	private boolean isJailing = TownySettings.isWorldJailingEnabled();

	
	private boolean isDisableCreatureTrample = TownySettings.isCreatureTramplingCropsDisabled();
	
	public Map<Location, Material> bedMap = new HashMap<>();
	public final List<UUID> tridentStrikeList = new ArrayList<>(0);

	public TownyWorld(String name) {
		super(name);
	}

	public TownyWorld(String name, UUID uuid) {
		super(name);
		this.uuid = uuid;
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof TownyWorld otherTownyWorld))
			return false;
		return this.getName().equals(otherTownyWorld.getName()); // TODO: Change this to UUID when the UUID database is in use.
	}

	@Override
	public int hashCode() {
		return Objects.hash(getUUID(), getName());
	}

	public UUID getUUID() {
		return uuid;
	}
	
	@ApiStatus.Internal
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	@Nullable
	public World getBukkitWorld() {
		World world = this.uuid != null ? Bukkit.getWorld(this.uuid) : Bukkit.getWorld(getName());
		
		if (world == null)
			world = Bukkit.getWorld(getName());
		
		return world;
	}
	
	/**
	 * @deprecated use {@link #getTownsInWorld()} instead.
	 */
	@Deprecated(since = "0.102.0.4")
	public HashMap<String, Town> getTowns() {
		return this.towns.values().stream().collect(Collectors.toMap(TownyObject::getName, UnaryOperator.identity(), (existingTown, newTown) -> newTown, HashMap::new));
	}

	public @Unmodifiable Collection<Town> getTownsInWorld() {
		return Set.copyOf(this.towns.values());
	}

	public boolean hasTowns() {

		return !towns.isEmpty();
	}

	public boolean hasTown(String name) {
		final Town town = TownyUniverse.getInstance().getTown(name);

		return town != null && towns.containsKey(town.getUUID());
	}

	public boolean hasTown(Town town) {
		return this.towns.containsKey(town.getUUID());
	}

	public void addTown(Town town) {
		towns.put(town.getUUID(), town);
	}

	public TownBlock getTownBlock(Coord coord) throws NotRegisteredException {
		if (!hasTownBlock(coord))
			throw new NotRegisteredException();
		return TownyUniverse.getInstance().getTownBlock(new WorldCoord(this.getName(), this.getUUID(), coord));
	}

	public boolean hasTownBlock(Coord key) {

		return TownyUniverse.getInstance().hasTownBlock(new WorldCoord(this.getName(), this.getUUID(), key));
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
	
	public void setWildernessMobs(boolean hasMonsters) {
		
		this.hasWildernessMonsters = hasMonsters;
	}
	
	public boolean hasWildernessMobs() {
		
		return this.hasWildernessMonsters;
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
		setUsingTowny(TownySettings.isUsingTowny());
		setClaimable(TownySettings.isNewWorldClaimable());
		setWarAllowed(TownySettings.isWarAllowed());
		setPVP(TownySettings.isPvP());
		setForcePVP(TownySettings.isForcingPvP());
		setFriendlyFire(TownySettings.isFriendlyFireEnabled());
		setFire(TownySettings.isFire());
		setForceFire(TownySettings.isForcingFire());
		setWorldMobs(TownySettings.isWorldMonstersOn());
		setWildernessMobs(TownySettings.isWildernessMonstersOn());
		setForceTownMobs(TownySettings.isForcingMonsters());
		setExpl(TownySettings.isExplosions());
		setForceExpl(TownySettings.isForcingExplosions());
		setEndermanProtect(TownySettings.getEndermanProtect());
		setDisableCreatureTrample(TownySettings.isCreatureTramplingCropsDisabled());
		// reset unclaiming deletes entities.
		unclaimDeleteEntityTypes = null;
		setDeletingEntitiesOnUnclaim(TownySettings.isDeletingEntitiesOnUnclaim());
		// reset unclaiming deletes blocks.
		setUsingPlotManagementDelete(TownySettings.isUsingPlotManagementDelete());
		plotManagementDeleteIds = null;
		// mayor's plot clear
		setUsingPlotManagementMayorDelete(TownySettings.isUsingPlotManagementMayorDelete());
		plotManagementMayorDelete = null;
		// revert on unclaim
		setUsingPlotManagementRevert(TownySettings.isUsingPlotManagementRevert());
		// revert ignore
		plotManagementIgnoreIds = null;
		revertOnUnclaimWhitelistMaterials = null;
		// wilderness entity explosion revert
		setUsingPlotManagementWildEntityRevert(TownySettings.isUsingPlotManagementWildEntityRegen());
		entityExplosionProtection = null;
		// wilderness block explosion revert
		setUsingPlotManagementWildBlockRevert(TownySettings.isUsingPlotManagementWildBlockRegen());
		blockExplosionProtection = null;
		plotManagementWildRevertBlockWhitelist = null;
		wildRevertMaterialsToNotOverwrite = null;
		// Entities protected from explosions
		entityExplosionProtection = null;
		setJailingEnabled(TownySettings.isWorldJailingEnabled());
	}

	public void setUsingPlotManagementDelete(boolean using) {

		isUsingPlotManagementDelete = using;
	}

	public boolean isUsingPlotManagementDelete() {

		return isUsingPlotManagementDelete;
	}

	public void setDeletingEntitiesOnUnclaim(boolean using) {
		isDeletingEntitiesOnUnclaim = using;
	}

	public boolean isDeletingEntitiesOnUnclaim() {
		return isDeletingEntitiesOnUnclaim;
	}

	public Collection<EntityType> getUnclaimDeleteEntityTypes() {
		if (unclaimDeleteEntityTypes == null)
			setUnclaimDeleteEntityTypes(TownySettings.getUnclaimDeleteEntityTypes());

		return unclaimDeleteEntityTypes;
	}

	public void setUnclaimDeleteEntityTypes(List<String> entityTypes) {
		this.unclaimDeleteEntityTypes = TownySettings.toEntityTypeSet(entityTypes);
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

	public Collection<Material> getPlotManagementDeleteIds() {

		if (plotManagementDeleteIds == null)
			return TownySettings.getPlotManagementDeleteIds();
		else
			return plotManagementDeleteIds;
	}

	public boolean isPlotManagementDeleteIds(Material material) {

		return getPlotManagementDeleteIds().contains(material);
	}

	public void setPlotManagementDeleteIds(List<String> plotManagementDeleteIds) {
		this.plotManagementDeleteIds = new HashSet<>(TownySettings.toMaterialSet(plotManagementDeleteIds));
	}

	public Collection<Material> getPlotManagementMayorDelete() {

		if (plotManagementMayorDelete == null)
			return TownySettings.getPlotManagementMayorDelete();
		else
			return plotManagementMayorDelete;
	}

	public boolean isPlotManagementMayorDelete(Material material) {

		return getPlotManagementMayorDelete().contains(material);
	}

	public void setPlotManagementMayorDelete(List<String> plotManagementMayorDelete) {
		this.plotManagementMayorDelete = new HashSet<>(TownySettings.toMaterialSet(plotManagementMayorDelete));
	}

	public boolean isUnclaimedBlockAllowedToRevert(Material mat) {
		if (!getRevertOnUnclaimWhitelistMaterials().isEmpty()) {
			return isRevertOnUnclaimWhitelistMaterial(mat);
		}

		return !isPlotManagementIgnoreIds(mat);
	}
	
	public Collection<Material> getPlotManagementIgnoreIds() {
		
		if (plotManagementIgnoreIds == null)
			return TownySettings.getPlotManagementIgnoreIds();
		else
			return plotManagementIgnoreIds;
	}

	public boolean isPlotManagementIgnoreIds(Material mat) {
		return getPlotManagementIgnoreIds().contains(mat);
	}

	public void setPlotManagementIgnoreIds(List<String> plotManagementIgnoreIds) {
		this.plotManagementIgnoreIds = new HashSet<>(TownySettings.toMaterialSet(plotManagementIgnoreIds));
	}

	public Collection<Material> getRevertOnUnclaimWhitelistMaterials() {
		if (revertOnUnclaimWhitelistMaterials == null)
			return TownySettings.getRevertOnUnclaimWhitelistMaterials();
		else 
			return revertOnUnclaimWhitelistMaterials;
	}

	public void setRevertOnUnclaimWhitelistMaterials(List<String> revertOnUnclaimWhitelistMaterials) {
		this.revertOnUnclaimWhitelistMaterials = new HashSet<>(TownySettings.toMaterialSet(revertOnUnclaimWhitelistMaterials));
	}

	public boolean isRevertOnUnclaimWhitelistMaterial(Material mat) {
		return getRevertOnUnclaimWhitelistMaterials().contains(mat);
	}

	/**
	 * @return the isUsingPlotManagementWildEntityRevert
	 */
	public boolean isUsingPlotManagementWildEntityRevert() {

		return isUsingPlotManagementWildEntityRevert;
	}
	
	/**
	 * @return the isUsingPlotManagementWildBlockRevert
	 */
	public boolean isUsingPlotManagementWildBlockRevert() {

		return isUsingPlotManagementWildBlockRevert;
	}

	/**
	 * @param isUsingPlotManagementWildEntityRevert the
	 *            isUsingPlotManagementWildRevert to set
	 */
	public void setUsingPlotManagementWildEntityRevert(boolean isUsingPlotManagementWildEntityRevert) {

		this.isUsingPlotManagementWildEntityRevert = isUsingPlotManagementWildEntityRevert;
	}
	
	/**
	 * @param isUsingPlotManagementWildBlockRevert the
	 *            isUsingPlotManagementWildBlockRevert to set
	 */
	public void setUsingPlotManagementWildBlockRevert(boolean isUsingPlotManagementWildBlockRevert) {

		this.isUsingPlotManagementWildBlockRevert = isUsingPlotManagementWildBlockRevert;
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

	public void setPlotManagementWildRevertEntities(List<String> entities) {
		entityExplosionProtection = new HashSet<>();
		entityExplosionProtection.addAll(TownySettings.toEntityTypeSet(entities));
	}

	public Collection<EntityType> getPlotManagementWildRevertEntities() {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return entityExplosionProtection;
	}

	public boolean isProtectingExplosionEntity(Entity entity) {

		if (entityExplosionProtection == null)
			setPlotManagementWildRevertEntities(TownySettings.getWildExplosionProtectionEntities());

		return entityExplosionProtection.contains(entity.getType());

	}

	public void setPlotManagementWildRevertBlockWhitelist(List<String> mats) {
		plotManagementWildRevertBlockWhitelist = new HashSet<>();
		plotManagementWildRevertBlockWhitelist.addAll(TownySettings.toMaterialSet(mats));
	}

	public Collection<Material> getPlotManagementWildRevertBlockWhitelist() {

		if (plotManagementWildRevertBlockWhitelist == null)
			setPlotManagementWildRevertBlockWhitelist(TownySettings.getWildExplosionRevertBlockWhitelist());

		return plotManagementWildRevertBlockWhitelist;
	}

	public boolean isPlotManagementWildRevertWhitelistedBlock(Material mat) {

		if (plotManagementWildRevertBlockWhitelist == null)
			setPlotManagementWildRevertBlockWhitelist(TownySettings.getWildExplosionRevertBlockWhitelist());

		return plotManagementWildRevertBlockWhitelist.isEmpty() || plotManagementWildRevertBlockWhitelist.contains(mat);
	}

	public boolean isExplodedBlockAllowedToRevert(Material mat) {
		if (getPlotManagementWildRevertBlockWhitelist().isEmpty())
			return !isPlotManagementIgnoreIds(mat);
		else
			return isPlotManagementWildRevertWhitelistedBlock(mat);
	}

	public void setWildRevertMaterialsToNotOverwrite(List<String> mats) {
		wildRevertMaterialsToNotOverwrite = new HashSet<>();
		wildRevertMaterialsToNotOverwrite.addAll(TownySettings.toMaterialSet(mats));
	}

	public Collection<Material> getWildRevertMaterialsToNotOverwrite() {
		if (wildRevertMaterialsToNotOverwrite == null)
			setWildRevertMaterialsToNotOverwrite(TownySettings.getWildExplosionRevertMaterialsToNotOverwrite());
		return wildRevertMaterialsToNotOverwrite;
	}

	public boolean isMaterialNotAllowedToBeOverwrittenByWildRevert(Material mat) {
		if (wildRevertMaterialsToNotOverwrite == null)
			setWildRevertMaterialsToNotOverwrite(TownySettings.getWildExplosionRevertMaterialsToNotOverwrite());

		return wildRevertMaterialsToNotOverwrite.contains(mat);
	}

	public void setPlotManagementWildRevertMaterials(List<String> mats) {
		blockExplosionProtection = new HashSet<>(TownySettings.toMaterialSet(mats));
	}

	public Collection<Material> getPlotManagementWildRevertBlocks() {

		if (blockExplosionProtection == null)
			setPlotManagementWildRevertMaterials(TownySettings.getWildExplosionProtectionBlocks());

		return blockExplosionProtection;
	}

	public boolean isProtectingExplosionBlock(Material material) {

		if (blockExplosionProtection == null)
			setPlotManagementWildRevertMaterials(TownySettings.getWildExplosionProtectionBlocks());

		return (blockExplosionProtection.contains(material));

	}

	public void setUnclaimedZoneIgnore(List<String> unclaimedZoneIgnoreIds) {
		if (unclaimedZoneIgnoreIds == null)
			this.unclaimedZoneIgnoreBlockMaterials = new HashSet<>(TownySettings.getUnclaimedZoneIgnoreMaterials());
		else
			this.unclaimedZoneIgnoreBlockMaterials = new HashSet<>(TownySettings.toMaterialSet(unclaimedZoneIgnoreIds));
	}
	
	public Collection<Material> getUnclaimedZoneIgnoreMaterials() {

		if (unclaimedZoneIgnoreBlockMaterials == null)
			return TownySettings.getUnclaimedZoneIgnoreMaterials();
		else
			return unclaimedZoneIgnoreBlockMaterials;
	}

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

	public String getFormattedUnclaimedZoneName() {
		return StringMgmt.remUnderscore(getUnclaimedZoneName());
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
	public int getMinDistanceFromOtherTownsHomeBlocks(Coord key) {
		return getMinDistanceFromOtherTownsHomeBlocks(key, null);
	}

	/**
	 * Checks the distance from a another town's homeblock, or the distance to
	 * another Town if homeTown is null.
	 * 
	 * @param key      Coord to check from.
	 * @param homeTown The town belonging to a player, so that the town's own
	 *                 homeblock is not returned, or null if this should apply to
	 *                 any townblock, and not just homeblocks.
	 * @return the closest distance to another towns homeblock.
	 */
	public int getMinDistanceFromOtherTownsHomeBlocks(Coord key, Town homeTown) {
		double minSqr = -1;
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		for (Town town : getTownsInWorld()) {
			try {
				Coord townCoord = town.getHomeBlock().getCoord();
				if (homeTown != null) {
					// If the townblock either: the town is the same as homeTown OR
					// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
					if (homeTown.getUUID().equals(town.getUUID())
						|| (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNationOrNull().equals(homeTown.getNationOrNull()))
						|| (TownySettings.isMinDistanceIgnoringTownsInAlliedNation() && homeTown.isAlliedWith(town)))
						continue;
				}
				if (!town.getHomeblockWorld().equals(this)) continue;
				
				final double distSqr = MathUtil.distanceSquared((double) townCoord.getX() - keyX, (double) townCoord.getZ() - keyZ);
				if (minSqr == -1 || distSqr < minSqr)
					minSqr = distSqr;
			} catch (TownyException ignored) {}
		}
		return minSqr == -1 ? Integer.MAX_VALUE : (int) Math.ceil(Math.sqrt(minSqr));
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
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		for (Town town : getTownsInWorld()) {
			if (homeTown != null)
				// If the townblock either: the town is the same as homeTown OR 
				// both towns are in the same nation (and this is set to ignore distance in the config,) skip over the proximity filter.
				if (homeTown.getUUID().equals(town.getUUID())
					|| (TownySettings.isMinDistanceIgnoringTownsInSameNation() && homeTown.hasNation() && town.hasNation() && town.getNationOrNull().equals(homeTown.getNationOrNull()))
					|| (TownySettings.isMinDistanceIgnoringTownsInAlliedNation() && homeTown.isAlliedWith(town)))
					continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this)) continue;

				final int tbX = b.getX();
				final int tbZ = b.getZ();
				
				if (keyX == tbX && keyZ == tbZ)
					continue;
				
				final double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
				if (minSqr == -1 || distSqr < minSqr)
					minSqr = distSqr;
			}
		}
		return minSqr == -1 ? Integer.MAX_VALUE : (int) Math.ceil(Math.sqrt(minSqr));
	}
	
	
	/**
	 * Returns the distance to the closest townblock 
	 * from the given coord, for the give town. 
	 * 
	 * @param key Coord to be tested from.
	 * @param town Town to have their townblocks measured.
	 * @return the closest distance to the towns townblock.
	 */
	public int getMinDistanceFromOtherPlotsOwnedByTown(Coord key, Town town) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		for (TownBlock b : town.getTownBlocks()) {
			if (!b.getWorld().equals(this)) continue;

			final int tbX = b.getX();
			final int tbZ = b.getZ();
			
			if (keyX == tbX && keyZ == tbZ)
				continue;
			
			final double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
			if (minSqr == -1 || distSqr < minSqr)
				minSqr = distSqr;
		}
		return minSqr == -1 ? Integer.MAX_VALUE : (int) Math.ceil(Math.sqrt(minSqr));
	}
	
	/**
	 * Returns the closest town with a nation from a given coord (key).
	 * 
	 * @param key - Coord.
	 * @param nearestTown - Closest town to given coord.
	 * @return the nearest town belonging to a nation.   
	 */
	public Town getClosestTownWithNationFromCoord(Coord key, Town nearestTown) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		for (Town town : getTownsInWorld()) {
			if (!town.hasNation()) continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this)) continue;
				
				final int tbX = b.getX();
				final int tbZ = b.getZ();
				
				double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
				if (minSqr == -1 || distSqr < minSqr) {
					minSqr = distSqr;
					nearestTown = town;
				}						
			}		
		}		
		return (nearestTown);		
	}

	/**
	 * Get the town block that belongs to the closest town with a nation
	 * from the specified coord.
	 * 
	 * @param key - Coordinate to compare distance to
	 * @return The nearest townblock that belongs to a town or
	 * null if there are no towns in the world.
	 */
	@Nullable
	public TownBlock getClosestTownblockWithNationFromCoord(Coord key) {
		final int keyX = key.getX();
		final int keyZ = key.getZ();
		
		double minSqr = -1;
		TownBlock tb = null;
		
		for (Town town : getTownsInWorld()) {
			if (!town.hasNation())
				continue;
			for (TownBlock b : town.getTownBlocks()) {
				if (!b.getWorld().equals(this))
					continue;

				final int tbX = b.getX();
				final int tbZ = b.getZ();
				
				double distSqr = MathUtil.distanceSquared((double) tbX - keyX, (double) tbZ - keyZ);
				if (minSqr == -1 || distSqr < minSqr) {
					minSqr = distSqr;
					tb = b;
				}
			}
		}
		
		return tb;
	}

	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}

	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
	}
	
	/**
	 * Does this world have an exploded bet at the location?
	 * @param location Location to test.
	 * @return true when the bed map contains the location.
	 */
	@ApiStatus.Internal
	public boolean hasBedExplosionAtBlock(Location location) {
		return bedMap.containsKey(location);
	}

	/**
	 * Gets the exploded bed material.
	 * @param location Location to get the material.
	 * @return material of the bed or null if the bedMap doesn't contain the location.
	 */
	@Nullable
	@ApiStatus.Internal
	public Material getBedExplosionMaterial(Location location) {
		if (hasBedExplosionAtBlock(location))
			return bedMap.get(location);
		return null;
	}
	
	@ApiStatus.Internal
	public void addBedExplosionAtBlock(Location location, Material material) {
		bedMap.put(location, material);
	}

	@ApiStatus.Internal
	public void removeBedExplosionAtBlock(Location location) {
		if (hasBedExplosionAtBlock(location))
			bedMap.remove(location);
	}
	
	@ApiStatus.Internal
	public boolean hasTridentStrike(UUID uuid) {
		return tridentStrikeList.contains(uuid);
	}

	@ApiStatus.Internal
	public void addTridentStrike(UUID uuid) {
		tridentStrikeList.add(uuid);
	}
	
	@ApiStatus.Internal
	public void removeTridentStrike(UUID uuid) {
		tridentStrikeList.remove(uuid);
	}

	public void setFriendlyFire(boolean parseBoolean) {
		isFriendlyFire = parseBoolean;
		
	}
	
	public boolean isFriendlyFireEnabled( ) {
		return isFriendlyFire;
	}

	public void setJailingEnabled(boolean parseBoolean) {
		isJailing = parseBoolean;
	}
	
	public boolean isJailingEnabled( ) {
		return isJailing;
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveWorld(this);
	}


	@Override
	public Map<String, Object> getObjectDataMap() throws ObjectSaveException {
		try {
			Map<String, Object> world_hm = new HashMap<>();
			world_hm.put("name", getName());
			world_hm.put("claimable", isClaimable());
			world_hm.put("pvp", isPVP());
			world_hm.put("forcepvp", isForcePVP());
			world_hm.put("forcetownmobs", isForceTownMobs());
			world_hm.put("friendlyFire", isFriendlyFireEnabled());
			world_hm.put("worldmobs", hasWorldMobs());
			world_hm.put("wildernessmobs", hasWildernessMobs());
			world_hm.put("firespread", isFire());
			world_hm.put("forcefirespread", isForceFire());
			world_hm.put("explosions", isExpl());
			world_hm.put("forceexplosions", isForceExpl());
			world_hm.put("endermanprotect", isEndermanProtect());
			world_hm.put("disablecreaturetrample", isDisableCreatureTrample());
			world_hm.put("unclaimedZoneBuild", getUnclaimedZoneBuild());
			world_hm.put("unclaimedZoneDestroy", getUnclaimedZoneDestroy());
			world_hm.put("unclaimedZoneSwitch", getUnclaimedZoneSwitch());
			world_hm.put("unclaimedZoneItemUse", getUnclaimedZoneItemUse());
			if (getUnclaimedZoneName() != null)
				world_hm.put("unclaimedZoneName", getUnclaimedZoneName());

			// Unclaimed Zone Ignore Ids
			if (getUnclaimedZoneIgnoreMaterials() != null)
				world_hm.put("unclaimedZoneIgnoreIds", StringMgmt.join(getUnclaimedZoneIgnoreMaterials(), "#"));

			// Using PlotManagement Delete
			world_hm.put("usingPlotManagementDelete", isUsingPlotManagementDelete());
			// Plot Management Delete Ids
			if (getPlotManagementDeleteIds() != null)
				world_hm.put("plotManagementDeleteIds", StringMgmt.join(getPlotManagementDeleteIds(), "#"));

			// Deleting EntityTypes from Townblocks on Unclaim.
			world_hm.put("isDeletingEntitiesOnUnclaim", isDeletingEntitiesOnUnclaim());
			if (getUnclaimDeleteEntityTypes() != null)
				world_hm.put("unclaimDeleteEntityTypes", StringMgmt.join(getUnclaimDeleteEntityTypes(), "#"));


			// Using PlotManagement Mayor Delete
			world_hm.put("usingPlotManagementMayorDelete", isUsingPlotManagementMayorDelete());
			// Plot Management Mayor Delete
			if (getPlotManagementMayorDelete() != null)
				world_hm.put("plotManagementMayorDelete", StringMgmt.join(getPlotManagementMayorDelete(), "#"));

			// Using PlotManagement Revert
			world_hm.put("usingPlotManagementRevert", isUsingPlotManagementRevert());

			// Plot Management Ignore Ids
			if (getPlotManagementIgnoreIds() != null)
				world_hm.put("plotManagementIgnoreIds", StringMgmt.join(getPlotManagementIgnoreIds(), "#"));

			world_hm.put("revertOnUnclaimWhitelistMaterials", StringMgmt.join(getRevertOnUnclaimWhitelistMaterials(), "#"));
			
			// Using PlotManagement Wild Regen
			world_hm.put("usingPlotManagementWildRegen", isUsingPlotManagementWildEntityRevert());

			// Wilderness Explosion Protection entities
			if (getPlotManagementWildRevertEntities() != null)
				world_hm.put("PlotManagementWildRegenEntities", StringMgmt.join(getPlotManagementWildRevertEntities(), "#"));

			// Wilderness Explosion Protection Block Whitelist
			if (getPlotManagementWildRevertBlockWhitelist() != null)
				world_hm.put("PlotManagementWildRegenBlockWhitelist", StringMgmt.join(getPlotManagementWildRevertBlockWhitelist(), "#"));

			world_hm.put("wildRegenBlocksToNotOverwrite", StringMgmt.join(getWildRevertMaterialsToNotOverwrite(), "#"));

			// Using PlotManagement Wild Regen Delay
			world_hm.put("plotManagementWildRegenSpeed", getPlotManagementWildRevertDelay());
			
			// Using PlotManagement Wild Block Regen
			world_hm.put("usingPlotManagementWildRegenBlocks", isUsingPlotManagementWildBlockRevert());

			// Wilderness Explosion Protection blocks
			if (getPlotManagementWildRevertBlocks() != null)
				world_hm.put("PlotManagementWildRegenBlocks", StringMgmt.join(getPlotManagementWildRevertBlocks(), "#"));

			world_hm.put("usingTowny", isUsingTowny());
			world_hm.put("warAllowed", isWarAllowed());
			world_hm.put("jailing", isJailingEnabled());
			world_hm.put("metadata", hasMeta() ? serializeMetadata(this) : "");

			return world_hm;

		} catch (Exception e) {
			throw new ObjectSaveException("An exception occurred when constructing data for world " + getName() + " (" + getUUID() + "), caused by: " + e.getMessage());
		}
	}

	public boolean load(Map<String, String> worldAsMap) {
		String line = "";
		try {
			setClaimable(getOrDefault(worldAsMap,"claimable", true));
			setPVP(getOrDefault(worldAsMap, "pvp", TownySettings.isPvP()));
			setForcePVP(getOrDefault(worldAsMap, "forcepvp", TownySettings.isForcingPvP()));
			setForceTownMobs(getOrDefault(worldAsMap, "forcetownmobs", TownySettings.isForcingMonsters()));
			setFriendlyFire(getOrDefault(worldAsMap, "friendlyFire", TownySettings.isFriendlyFireEnabled()));
			setWorldMobs(getOrDefault(worldAsMap, "worldmobs", TownySettings.isWorldMonstersOn()));
			setWildernessMobs(getOrDefault(worldAsMap, "wildernessmobs", TownySettings.isWildernessMonstersOn()));
			setFire(getOrDefault(worldAsMap, "firespread", TownySettings.isFire()));
			setForceFire(getOrDefault(worldAsMap, "forcefirespread", TownySettings.isForcingFire()));
			setExpl(getOrDefault(worldAsMap, "explosions", TownySettings.isExplosions()));
			setForceExpl(getOrDefault(worldAsMap, "forceexplosions", TownySettings.isForcingExplosions()));
			setEndermanProtect(getOrDefault(worldAsMap, "endermanprotect", TownySettings.getEndermanProtect()));
			setDisableCreatureTrample(getOrDefault(worldAsMap, "disablecreaturetrample", TownySettings.isCreatureTramplingCropsDisabled()));
			setUnclaimedZoneBuild(getOrDefault(worldAsMap, "unclaimedZoneBuild", TownySettings.getUnclaimedZoneBuildRights()));
			setUnclaimedZoneDestroy(getOrDefault(worldAsMap, "unclaimedZoneDestroy", TownySettings.getUnclaimedZoneDestroyRights()));
			setUnclaimedZoneSwitch(getOrDefault(worldAsMap, "unclaimedZoneSwitch", TownySettings.getUnclaimedZoneSwitchRights()));
			setUnclaimedZoneItemUse(getOrDefault(worldAsMap, "unclaimedZoneItemUse", TownySettings.getUnclaimedZoneItemUseRights()));
			setUnclaimedZoneName(worldAsMap.getOrDefault("unclaimedZoneName", TownySettings.getUnclaimedZoneName()));
			setUnclaimedZoneIgnore(toList(worldAsMap.get("unclaimedZoneIgnoreIds")));
			setUsingPlotManagementDelete(getOrDefault(worldAsMap, "usingPlotManagementDelete", TownySettings.isUsingPlotManagementDelete()));
			setPlotManagementDeleteIds(toList(worldAsMap.get("plotManagementDeleteIds")));
			setDeletingEntitiesOnUnclaim(getOrDefault(worldAsMap, "isDeletingEntitiesOnUnclaim", TownySettings.isDeletingEntitiesOnUnclaim()));
			setUnclaimDeleteEntityTypes(toList(worldAsMap.get("unclaimDeleteEntityTypes")));
			setUsingPlotManagementMayorDelete(getOrDefault(worldAsMap, "usingPlotManagementMayorDelete", TownySettings.isUsingPlotManagementMayorDelete()));
			setPlotManagementMayorDelete(toList(worldAsMap.get("plotManagementMayorDelete")));
			setUsingPlotManagementRevert(getOrDefault(worldAsMap, "usingPlotManagementRevert", TownySettings.isUsingPlotManagementRevert()));
			setPlotManagementIgnoreIds(toList(worldAsMap.get("plotManagementIgnoreIds")));
			setRevertOnUnclaimWhitelistMaterials(toList(worldAsMap.get("revertOnUnclaimWhitelistMaterials")));
			setUsingPlotManagementWildEntityRevert(getOrDefault(worldAsMap, "usingPlotManagementWildRegen", TownySettings.isUsingPlotManagementWildEntityRegen()));
			setPlotManagementWildRevertEntities(toList(worldAsMap.get("PlotManagementWildRegenEntities")));
			setPlotManagementWildRevertBlockWhitelist(toList(worldAsMap.get("PlotManagementWildRegenBlockWhitelist")));
			setWildRevertMaterialsToNotOverwrite(toList(worldAsMap.get("wildRegenBlocksToNotOverwrite")));
			setPlotManagementWildRevertDelay(getOrDefault(worldAsMap, "plotManagementWildRegenSpeed", TownySettings.getPlotManagementWildRegenDelay()));
			setUsingPlotManagementWildBlockRevert(getOrDefault(worldAsMap, "usingPlotManagementWildRegenBlocks", TownySettings.isUsingPlotManagementWildBlockRegen()));
			setPlotManagementWildRevertMaterials(toList(worldAsMap.get("PlotManagementWildRegenBlocks")));
			setUsingTowny(getOrDefault(worldAsMap, "usingTowny", TownySettings.isUsingTowny()));
			setWarAllowed(getOrDefault(worldAsMap, "warAllowed", TownySettings.isWarAllowed()));
			line = worldAsMap.get("metadata");
			if (hasData(line))
				MetadataLoader.getInstance().deserializeMetadata(this, line.trim());
			setJailingEnabled(getOrDefault(worldAsMap, "jailing", TownySettings.isWorldJailingEnabled()));

			TownyUniverse.getInstance().registerTownyWorld(this);
			if (exists())
				save();
		} catch (Exception e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, Translation.of("flatfile_err_exception_reading_world_file_at_line", getName(), line, getUUID().toString()), e);
			return false;
		}
		return true;
	}

	@ApiStatus.Internal
	@Override
	public boolean exists() {
		return TownyUniverse.getInstance().hasTownyWorld(getName());
	}
}
