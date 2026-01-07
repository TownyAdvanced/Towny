package com.palmergames.bukkit.towny.object;

import com.google.common.base.Preconditions;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
import com.palmergames.bukkit.towny.event.TownyObjectFormattedNameEvent;
import com.palmergames.bukkit.towny.event.town.TownPreRemoveResidentEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.gui.SelectionGUI.SelectionType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.metadata.BooleanDataField;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.scheduling.ScheduledTask;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

public class Resident extends TownyObject implements InviteReceiver, EconomyHandler, TownBlockOwner, Identifiable, ForwardingAudience.Single {
	private List<Resident> friends = new ArrayList<>();
	// private List<Object[][][]> regenUndo = new ArrayList<>(); // Feature is disabled as of MC 1.13, maybe it'll come back.
	private final UUID uuid;
	private Town town = null;
	private long lastOnline;
	private long registered;
	private long joinedTownAt;
	private boolean isNPC = false;
	private String title = "";
	private String surname = "";
	private String about = TownySettings.getDefaultResidentAbout();
	private transient Confirmation confirmation;
	private final transient List<Invite> receivedInvites = new ArrayList<>();
	private transient EconomyAccount account;
	private Jail jail = null;
	private int jailCell;
	private int jailHours;
	private double jailBail;

	private final List<String> townRanks = new ArrayList<>();
	private final List<String> nationRanks = new ArrayList<>();
	private List<TownBlock> townBlocks = new ArrayList<>();
	private final TownyPermission permissions = new TownyPermission();

	private ArrayList<Inventory> guiPages;
	private int guiPageNum = 0;
	private SelectionType guiSelectionType;
	private ScheduledTask respawnProtectionTask = null;
	private boolean respawnPickupWarningShown = false; // Prevents chat spam when a player attempts to pick up an item while under respawn protection.
	private String plotGroupName = null;
	private String districtName = null;
	protected CachedTaxOwing cachedTaxOwing = null;

	@ApiStatus.Internal
	public Resident(String name, UUID uuid) {
		super(name);
		this.uuid = uuid;
		permissions.loadDefault(this);
	}

	@Deprecated(since = "0.102.0.4")
	public Resident(String name) {
		this(name, UUID.randomUUID());
	}

	@Override
	public boolean equals(Object other) {
		if (other == this)
			return true;
		if (!(other instanceof Resident otherResident))
			return false;
		return this.getUUID().equals(otherResident.getUUID());
	}

	@Override
	public int hashCode() {
		return this.uuid.hashCode();
	}

	public void setLastOnline(long lastOnline) {

		this.lastOnline = lastOnline;
	}

	public long getLastOnline() {

		return lastOnline;
	}

	public void setNPC(boolean isNPC) {

		this.isNPC = isNPC;
	}

	public boolean isNPC() {

		return isNPC;
	}
	
	@Override
	public UUID getUUID() {
		return uuid;		
	}
	
	/**
	 * @deprecated Changing UUIDs of Resident objects is no longer supported.
	 */
	@Deprecated(since = "0.102.0.4")
	@Override
	public void setUUID(UUID uuid) {
	}
	
	public boolean hasUUID() {
		return this.uuid != null;
	}

	public Jail getJail() {
		return jail;
	}

	public void setJail(Jail jail) {
		this.jail = jail;
	}

	public boolean isJailed() {

		return jail != null;
	}
	
	public int getJailCell() {
		return jailCell;
	}
	
	public void setJailCell(int i) {
		if (jail.hasJailCell(i))
			jailCell = i;
		else
			jailCell = 0;
	}
	
	public Town getJailTown() {
		return jail.getTown();
	}

	public boolean hasJailTown(String jailtown) {
		return getJailTown().getName().equalsIgnoreCase(jailtown);
	}
	
	public int getJailHours() {
		return jailHours;
	}
	
	public void setJailHours(Integer hours) {
		jailHours = hours;
	}

	public double getJailBailCost() {
		return jailBail;
	}

	public void setJailBailCost(double bail) {
		jailBail = bail;
	}
	
	public boolean hasJailTime() {
		return jailHours > 0;
	}
	
	public Location getJailSpawn() {
		return getJail().getJailCellLocations().get(getJailCell());
	}

	public String getPrimaryRankPrefix() {
		return TownyPerms.getResidentPrimaryRankPrefix(this);
	}

