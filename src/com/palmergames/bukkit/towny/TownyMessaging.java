package com.palmergames.bukkit.towny;

import java.util.List;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;

/**
 * Towny message handling class
 * 
 * @author ElgarL
 *
 */

public class TownyMessaging {
	
	
	/**
	 * Sends an error message to the log
	 * 
	 * @param msg
	 */
	public static void sendErrorMsg(String msg) {
		TownyLogger.log.warning("[Towny] Error: " + msg);
    }
	
	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 * 
	 * @param sender
	 * @param msg
	 */
	public static void sendErrorMsg(Object sender, String msg) {
    	
    	boolean isPlayer =  false;
    	if (sender instanceof Player)
    		isPlayer = true;
    	
        for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.Rose + msg))
        	if (isPlayer)
        		((Player) sender).sendMessage(line);
        	else
        		((CommandSender) sender).sendMessage(Colors.strip(line));
        sendDevMsg(msg);
    }
	
	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 * 
	 * @param sender
	 * @param msg
	 */
	public static void sendErrorMsg(Object sender, String[] msg) {
    	
    	boolean isPlayer =  false;
    	if (sender instanceof Player)
    		isPlayer = true;
    	
        for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.Rose + msg))
        	if (isPlayer)
        		((Player) sender).sendMessage(line);
        	else
        		((CommandSender) sender).sendMessage(Colors.strip(line));
        sendDevMsg(msg);
    }
	
	/**
	 * Sends a message to console only
	 * prefixed by [Towny]
	 * 
	 * @param msg
	 */
	public static void sendMsg(String msg) {
        System.out.println("[Towny] " + ChatTools.stripColour(msg));
	}
	
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 * 
	 * @param sender
	 * @param msg
	 */
	public static void sendMsg(Object sender, String msg) {
    	
    	boolean isPlayer =  false;
    	if (sender instanceof Player)
    		isPlayer = true;
    	
        for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.Green + msg))
        	if (isPlayer)
        		((Player) sender).sendMessage(line);
        	else
        		((CommandSender) sender).sendMessage(Colors.strip(line));
        sendDevMsg(msg);
    }
	
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 * 
	 * @param sender
	 * @param msg
	 */
	public static void sendMsg(Player player, String[] msg) {
    	for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.Green + msg))
            player.sendMessage(line);
    }
	
	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 * 
	 * @param msg
	 */
	public static void sendDevMsg(String msg) {
        if (TownySettings.isDevMode()) {
                Player townyDev = TownyUniverse.plugin.getServer().getPlayer(TownySettings.getDevName());
                if (townyDev == null)
                        return;
                for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + " DevMode: " + Colors.Rose + msg))
                        townyDev.sendMessage(line);
        }
	}
	
	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 * 
	 * @param msg
	 */
	public static void sendDevMsg(String[] msg) {
        if (TownySettings.isDevMode()) {
                Player townyDev = TownyUniverse.plugin.getServer().getPlayer(TownySettings.getDevName());
                if (townyDev == null)
                        return;
                for (String line : ChatTools.color(TownySettings.getLangString("default_towny_prefix") + " DevMode: " + Colors.Rose + msg))
                        townyDev.sendMessage(line);
        }
	}
	
	/**
	 * Sends a message to the log and console
	 * prefixed by [Towny] Debug:
	 * 
	 * @param msg
	 */
	public static void sendDebugMsg(String msg) {
        if (TownySettings.getDebug())
        	TownyLogger.debug.info("[Towny] Debug: " + msg);	
        sendDevMsg(msg);
	}
	
	/////////////////
	
	/**
	 * Send a message to a player
	 * 
	 * @param player
	 * @param lines
	 */
	public static void sendMessage(Object sender, List<String> lines) {
        sendMessage(sender, lines.toArray(new String[0]));
	}
	
	/**
	 * Send a message to a player
	 * 
	 * @param player
	 * @param line
	 */
	public static void sendMessage(Object sender, String line) {
		boolean isPlayer = false;
		if (sender instanceof Player)
			isPlayer = true;

		if (isPlayer)
			((Player) sender).sendMessage(line);
		else
			((CommandSender) sender).sendMessage(line);
	}
	
	/**
	 * Send a message to a player
	 * 
	 * @param player
	 * @param lines
	 */
	public static void sendMessage(Object sender, String[] lines) {
		
		boolean isPlayer =  false;
    	if (sender instanceof Player)
    		isPlayer = true;
    	
        for (String line : lines) {
        	if (isPlayer)
        		((Player) sender).sendMessage(line);
        	else
        		((CommandSender) sender).sendMessage(line);
        }
	}

	/**
	 * Send a message to all online residents of a town
	 * 
	 * @param town
	 * @param lines
	 */
	public static void sendTownMessage(Town town, List<String> lines) {
        sendTownMessage(town, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to all online residents of a nation
	 * 
	 * @param nation
	 * @param lines
	 */
	public static void sendNationMessage(Nation nation, List<String> lines) {
        sendNationMessage(nation, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to ALL online players and the log.
	 * 
	 * @param lines
	 */
	public static void sendGlobalMessage(List<String> lines) {
        sendGlobalMessage(lines.toArray(new String[0]));
	}
	
	/**
	 * Send a message to ALL online players and the log.
	 * 
	 * @param lines
	 */
	public static void sendGlobalMessage(String[] lines) {
        for (String line : lines)
        	TownyUniverse.plugin.log("[Global Msg] " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers())
                for (String line : lines)
                        player.sendMessage(line);
	}

	/**
	 * Send a message to All online players and the log.
	 * @param line
	 */
	public static void sendGlobalMessage(String line) {
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers()) {
                player.sendMessage(line);
                TownyUniverse.plugin.log("[Global Message] " + player.getName() + ": " + line);
        }
	}

	/**
	 * Send a message to a specific resident
	 * 
	 * @param resident
	 * @param lines
	 * @throws TownyException
	 */
	public static void sendResidentMessage(Resident resident, String[] lines) throws TownyException {
        for (String line : lines)
        	TownyUniverse.plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
        Player player = TownyUniverse.plugin.getTownyUniverse().getPlayer(resident);
        for (String line : lines)
                player.sendMessage(line);
        
	}
	
	/**
	 * Send a message to a specific resident
	 * 
	 * @param resident
	 * @param lines
	 * @throws TownyException
	 */
	public static void sendResidentMessage(Resident resident, String line) throws TownyException {
        TownyUniverse.plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
        Player player = TownyUniverse.plugin.getTownyUniverse().getPlayer(resident);
        player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}

	/**
	 * Send a message to All online residents of a town and log
	 * 
	 * @param town
	 * @param lines
	 */
	public static void sendTownMessage(Town town, String[] lines) {
        for (String line : lines)
        	TownyUniverse.plugin.log("[Town Msg] " + town.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(town)){
                for (String line : lines)
                        player.sendMessage(line);
        }
	}
	
	/**
	 * Send a message to All online residents of a town and log
	 * 
	 * @param town
	 * @param lines
	 */
	public static void sendTownMessagePrefixed(Town town, String line) {
		TownyUniverse.plugin.log("[Town Msg] " + town.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(town))
                player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}
	
	/**
	 * Send a message to All online residents of a town and log
	 * 
	 * @param town
	 * @param lines
	 */
	public static void sendTownMessage(Town town, String line) {
		TownyUniverse.plugin.log("[Town Msg] " + town.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(town))
                player.sendMessage(line);
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * 
	 * @param nation
	 * @param lines
	 */
	public static void sendNationMessage(Nation nation, String[] lines) {
        for (String line : lines)
        	TownyUniverse.plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(nation))
                for (String line : lines)
                        player.sendMessage(line);
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * 
	 * @param nation
	 * @param lines
	 */
	public static void sendNationMessage(Nation nation, String line) {
		TownyUniverse.plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(nation))
                player.sendMessage(line);
	}
	
	/**
	 * Send a message to All online residents of a nation and log
	 * 
	 * @param nation
	 * @param lines
	 */
	public static void sendNationMessagePrefixed(Nation nation, String line) {
		TownyUniverse.plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
        for (Player player : TownyUniverse.plugin.getTownyUniverse().getOnlinePlayers(nation))
                player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}

	/**
	 * Send the town board to a player (in yellow)
	 * 
	 * @param player
	 * @param town
	 */
	public static void sendTownBoard(Player player, Town town) {
        for (String line : ChatTools.color(Colors.Gold + "[" + town.getName() + "] " + Colors.Yellow + town.getTownBoard()))
                player.sendMessage(line);
	}
	
}