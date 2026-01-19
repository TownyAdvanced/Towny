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
import com.palmergames.util.TimeTools;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class TownBlock extends TownyObject {

	private Town town = null;
	private Resident resident = null;
	private int minTownMembershipDays = -1;
	private int maxTownMembershipDays = -1;
	private TownBlockType type = TownBlockType.RESIDENTIAL;
	private final WorldCoord worldCoord;
	private double plotPrice = -1;
	private boolean taxed = true;
	private boolean outpost = false;
	private PlotGroup plotGroup;
	private District district;
	private long claimedAt;
	private Jail jail;
	private @Nullable Map<Resident, PermissionData> permissionOverrides = null;
	private @Nullable Set<Resident> trustedResidents = null;

	//Plot level permissions
	protected TownyPermission permissions = new TownyPermission();
	protected boolean isChanged = false;
	
	public TownBlock(int x, int z, TownyWorld world) {
		super("");
		this.worldCoord = new WorldCoord(world.getName(), world.getUUID(), x, z);
	}

	public TownBlock(WorldCoord worldCoord) {
		super("");
		this.worldCoord = worldCoord;
	}

	public void setTown(Town town) {
		setTown(town, true);
	}

	public void setTown(Town town, boolean updateClaimedAt) {

		if (hasTown()) {
			this.town.removeTownBlock(this);
			this.setTaxed(true);
		}
		this.town = town;
		try {
			TownyUniverse.getInstance().addTownBlock(this);
			town.addTownBlock(this);

			if (updateClaimedAt)
				setClaimedAt(System.currentTimeMillis());
			
			permissionOverrides = null;
			minTownMembershipDays = -1;
			maxTownMembershipDays = -1;
		} catch (AlreadyRegisteredException | NullPointerException ignored) {}
	}

	public Town getTown() throws NotRegisteredException {

		if (!hasTown())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", getWorld().getName(), getX(), getZ()));
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
					if (!plotPreClaimEvent.getCancelMessage().isEmpty())
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
			this.town.getTownBlockTypeCache().removeTownBlockOfTypeResidentOwned(this);
			new ArrayList<>(this.getTrustedResidents()).forEach(this::removeTrustedResident);
		}
		this.resident = resident;
		if (resident != null && !resident.hasTownBlock(this)) {
			try {
				resident.addTownBlock(this);
				successful = true;
				this.town.getTownBlockTypeCache().addTownBlockOfTypeResidentOwned(this);
			} catch (AlreadyRegisteredException ignored) {}
		}
		
		if (successful && callEvent)
			BukkitTools.fireEvent(new PlotClaimEvent(this.resident, resident, this));
		
		if (unclaim && callEvent)
			BukkitTools.fireEvent(new PlotUnclaimEvent(this.resident, resident, this));
		
		permissionOverrides = null;
		
		return true;
	}

	public Resident getResident() throws NotRegisteredException {

		if (!hasResident())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a resident.", getWorld().getName(), getX(), getZ()));
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

	public boolean isOwner(@NotNull TownBlockOwner owner) {

		if (hasTown() && owner == getTownOrNull())
			return true;

		return hasResident() && owner == getResidentOrNull();
	}

	public void setPlotPrice(double price) {
		if (this.town != null) {
			if (isForSale() && price < 0)
				// Plot is no longer for sale.
				this.town.getTownBlockTypeCache().removeTownBlockOfTypeForSale(this);
			else if (!isForSale() && price >= 0)
				// Plot is being put up for sale.
				this.town.getTownBlockTypeCache().addTownBlockOfTypeForSale(this);
		}
		
		if (price < 0)
			price = -1;

		this.plotPrice = price;
	}

	public double getPlotPrice() {

		return plotPrice;
	}

	public boolean isForSale() {

		return getPlotPrice() >= 0.0;
	}

	public boolean isTaxed() {
		return taxed;
	}

	public void setTaxed(boolean value) {
		this.taxed = value;
	}

	public double getPlotTax() {
		return getType().getTax(town);
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

		switch (type.getName().toLowerCase(Locale.ROOT)) {
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
			case "default":
			case "shop":
			case "embassy":
			case "bank":
			case "inn":
			default: // Any custom TownBlockTypes will also get caught here and reset to the town/resident default.
				if (this.hasResident()) {
					setPermissions(this.resident.getPermissions().toString());
				} else {
					setPermissions(this.town.getPermissions().toString());
				}
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
		return this.town != null && this.town.isHomeBlock(this);
	}
	
	@Override
	public void setName(String newName) {
		super.setName(newName.replace("_", " ")); 
	}

	public int getX() {

		return this.worldCoord.getX();
	}

	public int getZ() {

		return this.worldCoord.getZ();
	}

	public Coord getCoord() {

		return this.worldCoord;
	}

	public WorldCoord getWorldCoord() {

		return this.worldCoord;
	}

	public TownyWorld getWorld() {

		return this.worldCoord.getTownyWorld();
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		TownBlock townBlock = (TownBlock) o;
		return this.worldCoord.equals(townBlock.worldCoord);
	}

	@Override
	public int hashCode() {
		return Objects.hash(getWorld(), getX(), getZ());
	}

	public void clear() {

		setTown(null);
		removeResident();
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


	public boolean hasDistrict() { return district != null; }
	
	public District getDistrict() {
		return district;
	}
	
	public void removeDistrict() {
		this.district = null;
	}

	public void setDistrict(District district) {
		this.district = district;

		try {
			district.addTownBlock(this);
		} catch (NullPointerException e) {
			TownyMessaging.sendErrorMsg("Townblock failed to setDistrict(district), district is null. ");
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
	
	public boolean hasPermissionOverrides() {
		return this.permissionOverrides != null && !this.permissionOverrides.isEmpty();
	}

	@Nullable
	public PermissionData getPermissionOverride(final Resident resident) {
		if (this.permissionOverrides == null || resident == null) {
			return null;
		}

		return this.permissionOverrides.get(resident);
	}

	public Map<Resident, PermissionData> getPermissionOverrides() {
		if (this.permissionOverrides == null) {
			this.permissionOverrides = new LinkedHashMap<>();
		}

		return permissionOverrides;
	}

	public void addTrustedResidents(List<Resident> residents) {
		residents.forEach(this::addTrustedResident);
	}

	/**
	 * {@return whether this TownBlock has any trusted residents}
	 */
	public boolean hasTrustedResidents() {
		return this.trustedResidents != null && !this.trustedResidents.isEmpty();
	}

	public @NotNull Set<Resident> getTrustedResidents() {
		if (this.trustedResidents == null) {
			this.trustedResidents = new LinkedHashSet<>();
		}

		return trustedResidents;
	}
	
	public boolean hasTrustedResident(Resident resident) {
		return this.trustedResidents != null && this.trustedResidents.contains(resident);
	}
	
	public void addTrustedResident(Resident resident) {
		getTrustedResidents().add(resident);
	}
	
	public void removeTrustedResident(Resident resident) {
		getTrustedResidents().remove(resident);
	}
	
	public boolean hasResident(Resident resident) {
		if (this.resident == null || resident == null)
			return false;
		
		return resident.equals(this.resident);
	}

	public void setTrustedResidents(@Nullable Set<Resident> trustedResidents) {
		this.trustedResidents = trustedResidents == null || trustedResidents.isEmpty() ? null : new LinkedHashSet<>(trustedResidents);
	}

	public void setPermissionOverrides(@Nullable Map<Resident, PermissionData> permissionOverrides) {
		this.permissionOverrides = permissionOverrides == null || permissionOverrides.isEmpty() ? null : new LinkedHashMap<>(permissionOverrides);
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
		evictOwnerFromTownBlock(false);
	}

	public void evictOwnerFromTownBlock(boolean forsale) {
		this.removeResident();
		this.setPlotPrice(forsale ? town.getPlotPrice() : -1);
		this.setType(getType());
		this.save();
	}

	/**
	 * Tests whether a Resident's Town membership age (join date) is too high or
	 * low, preventing them from claiming this TownBlock personally with /plot
	 * claim.
	 * 
	 * @param resident Resident who wants to buy the TownBlock.
	 * @throws TownyException thrown with error message when the Resident is not
	 *                        allowed to claim the land.
	 */
	public void testTownMembershipAgePreventsThisClaimOrThrow(Resident resident) throws TownyException {
		if ((this.getType().equals(TownBlockType.EMBASSY) && !town.hasResident(resident)) 
				|| (!hasMinTownMembershipDays() && !hasMaxTownMembershipDays()))
			return;

		Town residentTown = resident.getTownOrNull();
		if (residentTown == null || !residentTown.equals(this.town))
			return;

		long joinDate = resident.getJoinedTownAt();

		if (hasMaxTownMembershipDays() && TimeTools.getTimeInMillisXDaysAgo(getMaxTownMembershipDays()) > joinDate)
			throw new TownyException(Translatable.of("msg_err_cannot_claim_plot_join_date_too_high", getMaxTownMembershipDays()));

		if (hasMinTownMembershipDays() && TimeTools.getTimeInMillisXDaysAgo(getMinTownMembershipDays()) < joinDate)
			throw new TownyException(Translatable.of("msg_err_cannot_claim_plot_join_date_too_low", getMinTownMembershipDays()));
	}

	/**
	 * @return does this plot have a min number of days the player has to be a
	 *         member of the town, before they can claim?
	 */
	public boolean hasMinTownMembershipDays() {
		return minTownMembershipDays > 0; 
	}

	/**
	 * @return how many days a town member has to be a part of the town in order to
	 *         claim this plot personally using /plot claim.
	 */
	public int getMinTownMembershipDays() {
		return minTownMembershipDays;
	}

	/**
	 * Sets the number of days that a town member must be a part of the town before
	 * they can claim the plot personally using /plot claim.
	 * 
	 * @param minTownMembershipDays days they have to be a part of the town for,
	 *                              before they can claim.
	 */
	public void setMinTownMembershipDays(int minTownMembershipDays) {
		// 32766 because this is stored as a SMALLINT when MYSQL is used.
		this.minTownMembershipDays = Math.min(32766, minTownMembershipDays);
	}

	/**
	 * @return does this plot have a max number of days the player can be a member
	 *         of the town, before they cannot claim?
	 */
	public boolean hasMaxTownMembershipDays() {
		return maxTownMembershipDays > 0; 
	}

	/**
	 * @return how the maximum number of days a town member can be a part of the
	 *         town before they are unable to claim this plot personally using /plot
	 *         claim.
	 */
	public int getMaxTownMembershipDays() {
		return maxTownMembershipDays;
	}

	/**
	 * Sets the maximum number of days that a town member can be a part of the town
	 * before they unable to claim the plot personally using /plot claim.
	 * 
	 * @param maxTownMembershipDays days they can be a part of the town for, until
	 *                              they cannot claim.
	 */
	public void setMaxTownMembershipDays(int maxTownMembershipDays) {
		// 32766 because this is stored as a SMALLINT when MYSQL is used.
		this.maxTownMembershipDays = Math.min(32766, maxTownMembershipDays);
	}

	@ApiStatus.Internal
	@Override
	public boolean exists() {
		return TownyUniverse.getInstance().hasTownBlock(getWorldCoord());
	}
}