	public void setTitle(String title) {
		this.title = title.trim();
	}

	public String getTitle() {

		return title;
	}

	public boolean hasTitle() {

		return !title.isEmpty();
	}

	public void setSurname(String surname) {
		this.surname = surname.trim();
	}

	public String getSurname() {

		return surname;
	}

	public boolean hasSurname() {

		return !surname.isEmpty();
	}
	
	public void setAbout(@NotNull String about) {
		Preconditions.checkNotNull(about, "about");
		this.about = about;
	}
	
	@NotNull
	public String getAbout() {
		return about;
	}

	public boolean isKing() {

		return hasNation() && town.getNationOrNull().isKing(this);
	}

	public boolean isMayor() {

		return hasTown() && town.isMayor(this);
	}

	public boolean hasTown() {

		return town != null;
	}

	public boolean hasNation() {

		return hasTown() && town.hasNation();
	}

	public Town getTown() throws NotRegisteredException {

		if (hasTown())
			return town;
		else
			throw new NotRegisteredException(Translation.of("msg_err_resident_doesnt_belong_to_any_town"));
	}
	
	/**
	 * Relatively safe to use after confirming there is a town using
	 * {@link #hasTown()}.
	 * 
	 * @return Town the resident belongs to or null.
	 */
	@Nullable 
	public Town getTownOrNull() {
		return town;
	}

	public void setTown(Town town) throws AlreadyRegisteredException {
		setTown(town, true);
	}

	public void setTown(Town town, boolean updateJoinedAt) throws AlreadyRegisteredException {

		if (this.town == town)
			return;

		Towny.getPlugin().deleteCache(this);
		setTitle("");
		setSurname("");

		if (town == null) {
			this.town = null;
			updatePerms();
			return;
		}

		if (hasTown())
			town.addResidentCheck(this);

		this.town = town;
		updatePerms();
		town.addResident(this);

		if (updateJoinedAt) {
			setJoinedTownAt(System.currentTimeMillis());
			BukkitTools.fireEvent(new TownAddResidentEvent(this, town));
		}
	}
	
	public void removeTown() {
		removeTown(false);
	}

	public void removeTown(boolean townDeleted) {
		if (!hasTown())
			return;

		Town town = this.town;
		
		BukkitTools.fireEvent(new TownPreRemoveResidentEvent(this, town));

		// Remove any non-embassy plots owned by the player in the town that the resident will leave.
		for (TownBlock townBlock : town.getTownBlocks()) {
			if (townBlock.getType() == TownBlockType.EMBASSY || !townBlock.hasResident(this))
				continue;
			
			if (townBlock.removeResident()) {
				this.townBlocks.remove(townBlock);
				townBlock.setPlotPrice(town.getPlotPrice());

				// Set the plot permissions to mirror the towns.
				townBlock.setType(townBlock.getType());
				townBlock.save();
			}
		}
		
		BukkitTools.fireEvent(new TownRemoveResidentEvent(this, town));

		try {
			town.removeResident(this);
		} catch (EmptyTownException e) {
			if (!townDeleted) {
				TownyMessaging.sendMsg(Translatable.of("msg_town_being_deleted_because_no_residents", town.getName()));
				TownyUniverse.getInstance().getDataSource().removeTown(town, DeleteTownEvent.Cause.NO_RESIDENTS, null, false);
			}
		}

		try {
			setTown(null);
			
		} catch (AlreadyRegisteredException ignored) {
			// It cannot reach the point in the code at which the exception can be thrown.
		}
		
		this.save();
		
		// Reset everyones cache permissions as this player losing their could affect multiple areas
		Towny.getPlugin().resetCache();
	}

	public void setFriends(List<Resident> newFriends) {

		friends = newFriends;
	}

	public List<Resident> getFriends() {
		return Collections.unmodifiableList(friends);
	}

	public void removeFriend(Resident resident) {

		if (hasFriend(resident))
			friends.remove(resident);
	}

	public boolean hasFriend(Resident resident) {

		return friends.contains(resident);
	}

	public void addFriend(Resident resident){

		if (hasFriend(resident) || this.equals(resident) || resident.isNPC())
			return;

		friends.add(resident);
	}

	public void removeAllFriends() {
		// Wipe the array.
		friends.clear();
	}

