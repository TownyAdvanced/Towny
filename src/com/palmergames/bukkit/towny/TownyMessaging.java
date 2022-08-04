package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.bossbar.BossBar.Color;
import net.kyori.adventure.bossbar.BossBar.Overlay;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

/**
 * Towny message handling class
 *
 * @author ElgarL
 *
 */

public class TownyMessaging {
	private static final Logger LOGGER = LogManager.getLogger("Towny");
	private static final Logger LOGGER_DEBUG = LogManager.getLogger("com.palmergames.bukkit.towny.debug");

	/*
	 * NON-TRANSLATABLE MESSAGING METHODS
	 * 
	 * Use these methods for sending messages, error 
	 * messages and debug/dev messages. Many other 
	 * messaging methods will end up using these to 
	 * send directly to the player/console. 
	 */
	
	/**
	 * Sends an error message to the log
	 *
	 * @param msg message to send
	 */
	public static void sendErrorMsg(String msg) {
		LOGGER.warn(ChatTools.stripColour("Error: " + msg));
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
		if (sender != null && sender instanceof CommandSender toSend) {
			if (toSend instanceof ConsoleCommandSender) {
				// Console
				toSend.sendMessage(Translatable.of("default_towny_prefix").stripColors(true).defaultLocale() + ChatColor.stripColor(msg));
			} else {
				// Player
				toSend.sendMessage(Translation.of("default_towny_prefix") + ChatColor.RED + msg);
			}
		} else if (sender != null && sender instanceof TownyObject townySender) {
			if (townySender instanceof Resident resident) {
				// Resident
				sendMessage(resident, Translation.of("default_towny_prefix") + ChatColor.RED + msg);
			} else if (townySender instanceof Town town) {
				// Town
				sendPrefixedTownMessage(town, ChatColor.RED + msg);
			} else if (townySender instanceof Nation nation) {
				// Nation
				sendPrefixedNationMessage(nation, ChatColor.RED + msg);
			}
		} else {
			sendErrorMsg("Sender cannot be null!");
		}
		
		sendDevMsg(msg);
	}

	/**
	 * Sends a message to console only
	 * prefixed by [Towny]
	 *
	 * @param msg the message to be sent
	 */
	public static void sendMsg(String msg) {
		
		LOGGER.info(ChatTools.stripColour(msg));
	}

	/**
	 * Towny's main endpoint for Towny-Prefixed messages.
	 * 
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the CommandSender receiving the msg
	 * @param msg the message being sent
	 */
	public static void sendMsg(CommandSender sender, String msg) {
		if (sender == null || msg.isEmpty())
			return;
		
		if (sender instanceof Player p) {
			p.sendMessage(Translatable.of("default_towny_prefix").forLocale(p) + ChatColor.GREEN + msg);
		} else if (sender instanceof ConsoleCommandSender) {
			sender.sendMessage(Translatable.of("default_towny_prefix").stripColors(true).defaultLocale() + ChatColor.stripColor(msg));
		} else {
			sender.sendMessage(Translatable.of("default_towny_prefix").forLocale(sender) + ChatColor.GREEN + msg);
		}
		
		sendDevMsg(msg);
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
			if (townyDev != null)
				townyDev.sendMessage(Translatable.of("default_towny_prefix").forLocale(townyDev) + " DevMode: " + ChatColor.RED + msg);
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

	/**
	 * Send a message to a player with no Towny prefix.
	 *
	 * @param sender the Object sending the message
	 * @param lines List of strings to send
	 */
	public static void sendMessage(Object sender, List<String> lines) {
		sendMessage(sender, lines.toArray(new String[0]));
	}

	/**
	 * Send a message to a player with no Towny prefix.
	 *
	 * @param sender the Object sending the message
	 * @param line the String to send
	 */
	public static void sendMessage(Object sender, String line) {
		if (line.isEmpty())
			return;
		
		if (sender instanceof Player player) {
			player.sendMessage(line);
		} else if (sender instanceof CommandSender commandSender) {
			commandSender.sendMessage(Colors.strip(line));
		} else if (sender instanceof Resident resident) {
			Player p = TownyAPI.getInstance().getPlayer(resident);
			if (p != null)
				p.sendMessage(line);
		}
	}

	/**
	 * Send a message to a player with no Towny prefix.
	 *
	 * @param sender the Object sending the message
	 * @param lines String array to send as message.
	 */
	public static void sendMessage(Object sender, String[] lines) {
		for (String line : lines)
			sendMessage(sender, line);
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
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				player.sendMessage(Translation.of("default_towny_prefix") + line);
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
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				player.sendMessage(line);
		}		
	}

