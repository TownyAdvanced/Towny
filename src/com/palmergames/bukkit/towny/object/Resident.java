package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class Resident extends TownyObject implements TownyInviteReceiver, EconomyHandler, TownBlockOwner {
	private List<Resident> friends = new ArrayList<>();
	// private List<Object[][][]> regenUndo = new ArrayList<>(); // Feature is disabled as of MC 1.13, maybe it'll come back.
	private UUID uuid = null;
	private Town town = null;
	private long lastOnline;
	private long registered;
	private boolean isNPC = false;
	private boolean isJailed = false;
	private int jailSpawn;
	private int jailDays;
	private String jailTown = "";
	private String title = "";
	private String surname = "";
	private long teleportRequestTime = -1;
	private Location teleportDestination;
	private double teleportCost = 0.0;
	private List<String> modes = new ArrayList<>();
	private transient Confirmation confirmation;
	private transient List<Invite> receivedinvites = new ArrayList<>();
	private transient EconomyAccount account = new EconomyAccount(getName());

	private List<String> townRanks = new ArrayList<>();
	private List<String> nationRanks = new ArrayList<>();
	private List<TownBlock> townBlocks = new ArrayList<>();
	private TownyPermission permissions = new TownyPermission();

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
	
	public UUID getUUID() {
		return uuid;		
	}
	
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}
	
	public boolean hasUUID() {
		return this.uuid != null;
	}

	public void setJailed(boolean isJailed) {
		this.isJailed = isJailed;
		
		if (isJailed)
			TownyUniverse.getInstance().getJailedResidentMap().add(this);
		else 
			TownyUniverse.getInstance().getJailedResidentMap().remove(this);
	}
	
	public void sendToJail(Player player, Integer index, Town town) {
		this.setJailed(true);
		this.setJailSpawn(index);
		this.setJailTown(town.getName());
		TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_you_have_been_sent_to_jail"));
		TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_player_has_been_sent_to_jail_number"), player.getName(), index));

	}
	
	public void freeFromJail(Player player, Integer index, boolean escaped) {
		this.setJailed(false);
		this.removeJailSpawn();
		this.setJailTown(" ");
		if (!escaped) {
			TownyMessaging.sendMsg(this, TownySettings.getLangString("msg_you_have_been_freed_from_jail"));
			TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_player_has_been_freed_from_jail_number"), this.getName(), index));
		} else
			try {
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_player_escaped_jail_into_wilderness"), player.getName(), TownyUniverse.getInstance().getDataSource().getWorld(player.getLocation().getWorld().getName()).getUnclaimedZoneName()));
			} catch (NotRegisteredException ignored) {}
	}

	public void setJailedByMayor(Player player, Integer index, Town town, Integer days) {

		if (this.isJailed) {
			try {
				Location loc = this.getTown().getSpawn();				
				if (BukkitTools.isOnline(player.getName())) {
					// Use teleport warmup
					player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
					TownyAPI.getInstance().jailTeleport(player, loc);
				}
				freeFromJail(player, index, false);
			} catch (TownyException e) {
				e.printStackTrace();
			}

		} else {
			try {
				Location loc = town.getJailSpawn(index);

				// Use teleport warmup
				player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
				TownyAPI.getInstance().jailTeleport(player, loc);

				sendToJail(player, index, town);
				if (days > 0) {
					if (days > 10000)
						days = 10000;
					this.setJailDays(days);
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you've_been_jailed_for_x_days"), days));
				}
			} catch (TownyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getInstance().getDataSource().saveResident(this);
	}

	public void setJailed(Resident resident, Integer index, Town town) {
		Player player = null;
		if (BukkitTools.isOnline(resident.getName()))
			player = BukkitTools.getPlayer(resident.getName());
		
		if (this.isJailed) {
			try {
				if (player != null) {
					Location loc;
					if (this.hasTown())
						loc = this.getTown().getSpawn();
					else
						loc = player.getWorld().getSpawnLocation();
					player.teleport(loc);
				}
				freeFromJail(player, index, false);
			} catch (TownyException e) {
				e.printStackTrace();
			}

		} else {
			try {
				Location loc = town.getJailSpawn(index);
				player.teleport(loc);
				sendToJail(player, index, town);
			} catch (TownyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getInstance().getDataSource().saveResident(this);
	}
	public boolean isJailed() {

		return isJailed;
	}

	public boolean hasJailSpawn() {
		return this.jailSpawn > 0;
	}

	public int getJailSpawn() {

		return jailSpawn;
	}

	public void setJailSpawn(Integer index) {

		this.jailSpawn = index;

	}

	public void removeJailSpawn() {

		this.jailSpawn = 0;
	}

	public String getJailTown() {

		return jailTown;
	}

	public void setJailTown(String jailTown) {
		if (jailTown == null) {
			this.jailTown = "";
			return;
		}
		this.jailTown = jailTown.trim();
	}

	public boolean hasJailTown(String jailtown) {

		return jailTown.equalsIgnoreCase(jailtown);
	}
	
	public int getJailDays() {
		return jailDays;
	}
	
	public void setJailDays(Integer days) {
		this.jailDays = days;
	}
	
	public boolean hasJailDays() {
		return this.jailDays > 0;
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

		try {
			return getTown().getNation().isKing(this);
		} catch (TownyException e) {
			return false;
		}
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
			throw new NotRegisteredException(TownySettings.getLangString("msg_err_resident_doesnt_belong_to_any_town"));
	}

	public void setTown(Town town) throws AlreadyRegisteredException {

		if (town == null) {
			this.town = null;
			setTitle("");
			setSurname("");
			updatePerms();
			return;
		}

		if (this.town == town)
			return;

		if (hasTown())
			throw new AlreadyRegisteredException();

		this.town = town;
		setTitle("");
		setSurname("");
		updatePerms();
	}

	public void setFriends(List<Resident> newFriends) {

		friends = newFriends;
	}

	public List<Resident> getFriends() {

		return friends;
	}

	public boolean removeFriend(Resident resident) throws NotRegisteredException {

		if (hasFriend(resident))
			return friends.remove(resident);
		else
			throw new NotRegisteredException();
	}

	public boolean hasFriend(Resident resident) {

		return friends.contains(resident);
	}

	public void addFriend(Resident resident) throws AlreadyRegisteredException {

		if (hasFriend(resident))
			throw new AlreadyRegisteredException();
		else
			friends.add(resident);
	}

	public void removeAllFriends() {

		for (Resident resident : new ArrayList<>(friends))
			try {
				removeFriend(resident);
			} catch (NotRegisteredException ignored) {}
	}

	public void clear() throws EmptyTownException {

		removeAllFriends();
		//setLastOnline(0);

		if (hasTown())
			try {
				town.removeResident(this);
				setTitle("");
				setSurname("");
				updatePerms();
			} catch (NotRegisteredException ignored) {}
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

		teleportRequestTime = -1;
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
	
	public List<String> getModes() {

		return this.modes;
	}
	
	public boolean hasMode(String mode) {

		return this.modes.contains(mode.toLowerCase());
	}
	
	public void toggleMode(String[] newModes, boolean notify) {

		/*
		 * Toggle any modes passed to us on/off.
		 */
		for (String mode : newModes) {
			mode = mode.toLowerCase();
			if (this.modes.contains(mode))
				this.modes.remove(mode);
			else
				this.modes.add(mode);
		}
		
		/*
		 *  If we have toggled all modes off we need to set their defaults.
		 */
		if (this.modes.isEmpty()) {

			clearModes();
			return;
		}

		if (notify)
			TownyMessaging.sendMsg(this, (TownySettings.getLangString("msg_modes_set") + StringMgmt.join(getModes(), ",")));
	}
	
	public void setModes(String[] modes, boolean notify) {

		this.modes.clear();
		this.toggleMode(modes, false);

		if (notify)
			TownyMessaging.sendMsg(this, (TownySettings.getLangString("msg_modes_set") + StringMgmt.join(getModes(), ",")));


	}
	
	public void clearModes() {

		this.modes.clear();

		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(this.getName(), true), 1) == -1)
			TownyMessaging.sendErrorMsg(TownySettings.getLangString("msg_err_could_not_set_default_modes_for") + getName() + ".");

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
			TownyMessaging.sendMsg(this, (TownySettings.getLangString("msg_modes_set") + StringMgmt.join(getModes(), ",")));
	}


	public boolean addTownRank(String rank) throws AlreadyRegisteredException {

		if (this.hasTown() && TownyPerms.getTownRanks().contains(rank)) {
			if (hasTownRank(rank))
				throw new AlreadyRegisteredException();

			rank = getTownRank(rank);
			townRanks.add(rank);
			if (BukkitTools.isOnline(this.getName()))
				TownyPerms.assignPermissions(this, null);
			BukkitTools.getPluginManager().callEvent(new TownAddResidentRankEvent(this, rank, town));
			return true;
		}

		return false;
	}

	public void setTownRanks(List<String> ranks) {
		for (String rank : ranks) 
			if (!this.hasTownRank(rank)) {
				rank = getTownRank(rank);
				if (rank != null)
					townRanks.add(rank);
			}
	}

	// Sometimes databases might have mis-matched rank casing.
	public boolean hasTownRank(String rank) {
		for (String ownedRank : townRanks) {
			if (ownedRank.equalsIgnoreCase(rank))
				return true;
		}
		return false;
	}

	public List<String> getTownRanks() {
		return townRanks;
	}
	
	// Required because we sometimes see the capitalizaton of ranks in the Townyperms change. 
	private String getTownRank(String rank) {
		for (String ownedRank : TownyPerms.getTownRanks()) {
			if (ownedRank.equalsIgnoreCase(rank))
				return ownedRank;
		}
		return null;
	}

	public boolean removeTownRank(String rank) throws NotRegisteredException {

		if (hasTownRank(rank)) {
			rank = getTownRank(rank);
			townRanks.remove(rank);
			if (BukkitTools.isOnline(this.getName())) {
				TownyPerms.assignPermissions(this, null);
			}
			BukkitTools.getPluginManager().callEvent(new TownRemoveResidentRankEvent(this, rank, town));
			return true;
		}

		throw new NotRegisteredException();
	}

	public boolean addNationRank(String rank) throws AlreadyRegisteredException {

		if (this.hasNation() && TownyPerms.getNationRanks().contains(rank)) {
			if (hasNationRank(rank))
				throw new AlreadyRegisteredException();

			rank = getNationRank(rank);
			nationRanks.add(rank);
			if (BukkitTools.isOnline(this.getName()))
				TownyPerms.assignPermissions(this, null);
			return true;
		}

		return false;
	}

	public void setNationRanks(List<String> ranks) {
		for (String rank : ranks)
			if (!this.hasNationRank(rank)) {
				rank = getNationRank(rank);
				if (rank != null)
					nationRanks.add(rank);
			}
	}

	// Sometimes databases might have mis-matched rank casing.
	public boolean hasNationRank(String rank) {
		for (String ownedRank : nationRanks) {
			if (ownedRank.equalsIgnoreCase(rank))
				return true;
		}
		return false;
	}

	public List<String> getNationRanks() {
		return nationRanks;
	}

	// Required because we sometimes see the capitalizaton of ranks in the Townyperms change.
	private String getNationRank(String rank) {
		for (String ownedRank : TownyPerms.getNationRanks()) {
			if (ownedRank.equalsIgnoreCase(rank))
				return ownedRank;
		}
		return null;
	}
	
	public boolean removeNationRank(String rank) throws NotRegisteredException {

		if (hasNationRank(rank)) {
			rank = getNationRank(rank);
			nationRanks.remove(rank);
			if (BukkitTools.isOnline(this.getName()))
				TownyPerms.assignPermissions(this, null);
			return true;
		}

		throw new NotRegisteredException();

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
		return receivedinvites;
	}

	@Override
	public void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedinvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) { // We only want 10 Invites, for residents, later we can make this number configurable
			// We use 9 because if it is = 9 it adds the tenth
			receivedinvites.add(invite);

		} else {
			throw new TooManyInvitesException(String.format(TownySettings.getLangString("msg_err_player_has_too_many_invites"),this.getName()));
		}
	}

	@Override
	public void deleteReceivedInvite(Invite invite) {
		receivedinvites.remove(invite);
	}

	public void addMetaData(CustomDataField md) {
		super.addMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveResident(this);
	}

	public void removeMetaData(CustomDataField md) {
		super.removeMetaData(md);

		TownyUniverse.getInstance().getDataSource().saveResident(this);
	}

	@Override
	public EconomyAccount getAccount() {
		if (account == null) {

			String accountName = StringMgmt.trimMaxLength(getName(), 32);
			World world;

			Player player = BukkitTools.getPlayer(getName());
			if (player != null) {
				world = player.getWorld();
			} else {
				world = BukkitTools.getWorlds().get(0);
			}

			account = new EconomyAccount(accountName, world);
		}
		
		return account;
	}

	@Override
	public String getFormattedName() {
		if (isKing()) {
			return NameUtil.translateColorCodes(hasTitle() ? getTitle() + " " : TownySettings.getKingPrefix(this)) + getName() + (hasSurname() ? " " + getSurname() : TownySettings.getKingPostfix(this));
		}
			
		if (isMayor()) {
			return NameUtil.translateColorCodes(hasTitle() ? getTitle() + " " : TownySettings.getMayorPrefix(this)) + getName() + (hasSurname() ? " " + getSurname() : TownySettings.getMayorPostfix(this));
		}
			
		return NameUtil.translateColorCodes(hasTitle() ? getTitle() + " " : "") + getName() + (hasSurname() ? " " + getSurname() : "");
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

	@Override
	public void setTownblocks(List<TownBlock> townBlocks) {
		this.townBlocks = townBlocks;
	}

	@Override
	public List<TownBlock> getTownBlocks() {
		return townBlocks;
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
	public void removeTownBlock(TownBlock townBlock) throws NotRegisteredException {
		if (!townBlocks.remove(townBlock))
			throw new NotRegisteredException();
	}

	@Override
	public void setPermissions(String line) {
		this.permissions.load(line);
	}

	@Override
	public TownyPermission getPermissions() {
		return permissions;
	}

	/**
	 * @deprecated As of 0.97.0.0+ please use {@link EconomyAccount#getWorld()} instead.
	 *
	 * @return The world this resides in.
	 */
	@Deprecated
	public World getBukkitWorld() {
		Player player = BukkitTools.getPlayer(getName());
		if (player != null) {
			return player.getWorld();
		} else {
			return BukkitTools.getWorlds().get(0);
		}
	}

	public Confirmation getConfirmation() {
		return confirmation;
	}

	public void setConfirmation(Confirmation confirmation) {
		this.confirmation = confirmation;
	}
}