	public void updatePerms() {
		townRanks.clear();
		nationRanks.clear();
		TownyPerms.assignPermissions(this, null);
	}
	
	public void updatePermsForNationRemoval() {
		nationRanks.clear();
		TownyPerms.assignPermissions(this, null);
	}

	public void setRegistered(long registered) {

		this.registered = registered;
	}

	public long getRegistered() {

		return registered;
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<>();
		out.add(getTreeDepth(depth) + "Resident (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "Registered: " + getRegistered());
		out.add(getTreeDepth(depth + 1) + "Last Online: " + getLastOnline());
		if (getFriends().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Friends (" + getFriends().size() + "): " + Arrays.toString(getFriends().toArray(new Resident[0])));
		return out;
	}

	@ApiStatus.Obsolete
	public long getTeleportRequestTime() {
		TeleportRequest request = TeleportWarmupTimerTask.getTeleportRequest(this);
		
		return request == null ? -1 : request.requestTime();
	}

	@ApiStatus.Obsolete
	public Location getTeleportDestination() {
		TeleportRequest request = TeleportWarmupTimerTask.getTeleportRequest(this);

		return request == null ? null : request.destinationLocation();
	}

	@ApiStatus.Obsolete
	public int getTeleportCooldown() {
		TeleportRequest request = TeleportWarmupTimerTask.getTeleportRequest(this);

		return request == null ? -1 : request.cooldown();
	}

	/**
	 * @return Whether this resident has an active teleport warmup that they're waiting for.
	 */
	public boolean hasRequestedTeleport() {
		return TeleportWarmupTimerTask.hasTeleportRequest(this);
	}

	@ApiStatus.Obsolete
	public double getTeleportCost() {
		TeleportRequest request = TeleportWarmupTimerTask.getTeleportRequest(this);

		return request == null ? 0 : request.teleportCost();
	}

	@ApiStatus.Obsolete
	public Account getTeleportAccount() {
		TeleportRequest request = TeleportWarmupTimerTask.getTeleportRequest(this);

		return request == null ? null : request.teleportAccount();
	}

	//TODO: Restore /tw regen and /tw regen undo functionality.
//	/**
//	 * Push a snapshot to the Undo queue
//	 *
//	 * Old version pre 1.13
//	 * @param snapshot
//	 */
//	public void addUndo(Object[][][] snapshot) {
//
//		if (regenUndo.size() == 5)
//			regenUndo.remove(0);
//		regenUndo.add(snapshot);
//	}
//
//	public void regenUndo() {
//
//		if (regenUndo.size() > 0) {
//			Object[][][] snapshot = regenUndo.get(regenUndo.size() - 1);
//			regenUndo.remove(snapshot);
//
//			TownyRegenAPI.regenUndo(snapshot, this);
//
//		}
//	}	
	
	/**
	 * @param node String representing a PermissionNode to test for.
	 * @return true if the Player of the Resident has the permission node assigned
	 *         to them according to the TownyPermissionSource.
	 */
	public boolean hasPermissionNode(String node) {
		return getPlayer() != null && TownyUniverse.getInstance().getPermissionSource().testPermission(getPlayer(), node);
	}
	
	/**
	 * @return true if the Player of the Resident is a server OP or has the
	 *         towny.admin permission node according to the TownyPermissionSource.
	 */
	public boolean isAdmin() {
		return getPlayer() != null && TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(getPlayer());
	}
	
	public List<String> getModes() {
		return ResidentModeHandler.getResidentModesNames(this);
	}
	
	public boolean hasMode(String mode) {
		return getModes().contains(mode.toLowerCase(Locale.ROOT));
	}

	/**
	 * @deprecated since 0.100.4.6. Use {@link ResidentModeHandler#toggleModes(Resident, String[], boolean, boolean)} instead.
	 * 
	 * @param newModes
	 * @param notify
	 */
	@Deprecated
	public void toggleMode(String[] newModes, boolean notify) {
		ResidentModeHandler.toggleModes(this, newModes, notify, false);
	}

	/**
	 * @deprecated since 0.100.4.6. Use {@link ResidentModeHandler#toggleModes(Resident, String[], boolean, boolean)} instead.
	 * @param modes
	 * @param notify
	 */
	@Deprecated
	public void setModes(String[] modes, boolean notify) {
		ResidentModeHandler.toggleModes(this, modes, notify, false);
	}

	public void clearModes() {
		clearModes(true);
	}

	public void clearModes(boolean notify) {
		ResidentModeHandler.clearModes(this, notify);
	}

	/**
	 * Only for internal Towny use. NEVER call this from any other plugin.
	 *
	 * @param modes - String Array of modes
	 * @param notify - If notifications should be sent
	 */
	public void resetModes(String[] modes, boolean notify) {
		ResidentModeHandler.resetModes(this, notify);
	}
	
	/**
	 * Get the {@link Player} of the Resident when the Resident is Online.
	 * @return the resident's Player when the player is online.
	 */
	@Nullable
	public Player getPlayer() {
		return BukkitTools.getPlayerExact(getName());
	}

	public boolean addTownRank(String rank) {
		if (!hasTownRank(rank)) {
			townRanks.add(rank);
			if (isOnline())
				TownyPerms.assignPermissions(this, null);
			return true;
		}
		return false;
	}

	public void setTownRanks(List<String> ranks) {
		for (String rank : ranks) {
			rank = TownyPerms.matchTownRank(rank);
			if (rank!= null && !this.hasTownRank(rank))
				townRanks.add(rank);
		}
	}

	// Sometimes databases might have mis-matched rank casing.
	public boolean hasTownRank(String rank) {
		rank = TownyPerms.matchTownRank(rank);
		if (rank != null)
			for (String ownedRank : townRanks) {
				if (ownedRank.equalsIgnoreCase(rank))
					return true;
			}
		return false;
	}

	public List<String> getTownRanks() {
		if (TownyPerms.ranksWithTownLevelRequirementPresent() && !townRanks.isEmpty() && hasTown()) {
			ArrayList<String> out = new ArrayList<>();
			int townLevel = getTownOrNull().getLevelNumber();
			for (String rank : new ArrayList<>(townRanks)) {
				int requiredTownLevelForRank = TownyPerms.getRankTownLevelReq(rank);
				if (requiredTownLevelForRank == 0 || townLevel >= requiredTownLevelForRank)
					out.add(rank);
			}
			// Return out modified list of ranks, so that the player will not lose ranks
			// they've been assigned when their town goes down in a level temporarily. This
			// ensures they save and load their proper list of ranks.
			return Collections.unmodifiableList(out);
		}
		return Collections.unmodifiableList(townRanks);
	}

	@ApiStatus.Internal
	public List<String> getTownRanksForSaving() {
		return Collections.unmodifiableList(townRanks);
	}

	public boolean removeTownRank(String rank) {

		if (hasTownRank(rank)) {
			townRanks.remove(rank);
			if (isOnline())
				TownyPerms.assignPermissions(this, null);
			return true;
		}
		return false;
	}

	@Nullable
	public String getHighestPriorityTownRank() {
		if (getTownRanks().isEmpty())
			return null;
		return TownyPerms.getHighestPriorityRank(this, getTownRanks(), r->TownyPerms.getTownRankPermissions(r));
	}

	public boolean addNationRank(String rank) {

		if (!hasNationRank(rank)) {
			nationRanks.add(rank);
			if (isOnline())
				TownyPerms.assignPermissions(this, null);
			return true;
		}

		return false;
	}

	public void setNationRanks(List<String> ranks) {
		for (String rank : ranks) {
			rank = TownyPerms.matchNationRank(rank);
			if (rank != null && !this.hasNationRank(rank))
				nationRanks.add(rank);
		}
	}

	// Sometimes databases might have mis-matched rank casing.
	public boolean hasNationRank(String rank) {
		rank = TownyPerms.matchNationRank(rank);
		if (rank != null)
			for (String ownedRank : nationRanks) {
				if (ownedRank.equalsIgnoreCase(rank))
					return true;
			}
		return false;
	}

	public List<String> getNationRanks() {
		if (TownyPerms.ranksWithNationLevelRequirementPresent() && !nationRanks.isEmpty() && hasNation()) {
			ArrayList<String> out = new ArrayList<>();
			int nationLevel = getNationOrNull().getLevelNumber();
			for (String rank : new ArrayList<>(nationRanks)) {
				int requiredNationLevelForRank = TownyPerms.getRankTownLevelReq(rank);
				if (requiredNationLevelForRank == 0 || nationLevel >= requiredNationLevelForRank)
					out.add(rank);
			}
			// Return out modified list of ranks, so that the player will not lose ranks
			// they've been assigned when their nation goes down in a level temporarily. This
			// ensures they save and load their proper list of ranks.
			return Collections.unmodifiableList(out);
		}
		return Collections.unmodifiableList(nationRanks);
	}

	@ApiStatus.Internal
	public List<String> getNationRanksForSaving() {
		return Collections.unmodifiableList(nationRanks);
	}

	public boolean removeNationRank(String rank){

		if (hasNationRank(rank)) {
			nationRanks.remove(rank);
			if (BukkitTools.isOnline(this.getName()))
				TownyPerms.assignPermissions(this, null);
			return true;
		}

		return false;

	}

	@Nullable
	public String getHighestPriorityNationRank() {
		if (getNationRanks().isEmpty())
			return null;
		return TownyPerms.getHighestPriorityRank(this, getNationRanks(), r->TownyPerms.getNationRankPermissions(r));
	}

	public boolean isAlliedWith(Resident otherresident) {
		return CombatUtil.isAlly(this, otherresident);
	}

	@Override
	public List<Invite> getReceivedInvites() {
		return Collections.unmodifiableList(receivedInvites);
	}

	@Override
	public void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedInvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) { // We only want 10 Invites, for residents, later we can make this number configurable
			// We use 9 because if it is = 9 it adds the tenth
			receivedInvites.add(invite);

		} else {
			throw new TooManyInvitesException(Translation.of("msg_err_player_has_too_many_invites", this.getName()));
		}
	}

