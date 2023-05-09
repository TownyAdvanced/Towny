package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.towny.utils.TownyComponents;
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
		LOGGER.warn(Colors.strip("Error: " + msg));
	}

	/**
	 * Sends an Error message (red) to the Player or console and to the named Dev if
	 * DevMode is enabled.
	 * 
	 * Uses default_towny_prefix.
	 * 
	 * If msg is empty nothing will be sent.
	 *
	 * @param sender the Object sending the message
	 * @param msg    the message to send
	 */
	public static void sendErrorMsg(Object sender, String msg) {
		if (sender == null || msg.isEmpty())
			return;

		if (sender instanceof CommandSender toSend) {
			sendMessage(toSend, Translatable.of("default_towny_prefix").stripColors(sender instanceof ConsoleCommandSender).append(Colors.Red + msg).forLocale(toSend));
		} else if (sender instanceof TownyObject townySender) {
			if (townySender instanceof Resident resident) {
				// Resident
				sendMessage(resident, Translation.of("default_towny_prefix") + Colors.Red + msg);
			} else if (townySender instanceof Town town) {
				// Town
				sendPrefixedTownMessage(town, Colors.Red + msg);
			} else if (townySender instanceof Nation nation) {
				// Nation
				sendPrefixedNationMessage(nation, Colors.Red + msg);
			}
		} else {
			sendErrorMsg(String.format("Unsupported TownyMessaging#sendErrorMsg sender class type: %s", sender.getClass().getName()));
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
		
		LOGGER.info(Colors.strip(msg));
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
			sendMessage(p, Translatable.of("default_towny_prefix").forLocale(p) + Colors.LightGreen + msg);
		} else if (sender instanceof ConsoleCommandSender) {
			sendMessage(sender, Translatable.of("default_towny_prefix").stripColors(true).defaultLocale() + Colors.strip(msg));
		} else {
			sendMessage(sender, Translatable.of("default_towny_prefix").forLocale(sender) + Colors.LightGreen + msg);
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
			Player townyDev = BukkitTools.getPlayerExact(TownySettings.getDevName());
			if (townyDev != null)
				sendMessage(townyDev, Translatable.of("default_towny_prefix").forLocale(townyDev) + " DevMode: " + Colors.Red + msg);
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
			LOGGER_DEBUG.info(Colors.strip("[Towny] Debug: " + msg));
		}
		sendDevMsg(msg);
	}

	/**
	 * Send a message. This is the main end point for all String based messages
	 * before being sent to the eventual reader.
	 *
	 * @param sender the Object sending the message
	 * @param line   the String to send
	 */
	public static void sendMessage(Object sender, String line) {
		if (line.isEmpty())
			return;
		
		if (sender instanceof Player player) {
			Towny.getAdventure().player(player).sendMessage(TownyComponents.miniMessage(line));
		} else if (sender instanceof CommandSender commandSender) {
			commandSender.sendMessage(Colors.strip(line));
		} else if (sender instanceof Resident resident) {
			resident.sendMessage(TownyComponents.miniMessage(line));
		}
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
		LOGGER.info(Colors.strip("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				sendMessage(player, Translation.of("default_towny_prefix") + line);
		}
	}
	
	/**
	 * Send a message to All online players and the log.
	 * Does not use the default_towny_prefix.
	 * 
	 * @param line the message to send.
	 */
	public static void sendPlainGlobalMessage(String line) {
		LOGGER.info(Colors.strip("[Global Message] " + line));
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null && TownyAPI.getInstance().isTownyWorld(player.getWorld()))
				sendMessage(player, line);
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
		LOGGER.info(Colors.strip("[Town Msg] " + StringMgmt.remUnderscore(town.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			sendMessage(player, Translation.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())) + line);
	}

	/**
	 * Send a message to All online residents of a nation and log
	 * with the [nationname] prefixed to the beginning
	 *
	 * @param nation nation to send to, and prefix message with
	 * @param line the message
	 */
	public static void sendPrefixedNationMessage(Nation nation, String line) {
		LOGGER.info(Colors.strip("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + line));
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			sendMessage(player, Translation.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())) + line);
	}

	/*
	 * TOWN AND NATION BOARD MESSAGES
	 */

	/**
	 * Send the town board to a player (in yellow)
	 *
	 * @param sender sender to show to
	 * @param town the town for which to show it's board
	 */
	public static void sendTownBoard(CommandSender sender, Town town) {
		String tbColor1 = Translation.of("townboard_message_colour_1");
		String tbColor2 = Translation.of("townboard_message_colour_2");
		
		sendMessage(sender, tbColor1 + "[" + StringMgmt.remUnderscore(town.getName()) + "] " + tbColor2 + town.getBoard());
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

		sendMessage(sender, nbColor1 + "[" + StringMgmt.remUnderscore(nation.getName()) + "] " + nbColor2 + nation.getBoard());
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
	 * <p>
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
			.hoverEvent(HoverEvent.showText(translator.component("msg_confirmation_spigot_click_accept", confirmline, "/" + confirmline)))
			.clickEvent(ClickEvent.runCommand("/towny:" + confirmline));

		// Create cancel button based on given params.
		TextComponent cancelComponent = Component.text("[/" + cancelline + "]")
			.color(NamedTextColor.RED)
			.hoverEvent(HoverEvent.showText(translator.component("msg_confirmation_spigot_click_cancel", cancelline, "/" + cancelline)))
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
		Component firstLineComponent = translator.component("confirmation_prefix").append(confirmation.getTitle().locale(sender).component());
		Component lastLineComponent = translator.component("this_message_will_expire2", confirmation.getDuration());

		// Create confirm button based on given params.
		Component confirmComponent = Component.text("[/" + confirmation.getConfirmCommand() + "]", NamedTextColor.GREEN)
			.hoverEvent(HoverEvent.showText(translator.component("msg_confirmation_spigot_click_accept", confirmation.getConfirmCommand(), "/" + confirmation.getConfirmCommand())))
			.clickEvent(ClickEvent.runCommand("/" + confirmation.getPluginPrefix() + ":" + confirmation.getConfirmCommand()));

		// Create cancel button based on given params.
		Component cancelComponent = Component.text("[/" + confirmation.getCancelCommand() + "]", NamedTextColor.RED)
			.hoverEvent(HoverEvent.showText(translator.component("msg_confirmation_spigot_click_cancel", confirmation.getCancelCommand(), "/" + confirmation.getCancelCommand())))
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
		Translator translator = Translator.locale(sender);
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
		sendMessage(sender, ChatTools.formatTitle(translator.of("town_plu")));
		sendMessage(sender, Colors.Blue + translator.of("town_name") + (TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + translator.of(compType.getName())));
		for (TextComponent textComponent : townsformatted)
			audience.sendMessage(textComponent);
		
		// Page navigation
		Component pageFooter = getPageNavigationFooter("towny:town list", page, compType.getCommandString(), total, translator);
		audience.sendMessage(pageFooter);
	}

	public static Component getPageNavigationFooter(String prefix, int page, String arg, int total, Translator translator) {
		Component backButton = Component.text("<<<", NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " " + (arg.isEmpty() ? "" : arg + " ") + (page - 1)))
			.hoverEvent(HoverEvent.showText(translator.component("msg_hover_previous_page")));
		
		Component forwardButton = Component.text(">>>", NamedTextColor.GOLD)
			.clickEvent(ClickEvent.runCommand("/" + prefix + " " +  (arg.isEmpty() ? "" : arg + " ") + (page + 1)))
			.hoverEvent(HoverEvent.showText(translator.component("msg_hover_next_page")));
		
		Component pageText = Component.text("   ").append(translator.component("list_page", page, total)).append(Component.text("   "));

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
		Translator translator = Translator.locale(sender);
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

		sendMessage(sender, ChatTools.formatTitle(translator.of("nation_plu")));
		sendMessage(sender, Colors.Blue + translator.of("nation_name") + Colors.Gray + " - " + Colors.LightBlue + translator.of(compType.getName()));
		Audience audience = Towny.getAdventure().sender(sender);
		for (TextComponent textComponent : nationsformatted) {
			audience.sendMessage(textComponent);
		}

		// Page navigation
		Component pageFooter = getPageNavigationFooter("towny:nation list", page, compType.getCommandString(), total, translator);
		audience.sendMessage(pageFooter);
	}

	public static void sendOutpostList(Player player, Town town, int page, int total) {
		Translator translator = Translator.locale(player);
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
			TextComponent dash = Component.text(" - ", NamedTextColor.DARK_GRAY);		
			TextComponent line = Component.text(Integer.toString(i + 1), NamedTextColor.GOLD)
				.clickEvent(ClickEvent.runCommand("/towny:town outpost " + (i + 1)))
				.append(dash);

			TextComponent outpostName = Component.text(name, NamedTextColor.GREEN);
			TextComponent worldName = Component.text(outpost.getWorld().getName(), NamedTextColor.BLUE);
			TextComponent coords = Component.text("(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")", NamedTextColor.BLUE);

			if (!name.equalsIgnoreCase("")) {
				line = line.append(outpostName).append(dash);
			}
			line = line.append(worldName).append(dash).append(coords);
			
			Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
			if (TownyEconomyHandler.isActive())
				spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			line = line.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", name.equalsIgnoreCase("") ? "outpost" : name).append("\n").append(spawnCost).locale(player).component()));
			outpostsFormatted[i % 10] = line;
		}
		
		Audience audience = Towny.getAdventure().player(player);
		sendMessage(player, ChatTools.formatTitle(translator.of("outpost_plu")));
		for (TextComponent textComponent : outpostsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		Component pageFooter = getPageNavigationFooter("towny:town outpost list", page, "", total, translator);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendJailList(Player player, Town town, int page, int total) {
		Translator translator = Translator.locale(player);
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

			TextComponent name = Component.text(jail.getName(), NamedTextColor.GREEN);
			TextComponent coord = Component.text(jail.getTownBlock().getWorldCoord().toString(), NamedTextColor.BLUE);
			TextComponent cellCount = Component.text(String.valueOf(jail.getJailCellLocations().size()), NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ", NamedTextColor.DARK_GRAY);

			TextComponent line = Component.text(Integer.toString(i + 1), NamedTextColor.GOLD);
			if (jail.hasName())
				line = line.append(dash).append(name);
			line = line.append(dash).append(coord).append(dash).append(cellCount);
				
			if (town.getPrimaryJail().getUUID().equals(jail.getUUID()))
				line = line.append(dash).append(Component.text("(Primary Jail)", NamedTextColor.RED));

			jailsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().player(player);
		sendMessage(player, ChatTools.formatTitle(Translatable.of("jail_plu").forLocale(player)));
		sendMessage(player, headerMsg);
		for (TextComponent textComponent : jailsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		Component pageFooter = getPageNavigationFooter("towny:town jail list", page, "", total, translator);
		audience.sendMessage(pageFooter);
	}
	
	public static void sendPlotGroupList(CommandSender sender, Town town, int page, int total) {
		Translator translator = Translator.locale(sender);
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
			TextComponent name = Component.text(group.getFormattedName(), NamedTextColor.GREEN);
			TextComponent size = Component.text(String.valueOf(group.getTownBlocks().size()), NamedTextColor.YELLOW);
			TextComponent dash = Component.text(" - ", NamedTextColor.DARK_GRAY);
			TextComponent line = Component.text(Integer.toString(i + 1), NamedTextColor.GOLD);
			line = line.append(dash).append(name).append(dash).append(size);
			
			if (TownyEconomyHandler.isActive() && group.getPrice() != -1)
				line = line.append(dash).append(Component.text("(", NamedTextColor.BLUE).append(translator.component("towny_map_forsale")).append(Component.text(": " + TownyEconomyHandler.getFormattedBalance(group.getPrice()) + ")", NamedTextColor.BLUE)));

			groupsFormatted[i % 10] = line;
		}
		Audience audience = Towny.getAdventure().sender(sender);
		sendMessage(sender, ChatTools.formatTitle(town.getName() + " " + translator.of("plotgroup_plu")));
		sendMessage(sender, headerMsg);
		for (TextComponent textComponent : groupsFormatted) {
			audience.sendMessage(textComponent);
		}
		
		// Page navigation
		Component pageFooter = getPageNavigationFooter("towny:town plotgrouplist" + town.getName(), page, "", total, translator);
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
		LOGGER.info(Colors.strip("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + message.translate()));
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(nation))
			sendMessage(player, Translatable.of("default_nation_prefix", StringMgmt.remUnderscore(nation.getName())).append(message));
	}
	
	/**
	 * Send a message to All online residents of a town and log
	 * preceded by the [Townname], translated for the end-user.
	 *
	 * @param town Town to pass the message to, and prefix message with.
	 * @param message Translatable object to be messaged to the player using their locale.
	 */
	public static void sendPrefixedTownMessage(Town town, Translatable message) {
		LOGGER.info(Colors.strip("[Town Msg] " + StringMgmt.remUnderscore(town.getName()) + ": " + message.translate()));
		
		for (Player player : TownyAPI.getInstance().getOnlinePlayers(town))
			sendMessage(player, Translatable.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())).append(message));
	}

	/**
	 * Sends a message to all online residents of the specified resident's town, if they have one.
	 * If the resident does not have a town, the message will be sent to the resident instead.
	 * 
	 * @param resident The resident.
	 * @param message The translatable message to be sent to the resident and/or their town.
	 */
	public static void sendPrefixedTownMessage(Resident resident, Translatable message) {
		Town town = resident.getTownOrNull();
		
		if (town == null)
			sendMsg(resident, message);
		else 
			sendPrefixedTownMessage(town, message);
	}
	
	/**
	 * Send a message to All online residents of a nation and log, 
	 * preceded by the default_towny_prefix
	 * 
	 * @param nation Nation which will receive the message.
	 * @param message Translatable message to be shown to the town.
	 */
	public static void sendNationMessagePrefixed(Nation nation, Translatable message) {
		LOGGER.info(Colors.strip("[Nation Msg] " + StringMgmt.remUnderscore(nation.getName()) + ": " + message.translate()));
		
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
		LOGGER.info(Colors.strip("[Town Msg] " + StringMgmt.remUnderscore(town.getName())) + ": " + message.translate());
		
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

	/**
	 * Send a message to All online ops or players with towny.admin, and the log.
	 * @param message Translatable message to send.
	 */
	public static void sendMsgToOnlineAdmins(Translatable message) {
		sendMsg(message);
		for (Player player : Bukkit.getOnlinePlayers())
			if (player.isOp() || player.hasPermission("towny.admin"))
				sendMsg(player, message);
	}
	
	/*
	 * TOWN/RESIDENT/NATION/TOWNBLCOK STATUS SCREENS
	 */
	
	public static void sendStatusScreen(CommandSender sender, StatusScreen screen) {
		Towny.getAdventure().sender(sender).sendMessage(screen.getFormattedStatusScreen());
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
		sendActionBarMessageToPlayer(player, TownyComponents.miniMessage(message));
	}
	
	/**
	 * Send an ActionBar message to the given player.
	 * @param player {@link Player} who will be shown the message.
	 * @param component {@link Component} message which will be shown to the player.
	 */
	public static void sendActionBarMessageToPlayer(Player player, Component component) {
		Towny.getAdventure().player(player).sendActionBar(component); 
	}
	
	/*
	 * BOSSBAR METHODS
	 */
	
	public static void sendBossBarMessageToPlayer(Player player, String message, float progress, Color color, Overlay overlay) {
		sendBossBarMessageToPlayer(player, TownyComponents.miniMessage(message), progress, color, overlay);
	}
	
	public static void sendBossBarMessageToPlayer(Player player, Component component, float progress, Color color, Overlay overlay) {
		Towny.getAdventure().player(player).showBossBar(BossBar.bossBar(component, progress, color, overlay));
	}
	
	public static void sendBossBarMessageToPlayer(Player player, BossBar bossBar) {
		Towny.getAdventure().player(player).showBossBar(bossBar);
	}
}
