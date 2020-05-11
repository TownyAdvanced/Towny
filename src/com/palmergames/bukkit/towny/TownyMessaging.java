package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Towny message handling class
 *
 * @author ElgarL
 *
 */

public class TownyMessaging {
	private static final Logger LOGGER = LogManager.getLogger(Towny.class);
	private static final Logger LOGGER_DEBUG = LogManager.getLogger("com.palmergames.bukkit.towny.debug");

	/**
	 * Sends an error message to the log
	 *
	 * @param msg message to send
	 */
	public static void sendErrorMsg(String msg) {
		LOGGER.warn(ChatTools.stripColour("[Towny] Error: " + msg));
	}

	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message to send
	 */
	public static void sendErrorMsg(Object sender, String msg) {
		if (sender != null) {
			CommandSender toSend = (CommandSender) sender;
			if (toSend instanceof ConsoleCommandSender) {
				toSend.sendMessage(ChatColor.stripColor(msg));
			} else {
				toSend.sendMessage(TownySettings.getLangString("default_towny_prefix") + ChatColor.RED + msg);
			}
		} else {
			sendErrorMsg("Sender cannot be null!");
		}
		
		sendDevMsg(msg);
	}

	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message array being sent.
	 */
	public static void sendErrorMsg(Object sender, String[] msg) {
		for (String line : msg) {
			sendErrorMsg(sender, line);
		}
	}

	/**
	 * Sends a message to console only
	 * prefixed by [Towny]
	 *
	 * @param msg the message to be sent
	 */
	public static void sendMsg(String msg) {
		
		LOGGER.info("[Towny] " + ChatTools.stripColour(msg));
	}

	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message being sent
	 */
	public static void sendMsg(Object sender, String msg) {
		if (sender != null) {
			if (sender instanceof Resident) {
				Player p = TownyAPI.getInstance().getPlayer((Resident) sender);
				if (p != null) {
					p.sendMessage(TownySettings.getLangString("default_towny_prefix") + ChatColor.GREEN + msg);
				}
			} else {
				CommandSender toSend = (CommandSender) sender;
				if (toSend instanceof ConsoleCommandSender) {
					toSend.sendMessage(ChatColor.stripColor(msg));
				} else {
					toSend.sendMessage(TownySettings.getLangString("default_towny_prefix") + ChatColor.GREEN + msg);
				}
			}
		} else {
			sendErrorMsg("Sender cannot be null!");
		}
		
		sendDevMsg(msg);
	}

	// todo: these two can probably be consolidated
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param player the player to receive the message
	 * @param msg the message to be sent
	 */
	public static void sendMsg(Player player, String[] msg) {
		for (String line : msg) {
			sendMsg(player, line);
		}
	}
	
	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param player the player to receive the message
	 * @param msg the message to be sent
	 */
	public static void sendMsg(Player player, List<String> msg) {
		for (String line : msg) {
			sendMsg(player, line);
		}
	}

	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDevMsg(String msg) {
		if (TownySettings.isDevMode()) {
			Player townyDev = BukkitTools.getPlayer(TownySettings.getDevName());
			if (townyDev == null) {
				return;
			}
			townyDev.sendMessage(TownySettings.getLangString("default_towny_prefix") + " DevMode: " + ChatColor.RED + msg);
		}
	}

	/**
	 * Sends a message (red) to the named Dev (if DevMode is enabled)
	 * Uses default_towny_prefix
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDevMsg(String[] msg) {
		for (String line : msg) {
			sendDevMsg(line);
		}
	}

	/**
	 * Sends a message to the log and console
	 * prefixed by [Towny] Debug:
	 *
	 * @param msg the message to be sent
	 */
	public static void sendDebugMsg(String msg) {
		if (TownySettings.getDebug()) {
			LOGGER_DEBUG.info(ChatTools.stripColour("[Towny] Debug: " + msg));
		}
		sendDevMsg(msg);
	}