	@Override
	public void deleteReceivedInvite(Invite invite) {
		receivedInvites.remove(invite);
	}

	@Override
	public void addMetaData(@NotNull CustomDataField<?> md) {
		this.addMetaData(md, true);
	}

	@Override
	public void removeMetaData(@NotNull CustomDataField<?> md) {
		this.removeMetaData(md, true);
	}

	@Override
	public Account getAccount() {
		if (account == null) {
			String accountName = StringMgmt.trimMaxLength(getName(), 32);

			UUID uuid = this.uuid;
			if (this.isNPC())
				uuid = TownyEconomyHandler.modifyNPCUUID(uuid);

			account = new EconomyAccount(this, accountName, uuid, () -> {
				final Player player = getPlayer();
				if (player != null)
					return TownyAPI.getInstance().getTownyWorld(player.getWorld());
				else
					return TownyUniverse.getInstance().getTownyWorlds().get(0);
			});
		}
		
		return account;
	}
	
	public @Nullable Account getAccountOrNull() {
		return account;
	}

	@Override
	public String getFormattedName() {
		
		String prefix = Colors.translateColorCodes(hasTitle() ? getTitle() + " " : 
			(isKing() && !TownySettings.getKingPrefix(this).isEmpty()) ? TownySettings.getKingPrefix(this) : 
				(isMayor() && !TownySettings.getMayorPrefix(this).isEmpty()) ? TownySettings.getMayorPrefix(this) : "");
		
		String postfix = Colors.translateColorCodes(hasSurname() ? " " + getSurname() : 
			(isKing() && !TownySettings.getKingPostfix(this).isEmpty()) ? TownySettings.getKingPostfix(this) : 
				(isMayor() && !TownySettings.getMayorPostfix(this).isEmpty()) ? TownySettings.getMayorPostfix(this) : "");

		TownyObjectFormattedNameEvent event = new TownyObjectFormattedNameEvent(this, prefix, postfix);
		BukkitTools.fireEvent(event);

		return event.getPrefix() + getName() + event.getPostfix();
	}

