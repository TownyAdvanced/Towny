package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.PlotChangeTypeEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotClaimEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotPreClaimEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotPreUnclaimEvent;
import com.palmergames.bukkit.towny.event.plot.changeowner.PlotUnclaimEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TownBlock extends TownyObject {

	private TownyWorld world;
	private Town town = null;
	private Resident resident = null;
	private TownBlockType type = TownBlockType.RESIDENTIAL;
	private int x, z;
	private double plotPrice = -1;
	private boolean outpost = false;
	private PlotGroup plotGroup;
	private long claimedAt;
	private Jail jail;
	private Map<Resident, PermissionData> permissionOverrides = new HashMap<>();
	private Set<Resident> trustedResidents = new HashSet<>();

	//Plot level permissions
	protected TownyPermission permissions = new TownyPermission();
	protected boolean isChanged = false;
	
	public TownBlock(int x, int z, TownyWorld world) {
		super("");
		this.x = x;
		this.z = z;
		this.setWorld(world);
	}

	public TownBlock(WorldCoord worldCoord) {
		super("");
		this.x = worldCoord.getX();
		this.z = worldCoord.getZ();
		this.setWorld(worldCoord.getTownyWorld());
	}

	public void setTown(Town town) {
		setTown(town, true);
	}

	public void setTown(Town town, boolean updateClaimedAt) {

		if (hasTown())
			this.town.removeTownBlock(this);
		this.town = town;
		try {
			TownyUniverse.getInstance().addTownBlock(this);
			town.addTownBlock(this);

			if (updateClaimedAt)
				setClaimedAt(System.currentTimeMillis());
			
			permissionOverrides.clear();
		} catch (AlreadyRegisteredException | NullPointerException ignored) {}
	}

	public Town getTown() throws NotRegisteredException {

		if (!hasTown())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", world.getName(), x, z));
		return town;
	}
	
	/**
	 * Relatively safe to use after confirming {@link #hasTown()} is true
	 * or {@link TownyAPI#isWilderness(org.bukkit.Location)} is false.
	 * 
	 * @return Town who owns the TownBlock or null.
	 */
	@Nullable 
	public Town getTownOrNull() {
		return town;
	}

	public boolean hasTown() {

		return town != null;
	}

	/**
	 * Removes the current resident as owner in this plot, while calling the appropriate events
	 * @return Whether the resident (if any) was successfully removed as owner.
	 */
	public boolean removeResident() {
		return setResident(null, true);
	}

	/**
	 * Changes the owner of the plot to the given resident.
	 * @param resident The resident to give ownership to, or {@code null} to give ownership back to the town.
	 * @return Whether the resident (if any) was successfully removed as owner.
	 */
	public boolean setResident(@Nullable Resident resident) {
		return setResident(resident, true);
	}

	/**
	 * Changes the owner of the plot to the given resident.
	 * @param resident The resident to give ownership to, or {@code null} to give ownership back to the town.
	 * @param callEvent Whether to call the related plot events or not, this is used by Towny to avoid calling events from database loading.   
	 * @return Whether the resident (if any) was successfully removed as owner.
	 */
	public boolean setResident(@Nullable Resident resident, boolean callEvent) {
		
		if (callEvent) {
			
			// There is a resident here already who is being replaced.
			if (this.resident != null && !this.resident.equals(resident)) {
				PlotPreUnclaimEvent plotPreUnclaimEvent = new PlotPreUnclaimEvent(this.resident, resident, this);
				if (BukkitTools.isEventCancelled(plotPreUnclaimEvent)) {
					if (!plotPreUnclaimEvent.getCancelMessage().isEmpty()) { 
						if (this.resident != null)
							TownyMessaging.sendErrorMsg(this.resident, plotPreUnclaimEvent.getCancelMessage());
						if (resident != null) 
							TownyMessaging.sendErrorMsg(resident, plotPreUnclaimEvent.getCancelMessage());
						}
					return false;
				}
			}
			
			// This is being claimed by a resident.
			if (resident != null && !resident.equals(this.resident)) {
				PlotPreClaimEvent plotPreClaimEvent = new PlotPreClaimEvent(this.resident, resident, this);
				if (BukkitTools.isEventCancelled(plotPreClaimEvent)) {
					if (!plotPreClaimEvent.getCancelMessage().isEmpty() && resident != null)
						TownyMessaging.sendErrorMsg(resident, plotPreClaimEvent.getCancelMessage());
	
					return false;
				}
			}
		}
		
		boolean successful = false;
		boolean unclaim = false;
		if (hasResident()) {
			this.resident.removeTownBlock(this);
			unclaim = true;
			getTownOrNull().getTownBlockTypeCache().removeTownBlockOfTypeResidentOwned(this);
		}
		this.resident = resident;
		if (resident != null && !resident.hasTownBlock(this)) {
			try {
				resident.addTownBlock(this);
				successful = true;
				getTownOrNull().getTownBlockTypeCache().addTownBlockOfTypeResidentOwned(this);
			} catch (AlreadyRegisteredException ignored) {}
		}
		
		if (successful && callEvent)
			BukkitTools.fireEvent(new PlotClaimEvent(this.resident, resident, this));
		
		if (unclaim && callEvent)
			BukkitTools.fireEvent(new PlotUnclaimEvent(this.resident, resident, this));
		
		permissionOverrides.clear();
		
		return true;
	}

	public Resident getResident() throws NotRegisteredException {

		if (!hasResident())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a resident.", world.getName(), x, z));
		return resident;
	}

	/**
	 * Relatively safe to use after testing {@link #hasResident()}.
	 * 
	 * @return Resident who personally owns the TownBlock or null.
	 */
	@Nullable
	public Resident getResidentOrNull() {
		return resident;
	}
	
	public boolean hasResident() {

		return resident != null;
	}

	public boolean isOwner(TownBlockOwner owner) {

		if (hasTown() && owner == getTownOrNull())
			return true;

		if (hasResident() && owner == getResidentOrNull())
			return true;

		return false;
	}

	public void setPlotPrice(double price) {
		if (isForSale() && price == -1.0)
			// Plot is no longer for sale.
			getTownOrNull().getTownBlockTypeCache().removeTownBlockOfTypeForSale(this);
		else if (!isForSale() && price > -1.0)
			// Plot is being put up for sale.
			getTownOrNull().getTownBlockTypeCache().addTownBlockOfTypeForSale(this);

		this.plotPrice = price;
	}

	public double getPlotPrice() {

		return plotPrice;
	}

	public boolean isForSale() {

		return getPlotPrice() != -1.0;
	}

	public void setPermissions(String line) {

		//permissions.reset(); not needed, already done in permissions.load()
		permissions.load(line);
	}

	public TownyPermission getPermissions() {

		/*
		 * Return our perms
		 */
		return permissions;
	}

	/**
	 * Have the permissions been manually changed.
	 * 
	 * @return the isChanged
	 */
	public boolean isChanged() {

		return isChanged;
	}

	/**
	 * Flag the permissions as changed.
	 * 
	 * @param isChanged the isChanged to set
	 */
	public void setChanged(boolean isChanged) {

		this.isChanged = isChanged;
	}

	/**
	 * @return the outpost
	 */
	public boolean isOutpost() {

		return outpost;
	}

	/**
	 * @param outpost the outpost to set
	 */
	public void setOutpost(boolean outpost) {

		this.outpost = outpost;
	}

	public TownBlockType getType() {
		return type;
	}
	
	public String getTypeName() {
		return type.getName();
	}
	
	public void setType(@NotNull String type) {
		setType(TownBlockTypeHandler.getType(type));
	}
	
	public void setType(@Nullable TownBlockType type) {
		if (type == null || !TownBlockTypeHandler.exists(type.getName()))
			type = TownBlockType.RESIDENTIAL;
		
		if (!type.equals(this.type))
			this.permissions.reset();		

		if (getTownOrNull() != null)
			adjustTownBlockTypeCache(getTownOrNull().getTownBlockTypeCache(), type);

		this.type = type;
		
		BukkitTools.fireEvent(new PlotChangeTypeEvent(this.type, type, this));

		switch (type.getName().toLowerCase()) {
			case "default":
			case "shop":
			case "embassy":
			case "bank":
			case "inn":
				if (this.hasResident()) {
					setPermissions(this.resident.getPermissions().toString());
				} else {
					setPermissions(this.town.getPermissions().toString());
				}

				break;
			case "arena":
				setPermissions("pvp");
				break; 
			case "jail":
				setPermissions("denyAll");
				break;
			case "farm":
			case "wilds":
				setPermissions("residentBuild,residentDestroy");
				break;
			default:
				break;
		}
		
		// Set the changed status.
		this.setChanged(false);
	}

	/**
	 * @param type The TownBlockType to set this plot to.
	 * @param resident The Resident who is trying to set the type.
	 * @throws TownyException If this townblock has a pvp toggle cooldown.
	 */
	public void setType(TownBlockType type, Resident resident) throws TownyException {
		
		int typeLimit = town.getTownBlockTypeLimit(type);
		if (typeLimit >= 0 && (typeLimit == 0 || town.getTownBlockTypeCache().getNumTownBlocks(type, TownBlockTypeCache.CacheType.ALL) >= typeLimit))
			throw new TownyException(Translatable.of("msg_town_plot_type_limit_reached", typeLimit, type.getFormattedName()));
		
		// Delete a jail if this is no longer going to be a jail.
		if (this.isJail() && !TownBlockType.JAIL.equals(type) && getJail() != null) {
			TownyUniverse.getInstance().getDataSource().removeJail(getJail());
			setJail(null);
		}

		if (TownBlockType.ARENA.equals(this.type) || TownBlockType.ARENA.equals(type)
			&& TownySettings.getPVPCoolDownTime() > 0
			&& !TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(resident.getPlayer())) {
			// Test to see if this town is on pvp cooldown.
			if (CooldownTimerTask.hasCooldown(town.getUUID().toString(), CooldownType.PVP))
				throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(town.getUUID().toString(), CooldownType.PVP)));
			// Test to see if the pvp cooldown timer is active for this plot.
			if (CooldownTimerTask.hasCooldown(getWorldCoord().toString(), CooldownType.PVP))
				throw new TownyException(Translation.of("msg_err_cannot_toggle_pvp_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(getWorldCoord().toString(), CooldownType.PVP)));

			setType(type);
			CooldownTimerTask.addCooldownTimer(getWorldCoord().toString(), CooldownType.PVP);
		} else
			setType(type);

		if (this.isJail() && resident.getPlayer() != null)
			JailUtil.createJailPlot(this, getTown(), resident.getPlayer().getLocation());

		this.save();
	}
	
	/**
	 * Towns track TownBlockTypes in a TownBlockTypeCache, allowing Towny to keep
	 * track of how many plots of each type, their forSale status and their
	 * hasResident status. When a TownBlock changes type the caches are adjusted for
	 * the Town.
	 * 
	 * @param townBlockTypeCache TownBlockTypeCache which is being adjusted.
	 * @param type               TownBlockType which is being set.
	 */
	private void adjustTownBlockTypeCache(@Nullable TownBlockTypeCache townBlockTypeCache, @NotNull TownBlockType type) {
		if (townBlockTypeCache == null)
			return;

		if (this.type != null) {
			// This townblock is not brand new, and not being loaded from the database,
			// we must remove the previous type from the caches.
			townBlockTypeCache.removeTownBlockOfType(this.type);
			if (isForSale())
				townBlockTypeCache.removeTownBlockOfTypeForSale(this.type);
			if (hasResident())
				townBlockTypeCache.removeTownBlockOfTypeResidentOwned(this.type);
		}

		// Add the townblock type to the Town's TownBlockTypeCache.
		townBlockTypeCache.addTownBlockOfType(type);
		if (isForSale())
			townBlockTypeCache.addTownBlockOfTypeForSale(type);
		if (hasResident())
			townBlockTypeCache.addTownBlockOfTypeResidentOwned(type);
	}

	public boolean isHomeBlock() {
		return hasTown() && getTownOrNull().isHomeBlock(this);
	}
	
	@Override
	public void setName(String newName) {
		super.setName(newName.replace("_", " ")); 
	}

	public void setX(int x) {

		this.x = x;
	}

	public int getX() {

		return x;
	}

	public void setZ(int z) {

		this.z = z;
	}

	public int getZ() {

		return z;
	}

	public Coord getCoord() {

		return new Coord(x, z);
	}

	public WorldCoord getWorldCoord() {

		return new WorldCoord(world.getName(), world.getUUID(), x, z);
	}

	/**
	 * Is the TownBlock locked
	 * 
	 * @deprecated as of 0.98.6.25, Towny will no longer block town blocks while taking snapshots.
	 * @return the locked
	 */
	@Deprecated
	public boolean isLocked() {
		return false;
	}

	/**
	 * @param locked is the to locked to set
	 * @deprecated as of 0.98.6.25, Towny will no longer block town blocks while taking snapshots.
	 */
	public void setLocked(boolean locked) {
	}

	public void setWorld(TownyWorld world) {

		this.world = world;
	}

	public TownyWorld getWorld() {

		return world;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TownBlock townBlock = (TownBlock) o;
		return x == townBlock.x &&
			z == townBlock.z &&
			world.equals(townBlock.world);
	}

	@Override
	public int hashCode() {
		return Objects.hash(world, x, z);
	}

	public void clear() {

		setTown(null);
		setResident(null);
		setWorld(null);
	}

	@Override
	public String toString() {

		return getWorld().getName() + " (" + getCoord() + ")";
	}

	public boolean isJail() {

		return this.getType() == TownBlockType.JAIL;
	}
	
	public Jail getJail() {
		return jail;
	}

	public void setJail(Jail _jail) {
		jail = _jail;
	}

	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}
	
	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
	}
	
	public boolean hasPlotObjectGroup() { return plotGroup != null; }
	
	public PlotGroup getPlotObjectGroup() {
		return plotGroup;
	}
	
	public void removePlotObjectGroup() {
		this.plotGroup = null;
	}

	public void setPlotObjectGroup(PlotGroup group) {
		this.plotGroup = group;

		try {
			group.addTownBlock(this);
			setTrustedResidents(group.getTrustedResidents());
			setPermissionOverrides(group.getPermissionOverrides());
		} catch (NullPointerException e) {
			TownyMessaging.sendErrorMsg("Townblock failed to setPlotObjectGroup(group), group is null. ");
		}
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveTownBlock(this);
	}

	public long getClaimedAt() {
		return claimedAt;
	}

	public void setClaimedAt(long claimedAt) {
		this.claimedAt = claimedAt;
	}

	public Map<Resident, PermissionData> getPermissionOverrides() {
		return permissionOverrides;
	}

	public void addTrustedResidents(List<Resident> residents) {
		residents.stream().forEach(r -> addTrustedResident(r));
	}

	public Set<Resident> getTrustedResidents() {
		return trustedResidents;
	}
	
	public boolean hasTrustedResident(Resident resident) {
		return trustedResidents.contains(resident);
	}
	
	public void addTrustedResident(Resident resident) {
		trustedResidents.add(resident);
	}
	
	public void removeTrustedResident(Resident resident) {
		trustedResidents.remove(resident);
	}
	
	public boolean hasResident(Resident resident) {
		if (this.resident == null || resident == null)
			return false;
		
		return resident.equals(this.resident);
	}

	public void setTrustedResidents(Set<Resident> trustedResidents) {
		this.trustedResidents = new HashSet<>(trustedResidents);
	}

	public void setPermissionOverrides(Map<Resident, PermissionData> permissionOverrides) {
		this.permissionOverrides = new HashMap<>(permissionOverrides);
	}

	/**
	 * Returns the TownBlockOwner: a resident (if the plot is personally-owned,)
	 * or the Town that owns the townblock.
	 * @return TownBlockOwner or null (highly unlikely.)
	 */
	@Nullable
	public TownBlockOwner getTownBlockOwner() {
		if (hasResident())
			return getResidentOrNull();
		else 
			return getTownOrNull();
	}
	
	public TownBlockData getData() {
		return type.getData();
	}
	
	public void evictOwnerFromTownBlock() {
		this.setResident(null);
		this.setPlotPrice(-1);
		this.setType(getType());
		this.save();
	}
}