	/////////////////

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param lines List of strings to send
	 */
	public static void sendMessage(Object sender, List<String> lines) {
		sendMessage(sender, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param line the String to send
	 */
	public static void sendMessage(Object sender, String line) {
		if ((sender instanceof Player)) {
			((Player) sender).sendMessage(line);
		} else if (sender instanceof CommandSender) {
			((CommandSender) sender).sendMessage(line);
		} else if (sender instanceof Resident) {
			Player p = TownyAPI.getInstance().getPlayer((Resident) sender);
			if (p == null) {
				return;
			}
			p.sendMessage(Colors.strip(line));
		}
	}

	/**
	 * Send a message to a player
	 *
	 * @param sender the Object sending the message
	 * @param lines String array to send as message.
	 */
	public static void sendMessage(Object sender, String[] lines) {
		boolean isPlayer = false;
		if (sender instanceof Player) {
			isPlayer = true;
		}

		for (String line : lines) {
			if (isPlayer) {
				((Player) sender).sendMessage(line);
			} else if (sender instanceof CommandSender) {
				((CommandSender) sender).sendMessage(line);
			} else if (sender instanceof Resident) {
				Player p = TownyAPI.getInstance().getPlayer((Resident) sender);
				if (p == null) {
					return;
				}
				p.sendMessage(Colors.strip(line));
			}
		}
	}

	/**
	 * Send a message to all online residents of a town
	 * Doesn't use a [Towny] or [TownName] prefix.
	 * It is prefered to use sendPrefixedTownMessage or sendTownMessagePrefixed.
	 *
	 * @param town to receive message
	 * @param lines String list to send as a message
	 */
	@Deprecated
	public static void sendTownMessage(Town town, List<String> lines) {
		sendTownMessage(town, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to all online residents of a nation
	 * Doesn't use a [Towny] or [NationName] prefix.
	 * It is prefered to use sendPrefixedNationMessage or sendNationMessagePrefixed.
	 *
	 * @param nation nation to receive message
	 * @param lines String list to send as a message
	 */
	@Deprecated
	public static void sendNationMessage(Nation nation, List<String> lines) {
		sendNationMessage(nation, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String list to send as a message
	 */
	public static void sendGlobalMessage(List<String> lines) {
		sendGlobalMessage(lines.toArray(new String[0]));
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String array to send as a message
	 */
	public static void sendGlobalMessage(String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Global Msg] " + line));
		}
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				for (String line : lines) {
					player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
				}
			}
		}
	}

	/**
	 * Send a message to All online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param line the message to send
	 */
	public static void sendGlobalMessage(String line) {
		LOGGER.info(ChatTools.stripColour("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null)
				try {
					if (TownyUniverse.getInstance().getDataSource().getWorld(player.getLocation().getWorld().getName()).isUsingTowny())
						player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
		}
	}
	

	/**
	 * Send a message to All online players and the log.
	 * Does not use the default_towny_prefix.
	 * 
	 * @param line the message to send.
	 */
	public static void sendPlainGlobalMessage(String line) {
		LOGGER.info(ChatTools.stripColour("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null)
				try {
					if (TownyUniverse.getInstance().getDataSource().getWorld(player.getLocation().getWorld().getName()).isUsingTowny())
						player.sendMessage(line);
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}
		}		
	}

	/**
	 * Send a message to a specific resident
	 * preceded by the default_towny_prefix
	 *
	 * @param resident the resident to receive the message
	 * @param line message String to send
	 * @throws TownyException if the player is null
	 */
	public static void sendResidentMessage(Resident resident, String line) throws TownyException {
		LOGGER.info(ChatTools.stripColour("[Resident Msg] " + resident.getName() + ": " + line));
		Player player = TownyAPI.getInstance().getPlayer(resident);
		if (player == null) {
			throw new TownyException("Player could not be found!");
		}
		player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}

	/**
	 * Send a multi-line message to All online residents of a town and log
	 * Doesn't use a [Towny] or [TownName] prefix.
	 * It is prefered to use sendPrefixedTownMessage or sendTownMessagePrefixed.
	 * 
	 * @param town the town to send a message to
	 * @param lines array of Strings constituting the message.
	 */
	@Deprecated
	public static void sendTownMessage(Town town, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Town Msg] " + town.getName() + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town)) {
			for (String line : lines) {
				player.sendMessage(line);
			}
		}
	}

	/**
	 * Send a message to All online residents of a town and log
	 * Doesn't use a [Towny] or [TownName] prefix.
	 * It is prefered to use sendPrefixedTownMessage or sendTownMessagePrefixed.
	 *
	 * @param town town to send message to
	 * @param line the message to be sent
	 */
	@Deprecated
	public static void sendTownMessage(Town town, String line) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + town.getName() + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(line);
	}

	/**
	 * Send a message to All online residents of a town and log, 
	 * preceded by the default_towny_prefix
	 *
	 * @param town town to receive the message
	 * @param line the message
	 */
	public static void sendTownMessagePrefixed(Town town, String line) {
		LOGGER.info(ChatTools.stripColour(line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}

	/**
	 * Send a message to All online residents of a town and log
	 * preceded by the [Townname]
	 *
	 * @param town the town to pass the message to, and prefix message with
	 * @param line the actual message
	 */
	public static void sendPrefixedTownMessage(Town town, String line) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + town.getName() + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(String.format(TownySettings.getLangString("default_town_prefix"), town.getName()) + line);
	}

	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines Array of Strings constituting the message.
	 */
	public static void sendPrefixedTownMessage(Town town, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour(line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			for (String line : lines) {
				player.sendMessage(String.format(TownySettings.getLangString("default_town_prefix"), town.getName()) + line);
			}
	}
	
	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines List of Strings constituting the message.
	 */
	public static void sendPrefixedTownMessage(Town town, List<String> lines) {
		sendPrefixedTownMessage(town, lines.toArray(new String[0]));
	}
	
	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * Doesn't use a [Towny] or [NationName] prefix.
	 * It is prefered to use sendPrefixedNationMessage or sendNationMessagePrefixed.
	 *
	 * @param nation the nation to send to
	 * @param lines array of Strings containing the message
	 */
	@Deprecated
	public static void sendNationMessage(Nation nation, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation)) {
			for (String line : lines) {
				player.sendMessage(line);
			}
		}
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * Doesn't use a [Towny] or [NationName] prefix.
	 * It is prefered to use sendPrefixedNationMessage or sendNationMessagePrefixed.
	 *
	 * @param nation nation to send message to
	 * @param line the message
	 */
	@Deprecated
	public static void sendNationMessage(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(line);
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation nation to send to, and prefix message with
	 * @param line the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(String.format(TownySettings.getLangString("default_nation_prefix"), nation.getName()) + line);
	}

	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation the nation to send to
	 * @param lines list of Strings containing the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, List<String> lines) {
		sendPrefixedNationMessage(nation, lines.toArray(new String[0]));
	}

	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation the nation to send to
	 * @param lines array of Strings containing the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation)) {
			for (String line : lines) {
				player.sendMessage(String.format(TownySettings.getLangString("default_nation_prefix"), nation.getName()) + line);
			}
		}
	}
	