	/*
	 * PREFIXED TOWN AND NATION MESSAGES
	 * 
	 * Used primarily for /n say and /t say.
	 */
	
	/**
	 * Send a message to All online residents of a town and log
	 * preceded by the [Townname]
	 *
	 * @param town the town to pass the message to, and prefix message with
	 * @param line the actual message
	 */
	public static void sendPrefixedTownMessage(Town town, String line) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + StringMgmt.remUnderscore(town.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(Translation.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())) + line);
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation nation to send to, and prefix message with
	 * @param line the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(Translation.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())) + line);
	}

	/*
	 * TOWN AND NATION BOARD MESSAGES
	 */

	/**
	 * Send the town board to a player (in yellow)
	 *
	 * @param player player to show to
	 * @param town the town for which to show it's board
	 */
	public static void sendTownBoard(Player player, Town town) {
		String tbColor1 = Translation.of("townboard_message_colour_1");
		String tbColor2 = Translation.of("townboard_message_colour_2");
		
		player.sendMessage(tbColor1 + "[" + StringMgmt.remUnderscore(town.getName()) + "] " + tbColor2 + town.getBoard());
	}
	
	/**
	 * Send the nation board to a player (in yellow)
	 *
	 * @param sender Sender to show to
	 * @param nation the nation for which to show it's board
	 */
	public static void sendNationBoard(CommandSender sender, Nation nation) {
		String nbColor1 = Translation.of("nationboard_message_colour_1");
		String nbColor2 = Translation.of("nationboard_message_colour_2");

		sender.sendMessage(nbColor1 + "[" + StringMgmt.remUnderscore(nation.getName()) + "] " + nbColor2 + nation.getBoard());
	}
	
	/*
	 * TITLE/SUBTITLE MESSAGES
	 */
	
	/**
	 * Send a Title and Subtitle to a resident
	 *
	 * @param resident resident to receive title &amp; subtitle message
	 * @param title title message to send
	 * @param subtitle subtitle message to send
	 */
	public static void sendTitleMessageToResident(Resident resident, String title, String subtitle) {
		Player player = resident.getPlayer();
		if (player == null)
			return;
		sendTitle(player, title, subtitle);
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
			sendTitle(player, title, subtitle);
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
			sendTitle(player, title, subtitle);
	}

	/**
	 * Send the player a Title message
	 * 
	 * As of MC 1.18 a null title will mean the message is never sent, so we are
	 * changing empty Strings to " ".
	 * 
	 * @param player   Player being send the Title message.
	 * @param title    String title message.
	 * @param subtitle String subtitle message.
	 */
	public static void sendTitle(Player player, String title, String subtitle) {
		player.sendTitle(title.isEmpty() ? " " : title, subtitle.isEmpty() ? " " : subtitle, 10, 70, 10);
	}

	/*
	 * REQUESTS/CONFIRMATION
	 */
	
	public static void sendRequestMessage(CommandSender player, Invite invite) {
		final Translator translator = Translator.locale(player);
		String senderName = invite.getSender().getName();
		if (invite.getSender() instanceof Town town) { // Town invited Resident
			String firstline = town.hasNation()
					? translator.of("invitation_prefix") + translator.of("you_have_been_invited_to_join3", Colors.colorTown(senderName), Colors.colorNation(town.getNationOrNull()))
					: translator.of("invitation_prefix") + translator.of("you_have_been_invited_to_join2", Colors.colorTown(senderName));
			String confirmline = TownySettings.getAcceptCommand() + " " + senderName;
			String cancelline = TownySettings.getDenyCommand() + " " + senderName;
			sendInvitationMessage(player, firstline, confirmline, cancelline);
		}
		if (invite.getSender() instanceof Nation) {
			if (invite.getReceiver() instanceof Town) { // Nation invited Town
				String firstline = translator.of("invitation_prefix") + translator.of("your_town_has_been_invited_to_join_nation", Colors.colorNation(senderName));
				String confirmline = "t invite accept " + senderName;
				String cancelline = "t invite deny " + senderName;
				sendInvitationMessage(player, firstline, confirmline, cancelline);
			}
			if (invite.getReceiver() instanceof Nation) { // Nation allied Nation
				String firstline = translator.of("invitation_prefix") + translator.of("you_have_been_requested_to_ally2", Colors.colorNation(senderName));
				String confirmline = "n ally accept " + senderName;
				String cancelline = "n ally deny " + senderName;
				sendInvitationMessage(player, firstline, confirmline, cancelline);
			}
		}
	}

	/**
	 * Sends a player click-able invitation messages.
	 * @param player - The player (CommandSender) to send the confirmation
	 * @param firstline - The question regarding the confirmation.
	 * @param confirmline - Line for sending the confirmation.
	 * @param cancelline - Line for sending the cancellation.
	 */
	public static void sendInvitationMessage(CommandSender player, String firstline, String confirmline, String cancelline) {
		final Translator translator = Translator.locale(player);
		// Create confirm button based on given params.
		TextComponent confirmComponent = Component.text("[/" + confirmline + "]")
			.color(NamedTextColor.GREEN)
			.hoverEvent(HoverEvent.showText(Component.text(translator.of("msg_confirmation_spigot_click_accept", confirmline, "/" + confirmline))))
			.clickEvent(ClickEvent.runCommand("/towny:" + confirmline));

		// Create cancel button based on given params.
		TextComponent cancelComponent = Component.text("[/" + cancelline + "]")
			.color(NamedTextColor.RED)
			.hoverEvent(HoverEvent.showText(Component.text(translator.of("msg_confirmation_spigot_click_cancel", cancelline, "/" + cancelline))))
			.clickEvent(ClickEvent.runCommand("/towny:" + cancelline));
		
		Towny.getAdventure().sender(player).sendMessage(Component.text(firstline).append(Component.newline())
			.append(confirmComponent).append(Component.space()).append(cancelComponent));
	}
	
	/**
	 * Sends a player click-able confirmation messages.
	 * @param sender - The player (CommandSender) to send the confirmation
	 * @param confirmation - Confirmation to send to the player.
	 */
	public static void sendConfirmationMessage(CommandSender sender, Confirmation confirmation) {
		final Translator translator = Translator.locale(sender);
		TextComponent firstLineComponent = Component.text(translator.of("confirmation_prefix") + confirmation.getTitle().forLocale(sender));
		TextComponent lastLineComponent = Component.text(translator.of("this_message_will_expire2", confirmation.getDuration()));

		// Create confirm button based on given params.
		TextComponent confirmComponent = Component.text("[/" + confirmation.getConfirmCommand() + "]")
			.color(NamedTextColor.GREEN)
			.hoverEvent(HoverEvent.showText(Component.text(translator.of("msg_confirmation_spigot_click_accept", confirmation.getConfirmCommand(), "/" + confirmation.getConfirmCommand()))))
			.clickEvent(ClickEvent.runCommand("/" + confirmation.getPluginPrefix() + ":" + confirmation.getConfirmCommand()));

		// Create cancel button based on given params.
		TextComponent cancelComponent = Component.text("[/" + confirmation.getCancelCommand() + "]")
			.color(NamedTextColor.RED)
			.hoverEvent(HoverEvent.showText(Component.text(translator.of("msg_confirmation_spigot_click_cancel", confirmation.getCancelCommand(), "/" + confirmation.getCancelCommand()))))
			.clickEvent(ClickEvent.runCommand("/" + confirmation.getPluginPrefix() + ":" + confirmation.getCancelCommand()));
		
		Towny.getAdventure().sender(sender).sendMessage(
			firstLineComponent.append(Component.newline())
			.append(confirmComponent).append(Component.space()).append(cancelComponent).append(Component.newline())
			.append(lastLineComponent)
		);
	}

	/*
	 * PAGINATED LIST METHODS
	 */
	
	public static void sendTownList(CommandSender sender, List<TextComponent> towns, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, towns.size());

		TextComponent[] townsformatted;
		
		if ((page * 10) > towns.size()) {
			townsformatted = new TextComponent[towns.size() % 10];
		} else {
			townsformatted = new TextComponent[10];
		}
		
		// Populate the page with TextComponents.
		for (int i = (page - 1) * 10; i < iMax; i++) {
			townsformatted[i % 10] = towns.get(i);
		}
		
		Audience audience = Towny.getAdventure().sender(sender);
		sender.sendMessage(ChatTools.formatTitle(Translation.of("town_plu")));
		sender.sendMessage(Colors.Blue + Translation.of("town_name") + (TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + Translation.of(compType.getName())));
		for (TextComponent textComponent : townsformatted)
			audience.sendMessage(textComponent);
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town list", page, compType.getCommandString(), total);
		audience.sendMessage(pageFooter);
	}

	public static TextComponent getPageNavigationFooter(String prefix, int page, String arg, int total) {
		TextComponent backButton = Component.text("<<<")
			.color(NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " " + (arg.isEmpty() ? "" : arg + " ") + (page - 1)))
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_hover_previous_page"))));
		
		TextComponent forwardButton = Component.text(">>>")
			.color(NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " " +  (arg.isEmpty() ? "" : arg + " ") + (page + 1)))
			.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_hover_next_page"))));
		
		TextComponent pageText = Component.text("   " + Translation.of("LIST_PAGE", page, total) + "   ");

		if (page == 1 && page == total) {
			backButton = backButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
			forwardButton = forwardButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		} else if (page == 1) {
			backButton = backButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		} else if (page == total) {
			forwardButton = forwardButton.clickEvent(null).hoverEvent(null).color(NamedTextColor.DARK_GRAY);
		}

		return backButton.append(pageText).append(forwardButton);
	}

	public static void sendNationList(CommandSender sender, List<TextComponent> nations, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, nations.size());

		TextComponent[] nationsformatted;
		if ((page * 10) > nations.size()) {
			nationsformatted = new TextComponent[nations.size() % 10];
		} else {
			nationsformatted = new TextComponent[10];
		}
		
		// Populate the page with TextComponents.
		for (int i = (page - 1) * 10; i < iMax; i++) {
			nationsformatted[i % 10] = nations.get(i);
		}

		sender.sendMessage(ChatTools.formatTitle(Translatable.of("nation_plu").forLocale(sender)));
		sender.sendMessage(Colors.Blue + Translatable.of("nation_name").forLocale(sender) + Colors.Gray + " - " + Colors.LightBlue + Translatable.of(compType.getName()).forLocale(sender));
		Audience audience = Towny.getAdventure().sender(sender);
		for (TextComponent textComponent : nationsformatted) {
			audience.sendMessage(textComponent);
		}

		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:nation list", page, compType.getCommandString(), total);
		audience.sendMessage(pageFooter);
	}

	public static void sendOutpostList(Player player, Town town, int page, int total) {
		int outpostsCount = town.getAllOutpostSpawns().size();
		int iMax = Math.min(page * 10, outpostsCount);
		List<Location> outposts = town.getAllOutpostSpawns();
		
		TextComponent[] outpostsFormatted;
		
		if ((page * 10) > outpostsCount) {
			outpostsFormatted = new TextComponent[outpostsCount % 10];
		} else {
			outpostsFormatted = new TextComponent[10];
		}
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Location outpost = outposts.get(i);
			TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
			if (tb == null)
				continue;
			String name = !tb.hasPlotObjectGroup() ? tb.getName() : tb.getPlotObjectGroup().getName();
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);		
			TextComponent line = Component.text(Integer.toString(i + 1))
				.color(NamedTextColor.GOLD)
				.clickEvent(ClickEvent.runCommand("/towny:town outpost " + (i + 1)))
				.append(dash);

			TextComponent outpostName = Component.text(name).color(NamedTextColor.GREEN);
			TextComponent worldName = Component.text(outpost.getWorld().getName()).color(NamedTextColor.BLUE);
			TextComponent coords = Component.text("(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")").color(NamedTextColor.BLUE);

			if (!name.equalsIgnoreCase("")) {
				line = line.append(outpostName).append(dash);
			}
			line = line.append(worldName).append(dash).append(coords);
			
			String spawnCost = "Free";

			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			line = line.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_click_spawn", name.equalsIgnoreCase("") ? "outpost" : name) + "\n" + spawnCost).color(NamedTextColor.GOLD)));
			outpostsFormatted[i % 10] = line;
		}
		
		Audience audience = Towny.getAdventure().player(player);
		player.sendMessage(ChatTools.formatTitle(Translatable.of("outpost_plu").forLocale(player)));
		for (TextComponent textComponent : outpostsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town outpost list", page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendJailList(Player player, Town town, int page, int total) {
		int jailCount = town.getJails().size();
		int iMax = Math.min(page * 10, jailCount);
		List<Jail> jails = new ArrayList<>(town.getJails());
		
		TextComponent[] jailsFormatted;
		
		if ((page * 10) > jailCount) {
			jailsFormatted = new TextComponent[jailCount % 10];
		} else {
			jailsFormatted = new TextComponent[10];
		}
		String headerMsg = ChatColor.GOLD + "# " +
							ChatColor.DARK_GRAY + "- "+
							ChatColor.GREEN + "Jail Name " +
							ChatColor.DARK_GRAY + "- "+
							ChatColor.BLUE + "Coord " +
							ChatColor.DARK_GRAY + "- " +
							ChatColor.YELLOW + "Cell Count " +
							ChatColor.DARK_GRAY + "- " +
							ChatColor.RED + "Primary Jail";
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Jail jail = jails.get(i);

			TextComponent name = Component.text(jail.getName()).color(NamedTextColor.GREEN);
			TextComponent coord = Component.text(jail.getTownBlock().getWorldCoord().toString()).color(NamedTextColor.BLUE);
			TextComponent cellCount = Component.text(String.valueOf(jail.getJailCellLocations().size())).color(NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);

			TextComponent line = Component.text(Integer.toString(i + 1)).color(NamedTextColor.GOLD);
			if (jail.hasName())
				line = line.append(dash).append(name);
			line = line.append(dash).append(coord).append(dash).append(cellCount);
				
			if (town.getPrimaryJail().getUUID().equals(jail.getUUID()))
				line = line.append(dash).append(Component.text("(Primary Jail)").color(NamedTextColor.RED));

			jailsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().player(player);
		player.sendMessage(ChatTools.formatTitle(Translatable.of("jail_plu").forLocale(player)));
		player.sendMessage(headerMsg);
		for (TextComponent textComponent : jailsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town jail list", page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendPlotGroupList(CommandSender sender, Town town, int page, int total) {
		int groupCount = town.getPlotGroups().size();
		int iMax = Math.min(page * 10,  groupCount);
		List<PlotGroup> groups = new ArrayList<>(town.getPlotGroups());
		
		TextComponent[] groupsFormatted;
		if ((page * 10) > groupCount) {
			groupsFormatted = new TextComponent[groupCount % 10];
		} else {
			groupsFormatted = new TextComponent[10];
		}
		
		String headerMsg = ChatColor.GOLD + "# " +
				ChatColor.DARK_GRAY + "- "+
				ChatColor.GREEN + "Group Name " +
				ChatColor.DARK_GRAY + "- " +
				ChatColor.YELLOW + "Plot Size " +
				ChatColor.DARK_GRAY + "- " +
				ChatColor.BLUE + "For Sale";
		for (int i = (page - 1) * 10; i < iMax; i++) {
			PlotGroup group = groups.get(i);
			TextComponent name = Component.text(group.getFormattedName()).color(NamedTextColor.GREEN);
			TextComponent size = Component.text(String.valueOf(group.getTownBlocks().size())).color(NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ").color(NamedTextColor.DARK_GRAY);
			TextComponent line = Component.text(Integer.toString(i + 1)).color(NamedTextColor.GOLD);
			line = line.append(dash).append(name).append(dash).append(size);
			
			if (TownyEconomyHandler.isActive() && group.getPrice() != -1)
				line = line.append(dash).append(Component.text("(" + Translatable.of("towny_map_forsale").forLocale(sender) + ": " + TownyEconomyHandler.getFormattedBalance(group.getPrice()) + ")").color(NamedTextColor.BLUE));

			groupsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().sender(sender);
		sender.sendMessage(ChatTools.formatTitle(town.getName() + " " + Translatable.of("plotgroup_plu").forLocale(sender)));
		sender.sendMessage(headerMsg);
		for (TextComponent textComponent : groupsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town plotgrouplist" + town.getName(), page, "", total);
		audience.sendMessage(pageFooter);
	}
	
	/*
	 * TRANSLATABLES FOLLOW
	 */
	
	/**
	 * Sends a message in green, prefixed by the default_towny_prefix to the sender,
	 * translated to the end-user's locale.
	 *  
	 * @param sender CommandSender who will see the message. 
	 * @param translatables Translatable object(s) which will be translated, joined with a space.
	 * @see #sendMsg(CommandSender, Translatable)
	 */
	public static void sendMsg(CommandSender sender, Translatable... translatables) {
		sendMsg(sender, Translation.translateTranslatables(sender, translatables));
	}

	/**
	 * Sends a message in green, prefixed by the default_towny_prefix to the sender,
	 * translated to the end-user's locale.
	 *
	 * @param sender CommandSender who will see the message. 
	 * @param translatable Translatable object which will be translated.
	 */
	public static void sendMsg(CommandSender sender, Translatable translatable) {
		sendMsg(sender, translatable.locale(sender).translate());
	}

	/**
	 * Sends a message translated to the end-user's locale, with no prefix.
	 *  
	 * @param sender CommandSender who will see the message. 
	 * @param translatables Translatable... object(s) which will be translated.
	 * @see #sendMessage(CommandSender, Translatable)    
	 */
	public static void sendMessage(CommandSender sender, Translatable... translatables) {
		sendMessage(sender, Translation.translateTranslatables(sender, translatables));
	}

	/**
	 * Sends a message translated to the end-user's locale, with no prefix.
	 *
	 * @param sender CommandSender who will see the message. 
	 * @param translatable Translatable object which will be translated.
	 */
	public static void sendMessage(CommandSender sender, Translatable translatable) {
		sendMessage(sender, translatable.locale(sender).translate());
	}
	
	/**
	 * Sends an Error message (red) to the sender
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix.
	 * Translates to the end-user's locale.
	 * 
	 * @param sender CommandSender who will receive the error message.
	 * @param translatables Translatable... object(s) to be translated using the locale of the end-user.
	 * @see #sendErrorMsg(CommandSender, Translatable)    
	 */
	public static void sendErrorMsg(CommandSender sender, Translatable... translatables) {
		sendErrorMsg(sender, Translation.translateTranslatables(sender, translatables));
	}

	/**
	 * Sends an Error message (red) to the sender
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix.
	 * Translates to the end-user's locale.
	 *
	 * @param sender CommandSender who will receive the error message.
	 * @param translatable Translatable object to be translated using the locale of the end-user.
	 */
	public static void sendErrorMsg(CommandSender sender, Translatable translatable) {
		sendErrorMsg(sender, translatable.locale(sender).translate());
	}
	
	/**
	 * Send a message to All online players and the log.
	 * Uses default_towny_prefix. Message is translated for the end-user.
	 *
	 * @param translatable Translatable object to be messaged to the player using their locale.
	 */
	public static void sendGlobalMessage(Translatable translatable) {
		for (Player player : Bukkit.getOnlinePlayers())
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				sendMsg(player, translatable);
		LOGGER.info("[Global Message] " + translatable.stripColors(true).translate());
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * preceded by the [NationName], translated for the end-user.
	 * 
	 * @param nation Nation to pass the message to, prefix message with.
	 * @param message Translatable object to be messaged to the player using their locale.
	 */
	public static void sendPrefixedNationMessage(Nation nation, Translatable message) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + message.translate()));
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			sendMessage(player, Translation.translateTranslatables(player, "", Translatable.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())), message));
	}
	
	/**
	 * Send a message to All online residents of a town and log
	 * preceded by the [Townname], translated for the end-user.
	 *
	 * @param town Town to pass the message to, and prefix message with.
	 * @param message Translatable object to be messaged to the player using their locale.
	 */
	public static void sendPrefixedTownMessage(Town town, Translatable message) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + StringMgmt.remUnderscore(town.getName()) + ": " + message.translate()));
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			sendMessage(player, Translation.translateTranslatables(player, "", Translatable.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())), message));
	}
	
	/**
	 * Send a message to All online residents of a nation and log, 
	 * preceded by the default_towny_prefix
	 * 
	 * @param nation Nation which will receive the message.
	 * @param message Translatable message to be shown to the town.
	 */
	public static void sendNationMessagePrefixed(Nation nation, Translatable message) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + message.translate()));
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			sendMsg(player, message);
	}
	
	/**
	 * Send a message to All online residents of a town and log, 
	 * preceded by the default_towny_prefix
	 * 
	 * @param town Town which will receive the message.
	 * @param message Translatable message to be shown to the town.
	 */
	public static void sendTownMessagePrefixed(Town town, Translatable message) {
		LOGGER.info(ChatTools.stripColour("[Town Msg] " + StringMgmt.remUnderscore(town.getName())) + ": " + message.translate());
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			sendMsg(player, message);
	}
	
	/**
	 * Send a translatable message to a resident if they are online,
	 * prefixed by the default_towny_prefix.
	 * 
	 * @param resident Resident to receive the message.
	 * @param message Translatable message for the resident.
	 */
	public static void sendMsg(Resident resident, Translatable message) {
		if (resident.isOnline())
			sendMsg(resident.getPlayer(), message);
	}
	
	/**
	 * Sends a translatable message to the console, prefixed
	 * by the default_towny_prefix.
	 * 
	 * @param message Translatable message to show the console.
	 */
	public static void sendMsg(Translatable message) {
		LOGGER.info(message.stripColors(true).translate());
	}
	
	/**
	 * Sends a translatable error message to the console, 
	 * prefixed by [Towny] Error:
	 * 
	 * @param message Translatable error message to show the console.
	 */
	public static void sendErrorMsg(Translatable message) {
		LOGGER.warn("Error: " + message.stripColors(true).translate());
	}

	/*
	 * TOWN/RESIDENT/NATION/TOWNBLCOK STATUS SCREENS
	 */
	
	public static void sendStatusScreen(CommandSender sender, StatusScreen screen) {
		Audience audience = Towny.getAdventure().sender(sender);
		for (Component string : screen.getFormattedStatusScreen())
			audience.sendMessage(string);
	}

	/*
	 * ACTIONBAR METHODS
	 */
	
	/**
	 * Send an ActionBar message to the given player.
	 * @param player {@link Player} who will be shown the message.
	 * @param message {@link String} message which will be made into a {@link TextComponent} and shown in the ActioBar.
	 */
	public static void sendActionBarMessageToPlayer(Player player, String message) {
		TextComponent component = LegacyComponentSerializer.builder().build().deserialize(message);
		sendActionBarMessageToPlayer(player, component);
	}
	
	/**
	 * Send an ActionBar message to the given player.
	 * @param player {@link Player} who will be shown the message.
	 * @param component {@link TextComponent} message which will be shown to the player.
	 */
	public static void sendActionBarMessageToPlayer(Player player, TextComponent component) {
		Audience audience = Towny.getAdventure().player(player);
		audience.sendActionBar(component); 
	}
	
	/*
	 * BOSSBAR METHODS
	 */
	
	public static void sendBossBarMessageToPlayer(Player player, String message, float progress, Color color, Overlay overlay) {
		Component component = LegacyComponentSerializer.builder().build().deserialize(message);
		sendBossBarMessageToPlayer(player, component, progress, color, overlay);
	}
	
	public static void sendBossBarMessageToPlayer(Player player, Component component, float progress, Color color, Overlay overlay) {
		Audience audience = Towny.getAdventure().player(player);
		audience.showBossBar(BossBar.bossBar(component, progress, color, overlay));
	}
	
	public static void sendBossBarMessageToPlayer(Player player, BossBar bossBar) {
		Audience audience = Towny.getAdventure().player(player);
		audience.showBossBar(bossBar);
	}
	
	/*
	 * DEPRECATED METHODS
	 */
	
	/**
	 * Sends a message (green) to the resident
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param resident to receive the msg
	 * @param msg the message being sent
	 * 
	 * @deprecated Deprecated as of 0.97.3.0 use {@link #sendMsg(Resident, Translatable)} instead.
	 */
	@Deprecated
	public static void sendMsg(Resident resident, String msg) {
		if (BukkitTools.isOnline(resident.getName()))
			sendMsg(resident.getPlayer(), msg);
	}

	/**
	 * Sends a message (green) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param player the player to receive the message
	 * @param msg the message to be sent
	 * 
	 * @deprecated Deprecated as of 0.97.3.0.
	 */
	@Deprecated
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
	 * 
	 * @deprecated Deprecated as of 0.97.3.0.
	 */
	@Deprecated
	public static void sendMsg(Player player, List<String> msg) {
		for (String line : msg) {
			sendMsg(player, line);
		}
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String list to send as a message
	 * @deprecated since 0.97.3.0 use {@link #sendGlobalMessage(Translatable)}
	 */
	@Deprecated
	public static void sendGlobalMessage(List<String> lines) {
		sendGlobalMessage(lines.toArray(new String[0]));
	}

	/**
	 * Send a message to ALL online players and the log.
	 * Uses default_towny_prefix
	 *
	 * @param lines String array to send as a message
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendGlobalMessage(Translatable)}
	 */
	@Deprecated
	public static void sendGlobalMessage(String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Global Msg] " + line));
		}
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				for (String line : lines) {
					player.sendMessage(Translation.of("default_towny_prefix") + line);
				}
			}
		}
	}

	/**
	 * Sends an Error message (red) to the Player or console
	 * and to the named Dev if DevMode is enabled.
	 * Uses default_towny_prefix
	 *
	 * @param sender the Object sending the message
	 * @param msg the message array being sent.
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendErrorMsg(CommandSender, Translatable...)}
	 */
	@Deprecated 
	public static void sendErrorMsg(Object sender, String[] msg) {
		for (String line : msg) {
			sendErrorMsg(sender, line);
		}
	}

	/**
	 * Send a message to all residents in the list with the required mode
	 *
	 * @param residents List of residents to show the message to
	 * @param msg the message to send
	 * @param modeRequired a resident mode required for the resident to receive the message.
	 * @deprecated since 0.97.3.0
	 */
	@Deprecated
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
	 * @deprecated since 0.97.3.0
	 */
	@Deprecated
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
	 * @deprecated since 0.97.3.0
	 */
	@Deprecated
	public static void sendMessageToMode(Nation nation, String msg, String modeRequired) {

		for (Resident resident : nation.getResidents())
			if (BukkitTools.isOnline(resident.getName()))
				sendMessage(resident,msg);
	}

	/**
	 * Send a message to All online residents of a town and log, 
	 * preceded by the default_towny_prefix
	 *
	 * @param town town to receive the message
	 * @param line the message
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendTownMessagePrefixed(Town, Translatable)}
	 */
	@Deprecated
	public static void sendTownMessagePrefixed(Town town, String line) {
		LOGGER.info(ChatTools.stripColour(line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			player.sendMessage(Translation.of("default_towny_prefix") + line);
	}

	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines Array of Strings constituting the message.
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendPrefixedTownMessage(Town, Translatable)}
	 */
	@Deprecated
	public static void sendPrefixedTownMessage(Town town, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour(line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			for (String line : lines) {
				player.sendMessage(Translation.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())) + line);
			}
	}
	
	/**
	 * Send a multi-line message to All online residents of a town and log, 
	 * preceded by the [Townname]
	 *
	 * @param town town to receive the message
	 * @param lines List of Strings constituting the message.
	 * @deprecated since 0.97.3.0 use {@link #sendPrefixedTownMessage(Town, Translatable)}
	 */
	@Deprecated
	public static void sendPrefixedTownMessage(Town town, List<String> lines) {
		sendPrefixedTownMessage(town, lines.toArray(new String[0]));
	}
	
	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation the nation to send to
	 * @param lines list of Strings containing the message
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendPrefixedNationMessage(Nation, Translatable)}
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
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendPrefixedNationMessage(Nation, Translatable)}
	 */
	@Deprecated
	public static void sendPrefixedNationMessage(Nation nation, String[] lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation)) {
			for (String line : lines) {
				player.sendMessage(Translation.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())) + line);
			}
		}
	}
	
	/**
	 * Send a message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param line the message
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendNationMessagePrefixed(Nation, Translatable)}
	 */
	@Deprecated
	public static void sendNationMessagePrefixed(Nation nation, String line) {
		LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			player.sendMessage(Translation.of("default_towny_prefix") + line);
	}
	
	/**
	 * Send a multi-line message to All online residents of a nation and log
	 * Uses default_towny_prefix
	 *
	 * @param nation the nation to send message to
	 * @param lines the list of lines of the message
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendNationMessagePrefixed(Nation, Translatable)}
	 */
	@Deprecated
	public static void sendNationMessagePrefixed(Nation nation, List<String> lines) {
		for (String line : lines) {
			LOGGER.info(ChatTools.stripColour("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		}
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			for (String line : lines) {
				player.sendMessage(Translation.of("default_towny_prefix") + line);
			}
	}

	/**
	 * Send a message to a specific resident
	 * preceded by the default_towny_prefix
	 *
	 * @param resident the resident to receive the message
	 * @param line message String to send
	 * @throws TownyException if the player is null
	 * 
	 * @deprecated since 0.97.3.0 use {@link #sendMsg(Resident, Translatable)}
	 */
	@Deprecated
	public static void sendResidentMessage(Resident resident, String line) throws TownyException {
		LOGGER.info(ChatTools.stripColour("[Resident Msg] " + resident.getName() + ": " + line));
		Player player = TownyAPI.getInstance().getPlayer(resident);
		if (player == null) {
			throw new TownyException("Player could not be found!");
		}
		player.sendMessage(Translation.of("default_towny_prefix") + line);
	}

}
