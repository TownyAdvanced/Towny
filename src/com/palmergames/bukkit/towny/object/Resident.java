package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.BaseCommand;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.TownAddResidentEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentEvent;
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
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.towny.utils.MetaDataUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.audience.ForwardingAudience;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Resident extends TownyObject implements InviteReceiver, EconomyHandler, TownBlockOwner, Identifiable, ForwardingAudience.Single {
	private List<Resident> friends = new ArrayList<>();
	// private List<Object[][][]> regenUndo = new ArrayList<>(); // Feature is disabled as of MC 1.13, maybe it'll come back.
	private UUID uuid = null;
	private Town town = null;
	private long lastOnline;
	private long registered;
	private long joinedTownAt;
	private boolean isNPC = false;
	private String title = "";
	private String surname = "";
	private long teleportRequestTime = -1;
	private Location teleportDestination;
	private double teleportCost = 0.0;
	private Account teleportAccount;
	private final List<String> modes = new ArrayList<>();
	private transient Confirmation confirmation;
	private final transient List<Invite> receivedInvites = new ArrayList<>();
	private transient EconomyAccount account;
	private Jail jail = null;
	private int jailCell;
	private int jailHours;

	private final List<String> townRanks = new ArrayList<>();
	private final List<String> nationRanks = new ArrayList<>();
	private List<TownBlock> townBlocks = new ArrayList<>();
	private final TownyPermission permissions = new TownyPermission();

	private ArrayList<Inventory> guiPages;
	private int guiPageNum = 0;
	private SelectionType guiSelectionType;
	private int respawnProtectionTaskID = -1;
	private boolean respawnPickupWarningShown = false; // Prevents chat spam when a player attempts to pick up an item while under respawn protection.

	public Resident(String name) {
		super(name);
		permissions.loadDefault(this);
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
	
	@Override
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
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
	
	public boolean hasJailTime() {
		return jailHours > 0;
	}
	
	public Location getJailSpawn() {
		return getJail().getJailCellLocations().get(getJailCell());
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
			BukkitTools.getPluginManager().callEvent(new TownAddResidentEvent(this, town));
		}
	}
	
	public void removeTown() {
		
		if (!hasTown())
			return;

		Town town = this.town;
		
		BukkitTools.getPluginManager().callEvent(new TownPreRemoveResidentEvent(this, town));
		
		try {
			
			town.removeResident(this);
			
		} catch (NotRegisteredException e1) {
			e1.printStackTrace();
		} catch (EmptyTownException ignore) {
		}

		BukkitTools.getPluginManager().callEvent(new TownRemoveResidentEvent(this, town));

		// Use an iterator to be able to keep track of element modifications.
		Iterator<TownBlock> townBlockIterator = townBlocks.iterator();
		
		while (townBlockIterator.hasNext()) {
			TownBlock townBlock = townBlockIterator.next();

			// Do not remove Embassy plots
			if (townBlock.getType() != TownBlockType.EMBASSY) {
				
				// Make sure the element is removed from the iterator, to 
				// prevent concurrent modification exceptions.
				townBlockIterator.remove();
				townBlock.setResident(null);
				
				townBlock.setPlotPrice(townBlock.getTownOrNull().getPlotPrice());
				// Set the plot permissions to mirror the towns.
				townBlock.setType(townBlock.getType());
				townBlock.save();
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

	public void addFriend(Resident resident) throws AlreadyRegisteredException {

		if (hasFriend(resident) || this.equals(resident) || resident.isNPC())
			throw new AlreadyRegisteredException();
		else
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

	public void clearTeleportRequest() {
		teleportCost = 0;
		teleportRequestTime = -1;
		teleportAccount = null;
	}

	public void setTeleportRequestTime() {

		teleportRequestTime = System.currentTimeMillis();
	}

	public long getTeleportRequestTime() {

		return teleportRequestTime;
	}

	public void setTeleportDestination(Location spawnLoc) {

		teleportDestination = spawnLoc;
	}

	public Location getTeleportDestination() {

		return teleportDestination;
	}

	public boolean hasRequestedTeleport() {

		return teleportRequestTime != -1;
	}

	public void setTeleportCost(double cost) {

		teleportCost = cost;
	}

	public double getTeleportCost() {

		return teleportCost;
	}
	
	public void setTeleportAccount(Account payee) {
		teleportAccount = payee;
	}
	
	public Account getTeleportAccount() {
		return teleportAccount;
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
		return Collections.unmodifiableList(modes);
	}
	
	public boolean hasMode(String mode) {
		return this.modes.contains(mode.toLowerCase());
	}
	
	public void toggleMode(String[] newModes, boolean notify) {

		/*
		 * Toggle any modes passed to us on/off.
		 */
		for (int i = 0; i < newModes.length; i++) {
			String mode = newModes[i].toLowerCase();
			
			Optional<Boolean> choice = Optional.empty();
			if (i + 1 < newModes.length) {
				String bool = newModes[i + 1].toLowerCase();
				if (BaseCommand.setOnOffCompletes.contains(bool)) {
					choice = Optional.of(bool.equals("on"));
					i++;
				}
			}
			
			boolean modeEnabled = this.modes.contains(mode);
			if (choice.orElse(!modeEnabled)) {
				if (!modeEnabled) {
					this.modes.add(mode);
				}
			} else {
				this.modes.remove(mode);
			}
		}
		
		/*
		 *  If we have toggled all modes off we need to set their defaults.
		 */
		if (this.modes.isEmpty()) {

			clearModes();
			return;
		}

		if (notify)
			TownyMessaging.sendMsg(this, Translatable.of("msg_modes_set").append(StringMgmt.join(getModes(), ",")));
	}
	
	public void setModes(String[] modes, boolean notify) {

		this.modes.clear();
		this.toggleMode(modes, false);

		if (notify)
			TownyMessaging.sendMsg(this, Translatable.of("msg_modes_set").append(StringMgmt.join(getModes(), ",")));


	}
	
	public void clearModes() {

		this.modes.clear();
		TownyMessaging.sendMsg(this, (Translatable.of("msg_modes_set")));

		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(this.getName(), true), 1) == -1)
			TownyMessaging.sendErrorMsg(Translation.of("msg_err_could_not_set_default_modes_for") + getName() + ".");

	}
	
	/**
	 * Only for internal Towny use. NEVER call this from any other plugin.
	 *
	 * @param modes - String Array of modes
	 * @param notify - If notifications should be sent
	 */
	public void resetModes(String[] modes, boolean notify) {

		if (modes.length > 0)
			this.toggleMode(modes, false);

		if (notify)
			TownyMessaging.sendMsg(this, Translatable.of("msg_modes_set").append(StringMgmt.join(getModes(), ",")));
	}
	
	@Nullable
	public Player getPlayer() {
		return BukkitTools.getPlayer(getName());
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

	public boolean isAlliedWith(Resident otherresident) {
		if (this.hasNation() && this.hasTown() && otherresident.hasTown() && otherresident.hasNation()) {
			try {
				if (this.getTown().getNation().hasAlly(otherresident.getTown().getNation())) {
					return true;
				} else {
					
					return this.getTown().getNation().equals(otherresident.getTown().getNation());
				}
			} catch (NotRegisteredException e) {
				return false;
			}
		} else {
			return false;
		}
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
			World world;

			Player player = getPlayer();
			if (player != null) {
				world = player.getWorld();
			} else {
				world = BukkitTools.getWorlds().get(0);
			}

			account = new EconomyAccount(accountName, world);
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
		return prefix + getName() + postfix;
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
		if (hasNation())
			return getTownOrNull().getNationOrNull();
		else
			return null;
	}
	
	public boolean isOnline() {
		return BukkitTools.isOnline(getName());
	}

	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public int getRespawnProtectionTaskID() {
		return -1;
	}

	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public void setRespawnProtectionTaskID(int respawnProtectionTaskID) {
		
	}
	
	public boolean hasRespawnProtection() {
		return respawnProtectionTaskID != -1;
	}
	
	public void addRespawnProtection(long protectionTime) {
		if (protectionTime <= 0)
			return;
		
		// Cancel existing respawn protection task without message
		if (respawnProtectionTaskID != -1)
			Bukkit.getScheduler().cancelTask(respawnProtectionTaskID);

		respawnPickupWarningShown = false;
		this.respawnProtectionTaskID = Bukkit.getScheduler().runTaskLater(Towny.getPlugin(), this::removeRespawnProtection, protectionTime).getTaskId();
	}
	
	public void removeRespawnProtection() {
		Bukkit.getScheduler().cancelTask(respawnProtectionTaskID);
		respawnProtectionTaskID = -1;
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
		return player == null ? Audience.empty() : Towny.getAdventure().player(player);
	}
	
	public boolean isSeeingBorderTitles() {
		BooleanDataField borderMeta = new BooleanDataField("bordertitles");
		return !MetaDataUtil.hasMeta(this, borderMeta) || MetaDataUtil.getBoolean(this, borderMeta);
	}
}