	/**
	 * Send a message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param line the message
	 */
	public static void sendNationMessagePrefixed(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
	}
	
	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param lines the list of lines of the message
	 */
	public static void sendNationMessagePrefixed(Nation nation, List<String> lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + nation.getName() + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			for (String line : lines) {
				player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
			}
	}

	/**
	 * Send the town board to a player (in yellow)
	 *
	 * @param player player to show to
	 * @param town the town for which to show it's board
	 */
	public static void sendTownBoard(Player player, Town town) {
		String tbColor1 = TownySettings.getLangString("townboard_message_colour_1");
		String tbColor2 = TownySettings.getLangString("townboard_message_colour_2");
		
		player.sendMessage(tbColor1 + "[" + town.getName() + "] " + tbColor2 + town.getTownBoard());
	}
	
	/**
	 * Send the nation board to a player (in yellow)
	 *
	 * @param player player to show to
	 * @param nation the nation for which to show it's board
	 */
	public static void sendNationBoard(Player player, Nation nation) {
		String nbColor1 = TownySettings.getLangString("nationboard_message_colour_1");
		String nbColor2 = TownySettings.getLangString("nationboard_message_colour_2");

		player.sendMessage(nbColor1 + "[" + nation.getName() + "] " + nbColor2 + nation.getNationBoard());
	}
	
	/**
	 * Send a message to all residents in the list with the required mode
	 *
	 * @param residents List of residents to show the message to
	 * @param msg the message to send
	 * @param modeRequired a resident mode required for the resident to receive the message.
	 */
	public static void sendMessageToMode(ResidentList residents, String msg, String modeRequired) {

		for (Resident resident : TownyAPI.getInstance().getOnlineResidents(residents))
			if (resident.hasMode(modeRequired))
				sendMessage(resident, msg);
	}
	
	/**
	 * Send a message to all residents in the town with the required mode
	 * no prefix used
	 * 
	 * @param town the town to send message to
	 * @param msg the message to send
	 * @param modeRequired mode a resident must have to receive message
	 */
	public static void sendMessageToMode(Town town, String msg, String modeRequired) {

		for (Resident resident : town.getResidents())
			if (BukkitTools.isOnline(resident.getName()))
				sendMessage(resident,msg);
	}
	
