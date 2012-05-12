package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Resident extends TownBlockOwner implements ResidentModes {

	private List<Resident> friends = new ArrayList<Resident>();
	private List<ChunkSnapshot> regenUndo = new ArrayList<ChunkSnapshot>();
	private Town town;
	private long lastOnline, registered;
	private boolean isNPC = false;
	private String title, surname;
	private long teleportRequestTime;
	private Location teleportDestination;
	private double teleportCost;
	private String chatFormattedName;
	private List<String> modes = new ArrayList<String>();

	public Resident(String name) {

		setChatFormattedName(name);
		setName(name);
		setTitle("");
		setSurname("");
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

		return !(town == null);
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
			return;
		}
		if (this.town == town)
			return;
		if (hasTown())
			throw new AlreadyRegisteredException();
		this.town = town;
		setTitle("");
		setSurname("");
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
			} catch (NotRegisteredException e) {
			}
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

	/**
	 * Push a snapshot to the Undo queue
	 * 
	 * @param snapshot
	 */
	public void addUndo(ChunkSnapshot snapshot) {

		if (regenUndo.size() == 5)
			regenUndo.remove(0);
		regenUndo.add(snapshot);
	}

	public void regenUndo() {

		if (regenUndo.size() > 0) {
			ChunkSnapshot snapshot = regenUndo.get(regenUndo.size() - 1);
			regenUndo.remove(snapshot);

			TownyRegenAPI.regenUndo(snapshot, this);

		}
	}

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
		
		if (newModes.length == 0) {
			clearModes();
			return;
		}

		for (String mode : newModes) {
			mode = mode.toLowerCase();
			if (this.modes.contains(mode))
				this.modes.remove(mode);
			else
				this.modes.add(mode);
		}
		
		if (notify)
			TownyMessaging.sendMsg(this, ("Modes set: " + StringMgmt.join(getModes(), ",")));
	}

	@Override
	public void setModes(String[] modes, boolean notify) {

		if (modes.length == 0) {
			clearModes();
			return;
		}
			
		this.modes.clear();
		this.toggleMode(modes, false);
		
		if (notify)
			TownyMessaging.sendMsg(this, ("Modes set: " + StringMgmt.join(getModes(), ",")));
		
		
	}

	@Override
	public void clearModes() {

		modes.clear();
		
		if (BukkitTools.scheduleSyncDelayedTask(new SetDefaultModes(this.getName(), true), 1) == -1)
			TownyMessaging.sendErrorMsg("Could not set default modes for " + getName() + ".");
		
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

}
