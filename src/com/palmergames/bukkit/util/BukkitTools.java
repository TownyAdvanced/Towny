package com.palmergames.bukkit.util;

import com.google.common.base.Charsets;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import de.themoep.idconverter.IdMappings;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

/**
 * A class of functions related to Bukkit in general.
 * 
 * @author Shade (Chris H, ElgarL)
 * @version 1.0
 */

public class BukkitTools {

	private static Towny plugin = null;
	private static Server server = null;
	
	public static void initialize(Towny plugin) {
		BukkitTools.plugin = plugin;
		BukkitTools.server = plugin.getServer();
	}
	
	/**
	 * Get an array of all online players
	 * 
	 * @return array of online players
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return getServer().getOnlinePlayers();
	}
	
	public static List<Player> matchPlayer(String name) {
		List<Player> matchedPlayers = new ArrayList<>();
		
		for (Player iterPlayer : Bukkit.getOnlinePlayers()) {
			String iterPlayerName = iterPlayer.getName();
			if (plugin.isCitizens2()) {
				if (CitizensAPI.getNPCRegistry().isNPC(iterPlayer)) {
					continue;
				}
			}
			if (name.equalsIgnoreCase(iterPlayerName)) {
				// Exact match
				matchedPlayers.clear();
				matchedPlayers.add(iterPlayer);
				break;
			}
			if (iterPlayerName.toLowerCase(java.util.Locale.ENGLISH).contains(name.toLowerCase(java.util.Locale.ENGLISH))) {
				// Partial match
				matchedPlayers.add(iterPlayer);
			}
		}
		
		return matchedPlayers;
	}
	
	public static Player getPlayerExact(String name) {
		return getServer().getPlayerExact(name);
	}
	
	public static Player getPlayer(String playerId) {
		return getServer().getPlayer(playerId);
	}
	
	/**
	 * Tests if this player is online.
	 * 
	 * @param playerId the UUID or name of the player.
	 * @return a true value if online
	 */
	public static boolean isOnline(String playerId) {
		for (Player players : getOnlinePlayers()) {
			if (players.getName().equals(playerId))
				return true;
		}
		return false; 
	}
	
	public static List<World> getWorlds() {
		return  getServer().getWorlds();
	}
	
	public static World getWorld(String name) {
		return  getServer().getWorld(name);
	}
	
	public static Server getServer() {
		synchronized(server) {
			return server;
		}
	}
	
	public static PluginManager getPluginManager() {
		return getServer().getPluginManager();
	}
	
	public static BukkitScheduler getScheduler() {
		return getServer().getScheduler();
	}
	
	/**
	 * Accepts a Runnable object and a delay (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleSyncDelayedTask(Runnable task, long delay) {
		return getScheduler().scheduleSyncDelayedTask(plugin, task, delay);
	}
	
	/**
	 * Accepts a {@link Runnable} object and a delay (-1 for no delay)
	 * 
	 * @param task - Runnable
	 * @param delay - ticks to delay starting ({@link Long})
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleAsyncDelayedTask(Runnable task, long delay) {
		return getScheduler().runTaskLaterAsynchronously(plugin, task, delay).getTaskId();
	}
	
	/**
	 * Accepts a {@link Runnable} object with a delay/repeat (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting ({@link Long})
	 * @param repeat ticks to repeat after ({@link Long})
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleSyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, repeat);
	}
	
	/**
	 * Accepts a {@link Runnable} object with a delay/repeat (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting ({@link Long})
	 * @param repeat ticks to repeat after ({@link Long})
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleAsyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().runTaskTimerAsynchronously(plugin, task, delay, repeat).getTaskId();
	}
	
	/**
	 * Count the number of players online in each world
	 * 
	 * @return Map of world to online players.
	 */
	public static HashMap<String, Integer> getPlayersPerWorld() {

		HashMap<String, Integer> m = new HashMap<>();
		for (World world : getServer().getWorlds())
			m.put(world.getName(), 0);
		for (Player player :  getServer().getOnlinePlayers())
			m.put(player.getWorld().getName(), m.get(player.getWorld().getName()) + 1);
		return m;
	}

	/*
	 * Material handling Methods.
	 */
	
	/**
	 * Find a Material from an Id.
	 * Helpfully using Phoenix616's useful IdConverter.jar
	 * https://www.spigotmc.org/resources/id-converter.52099/
	 * 
	 * @param id - ID for a material ({@link Integer})
	 * @return a Material parsed from {@link IdMappings}
	 */
	@Deprecated
	public static Material getMaterial(int id) {
		return Material.getMaterial(IdMappings.getById(String.valueOf(id)).getFlatteningType());
	}

	/**
	 * Accepts an X or Z value and returns the associated Towny plot value.
	 * 
	 * @param value - Value to calculate for X or Z ({@link Integer})
	 * @return int of the relevant townblock x/z.
	 */
	public static int calcChunk(int value) {

		return (value * TownySettings.getTownBlockSize()) / 16;
	}

	public static OfflinePlayer getOfflinePlayer(String name) {

		return Bukkit.getOfflinePlayer(getPlayerExact(name).getUniqueId());
	}
	
	public static OfflinePlayer getOfflinePlayerForVault(String name) {

		return Bukkit.getOfflinePlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)));
	}
	
	public static String convertCoordtoXYZ(Location loc) {
		return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	/**
	 * 
	 * @return whether server is running spigot (and not CraftBukkit.)
	 */
	public static boolean isSpigot() {
		try {
			Class.forName("org.bukkit.entity.Player$Spigot");
			return true;
		} catch (Throwable tr) {
			return false;
		}

	}
}