	/**
	 * Send a message to all residents in the nation with the required mode
	 * no prefix used
	 * 
	 * @param nation the nation to receive the message
	 * @param msg the message to send
	 * @param modeRequired mode a resident must have to receive message
	 */
	public static void sendMessageToMode(Nation nation, String msg, String modeRequired) {

		for (Resident resident : nation.getResidents())
			if (BukkitTools.isOnline(resident.getName()))
				sendMessage(resident,msg);
	}
	
	/**
	 * Send a Title and Subtitle to a resident
	 *
	 * @param resident resident to receive title &amp; subtitle message
	 * @param title title message to send
	 * @param subtitle subtitle message to send
	 * @throws TownyException if the player is null
	 */
	public static void sendTitleMessageToResident(Resident resident, String title, String subtitle) throws TownyException {
		Player player = TownyAPI.getInstance().getPlayer(resident);
		if (player == null) {
			throw new TownyException("Player could not be found!");
		}
		player.sendTitle(title, subtitle, 10, 70, 10);
	}
	
	/**
	 * Send a Title and Subtitle to a town
	 *
	 * @param town town to receive title &amp; subtitle messages
	 * @param title title message to send
	 * @param subtitle subtitle message to send
	 */
	public static void sendTitleMessageToTown(Town town, String title, String subtitle) {
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendTitle(title, subtitle, 10, 70, 10);
	}

	/**
	 * Send a Title and Subtitle to a nation
	 *
	 * @param nation   - Nation object
	 * @param title    - Title
	 * @param subtitle - Subtitle
	 */
	public static void sendTitleMessageToNation(Nation nation, String title, String subtitle) {
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendTitle(title, subtitle, 10, 70, 10);
	}

	public static void sendConfirmationMessage(CommandSender sender, String firstline, String confirmline, String cancelline, String lastline) {
		
		if (Towny.isSpigot && sender instanceof Player) {
			TownySpigotMessaging.sendSpigotConfirmMessage(sender, firstline, confirmline, cancelline, lastline);
			return;
		}
		
		if (firstline == null) {
			firstline = TownySettings.getLangString("confirmation_prefix") + TownySettings.getLangString("are_you_sure_you_want_to_continue");
		}
		if (confirmline == null) {
			confirmline = ChatColor.GREEN + "          /" + TownySettings.getConfirmCommand();
		}
		if (cancelline == null) {
			cancelline = ChatColor.GREEN + "          /" + TownySettings.getCancelCommand();
		}
		if (lastline != null && lastline.equals("")) {
			String[] message = new String[]{firstline, confirmline, cancelline};
			sendMessage(sender, message);
			return;
		}
		if (lastline == null) {
			lastline = TownySettings.getLangString("this_message_will_expire2");
			String[] message = new String[]{firstline, confirmline, cancelline, lastline};
			sendMessage(sender, message);
		}
	}

	

	public static void sendRequestMessage(CommandSender player, Invite invite) {
		
		if (Towny.isSpigot) {
			TownySpigotMessaging.sendSpigotRequestMessage(player, invite);
			return;
		}
		
		if (invite.getSender() instanceof Town) { // Town invited Resident
			String firstline = TownySettings.getLangString("invitation_prefix") + String.format(TownySettings.getLangString("you_have_been_invited_to_join2"), invite.getSender().getName());
			String secondline = ChatColor.GREEN + "          /" + TownySettings.getAcceptCommand() + " " + invite.getSender().getName();
			String thirdline = ChatColor.GREEN +  "          /" + TownySettings.getDenyCommand() + " " + invite.getSender().getName();
			sendConfirmationMessage(player, firstline, secondline, thirdline, "");
		}
		if (invite.getSender() instanceof Nation) {
			if (invite.getReceiver() instanceof Town) { // Nation invited Town
				String firstline = TownySettings.getLangString("invitation_prefix") + String.format(TownySettings.getLangString("you_have_been_invited_to_join2"), invite.getSender().getName());
				String secondline = ChatColor.GREEN + "          /t invite accept " + invite.getSender().getName();
				String thirdline = ChatColor.GREEN +  "          /t invite deny " + invite.getSender().getName();
				sendConfirmationMessage(player, firstline, secondline, thirdline, "");
			}
			if (invite.getReceiver() instanceof Nation) { // Nation allied Nation
				String firstline = TownySettings.getLangString("invitation_prefix") + String.format(TownySettings.getLangString("you_have_been_requested_to_ally2"), invite.getSender().getName());
				String secondline = ChatColor.GREEN + "          /n ally accept " + invite.getSender().getName();
				String thirdline = ChatColor.GREEN +  "          /n ally deny " + invite.getSender().getName();
				sendConfirmationMessage(player, firstline, secondline, thirdline, "");
			}
		}
	}

}
