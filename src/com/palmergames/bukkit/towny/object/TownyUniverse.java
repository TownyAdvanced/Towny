package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Hashtable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @deprecated Use {@link com.palmergames.bukkit.towny.TownyAPI}.getInstance() for future calls.
 */
@Deprecated()
public class TownyUniverse {
	public TownyUniverse(Towny plugin) {
	}

	public Location getTownSpawnLocation(Player player) {
		return TownyAPI.getInstance().getTownSpawnLocation(player);
	}

	public Location getNationSpawnLocation(Player player) {
		return TownyAPI.getInstance().getNationSpawnLocation(player);
	}

	public static Player getPlayer(Resident resident) {
		return TownyAPI.getInstance().getPlayer(resident);
	}
	
	public static UUID getPlayerUUID(Resident resident) {
		return TownyAPI.getInstance().getPlayerUUID(resident);
	}

	public static List<Player> getOnlinePlayers(ResidentList residentList) {
		return TownyAPI.getInstance().getOnlinePlayers(residentList);
	}

	public static List<Player> getOnlinePlayers(Town town) {
		return TownyAPI.getInstance().getOnlinePlayers(town);
	}

	public static List<Player> getOnlinePlayers(Nation nation) {
		return TownyAPI.getInstance().getOnlinePlayers(nation);
	}

	public static boolean isWilderness(Block block) {
		return TownyAPI.getInstance().isWilderness(block.getLocation());
	}

	public static String getTownName(Location location) {
		return TownyAPI.getInstance().getTownName(location);
	}
	
	public static UUID getTownUUID(Location location) {
		return TownyAPI.getInstance().getTownUUID(location);
	}

	public static TownBlock getTownBlock(Location location) {
		return TownyAPI.getInstance().getTownBlock(location);
	}

	public List<Resident> getActiveResidents() {
		return TownyAPI.getInstance().getActiveResidents();
	}

	public boolean isActiveResident(Resident resident) {
		return TownyAPI.getInstance().isActiveResident(resident);
	}
	
	public String getRootFolder() {
		return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getRootFolder();
	}

	public static TownyDataSource getDataSource() {
		return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getDataSource();
	}

	public void setPermissionSource(TownyPermissionSource permissionSource) {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().setPermissionSource(permissionSource);
	}

	public static TownyPermissionSource getPermissionSource() {
		return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getPermissionSource();
	}

	public Hashtable<String, Resident> getResidentMap() {
		return new Hashtable<>(com.palmergames.bukkit.towny.TownyUniverse.getInstance().getResidentMap());
	}

	public Hashtable<String, Town> getTownsMap() {
		return new Hashtable<>(com.palmergames.bukkit.towny.TownyUniverse.getInstance().getTownsMap());
	}

	public Hashtable<String, Nation> getNationsMap() {
		return new Hashtable<>(
			com.palmergames.bukkit.towny.TownyUniverse.getInstance().getNations().stream()
			.collect(Collectors.toMap(TownyObject::getName, nation -> nation))
		);
	}

	public Hashtable<String, TownyWorld> getWorldMap() {
		return new Hashtable<>(com.palmergames.bukkit.towny.TownyUniverse.getInstance().getWorldMap());
	}

	public static boolean isWarTime() {

		return TownyAPI.getInstance().isWarTime();
	}

	public void startWarEvent() {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().startWarEvent();
	}

	public void endWarEvent() {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().endWarEvent();
	}

	public void clearWarEvent() {

		getWarEvent().cancelTasks(BukkitTools.getScheduler());
		setWarEvent(null);
	}

	//TODO: throw error if null
	public War getWarEvent() {
		return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getWarEvent();
	}

	public void setWarEvent(War event) {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().setWarEvent(event);
	}

	public List<String> getTreeString(int depth) {
		return com.palmergames.bukkit.towny.TownyUniverse.getInstance().getTreeString(depth);
	}
	
	public void sendUniverseTree(CommandSender commandSender) {
		for (String line : getTreeString(0)) {
			commandSender.sendMessage(line);
		}
	}

	public static List<Resident> getOnlineResidents(ResidentList residentList) {
		return TownyAPI.getInstance().getOnlineResidents(residentList);
	}

	public void abortTeleportRequest(Resident resident) {
		TownyAPI.getInstance().abortTeleportRequest(resident);
	}

	public static void jailTeleport(final Player player, final Location location) {
		TownyAPI.getInstance().jailTeleport(player, location);
	}

	/**
	 * @deprecated Use {@link TownyWorld#addWarZone(Coord)} instead.
	 * @param worldCoord Coordinates of WarZone
	 */
	@Deprecated
	public void addWarZone(WorldCoord worldCoord) {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().addWarZone(worldCoord);
	}

	/**
	 * @deprecated Use {@link TownyWorld#removeWarZone(Coord)} instead
	 * @param worldCoord Cordinates of WarZone
	 */
	@Deprecated
	public void removeWarZone(WorldCoord worldCoord) {
		com.palmergames.bukkit.towny.TownyUniverse.getInstance().removeWarZone(worldCoord);
	}
}
