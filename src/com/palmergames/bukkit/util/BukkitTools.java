package com.palmergames.bukkit.util;

import com.google.common.base.Charsets;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import net.citizensnpcs.api.CitizensAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A class of functions related to Bukkit in general.
 * 
 * @author Shade (Chris H, ElgarL)
 * @version 1.0
 */

public class BukkitTools {

	private static Towny plugin = null;
	
	public static void initialize(Towny plugin) {
		BukkitTools.plugin = plugin;
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
	
	/**
	 * Given a name this method should only return a UUID that is stored in the server cache,
	 * without pinging Mojang servers.
	 * 
	 * @param name - Resident/Player name to get a UUID for.
	 * @return UUID of player or null if the player is not in the cache.
	 */
	@Nullable
	public static UUID getUUIDSafely(String name) {
		if (hasPlayedBefore(name))
			return getOfflinePlayer(name).getUniqueId();
		else
			return null;
	}
	
	/**
	 * Tests if this player is online.
	 * 
	 * @param name the name of the player.
	 * @return a true value if online
	 */
	public static boolean isOnline(String name) {
		return Bukkit.getPlayer(name) != null;
	}
	
	/**
	 * Accepts a Runnable object and a delay (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleSyncDelayedTask(Runnable task, long delay) {
		return Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, task, delay);
	}
	
	/**
	 * Accepts a {@link Runnable} object and a delay (-1 for no delay)
	 * 
	 * @param task - Runnable
	 * @param delay - ticks to delay starting ({@link Long})
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleAsyncDelayedTask(Runnable task, long delay) {
		return Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, task, delay).getTaskId();
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
		return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, repeat);
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
		return Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, task, delay, repeat).getTaskId();
	}
	
	/**
	 * Count the number of players online in each world
	 * 
	 * @return Map of world to online players.
	 */
	public static HashMap<String, Integer> getPlayersPerWorld() {

		HashMap<String, Integer> m = new HashMap<>();
		for (World world : Bukkit.getServer().getWorlds())
			m.put(world.getName(), 0);
		for (Player player : Bukkit.getServer().getOnlinePlayers())
			m.put(player.getWorld().getName(), m.get(player.getWorld().getName()) + 1);
		return m;
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


	@SuppressWarnings("deprecation")
	public static boolean hasPlayedBefore(String name) {
		return Bukkit.getServer().getOfflinePlayer(name).hasPlayedBefore();
	}
	
	/**
	 * Do not use without first using {@link #hasPlayedBefore(String)}
	 * 
	 * @param name - name of resident
	 * @return OfflinePlayer
	 */
	@SuppressWarnings("deprecation")
	public static OfflinePlayer getOfflinePlayer(String name) {

		return Bukkit.getOfflinePlayer(name);
	}
	
	public static OfflinePlayer getOfflinePlayerForVault(String name) {
		return Bukkit.getOfflinePlayer(UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8)));
	}
	
	public static String convertCoordtoXYZ(Location loc) {
		return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public static List<String> getWorldNames() {
		return Bukkit.getWorlds().stream().map(World::getName).collect(Collectors.toList());
	}
	
	public static List<String> getWorldNames(boolean lowercased) {
		return lowercased ? Bukkit.getWorlds().stream().map(world -> world.getName().toLowerCase()).collect(Collectors.toList()) : getWorldNames();
	}
}