	/**
	 * Returns King or Mayor prefix set in the Town and Nation Levels of the config.
	 * 
	 * @return Prefix of a King or Mayor if resident is a king or mayor.
	 */	
	public String getNamePrefix() {
		if (isKing())
			return TownySettings.getKingPrefix(this);
		if (isMayor())
			return TownySettings.getMayorPrefix(this);
		return "";
	}
	
	/**
	 * Returns King or Mayor postfix set in the Town and Nation Levels of the config.
	 * 
	 * @return Postfix of a King or Mayor if resident is a king or mayor.
	 */	
	public String getNamePostfix() {
		if (isKing())
			return TownySettings.getKingPostfix(this);
		if (isMayor())
			return TownySettings.getMayorPostfix(this);
		return "";
	}
	
	public String getFormattedTitleName() {
		if (!hasTitle())
			return getFormattedName();
		else
			return getTitle() + " " + getName();
	}

	public void setTownblocks(Collection<TownBlock> townBlocks) {
		this.townBlocks = new ArrayList<>(townBlocks);
	}

	@Override
	public Collection<TownBlock> getTownBlocks() {
		return Collections.unmodifiableCollection(townBlocks);
	}

	/**
	 * @return All towns that the resident is outlawed in
	 */
	public List<Town> getTownsOutlawedIn() {
		return TownyUniverse.getInstance().getTowns().stream().filter(t -> t.hasOutlaw(this)).collect(Collectors.toList());
	}

