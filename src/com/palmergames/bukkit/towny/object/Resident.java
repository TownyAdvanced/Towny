package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.confirmations.ConfirmationType;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Resident extends TownBlockOwner implements ResidentModes, TownyInviteReceiver{

	private List<Resident> friends = new ArrayList<Resident>();
	private List<Object[][][]> regenUndo = new ArrayList<Object[][][]>();
	private Town town = null;
	private long lastOnline, registered;
	private boolean isNPC = false;
	private boolean isJailed = false;
	private int JailSpawn;
	private String JailTown;
	private String title, surname;
	private long teleportRequestTime;
	private Location teleportDestination;
	private double teleportCost;
	private String chatFormattedName;
	private List<String> modes = new ArrayList<String>();
	private ConfirmationType confirmationType;

	private List<String> townRanks = new ArrayList<String>();
	private List<String> nationRanks = new ArrayList<String>();

	public Resident(String name) {

		setChatFormattedName(name);
		setName(name);
		setTitle("");
		setSurname("");
		setJailTown("");
		permissions.loadDefault(this);
		teleportRequestTime = -1;
		teleportCost = 0.0;
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

	public void setJailed(boolean isJailed) {

		this.isJailed = isJailed;
	}

	public void setJailedByMayor(Player player, Integer index, Town town) {

		if (this.isJailed) {
			this.setJailed(false);
			try {
				Location loc = this.getTown().getSpawn();
				
				// Use teleport warmup
				player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
				TownyUniverse.jailTeleport(player, loc);

				this.removeJailSpawn();
				this.setJailTown(" ");
				TownyMessaging.sendMsg(player, "You have been freed from jail.");
				TownyMessaging.sendTownMessagePrefixed(town, player.getName() + " has been freed from jail number " + index);
			} catch (TownyException e) {
				e.printStackTrace();
			}

		} else {
			try {
				Location loc = town.getJailSpawn(index);

				// Use teleport warmup
				player.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
				TownyUniverse.jailTeleport(player, loc);

				this.setJailed(true);
				this.setJailSpawn(index);
				this.setJailTown(town.toString());
				TownyMessaging.sendMsg(player, "You have been sent to jail.");
				TownyMessaging.sendTownMessagePrefixed(town, player.getName() + " has been sent to jail number " + index);
			} catch (TownyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getDataSource().saveResident(this);
	}

	public void setJailed(Player player, Integer index, Town town) {

		if (this.isJailed) {
			this.setJailed(false);
			try {
				Location loc = null;
				if (this.hasTown())
					loc = this.getTown().getSpawn();
				else
					loc = player.getWorld().getSpawnLocation();

				player.teleport(loc);
				this.removeJailSpawn();
				this.setJailTown(" ");
				TownyMessaging.sendMsg(player, "You have been freed from jail.");
				TownyMessaging.sendTownMessagePrefixed(town, player.getName() + " has been freed from jail number " + index);
			} catch (TownyException e) {
				e.printStackTrace();
			}

		} else {
			try {
				Location loc = town.getJailSpawn(index);
				player.teleport(loc);
				this.setJailed(true);
				this.setJailSpawn(index);
				this.setJailTown(town.toString());
				TownyMessaging.sendMsg(player, "You have been sent to jail.");
				TownyMessaging.sendTownMessagePrefixed(town, player.getName() + " has been sent to jail number " + index);
			} catch (TownyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getDataSource().saveResident(this);
	}

	public boolean isJailed() {

		return isJailed;
	}

	public boolean hasJailSpawn() {

		if (this.JailSpawn <= 1) {
			return true;
		} else {
			return false;
		}

	}

	public int getJailSpawn() {

		return JailSpawn;
	}

	public void setJailSpawn(Integer index) {

		this.JailSpawn = index;

	}

	public void removeJailSpawn() {

		this.JailSpawn = 0;
	}

	public String getJailTown() {

		return JailTown;
	}

	public void setJailTown(String jailTown) {
		if (jailTown == null)
			jailTown = "";
		if (jailTown.matches(" "))
			jailTown = "";
		this.JailTown = jailTown;
	}

	public boolean hasJailTown(String jailtown) {

		return JailTown.equalsIgnoreCase(jailtown);
	}

	public void setTitle(String title) {

		if (title.matches(" "))
			title = "";
		this.title = title;
		setChangedName(true);
	}

	public String getTitle() {

		return title;
	}

	public boolean hasTitle() {

		return !title.isEmpty();
	}

	public void setSurname(String surname) {

		if (surname.matches(" "))
			surname = "";
		this.surname = surname;
		setChangedName(true);
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

		return hasTown() ? town.isMayor(this) : false;
	}

	public boolean hasTown() {

		return town != null;
	}

	public boolean hasNation() {

		return hasTown() ? town.hasNation() : false;
	}

	public Town getTown() throws NotRegisteredException {

		if (hasTown())
			return town;
		else
			throw new NotRegisteredException("Resident doesn't belong to any town");
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

		for (Resident resident : new ArrayList<Resident>(friends))
			try {
				removeFriend(resident);
			} catch (NotRegisteredException e) {
			}
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
			} catch (NotRegisteredException e) {
			}
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

		List<String> out = new ArrayList<String>();
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

	/**
	 * @return the chatFormattedName
	 */
	public String getChatFormattedName() {

		return chatFormattedName;
	}

	/**
	 * @param chatFormattedName the chatFormattedName to set
	 */
	public void setChatFormattedName(String chatFormattedName) {

		this.chatFormattedName = chatFormattedName;
		setChangedName(false);
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

	@Override
	public List<String> getModes() {

		return this.modes;
	}

	@Override
	public boolean hasMode(String mode) {

		return this.modes.contains(mode.toLowerCase());
	}

	@Override
	public void toggleMode(String newModes[], boolean notify) {

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
			TownyMessaging.sendMsg(this, ("Modes set: " + StringMgmt.join(getModes(), ",")));
	}

	@Override
	public void setModes(String[] modes, boolean notify) {

		this.modes.clear();
		this.toggleMode(modes, false);

		if (notify)
			TownyMessaging.sendMsg(this, ("Modes set: " + StringMgmt.join(getModes(), ",")));


	}

	@Override
	public void clearModes() {

		this.modes.clear();

		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(this.getName(), true), 1) == -1)
			TownyMessaging.sendErrorMsg("Could not set default modes for " + getName() + ".");

	}

	/**
	 * Only for internal Towny use. NEVER call this from any other plugin.
	 *
	 * @param modes
	 * @param notify
	 */
	public void resetModes(String[] modes, boolean notify) {

		if (modes.length > 0)
			this.toggleMode(modes, false);

		if (notify)
			TownyMessaging.sendMsg(this, ("Modes set: " + StringMgmt.join(getModes(), ",")));
	}


	public boolean addTownRank(String rank) throws AlreadyRegisteredException {

		if (this.hasTown() && TownyPerms.getTownRanks().contains(rank)) {
			if (townRanks.contains(rank))
				throw new AlreadyRegisteredException();

			townRanks.add(rank);
			TownyPerms.assignPermissions(this, null);
			return true;
		}

		return false;
	}

	public void setTownRanks(List<String> ranks) {
		townRanks.addAll(ranks);
	}

	public boolean hasTownRank(String rank) {
		return townRanks.contains(rank.toLowerCase());
	}

	public List<String> getTownRanks() {
		return townRanks;
	}

	public boolean removeTownRank(String rank) throws NotRegisteredException {

		if (townRanks.contains(rank)) {
			townRanks.remove(rank);
			TownyPerms.assignPermissions(this, null);
			return true;
		}

		throw new NotRegisteredException();
	}

	public boolean addNationRank(String rank) throws AlreadyRegisteredException {

		if (this.hasNation() && TownyPerms.getNationRanks().contains(rank)) {
			if (nationRanks.contains(rank))
				throw new AlreadyRegisteredException();

			nationRanks.add(rank);
			TownyPerms.assignPermissions(this, null);
			return true;
		}

		return false;
	}

	public void setNationRanks(List<String> ranks) {
		nationRanks.addAll(ranks);
	}

	public boolean hasNationRank(String rank) {
		return nationRanks.contains(rank.toLowerCase());
	}

	public List<String> getNationRanks() {
		return nationRanks;
	}

	public boolean removeNationRank(String rank) throws NotRegisteredException {

		if (nationRanks.contains(rank)) {
			nationRanks.remove(rank);
			TownyPerms.assignPermissions(this, null);
			return true;
		}

		throw new NotRegisteredException();

	}

	@Override
	protected World getBukkitWorld() {
		Player player = BukkitTools.getPlayer(getName());
		if (player != null) {
			return player.getWorld();
		} else {
			return super.getBukkitWorld();
		}
	}

	public boolean isAlliedWith(Resident otherresident) {
		if (this.hasNation() && this.hasTown() && otherresident.hasTown() && otherresident.hasNation()) {
			try {
				if (this.getTown().getNation().hasAlly(otherresident.getTown().getNation())) {
					return true;
				} else {

					if (this.getTown().getNation().equals(otherresident.getTown().getNation())) {
						return true;
					} else {
						return false;
					}
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

	private List<Invite> receivedinvites = new ArrayList<Invite>();

	public void setConfirmationType(ConfirmationType confirmationType) {
		this.confirmationType = confirmationType;
	}

	public ConfirmationType getConfirmationType() {
		return confirmationType;
	}

}

