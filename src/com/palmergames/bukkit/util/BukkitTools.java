package com.palmergames.bukkit.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitScheduler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;

import de.themoep.idconverter.IdMappings;

/**
 * A class of functions related to Bukkit in general.
 * 
 * @author Shade (Chris H, ElgarL)
 * @version 1.0
 */

@SuppressWarnings("deprecation")
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
		return getServer().matchPlayer(name);
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
		return getServer().getPlayer(playerId) != null;
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
	
	public static boolean isPrimaryThread() {
		return Bukkit.isPrimaryThread();
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
		return getScheduler().runTaskLaterAsynchronously(plugin, task, delay).getTaskId();
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
		return getScheduler().runTaskTimerAsynchronously(plugin, task, delay, repeat).getTaskId();
	}
	
	/**
	 * Count the number of players online in each world
	 * 
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
	
	
	/*
	 * Block handling Methods.
	 */
	
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

	// Will be removed completely when the new plotsnapshot system is made.
	@Deprecated
	public static int getTypeId(Block block) {
		return block.getType().getId();
	}	
	// Will be removed completely when the new plotsnapshot system is made.
	@Deprecated
	public static byte getData(Block block) {
		return block.getData();
	}
	// No Longer Used, used to be used in PlotBlockData's restorenextblock.
	@Deprecated
	public static void setTypeIdAndData(Block block, int type, byte data, boolean applyPhysics) {
		Material mat = Material.getMaterial(IdMappings.getById(String.format("%s:%s", type, data)).getFlatteningType());
		block.setType(mat, applyPhysics);		
	}
	// No Longer Used, used to be used in PlotBlockData's restorenextblock.
	@Deprecated
	public static void setTypeId(Block block, int type, boolean applyPhysics) {
		Material mat = Material.getMaterial(IdMappings.getById(String.valueOf(type)).getFlatteningType());
		block.setType(mat, applyPhysics);
	}
		
	
	/*
	 * BlockState Methods
	 */

	public static Material getType(BlockState state) {
		
		return state.getType();
	}
	
	public static MaterialData getData(BlockState state) {
		
		return state.getData();
	}
	
	public static byte getDataData(BlockState state) {
		
		return getData(state).getData();
	}
	
	
	/*
	 * Item Handling Methods
	 */
	
	public static MaterialData getData(ItemStack stack) {
		
		return stack.getData();
	}
	
	public static byte getDataData(ItemStack stack) {
		
		return getData(stack).getData();
	}
	
	
	/*
	 * Material handling Methods.
	 */
	
	/**
	 * Find a Material from an Id.
	 * Helpfully using Phoenix616's useful IdConverter.jar
	 * https://www.spigotmc.org/resources/id-converter.52099/
	 * 
	 * @param id
	 * @return
	 */
	public static Material getMaterial(int id) {
		return Material.getMaterial(IdMappings.getById(String.valueOf(id)).getFlatteningType());
	}
	
	/**
	 * Find a Material from an enum name.
	 * 
	 * @param name
	 * @return
	 */
	public static Material getMaterial(String name) {
		
		return Material.getMaterial(name);
	}
	
	/**
	 * Get the Id (magic number) of a Material type.
	 * 
	 * @param material
	 * @return
	 */
	@Deprecated
	public static int getMaterialId(Material material) {
		
		return material.getId();
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
