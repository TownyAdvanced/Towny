package com.palmergames.bukkit.util;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownySettings;

/**
 * A class of functions related to minecraft in general.
 * 
 * @author Shade (Chris H)
 * @version 1.0
 */

public class MinecraftTools {

	/**
	 * Count the number of players online in each world
	 * 
	 * @param server
	 * @return Map of world to online players.
	 */
	public static HashMap<String, Integer> getPlayersPerWorld(Server server) {

		HashMap<String, Integer> m = new HashMap<String, Integer>();
		for (World world : server.getWorlds())
			m.put(world.getName(), 0);
		for (Player player : server.getOnlinePlayers())
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
	 * Tests if this player is online.
	 * 
	 * @param playerName
	 * @return true if online
	 */
	public static boolean isOnline(String playerName) {

		return Bukkit.getServer().getPlayer(playerName) != null;
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