	@Override
	public boolean hasTownBlock(TownBlock townBlock) {
		return townBlocks.contains(townBlock);
	}

	@Override
	public void addTownBlock(TownBlock townBlock) throws AlreadyRegisteredException {
		if (hasTownBlock(townBlock))
			throw new AlreadyRegisteredException();
		else
			townBlocks.add(townBlock);
	}

	@Override
	public void removeTownBlock(TownBlock townBlock) {
		townBlocks.remove(townBlock);
	}

	@Override
	public void setPermissions(String line) {
		this.permissions.load(line);
	}

	@Override
	public TownyPermission getPermissions() {
		return permissions;
	}

	public Confirmation getConfirmation() {
		return confirmation;
	}

	public void setConfirmation(Confirmation confirmation) {
		this.confirmation = confirmation;
	}

	/**
	 * @return the current inventory which the player is looking at for the GUIs.
	 */
	public Inventory getGUIPage() {
		return guiPages.get(guiPageNum);
	}

	public ArrayList<Inventory> getGUIPages() {
		return guiPages;
	}
	
	public void setGUIPages(ArrayList<Inventory> inventory) {
		this.guiPages = inventory;
	}
	
	public int getGUIPageNum() {
		return guiPageNum;
	}

	public void setGUIPageNum(int currentInventoryPage) {
		this.guiPageNum = currentInventoryPage;
	}

	public SelectionType getGUISelectionType() {
		return guiSelectionType;
	}

	public void setGUISelectionType(SelectionType selectionType) {
		this.guiSelectionType = selectionType;
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveResident(this);
	}

	/**
	 * Gets a list of Towns which the given resident owns embassy plots in.
	 * @return List of Towns in which the resident owns embassies.
	 */
	public List<Town> getEmbassyTowns() {
		List<Town> townEmbassies = new ArrayList<>();

		for(TownBlock tB : getTownBlocks()) {
			Town town = tB.getTownOrNull();
			if (town == null || townEmbassies.contains(town) || town.hasResident(this))
				continue;
			townEmbassies.add(town);
		}
		return townEmbassies;
	}

	public long getJoinedTownAt() {
		return joinedTownAt;
	}

	public void setJoinedTownAt(long joinedTownAt) {
		this.joinedTownAt = joinedTownAt;
	}
	
	public Nation getNation() throws TownyException {
		return getTown().getNation();
	}
	
	@Nullable
	public Nation getNationOrNull() {
		if (!hasNation())
			return null;
		return getTownOrNull().getNationOrNull();
	}
	
	public boolean isOnline() {
		return BukkitTools.isOnline(getName());
	}

	public boolean hasRespawnProtection() {
		return this.respawnProtectionTask != null && !this.respawnProtectionTask.isCancelled();
	}
	
	public void addRespawnProtection(long protectionTime) {
		if (protectionTime <= 0)
			return;
		
		// Cancel existing respawn protection task without message
		if (respawnProtectionTask != null)
			respawnProtectionTask.cancel();

		respawnPickupWarningShown = false;
		this.respawnProtectionTask = Towny.getPlugin().getScheduler().runAsyncLater(this::removeRespawnProtection, protectionTime);
	}
	
	public void removeRespawnProtection() {
		if (this.respawnProtectionTask == null)
			return;
		
		this.respawnProtectionTask.cancel();
		this.respawnProtectionTask = null;
		TownyMessaging.sendMsg(this, Translatable.of("msg_you_have_lost_your_respawn_protection"));
	}
	
