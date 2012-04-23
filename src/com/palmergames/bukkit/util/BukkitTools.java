package com.palmergames.bukkit.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

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
	public static Player[] getOnlinePlayers() {
		return getServer().getOnlinePlayers();
	}
	
	public static List<Player> matchPlayer(String name) {
		return getServer().matchPlayer(name);
	}
	
	/**
	 * Tests if this player is online.
	 * 
	 * @param playerName
	 * @return true if online
	 */
	public static boolean isOnline(String playerName) {
		return getServer().getPlayer(playerName) != null;
	}
	
	public static List<World> getWorlds() {
			return server.getWorlds();
	}
	
	public static Server getServer() {
		synchronized(server) {
			return server;
		}
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
	 * Accepts a Runnable object and a delay (-1 for no delay)
	 * 
	 * @param task
	 * @param delay ticks to delay starting
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleAsyncDelayedTask(Runnable task, long delay) {
		return getScheduler().scheduleAsyncDelayedTask(plugin, task, delay);
	}
	
	/**
	 * Accepts a Runnable object with a delay/repeat (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting
	 * @param repeat ticks to repeat after
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleSyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().scheduleSyncRepeatingTask(plugin, task, delay, repeat);
	}
	
	/**
	 * Accepts a Runnable object with a delay/repeat (-1 for no delay)
	 * 
	 * @param task runnable object
	 * @param delay ticks to delay starting
	 * @param repeat ticks to repeat after
	 * @return -1 if unable to schedule or an index to the task is successful.
	 */
	public static int scheduleAsyncRepeatingTask(Runnable task, long delay, long repeat) {
		return getScheduler().scheduleAsyncRepeatingTask(plugin, task, delay, repeat);
	}
	
	/**
	 * Count the number of players online in each world
	 * 
	 * @param server
	 * @return Map of world to online players.
	 */
	public static HashMap<String, Integer> getPlayersPerWorld() {

		HashMap<String, Integer> m = new HashMap<String, Integer>();
		for (World world : getServer().getWorlds())
			m.put(world.getName(), 0);
		for (Player player :  getServer().getOnlinePlayers())
			m.put(player.getWorld().getName(), m.get(player.getWorld().getName()) + 1);
		return m;
	}

	/**
	 * Find a block at a specific offset.
	 * 
	 * @param block
	 * @param xOffset
	 * @param yOffset
	 * @param zOffset
	 * @return Block at the new location.
	 */
	public static Block getBlockOffset(Block block, int xOffset, int yOffset, int zOffset) {

		return block.getWorld().getBlockAt(block.getX() + xOffset, block.getY() + yOffset, block.getZ() + zOffset);
	}

	/**
	 * Compiles a list of all whitelisted users.
	 * 
	 * @return List of all whitelist player names.
	 */
	public static List<String> getWhiteListedUsers() {

		List<String> names = new ArrayList<String>();
		try {
			BufferedReader fin = new BufferedReader(new FileReader("white-list.txt"));

			try {
				String line;
				while ((line = fin.readLine()) != null)
					names.add(line);
			} catch (IOException e) {
			}

			try {
				fin.close();
			} catch (IOException e) {
			}
		} catch (FileNotFoundException e) {
		}
		return names;
	}

	/**
	 * Accepts an X or Z value and returns the associated Towny plot value.
	 * 
	 * @param value
	 * @return int of the relevant townblock x/z.
	 */
	public static int calcChunk(int value) {

		return (value * TownySettings.getTownBlockSize()) / 16;
	}
}