	public boolean isRespawnPickupWarningShown() {
		return this.respawnPickupWarningShown;
	}

	public void setRespawnPickupWarningShown(boolean respawnPickupWarningShown) {
		this.respawnPickupWarningShown = respawnPickupWarningShown;
	}

	@NotNull
	@Override
	public Audience audience() {
		Player player = getPlayer();
		return player == null ? Audience.empty() : player;
	}
	
	public boolean isSeeingBorderTitles() {
		BooleanDataField borderMeta = new BooleanDataField("bordertitles");
		return !MetaDataUtil.hasMeta(this, borderMeta) || MetaDataUtil.getBoolean(this, borderMeta);
	}

	public boolean hasPlotGroupName() {
		return plotGroupName != null;
	}

	public String getPlotGroupName() {
		return plotGroupName;
	}

	public void setPlotGroupName(String plotGroupName) {
		this.plotGroupName = plotGroupName;
	}

	public boolean hasDistrictName() {
		return districtName != null;
	}

	public String getDistrictName() {
		return districtName;
	}

	public void setDistrictName(String districtName) {
		this.districtName = districtName;
	}

	@ApiStatus.Internal
	@Override
	public boolean exists() {
		return TownyUniverse.getInstance().hasResident(getName());
	}

	public double getTaxOwing(boolean useCache) {
		if (useCache)
			return getCachedTaxOwing();

		Town town;
		boolean taxExempt = hasPermissionNode("towny.tax_exempt");
		double plotTax = 0.0;
		double townTax = 0.0;

		if (hasTown() && !taxExempt) {
			town = getTownOrNull();
			townTax = town.getTaxes();
			if (town.isTaxPercentage())
				townTax = Math.min(getAccount().getHoldingBalance() * townTax / 100, town.getMaxPercentTaxAmount());
		}

		if (getTownBlocks().size() > 0) {
			for (TownBlock townBlock : new ArrayList<>(getTownBlocks())) {
				town = townBlock.getTownOrNull();
				if (town == null) // Shouldn't happen but worth checking.
					continue;

				if (taxExempt && town.hasResident(this)) // Resident will not pay any tax for plots owned by their towns.
					continue;
				plotTax += townBlock.getType().getTax(town);
			}
		}
		return plotTax + townTax;
	}


	/**
	 * Returns a cached amount of taxes that a resident will pay daily.
	 *
	 * @return tax {@link Double} which is from a {@link CachedTaxOwing#owing}.
	 */
	public double getCachedTaxOwing() {
		return getCachedTaxOwing(true);
	}

	/**
	 * Returns a cached amount of taxes that a resident will pay daily, with the
	 * ability to only refresh the cache if it is stale.
	 *
	 * @param refreshIfStale when true, if the cache is stale it will update.
	 * @return cachedTaxOwing {@link Double} which is from a {@link CachedTaxOwing#owing}.
	 */
	public synchronized double getCachedTaxOwing(boolean refreshIfStale) {
		if (cachedTaxOwing == null)
			cachedTaxOwing = new CachedTaxOwing(getTaxOwing(false));
		else if (refreshIfStale && cachedTaxOwing.isStale())
			cachedTaxOwing.updateCache();

		return cachedTaxOwing.getOwing();
	}

	class CachedTaxOwing {
		private double owing = 0;
		private long time;

		CachedTaxOwing(double _owing) {
			owing = _owing;
			time = System.currentTimeMillis();
		}

		double getOwing() {
			return owing;
		}

		boolean isStale() {
			return System.currentTimeMillis() - time > TownySettings.getCachedBankTimeout();
		}

		void setOwing(double _balance) {
			owing = _balance;
			time = System.currentTimeMillis();
		}

		void updateCache() {
			time = System.currentTimeMillis();
			TownyEconomyHandler.economyExecutor().execute(() -> setOwing(getTaxOwing(false)));
		}
	}

	public void removeTrustInTown(Town town) {
		if (town.hasTrustedResident(this)) {
			town.removeTrustedResident(this);
			town.save();
		}
		town.getTownBlocks().stream()
			.filter(tb -> tb.hasTrustedResident(this))
			.forEach(tb -> {
				tb.removeTrustedResident(this);
				tb.save();
			});
	}
}
