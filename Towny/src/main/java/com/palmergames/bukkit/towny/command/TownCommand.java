package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.confirmations.ConfirmationTransaction;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.NewTownEvent;
import com.palmergames.bukkit.towny.event.PreNewTownEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.event.TownBlockPermissionChangeEvent;
import com.palmergames.bukkit.towny.event.TownInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.TownPreClaimEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.event.nation.NationKingChangeEvent;
import com.palmergames.bukkit.towny.event.teleport.OutlawTeleportEvent;
import com.palmergames.bukkit.towny.event.TownPreAddResidentEvent;
import com.palmergames.bukkit.towny.event.town.TownCedePlotEvent;
import com.palmergames.bukkit.towny.event.town.TownKickEvent;
import com.palmergames.bukkit.towny.event.town.TownLeaveEvent;
import com.palmergames.bukkit.towny.event.town.TownMayorChangeEvent;
import com.palmergames.bukkit.towny.event.town.TownMergeEvent;
import com.palmergames.bukkit.towny.event.town.TownOutlawAddEvent;
import com.palmergames.bukkit.towny.event.town.TownOutlawRemoveEvent;
import com.palmergames.bukkit.towny.event.town.TownPreInvitePlayerEvent;
import com.palmergames.bukkit.towny.event.town.TownPreMergeEvent;
import com.palmergames.bukkit.towny.event.town.TownPreSetHomeBlockEvent;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.event.town.TownSetOutpostSpawnEvent;
import com.palmergames.bukkit.towny.event.town.TownSetSpawnEvent;
import com.palmergames.bukkit.towny.event.town.TownTrustAddEvent;
import com.palmergames.bukkit.towny.event.town.TownTrustRemoveEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleUnknownEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleExplosionEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleFireEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleMobsEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleNationZoneEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleOpenEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePVPEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownTogglePublicEvent;
import com.palmergames.bukkit.towny.event.town.toggle.TownToggleTaxPercentEvent;
import com.palmergames.bukkit.towny.event.town.TownTrustTownAddEvent;
import com.palmergames.bukkit.towny.event.town.TownTrustTownRemoveEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.CancelledEventException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermissionChange;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache.CacheType;
import com.palmergames.bukkit.towny.object.inviteobjects.PlayerJoinTownInvite;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.BorderUtil;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.towny.utils.TownUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.Pair;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import com.palmergames.util.TimeTools;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InvalidObjectException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;

/**
 * Send a list of all town help commands to player Command: /town
 */

public class TownCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;

	@VisibleForTesting
	public static final List<String> townTabCompletes = Arrays.asList(
		"add",
		"allylist",
		"baltop",
		"buytown",
		"ban",
		"bankhistory",
		"buy",
		"cede",
		"claim",
		"create",
		"deposit",
		"delete",
		"enemylist",
		"forsale",
		"fs",
		"here",
		"invite",
		"jail",
		"join",
		"kick",
		"leave",
		"list",
		"mayor",
		"merge",
		"new",
		"nfs",
		"notforsale",
		"online",
		"outpost",
		"outlaw",
		"outlawlist",
		"plotgrouplist",
		"plots",
		"purge",
		"rank",
		"ranklist",
		"reclaim",
		"reslist",
		"say",
		"set",
		"spawn",
		"takeoverclaim",
		"toggle",
		"trust",
		"trusttown",
		"unclaim",
		"unjail",
		"withdraw"
		);
	@VisibleForTesting
	public static final List<String> townSetTabCompletes = Arrays.asList(
		"board",
		"mayor",
		"homeblock",
		"spawn",
		"spawncost",
		"mapcolor",
		"name",
		"outpost",
		"perm",
		"tag",
		"taxes",
		"plottax",
		"plotprice",
		"shopprice",
		"shoptax",
		"embassyprice",
		"embassytax",
		"title",
		"surname",
		"taxpercentcap",
		"primaryjail"
	);
	private static final List<String> townListTabCompletes = Arrays.asList(
		"residents",
		"balance",
		"bankrupt",
		"forsale",
		"founded",
		"name",		
		"online",
		"open",
		"public",
		"ruined",
		"townblocks",
		"upkeep"
	);
	static final List<String> townToggleTabCompletes = Arrays.asList(
		"explosion",
		"fire",
		"mobs",
		"nationzone",
		"neutral",
		"peaceful",
		"public",
		"pvp",
		"taxpercent",
		"open"
	);
	
	private static final List<String> townConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list",
		"reslist"
	);
	
	static final List<String> townAddRemoveTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> townClaimTabCompletes = Arrays.asList(
		"outpost",
		"auto",
		"circle",
		"rect",
		"fill"
	);
	
	public static final List<String> townUnclaimTabCompletes = Arrays.asList(
		"circle",
		"rect",
		"all"
	);
	
	private static final List<String> townInviteTabCompletes = Arrays.asList(
		"sent",
		"received",
		"accept",
		"deny"
	);
	
	private static final List<String> townSetBoardTabCompletes = Arrays.asList(
		"none",
		"reset"
	);

	public TownCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		if (!(sender instanceof Player player)) {
			if (args.length == 1)
				return filterByStartOrGetTownyStartingWith(townConsoleTabCompletes, args[0], "t");
			else 
				return Collections.emptyList();
		}

		Town town = TownyAPI.getInstance().getTown(player);

		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "online":
		case "reslist":
		case "outlawlist":
		case "plots":
		case "delete":
		case "join":
		case "merge":
		case "plotgrouplist":
		case "allylist":
		case "enemylist":
		case "baltop":
		case "ranklist":
			if (args.length == 2)
				return getTownyStartingWith(args[1], "t");
			break;
		case "deposit":
			if (args.length == 3)
				return getTownyStartingWith(args[2], "t");
			break;
		case "spawn":
			if (args.length == 2) {
				List<String> townOrIgnore = getTownyStartingWith(args[1], "t");
				townOrIgnore.add("-ignore");
				return NameUtil.filterByStart(townOrIgnore, args[1]);
			}
			if (args.length == 3)
				return Collections.singletonList("-ignore");
			break;
		case "leave":
			if (args.length == 2)
				return Collections.singletonList("-ignore");
			break;
		case "rank":
			switch (args.length) {
			case 2:
				return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
			case 3:
				return getTownResidentNamesOfPlayerStartingWith(player, args[2]);
			case 4:
				switch (args[1].toLowerCase(Locale.ROOT)) {
				case "add":
					if (town == null)
						return Collections.emptyList();
					return NameUtil.filterByStart(TownyPerms.getTownRanks(town), args[3]);
				case "remove": {
					Resident res = TownyUniverse.getInstance().getResident(args[2]);
					if (res != null)
						return res.getTownRanks().isEmpty() ? Collections.emptyList() : NameUtil.filterByStart(res.getTownRanks(), args[3]);
					break;
				}
				default:
					return Collections.emptyList();
				}
			default:
				return Collections.emptyList();
			}
		case "jail":
			if (args.length == 2) {
				List<String> residentOrList = getTownResidentNamesOfPlayerStartingWith(player, args[1]);
				residentOrList.add("list");
				return NameUtil.filterByStart(residentOrList, args[1]);
			}
			break;
		case "unjail":
			if (args.length == 2 && town != null) {
				List<String> jailedResidents = TownyUniverse.getInstance().getJailedResidentMap().stream()
						.filter(jailee -> jailee.hasJailTown(town.getName()))
						.map(jailee -> jailee.getName())
						.collect(Collectors.toList());
				return NameUtil.filterByStart(jailedResidents, args[1]);
			}
			break;
		case "outpost":
			if (args.length == 2) {
				List<String> outpostNames = town == null ? new ArrayList<>() : town.getOutpostNames();
				outpostNames.add("list");
				return NameUtil.filterByStart(outpostNames, args[1]);
			}
			break;
		case "outlaw":
		case "ban":
			switch (args.length) {
			case 2:
				return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
			case 3:
				switch (args[1].toLowerCase(Locale.ROOT)) {
				case "add":
					return getTownyStartingWith(args[2], "r");
				case "remove":
					if (town != null)
						return NameUtil.filterByStart(NameUtil.getNames(town.getOutlaws()), args[2]);
				}
			default:
				return Collections.emptyList();
			}
		case "cede":
			if (args.length == 2)
				return Arrays.asList("plot");
			if (args.length == 3)
				return getTownyStartingWith(args[2], "t");
			break;
		case "claim":
			switch (args.length) {
			case 2:
				return NameUtil.filterByStart(townClaimTabCompletes, args[1]);
			case 3:
				if (!args[1].equalsIgnoreCase("outpost"))
					return NameUtil.filterByStart(Collections.singletonList("auto"), args[2]);
			default:
				return Collections.emptyList();
			}
		case "unclaim":
			if (args.length == 2)
				return NameUtil.filterByStart(townUnclaimTabCompletes, args[1]);
			break;
		case "add":
			if (args.length == 2)
				return getVisibleResidentsForPlayerWithoutTownsStartingWith(args[1], sender);
			break;
		case "kick":
			if (args.length == 2)
				return getTownResidentNamesOfPlayerStartingWith(player, args[1]);
			break;
		case "set":
			return townSetTabComplete(sender, town, args);
		case "invite":
			return townInviteTabComplete(sender, args, player, town);
		case "buy":
			if (args.length == 2)
				return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_BUY, Collections.singletonList("bonus")), args[1]);
			break;
		case "toggle":
			return switch (args.length) {
			case 2 -> NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_TOGGLE, townToggleTabCompletes), args[1]);
			case 3 -> NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
			case 4 -> getTownResidentNamesOfPlayerStartingWith(player, args[3]);
			default -> Collections.emptyList();
			};
		case "list":
			return switch (args.length) {
			case 2 -> Collections.singletonList("by");
			case 3 -> NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_LIST_BY, townListTabCompletes), args[2]);
			default -> Collections.emptyList();
			};
		case "trust":
			switch (args.length) {
			case 2:
				return NameUtil.filterByStart(Arrays.asList("add", "remove", "list"), args[1]);
			case 3:
				if (args[1].equalsIgnoreCase("add")) {
					List<String> resList = getTownyStartingWith(args[2], "r");
					resList.removeAll(getTrustedResidentsFromResident(player));
					return resList;
				}
				if (args[1].equalsIgnoreCase("remove"))
					return NameUtil.filterByStart(getTrustedResidentsFromResident(player), args[2]);
				return Collections.emptyList();
			default:
				return Collections.emptyList();
			}
		case "trusttown":
			switch (args.length) {
			case 2:
				return NameUtil.filterByStart(Arrays.asList("add", "remove", "list"), args[1]);
			case 3:
				if (args[1].equalsIgnoreCase("add")) {
					List<String> townsList = getTownyStartingWith(args[2], "t");
					townsList.removeAll(getTrustedTownsFromResident(player));
					return townsList;
				}
				if (args[1].equalsIgnoreCase("remove"))
					return NameUtil.filterByStart(getTrustedTownsFromResident(player), args[2]);
				return Collections.emptyList();
			default:
				return Collections.emptyList();
			}
		case "buytown":
			if (args.length == 2) {
				List<String> townsList = getTownyStartingWith(args[1], "t");
				townsList.removeIf(n -> !TownyAPI.getInstance().getTown(n).isForSale());
				return townsList;
			}
			break;
		default:
			if (args.length == 1)
				return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN, townTabCompletes), args[0], "t");
			else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN, args[0]))
				return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN, args[0]).getTabCompletion(sender, args), args[args.length-1]);
		}
		return Collections.emptyList();
	}

	private List<String> townInviteTabComplete(CommandSender sender, String[] args, Player player, Town town) {
		if (town == null)
			return Collections.emptyList();
		switch (args.length) {
		case 2:
			List<String> returnValue = NameUtil.filterByStart(townInviteTabCompletes, args[1]);
			if (returnValue.size() > 0) {
				return returnValue;
			} else {
				if (args[1].startsWith("-")) {
					return NameUtil.filterByStart(town.getSentInvites()
							// Get all sent invites
							.stream()
							.map(Invite::getReceiver)
							.map(InviteReceiver::getName)
							.collect(Collectors.toList()), args[1].substring(1))
								// Add the hyphen back to the front
								.stream()
								.map(e -> "-"+e)
								.collect(Collectors.toList());
				} else {
					return getVisibleResidentsForPlayerWithoutTownsStartingWith(args[1], sender);
				}
			}
		case 3:
			switch (args[1].toLowerCase(Locale.ROOT)) {
			case "accept", "deny" -> {
				return NameUtil.filterByStart(town.getReceivedInvites()
					// Get the names of all received invites
					.stream()
					.map(Invite::getSender)
					.map(InviteSender::getName)
					.collect(Collectors.toList()),args[2]);
			}
			default -> Collections.emptyList();
			}
		default:
			return Collections.emptyList();
		}
	}
	
	static List<String> townSetTabComplete(CommandSender sender, Town town, String[] args) {
		if (town == null)
			return Collections.emptyList();

		if (args.length == 2) {
			return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_SET, townSetTabCompletes), args[1]);
		} else if (args.length > 2) {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_SET, args[1]))
				return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
			
			switch (args[1].toLowerCase(Locale.ROOT)) {
				case "mayor":
					return NameUtil.filterByStart(NameUtil.getNames(town.getResidents()), args[2]);
				case "perm":
					return permTabComplete(StringMgmt.remArgs(args, 2));
				case "tag":
					if (args.length == 3)
						return NameUtil.filterByStart(Collections.singletonList("clear"), args[2]);
					break;
				case "title":
				case "surname":
					if (args.length == 3)
						return NameUtil.filterByStart(NameUtil.getNames(town.getResidents()), args[2]);
					break;
				case "board":
					if (args.length == 3)
						return NameUtil.filterByStart(townSetBoardTabCompletes, args[2]);
					break;
				case "mapcolor":
					if (args.length == 3)
						return NameUtil.filterByStart(new ArrayList<>(TownySettings.getTownColorsMap().keySet()), args[2]);
					break;
				default:
					return Collections.emptyList();
			}
		}
		
		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}

			try {
				parseTownCommand(player, args);
			} catch (TownyException te) {
				TownyMessaging.sendErrorMsg(player, te.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}
		} else {
			
			try {
				parseTownCommandForConsole(sender, args);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
		}
		return true;
	}

	private void parseTownCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_HELP_CONSOLE.send(sender);
			return;
		}

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "list" -> listTowns(sender, split);
		case "reslist" -> townResList(sender, split); 
		default -> {
			// Test if this is an addon command
			if (tryTownAddonCommand(sender, split))
				return;
			// Test if this is a town status screen lookup.
			if (tryTownStatusScreen(sender, split))
				return;
			
			// Alert the player that the subcommand doesn't exist.
			throw new TownyException(Translatable.of("msg_err_invalid_sub"));
		}
		}
	}

	private void parseTownCommand(final Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			townStatusScreen(player, getTownFromPlayerOrThrow(player));
			return;
		}		

		if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_HELP.send(player);
			return;
		}

		String[] subArg = StringMgmt.remFirstArg(split);
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add"-> townAdd(player, null, subArg);
		case "allylist"-> townAllyList(player, split);
		case "baltop"-> parseTownBaltop(player, split.length > 1 ? getTownOrThrow(split[1]) : getTownFromPlayerOrThrow(player));
		case "bankhistory"-> parseTownBankHistoryCommand(player, split);
		case "buy"-> townBuy(player, subArg, null, false);
		case "buytown"-> parseTownBuyTownCommand(player, subArg);
		case "cede" -> parseTownCedeCommand(player, subArg);
		case "claim"-> parseTownClaimCommand(player, subArg);
		case "delete"-> townDelete(player, subArg);
		case "deposit"-> parseTownDepositCommand(player, split);
		case "enemylist"-> townEnemyList(player, split);
		case "forsale", "fs"-> parseTownForSaleCommand(player, subArg);
		case "here" -> parseTownHereCommand(player);
		case "invite", "invites"-> parseInviteCommand(player, subArg);
		case "jail"-> parseJailCommand(player, null, subArg, false);
		case "join"-> parseTownJoin(player, subArg);
		case "kick"-> townKick(player, subArg);
		case "leave"-> townLeave(player, StringMgmt.remFirstArg(split));
		case "list" -> listTowns(player, split);
		case "mayor" -> parseTownyMayorCommand(player);
		case "merge"-> parseTownMergeCommand(player, subArg);
		case "new", "create"-> parseTownNewCommand(player, split);
		case "notforsale", "nfs"-> parseTownNotForSaleCommand(player);
		case "online"-> parseTownOnlineCommand(player, subArg);
		case "outlaw", "ban"-> parseTownOutlawCommand(player, subArg, false, getResidentOrThrow(player).getTown());
		case "outlawlist"-> townOutlawList(player, split);
		case "outpost"-> townOutpost(player, subArg);
		case "plotgrouplist"-> townPlotGroupList(player, split);
		case "plots"-> townPlots(player, split);
		case "purge"-> parseTownPurgeCommand(player, subArg);
		case "rank"-> townRank(player, subArg);
		case "ranklist"-> parseTownRanklistCommand(player, split);
		case "reclaim"-> parseTownReclaimCommand(player);
		case "reslist"-> townResList(player, split);
		case "say"-> parseTownSayCommand(player, split);
		case "set"-> townSet(player, subArg, false, null);
		case "spawn"-> townSpawn(player, subArg, false, split.length > 2 && split[2].equals("-ignore"));
		case "takeoverclaim"-> parseTownTakeoverClaimCommand(player);
		case "toggle"-> townToggle(player, subArg, false, null);
		case "trust"-> parseTownTrustCommand(player, subArg, null);
		case "trusttown"-> parseTownTrustTownCommand(player, subArg, null);
		case "unclaim"-> parseTownUnclaimCommand(player, subArg);
		case "unjail"-> parseUnJailCommand(player, null, subArg, false);
		case "withdraw"-> parseTownWithdrawCommand(player, split);
		default -> {
			// Test if this is an addon command
			if (tryTownAddonCommand(player, split))
				return;
			// Test if this is a town status screen lookup.
			if (tryTownStatusScreen(player, split))
				return;

			// Alert the player that the subcommand doesn't exist.
			throw new TownyException(Translatable.of("msg_err_invalid_sub"));
		}
		}
	}

	private boolean tryTownStatusScreen(CommandSender sender, String[] split) throws NoPermissionException {
		Town town = TownyUniverse.getInstance().getTown(split[0]);
		if (town != null) {
			if (sender instanceof Player player && !town.hasResident(player.getName()))
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_OTHERTOWN.getNode());

			townStatusScreen(sender, town);
			return true;
		}
		return false;
	}

	private boolean tryTownAddonCommand(CommandSender sender, String[] split) {
		if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN, split[0]).execute(sender, "town", split);
			return true;
		}
		return false;
	}

	private static void catchRuinedTown(Player player) throws TownyException {
		if (TownRuinUtil.isPlayersTownRuined(player))
			throw new TownyException(Translatable.of("msg_err_cannot_use_command_because_town_ruined"));
	}

	private void parseTownyMayorCommand(final Player player) throws NoPermissionException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode());
		HelpMenu.TOWN_MAYOR_HELP.send(player);
	}

	private void townEnemyList(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_ENEMYLIST.getNode());
		Town town = split.length == 1 ? getTownFromPlayerOrThrow(player) : getTownOrThrow(split[1]);

		if (town.getEnemies().isEmpty())
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_town_has_no_enemies")); 
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(town.getName() + " " + Translatable.of("status_nation_enemies").forLocale(player)));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_enemies").forLocale(player), new ArrayList<>(town.getEnemies())));
		}
	}

	private void townAllyList(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_ALLYLIST.getNode());
		Town town = split.length == 1 ? getTownFromPlayerOrThrow(player) : getTownOrThrow(split[1]);

		if (town.getAllies().isEmpty())
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_town_has_no_allies")); 
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(town.getName() + " " + Translatable.of("status_nation_allies").forLocale(player)));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_allies").forLocale(player), new ArrayList<>(town.getAllies())));
		}
	}

	private void parseTownPurgeCommand(Player player, String[] arg) throws TownyException {
		if (arg.length == 0) {
			HelpMenu.TOWN_PURGE.send(player);
			return;
		}
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PURGE.getNode());
		catchRuinedTown(player);
		Town town = getTownFromPlayerOrThrow(player);

		int days = MathUtil.getIntOrThrow(arg[0]);
		if (days < 1)
			throw new TownyException(Translatable.of("msg_err_days_must_be_greater_than_0"));

		List<Resident> kickList = TownUtil.gatherInactiveResidents(town.getResidents(), days);
		if (kickList.isEmpty())
			throw new TownyException(Translatable.of("msg_err_no_one_to_purge"));

		Confirmation.runOnAccept(()-> {
			kickList.forEach(Resident::removeTown);
			TownyMessaging.sendMsg(player, Translatable.of("msg_purge_complete_x_removed", kickList.size()));
		})
		.setTitle(Translatable.of("msg_purging_will_remove_the_following_residents", StringMgmt.join(kickList, ", ")))
		.sendTo(player);
	}

	private void parseInviteCommand(Player player, String[] args) throws TownyException {
		catchRuinedTown(player);
		Resident resident = getResidentOrThrow(player);

		String received = Translatable.of("town_received_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getTown().getReceivedInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown())));
		String sent = Translatable.of("town_sent_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getTown().getSentInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown())));

		if (args.length == 0 || args[0].equalsIgnoreCase("help") || args[0].equalsIgnoreCase("?") ) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_SEE_HOME.getNode());
			HelpMenu.TOWN_INVITE.send(player);
			TownyMessaging.sendMessage(player, sent);
			TownyMessaging.sendMessage(player, received);
			return;
		}
		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "sent" -> parseTownInviteSentCommand(player, StringMgmt.remFirstArg(args), resident, sent);
		case "received" -> parseTownInviteReceivedCommand(player, StringMgmt.remFirstArg(args), resident, received);
		case "accept" -> parseTownInviteAcceptCommand(player, StringMgmt.remFirstArg(args), resident);
		case "deny" -> parseTownInviteDenyCommand(player, StringMgmt.remFirstArg(args), resident);
		case "add" -> parseTownInviteAddCommand(player, StringMgmt.remFirstArg(args));
		default -> {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());
			townAdd(player, null, args);
		}
		}
	}

	private void parseTownInviteSentCommand(Player player, String[] args, Resident resident, String sent) throws TownyException {
		if (args.length == 0) {
			listTownSentInvites(player, args, resident, sent);
			return;
		}
		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "removeall" -> removeAllTownSentInvites(resident, player);
		default -> HelpMenu.TOWN_INVITE.send(player);
		}
	}

	private void listTownSentInvites(Player player, String[] args, Resident resident, String sent) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_SENT.getNode());

		List<Invite> sentinvites = resident.getTown().getSentInvites();
		int page = args.length > 0 ? MathUtil.getPositiveIntOrThrow(args[0]) : 1;
		InviteCommand.sendInviteList(player, sentinvites, page, true);
		TownyMessaging.sendMessage(player, sent);
	}

	private void removeAllTownSentInvites(Resident resident, Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());

		for (final Invite invite : new ArrayList<>(resident.getTown().getSentInvites()))
			invite.decline(true);

		TownyMessaging.sendMessage(player, Translatable.of("msg_all_of_your_towns_sent_invites_have_been_cancelled"));
	}

	private void parseTownInviteReceivedCommand(Player player, String[] args, Resident resident, String received) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED.getNode());

		List<Invite> receivedinvites = resident.getTown().getReceivedInvites();
		int page = args.length > 0 ? MathUtil.getPositiveIntOrThrow(args[0]) : 1;
		InviteCommand.sendInviteList(player, receivedinvites, page, false);
		TownyMessaging.sendMessage(player, received);
	}

	private void parseTownInviteAcceptCommand(Player player, String[] args, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode());
		Town town = resident.getTown();
		List<Invite> invites = town.getReceivedInvites();

		if (invites.size() == 0)
			throw new TownyException(Translatable.of("msg_err_town_no_invites"));

		if (args.length == 0) { // /invite accept
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_specify_invite"));
			InviteCommand.sendInviteList(player, invites, 1, false);
			return;
		}

		Nation nation = getNationOrThrow(args[0]);
		Invite toAccept = null;
		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(nation) && invite.getReceiver().equals(town)) {
				toAccept = invite;
				break;
			}
		}
		if (toAccept != null) {
			try {
				InviteHandler.acceptInvite(toAccept);
				return;
			} catch (TownyException | InvalidObjectException e) {
				plugin.getLogger().log(Level.WARNING, "unknown exception occurred while accepting invite", e);
			}
		}
	}

	private void parseTownInviteDenyCommand(Player player, String[] args, Resident resident) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_DENY.getNode());
		Town town = resident.getTown();
		List<Invite> invites = town.getReceivedInvites();

		if (invites.size() == 0)
			throw new TownyException(Translatable.of("msg_err_town_no_invites"));

		if (args.length == 0) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_specify_invite"));
			InviteCommand.sendInviteList(player, invites, 1, false);
			return;
		}

		Nation nation = getNationOrThrow(args[0]);
		Invite toDecline = null;
		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(nation) && invite.getReceiver().equals(town)) {
				toDecline = invite;
				break;
			}
		}
		if (toDecline != null) {
			try {
				InviteHandler.declineInvite(toDecline, false);
				TownyMessaging.sendMsg(player, Translatable.of("successful_deny"));
			} catch (InvalidObjectException e) {
				plugin.getLogger().log(Level.WARNING, "unknown exception occurred while declining invite", e); // Shouldn't happen, however like i said a fallback
			}
		}
	}

	private void parseTownInviteAddCommand(Player player, String[] newSplit) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());
		townAdd(player, null, newSplit);
	}

	public static void parseTownOutlawCommand(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length == 0) {
			// Help output.
			if (!admin)
				HelpMenu.TOWN_OUTLAW_HELP.send(sender);
			else
				HelpMenu.TA_TOWN_OUTLAW.send(sender);
			return;
		}

		/*
		 * Does the command have enough arguments?
		 */
		if (split.length < 2)
			throw new TownyException(Translatable.of("msg_usage", "/town outlaw add/remove [name]"));

		Resident resident;
		if (!admin) {
			resident = getResidentOrThrow(sender.getName());
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_OUTLAW.getNode());
			catchRuinedTown((Player) sender);
		} else
			resident = town.getMayor();	// if this is an Admin-initiated command, dupe the action as if it were done by the mayor.

		Resident target = getResidentOrThrow(split[1]);

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add" -> parseTownOutlawAddCommand(sender, admin, town, resident, target);
		case "remove" -> parseTownOutlawRemoveCommand(sender, admin, town, target);
		default -> throw new TownyException(Translatable.of("msg_err_invalid_property", split[0]));
		}
	}

	private static void parseTownOutlawAddCommand(CommandSender sender, boolean admin, Town town, Resident resident, Resident target) throws TownyException {
		// Don't allow a resident to outlaw their own mayor.
		if (town.getMayor().equals(target))
			throw new TownyException(Translatable.of("msg_err_you_cannot_outlaw_your_mayor"));

		if (!resident.isMayor() && town.hasResident(target) && TownySettings.getTownUnkickableRanks().stream().anyMatch(target::hasTownRank))
			throw new TownyException(Translatable.of("msg_err_you_cannot_outlaw_because_of_rank", target.getName()));

		if (town.hasOutlaw(target))
			throw new TownyException(Translatable.of("msg_err_resident_already_an_outlaw"));

		// Call cancellable event.
		BukkitTools.ifCancelledThenThrow(new TownOutlawAddEvent(sender, target, town));

		// Remove any trust they have in the town or town's plots.
		target.removeTrustInTown(town);

		// Kick outlaws from town if they are residents.
		if (town.hasResident(target)) {
			target.removeTown();
			Object outlawer = (admin ? Translatable.of("admin_sing") : sender.getName());
			TownyMessaging.sendMsg(target, Translatable.of("msg_kicked_by", outlawer));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_kicked", outlawer, target.getName()));
		}

		// Add the outlaw and save the town.
		try {
			town.addOutlaw(target);	
		} catch (AlreadyRegisteredException ignored) {}
		town.save();

		// Send feedback messages and potentially teleport the new outlaw out of town.
		if (target.isOnline())
			handleOnlineNewlyMintedOutlaw(town, target);
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
	}

	private static void handleOnlineNewlyMintedOutlaw(Town town, Resident target) {
		TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_declared_outlaw", town.getName()));
		Player player = target.getPlayer();
		Location loc = player.getLocation();

		if (!TownySettings.areNewOutlawsTeleportedAway() || TownyAPI.getInstance().isWilderness(loc)
			|| !TownyAPI.getInstance().getTown(loc).equals(town) || BukkitTools.isEventCancelled(new OutlawTeleportEvent(target, town, loc)))
			return;

		// If the newly-outlawed player is within the town's borders send them using the
		// outlaw teleport warmup time, potentially giving them the chance to escape the
		// borders.
		int warmupTime = TownySettings.getOutlawTeleportWarmup();
		if (warmupTime > 0)
			TownyMessaging.sendMsg(target, Translatable.of("msg_outlaw_kick_cooldown", town, TimeMgmt.formatCountdownTime(warmupTime)));

		plugin.getScheduler().runLater(() -> {
			// Send them away if they're still within the town.
			if (player.isOnline() && TownyAPI.getInstance().getTown(player.getLocation()).equals(town))
				SpawnUtil.outlawTeleport(town, target);
		}, warmupTime * 20L);
	}

	private static void parseTownOutlawRemoveCommand(CommandSender sender, boolean admin, Town town, Resident target) throws TownyException {
		if (!town.hasOutlaw(target))
			throw new TownyException(Translatable.of("msg_err_player_not_an_outlaw"));

		// Call cancellable event.
		BukkitTools.ifCancelledThenThrow(new TownOutlawRemoveEvent(sender, target, town));

		town.removeOutlaw(target);
		town.save();

		// Send feedback messages.
		if (target.isOnline())
			TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_undeclared_outlaw", town.getName()));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
	}

	private void parseTownWithdrawCommand(final Player player, String[] split) throws NoPermissionException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode());
		TownyEconomyHandler.economyExecutor().execute(() -> townTransaction(player, StringMgmt.remFirstArg(split), true));
	}

	private void parseTownDepositCommand(final Player player, String[] split) throws NoPermissionException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode());
		TownyEconomyHandler.economyExecutor().execute(() -> townTransaction(player, StringMgmt.remFirstArg(split), false));
	}

	private void townPlots(CommandSender sender, String[] args) throws TownyException {
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;

		if (player != null)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode());

		Town town;
		if (args.length == 1 && player != null) {
			catchRuinedTown(player);
			town = getTownFromPlayerOrThrow(player);
		} else {
			town = getTownOrThrow(args[1]);
		}
		
		Translator translator = Translator.locale(sender);

		List<String> out = new ArrayList<>();
		out.add(ChatTools.formatTitle(town + translator.of("msg_town_plots_title")));
		String townSize = translator.of("msg_town_plots_town_size", town.getTownBlocks().size(), town.getMaxTownBlocksAsAString());
		if (!town.hasUnlimitedClaims()) {
			if (TownySettings.isSellingBonusBlocks(town))
				townSize += translator.of("msg_town_plots_town_bought", town.getPurchasedBlocks(), TownySettings.getMaxPurchasedBlocks(town));
			if (town.getBonusBlocks() > 0)
				townSize += translator.of("msg_town_plots_town_bonus", town.getBonusBlocks());
			if (TownySettings.getNationBonusBlocks(town) > 0)
				townSize += translator.of("msg_town_plots_town_nationbonus", TownySettings.getNationBonusBlocks(town));
		}
		out.add(townSize);
		TownBlockTypeCache typeCache = town.getTownBlockTypeCache();
		out.add(translator.of("msg_town_plots_town_owned_land", (town.getTownBlocks().size() - typeCache.getNumberOfResidentOwnedTownBlocks())));
		String typeHeader = translator.of("msg_town_plots_type_header");
		if (TownyEconomyHandler.isActive())
			typeHeader += translator.of("msg_town_plots_type_header_revenue");
		out.add(typeHeader);
		for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
			int residentOwned = typeCache.getNumTownBlocks(type, CacheType.RESIDENTOWNED);
			String plotTypeLine = translator.of("msg_town_plots_type_line", type.getFormattedName(), residentOwned, 
				typeCache.getNumTownBlocks(type, CacheType.FORSALE), typeCache.getNumTownBlocks(type, CacheType.ALL));
			if (TownyEconomyHandler.isActive())
				plotTypeLine += translator.of("msg_town_plots_type_line_revenue", prettyMoney(residentOwned * type.getTax(town)));
			out.add(plotTypeLine);
		}
		out.add(Translatable.of("msg_town_plots_revenue_disclaimer").forLocale(player));
		TownyMessaging.sendMessage(sender, out);

	}

	private void parseTownOnlineCommand(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode());
		catchRuinedTown(player);
		Translator translator = Translator.locale(player);
		Town town = split.length > 0 ? getTownOrThrow(split[0]) : getTownFromPlayerOrThrow(player);
		List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, town);
		String output = onlineResidents.size() > 0
			? TownyFormatter.getFormattedOnlineResidents(translator.of("msg_town_online"), town, player)
			: Colors.White + "0 " + translator.of("res_list") + " " + (translator.of("msg_town_online") + ": " + town);
		TownyMessaging.sendMessage(player, output);
	}

	private void parseTownHereCommand(final Player player) throws NoPermissionException, TownyException {
		Town town;
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode());
		town = TownyAPI.getInstance().getTown(player.getLocation());
		if (town == null)
			throw new TownyException(Translatable.of("msg_not_claimed", Coord.parseCoord(player.getLocation())));
		townStatusScreen(player, town);
	}

	/**
	 * Send a list of all towns in the universe to player Command: /town list
	 *
	 * @param sender - Sender (player or console.)
	 * @param split  - Current command arguments.
	 * @throws TownyException when a player lacks the permission node.
	 */
	public void listTowns(CommandSender sender, String[] split) throws TownyException {

		boolean console = true;
		Player player = null;
		
		if (split.length == 2 && split[1].equals("?")) {
			HelpMenu.TOWN_LIST.send(sender);
			return;
		}
		
		if (sender instanceof Player) {
			console = false;
			player = (Player) sender;
		}

		/*
		 * The default comparator on /t list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST_RESIDENTS.getNode());
		
		List<Town> townsToSort = new ArrayList<>(TownyUniverse.getInstance().getTowns());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) townsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_LIST_BY, split[i+1])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_LIST_BY, split[i+1]).execute(sender, "town", split);
					return;
				}

				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_multiple_comparators"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console)
						checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode(split[i]));
					
					if (!townListTabCompletes.contains(split[i].toLowerCase(Locale.ROOT)))
						throw new TownyException(Translatable.of("msg_error_invalid_comparator_town", townListTabCompletes.stream().filter(comp -> sender.hasPermission(PermissionNodes.TOWNY_COMMAND_TOWN_LIST.getNode(comp))).collect(Collectors.joining(", "))));

					type = ComparatorType.valueOf(split[i].toUpperCase(Locale.ROOT));

				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /t list, /t list # or /t list by {comparator} #
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_too_many_pages"));
					return;
				}
				page = MathUtil.getPositiveIntOrThrow(split[i]);
				if (page == 0)
					throw new TownyException(Translatable.of("msg_error_must_be_int"));
				pageSet = true;
			}
		}

		if (page > total) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("list_err_not_enough_pages", total));
			return;
		}
		
		final int pageNumber = page;
		final int totalNumber = total; 
		final ComparatorType finalType = type;
		try {
			if (!TownySettings.isTownListRandom()) {
				plugin.getScheduler().runAsync(() -> TownyMessaging.sendTownList(sender, ComparatorCaches.getTownListCache(finalType), finalType, pageNumber, totalNumber));
			} else { 
				// Make a randomly sorted output.
				List<Pair<UUID, Component>> output = new ArrayList<>();
				List<Town> towns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
				Collections.shuffle(towns);

				boolean spawningFullyDisabled = !TownySettings.isConfigAllowingTownSpawn() && !TownySettings.isConfigAllowingPublicTownSpawnTravel()
						&& !TownySettings.isConfigAllowingTownSpawnNationTravel() && !TownySettings.isConfigAllowingTownSpawnNationAllyTravel();

				for (Town town : towns) {
					TextComponent townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA);
					townName = townName.append(Component.text(" - ", NamedTextColor.DARK_GRAY).append(Component.text("(" + town.getResidents().size() + ")", NamedTextColor.AQUA)));

					if (town.isOpen())
						townName = townName.append(Component.space()).append(Translatable.of("status_title_open").locale(sender).component());
					
					if (!spawningFullyDisabled) {
						Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
						if (TownyEconomyHandler.isActive())
							spawnCost = Translatable.of("msg_spawn_cost", prettyMoney(town.getSpawnCost()));

						townName = townName.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
						townName = townName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", town).append("\n").append(spawnCost).locale(sender).component()));
					}
					output.add(Pair.pair(town.getUUID(), townName));
				}
				TownyMessaging.sendTownList(sender, output, finalType, pageNumber, totalNumber);
			}
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_comparator_failed"));
		}
	}
	
	public static void townToggle(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_TOGGLE_HELP.send(sender);
			return;
		}
		if (!admin) {
			catchRuinedTown((Player) sender);
			Resident resident = getResidentOrThrow(sender.getName());
			town = resident.getTown();
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase(Locale.ROOT)));
		}

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "public" -> townTogglePublic(sender, admin, town, choice);
		case "pvp" -> townTogglePVP(sender, split, admin, town, permSource, choice);
		case "explosion" -> townToggleExplosion(sender, split, admin, town, choice);
		case "fire" -> townToggleFire(sender, split, admin, town, choice);
		case "mobs" -> townToggleMobs(sender, split, admin, town, choice);
		case "taxpercent" -> townToggleTaxPercent(sender, admin, town, choice);
		case "open" -> townToggleOpen(sender, admin, town, choice);
		case "neutral", "peaceful" -> townToggleNeutral(sender, admin, town, permSource, choice);
		case "nationzone" -> townToggleNationZone(sender, admin, town, choice);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_TOGGLE, split[0]).execute(sender, "town", split);
				return;
			}
			/*
			 * Fire off an event if we don't recognize the command being used. The event is
			 * cancelled by default, leaving our standard error message to be shown to the
			 * player, unless the user of the event does a) uncancel the event, or b) alters
			 * the cancellation message.
			 */
			BukkitTools.ifCancelledThenThrow(new TownToggleUnknownEvent(sender, town, admin, split));
		}
		}

		// Propagate perms to all unchanged, town owned, townblocks because it is a
		// townblock-affecting toggle.
		switch(split[0].toLowerCase(Locale.ROOT)) {
		case "pvp", "explosion", "fire", "mobs" -> {
			for (TownBlock townBlock : town.getTownBlocks()) {
				if (!townBlock.hasResident() && !townBlock.isChanged()) {
					townBlock.setType(townBlock.getType());
					townBlock.save();
				}
			}
		}}

		//Change settings event
		BukkitTools.fireEvent(new TownBlockSettingsChangedEvent(town));

		// Save the Town.
		town.save();
	}

	private static void townTogglePublic(CommandSender sender, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		TownTogglePublicEvent preEvent = new TownTogglePublicEvent(sender, town, admin, choice.orElse(!town.isPublic()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setPublic(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_public", town.isPublic() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_public", town.isPublic() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void townTogglePVP(CommandSender sender, String[] split, boolean admin, Town town, TownyPermissionSource permSource, Optional<Boolean> choice) throws TownyException {
		String uuid = town.getUUID().toString();
		// If we aren't dealing with an admin using /t toggle pvp:
		if (!admin) {
			// Make sure we are allowed to set these permissions.
			toggleTest(town, StringMgmt.join(split, " "));

			// Test to see if the pvp cooldown timer is active for the town.
			if (TownySettings.getPVPCoolDownTime() > 0 &&
				CooldownTimerTask.hasCooldown(uuid, CooldownType.PVP) &&
				!permSource.isTownyAdmin(sender))
				throw new TownyException(Translatable.of("msg_err_cannot_toggle_pvp_x_seconds_remaining",
						CooldownTimerTask.getCooldownRemaining(uuid, CooldownType.PVP)));

			// Test to see if an outsider being inside of the Town would prevent toggling PVP.
			if (TownySettings.getOutsidersPreventPVPToggle() && choice.orElse(!town.isPVP())) {
				for (Player target : Bukkit.getOnlinePlayers()) {
					if (!town.hasResident(target) && town.equals(TownyAPI.getInstance().getTown(target.getLocation())))
						throw new TownyException(Translatable.of("msg_cant_toggle_pvp_outsider_in_town"));
				}
			}
		}

		// Fire cancellable event directly before setting the toggle.
		TownTogglePVPEvent preEvent = new TownTogglePVPEvent(sender, town, admin, choice.orElse(!town.isPVP()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setPVP(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_pvp", town.getName(), town.isPVP() ? Translatable.of("enabled") : Translatable.of("disabled")));

		// Add a cooldown to PVP toggling.
		if (TownySettings.getPVPCoolDownTime() > 0 && !admin && !permSource.isTownyAdmin(sender))
			CooldownTimerTask.addCooldownTimer(uuid, CooldownType.PVP);
	}

	private static void townToggleExplosion(CommandSender sender, String[] split, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		if (!admin)
			toggleTest(town, StringMgmt.join(split, " "));

		// Fire cancellable event directly before setting the toggle.
		TownToggleExplosionEvent preEvent = new TownToggleExplosionEvent(sender, town, admin, choice.orElse(!town.isExplosion()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setExplosion(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_expl", town.getName(), town.isExplosion() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_expl", town.getName(), town.isExplosion() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void townToggleFire(CommandSender sender, String[] split, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		if (!admin)
			toggleTest(town, StringMgmt.join(split, " "));

		// Fire cancellable event directly before setting the toggle.
		TownToggleFireEvent preEvent = new TownToggleFireEvent(sender, town, admin, choice.orElse(!town.isFire()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setFire(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_fire", town.getName(), town.isFire() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_fire", town.getName(), town.isFire() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void townToggleMobs(CommandSender sender, String[] split, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Make sure we are allowed to set these permissions.
		if (!admin)
			toggleTest(town, StringMgmt.join(split, " "));

		// Fire cancellable event directly before setting the toggle.
		TownToggleMobsEvent preEvent = new TownToggleMobsEvent(sender, town, admin, choice.orElse(!town.hasMobs()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setHasMobs(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_mobs", town.getName(), town.hasMobs() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void townToggleTaxPercent(CommandSender sender, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		TownToggleTaxPercentEvent preEvent = new TownToggleTaxPercentEvent(sender, town, admin, choice.orElse(!town.isTaxPercentage()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setTaxPercentage(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_taxpercent", town.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void townToggleOpen(CommandSender sender, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		if(town.isBankrupt())
			throw new TownyException(Translatable.of("msg_err_bankrupt_town_cannot_toggle_open"));

		// Fire cancellable event directly before setting the toggle.
		TownToggleOpenEvent preEvent = new TownToggleOpenEvent(sender, town, admin, choice.orElse(!town.isOpen()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setOpen(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_open", town.isOpen() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_open", town.isOpen() ? Translatable.of("enabled") : Translatable.of("disabled")));

		// Send a warning when toggling on (a reminder about plot permissions).
		if (town.isOpen())
			TownyMessaging.sendMsg(sender, Translatable.of("msg_toggle_open_on_warning"));
	}

	private static void townToggleNeutral(CommandSender sender, boolean admin, Town town, TownyPermissionSource permSource, Optional<Boolean> choice) throws TownyException {
		String uuid = town.getUUID().toString();
		if (TownySettings.getPeacefulCoolDownTime() > 0 && 
			!admin && !permSource.isTownyAdmin(sender) &&
			CooldownTimerTask.hasCooldown(uuid, CooldownType.NEUTRALITY))
			throw new TownyException(Translatable.of("msg_err_cannot_toggle_neutral_x_seconds_remaining",
					CooldownTimerTask.getCooldownRemaining(uuid, CooldownType.NEUTRALITY)));

		boolean peacefulState = choice.orElse(!town.isNeutral());
		double cost = TownySettings.getTownNeutralityCost(town);

		if (TownySettings.nationCapitalsCantBeNeutral() && town.isCapital())
			throw new TownyException(Translatable.of("msg_err_capital_cannot_be_peaceful"));

		if (town.isNeutral() && peacefulState) throw new TownyException(Translatable.of("msg_town_already_peaceful"));
		else if (!town.isNeutral() && !peacefulState) throw new TownyException(Translatable.of("msg_town_already_not_peaceful"));

		if (peacefulState && TownyEconomyHandler.isActive() && !town.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_town_cant_peaceful", prettyMoney(cost)));

		// Fire cancellable event directly before setting the toggle.
		TownToggleNeutralEvent preEvent = new TownToggleNeutralEvent(sender, town, admin, choice.orElse(!town.isNeutral()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// If they setting neutral status on send a message confirming they paid something, if they did.
		if (peacefulState && TownyEconomyHandler.isActive() && cost > 0) {
			town.getAccount().withdraw(cost, "Peaceful Town Cost");
			TownyMessaging.sendMsg(sender, Translatable.of("msg_you_paid", prettyMoney(cost)));
		}

		// Set the toggle setting.
		town.setNeutral(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_peaceful", town.isNeutral() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_peaceful", town.isNeutral() ? Translatable.of("enabled") : Translatable.of("disabled")));

		// Add a cooldown to Peacful toggling.
		if (TownySettings.getPeacefulCoolDownTime() > 0 && !admin && !permSource.isTownyAdmin(sender))
			CooldownTimerTask.addCooldownTimer(uuid, CooldownType.NEUTRALITY);

		// Reassign permissions because neutrality can add/remove nodes.
		if(TownyPerms.hasPeacefulNodes())
			TownyPerms.updateTownPerms(town);
	}

	private static void townToggleNationZone(CommandSender sender, boolean admin, Town town, Optional<Boolean> choice) throws TownyException {
		// Towns don't always have nationzones.
		if (town.getNationZoneSize() < 1)
			throw new TownyException(Translatable.of("msg_err_your_town_has_no_nationzone_to_toggle"));

		// Fire cancellable event directly before setting the toggle.
		TownToggleNationZoneEvent preEvent = new TownToggleNationZoneEvent(sender, town, admin, choice.orElse(!town.isNationZoneEnabled()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// Set the toggle setting.
		town.setNationZoneEnabled(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_changed_nationzone", town.isNationZoneEnabled() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_nationzone", town.isNationZoneEnabled() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}
	
	private static void parseUnJailCommand(CommandSender sender, Town town, String[] split, boolean admin) throws TownyException {
		
		if (!admin) {
			town = getTownFromPlayerOrThrow((Player) sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_UNJAIL.getNode());
			catchRuinedTown((Player) sender);
		}
		
		if (split.length != 1) {
			HelpMenu.TOWN_UNJAIL.send(sender);
			return;
		}
		
		Resident jailedResident = TownyUniverse.getInstance().getResident(split[0]);
		if (jailedResident == null || !jailedResident.isJailed() || (jailedResident.isJailed() && !jailedResident.getJail().getTown().equals(town)))
			throw new TownyException(Translatable.of("msg_player_not_jailed_in_your_town"));
		
		JailUtil.unJailResident(jailedResident, UnJailReason.PARDONED);
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_unjailed", jailedResident));
	}

	private static void parseJailCommand(CommandSender sender, Town town, String[] split, boolean admin) throws TownyException {
		
		if (!admin) {
			town = getTownFromPlayerOrThrow((Player) sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_JAIL.getNode());
			catchRuinedTown((Player) sender);
		}
			
		if (!town.hasJails())
			throw new TownyException(Translatable.of("msg_town_has_no_jails"));

		boolean bailEnabled = TownySettings.isAllowingBail() && TownyEconomyHandler.isActive();

		if (split.length == 0) {
			if (bailEnabled)
				HelpMenu.TOWN_JAILWITHBAIL.send(sender);
			else
				HelpMenu.TOWN_JAIL.send(sender);
			return;
		} 

		if (split[0].equalsIgnoreCase("list")) {
			parseJailListCommand(sender, town, StringMgmt.remFirstArg(split));
			return;
		}

		// Set default values.
		int hours = 2; // default set to two in relation to https://github.com/TownyAdvanced/Towny/issues/6029
		int jailNum = 1;
		int cell = 1;
		Jail jail = town.getPrimaryJail();
		double bail = bailEnabled ? TownySettings.getBailAmount() : 0.0;
		double initialJailFee = TownyEconomyHandler.isActive() && TownySettings.initialJailFee() > 0 ? TownySettings.initialJailFee() : 0;

		// Vet the potential resident to be jailed, throws an exception with error message if they cannot be jailed.
		Resident jailedResident = getResidentAllowedToBeJailedOrThrow(town, split);

		// Players used to be able to get places faster by using jailing exploits. 
		checkTeleportExploitsOrThrow(sender, admin, jailedResident);

		// Begin getting hours, bail, jail and cell numbers from the inputted arguments, otherwise use the defaults.
		if (split.length > 1) {
			// offset is used to determine what argument in split is used for bail, jail # and cell #. 
			int offset = bailEnabled ? 1 : 0;

			/*
			 * Make sure that the arguments being passed in are actually numbers we can use.
			 */
			if (!checkArgumentsPassedForJail(sender, split, offset))
				return;

			// Set the hours, which are mandatory.
			hours = setJailHours(sender, split);

			// Set the bail if bailing is enabled and if the argument is given.
			if (offset == 1 && split.length >= 3)
				bail = setBail(sender, split);

			// Set the jail number if the argument is given.
			if (split.length >= 3 + offset) {
				jail = town.getJail(MathUtil.getPositiveIntOrThrow(split[2 + offset]));
				if (jail == null) 
					throw new TownyException(Translatable.of("msg_err_the_town_does_not_have_that_many_jails"));
			}

			// Set the jail cell if the argument is given.
			if (split.length == 4 + offset) {
				cell = MathUtil.getPositiveIntOrThrow(split[3 + offset]);
				if (!jail.hasJailCell(cell))
					throw new TownyException(Translatable.of("msg_err_that_jail_plot_does_not_have_that_many_cells"));
			}
		}

		// Can the Town jail this resident?
		testTownCanJailResidentOrThrow(town, initialJailFee, jailedResident);

		// Jail the resident, when enabled it will apply the bail.
		JailUtil.jailResidentWithBail(jailedResident, jail, cell, hours, bail, JailReason.MAYOR, sender);

		// Send an admin a message if the player was jailed via Admin.
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_player_has_been_sent_to_jail_number", jailedResident.getName(), jailNum));

		// If fee exists (already sanitised for) deduct it from Town bank and inform in chat
		if (initialJailFee > 0) {
			town.getAccount().withdraw(initialJailFee, "New Prisoner fee for " + jailedResident.getName());
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_x_has_been_withdrawn_for_jailing_of_prisoner_x", initialJailFee, jailedResident));
		}
	}

	private static Resident getResidentAllowedToBeJailedOrThrow(Town town, String[] split) throws TownyException {
		Resident jailedResident = getResidentOrThrow(split[0]);

		// You can only jail your members of your own town.
		if (!town.hasResident(jailedResident))
			throw new TownyException(Translatable.of("msg_resident_not_your_town"));

		// Make sure they aren't already jailed.
		if (jailedResident.isJailed())
			throw new TownyException(Translatable.of("msg_err_resident_is_already_jailed", jailedResident.getName()));

		// Make sure they're not a new player who is jail-immune.
		if (TownySettings.newPlayerJailImmunity() > 0) {
			long time = (jailedResident.getRegistered() + TownySettings.newPlayerJailImmunity()) - System.currentTimeMillis();
			if (time > 0)
				throw new TownyException(Translatable.of("msg_resident_has_not_played_long_enough_to_be_jailed", jailedResident.getName(), TimeMgmt.getFormattedTimeValue(time)));
		}

		if (!jailedResident.isOnline())
			throw new TownyException(Translatable.of("msg_player_is_not_online", jailedResident.getName()));

		return jailedResident;
	}

	private static void checkTeleportExploitsOrThrow(CommandSender sender, boolean admin, Resident jailedResident) throws TownyException {
		Player jailedPlayer = jailedResident.getPlayer();
		// Make sure this isn't someone jailing themselves to get a free teleport.
		if (!admin && jailedPlayer.getUniqueId().equals(((Player) sender).getUniqueId()))
			throw new TownyException(Translatable.of("msg_no_self_jailing"));

		// Test if a player is located where they are outlawed/enemied and unable to teleport.
		Town jaileeLocTown = TownyAPI.getInstance().getTown(jailedPlayer.getLocation());
		if (jaileeLocTown != null) {
			if (!TownySettings.canOutlawsTeleportOutOfTowns() && jaileeLocTown.hasOutlaw(jailedResident))
				throw new TownyException(Translatable.of("msg_err_resident_cannot_be_jailed_because_they_are_outlawed_there"));

			Nation jaileeLocNation = jaileeLocTown.getNationOrNull();

			if (jaileeLocNation != null && jailedResident.hasNation() &&
				TownySettings.getDisallowedTownSpawnZones().contains("enemy") &&
				jaileeLocNation.hasEnemy(jailedResident.getNationOrNull()))
					throw new TownyException(Translatable.of("msg_err_resident_cannot_be_jailed_because_they_are_enemied_there"));
		}
	}

	private static boolean checkArgumentsPassedForJail(CommandSender sender, String[] split, int offset) {
		try {
			Integer.parseInt(split[1]); // Hours
			if (offset == 1 && split.length > 2)
				Double.parseDouble(split[2]); // Bail
			if (split.length > 2 + offset)
				Integer.parseInt(split[2 + offset]); // Jail
			if (split.length > 3 + offset)
				Integer.parseInt(split[3 + offset]); // Cell
		} catch (NumberFormatException e) {
			if (offset == 1)
				HelpMenu.TOWN_JAILWITHBAIL.send(sender);
			else 
				HelpMenu.TOWN_JAIL.send(sender);
			return false;
		}
		return true;
	}

	private static int setJailHours(CommandSender sender, String[] split) throws TownyException {
		int hours = Math.min(2, MathUtil.getPositiveIntOrThrow(split[1]));

		if (hours > TownySettings.getJailedMaxHours()) {
			hours = TownySettings.getJailedMaxHours();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_higher_than_max_allowed_hours_x", TownySettings.getJailedMaxHours()));
		}
		return hours;
	}

	private static double setBail(CommandSender sender, String[] split) throws TownyException {
		double bail = Math.max(1, MathUtil.getDoubleOrThrow(split[2])); 

		if (bail > TownySettings.getBailMaxAmount()) {
			bail = TownySettings.getBailMaxAmount();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_higher_than_max_allowed_bail_x", TownySettings.getBailMaxAmount()));
		}
		return bail;
	}

	private static void testTownCanJailResidentOrThrow(Town town, double initialJailFee, Resident jailedResident) throws TownyException {
		// Make sure the town can afford to jail. 
		if (initialJailFee > 0 && !town.getAccount().canPayFromHoldings(initialJailFee))
			throw new TownyException(Translatable.of("msg_not_enough_money_in_bank_to_jail_x_fee_is_x", jailedResident, initialJailFee));

		// Is this resident in a jailable world?
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(jailedResident.getPlayer().getWorld());
		if (world != null && !world.isJailingEnabled())
			throw new TownyException(Translatable.of("msg_err_x_in_unjailable_world", jailedResident));

		// Check if Town has reached max potential jailed and react according to maxJailedNewJailBehavior in config
		if (TownySettings.getMaxJailedPlayerCount() > 0 && town.getJailedPlayerCount() >= TownySettings.getMaxJailedPlayerCount()) {
			if (TownySettings.getMaxJailedNewJailBehavior() == 0)
				// simple mode, rejects new jailed people outright
				throw new TownyException(Translatable.of("msg_town_has_no_jailslots"));
			//Pass to JailUtil method to unjail someone to make room.
			JailUtil.maxJailedUnjail(town);
		}
	}

	private static void parseJailListCommand(CommandSender sender, Town town, String[] args) {
		try {
			if (sender instanceof Player player) {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_JAIL_LIST.getNode());
				
				Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
				if (resident == null || !resident.hasTown())
					throw new TownyException(Translatable.of("msg_err_must_belong_town"));
			}

			int page = 1;
			int jailCount = town.getJails() == null ? 0 : town.getJails().size();
			int total = (int) Math.ceil(jailCount / 10D);
			if (args.length == 1) {
				page = MathUtil.getPositiveIntOrThrow(args[0]);
				if (page == 0)
					throw new TownyException(Translatable.of("msg_error_must_be_int"));

			}
			if (page > total)
				throw new TownyException(Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));

			TownyMessaging.sendJailList(sender, town, page, total);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}

		
	}

	private static void toggleTest(Town town, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase(Locale.ROOT);

		if (split.contains("mobs")) {
			if (town.getHomeblockWorld().isForceTownMobs())
				throw new TownyException(Translatable.of("msg_world_mobs"));
			if (town.isAdminEnabledMobs())
				throw new TownyException(Translatable.of("msg_town_mobs"));
		}

		if (split.contains("fire") && town.getHomeblockWorld().isForceFire())
			throw new TownyException(Translatable.of("msg_world_fire"));

		if (split.contains("explosion") && town.getHomeblockWorld().isForceExpl())
			throw new TownyException(Translatable.of("msg_world_expl"));

		if (split.contains("pvp") && town.getHomeblockWorld().isForcePVP())
			throw new TownyException(Translatable.of("msg_world_pvp"));

	}

	public void townRank(Player player, String[] split) throws TownyException {
		catchRuinedTown(player);
		if (split.length == 0) {
			// Help output.
			HelpMenu.TOWN_RANK.send(player);
			return;
		}

		// Does the command have enough arguments?
		if (split.length < 3)
			throw new TownyException("Eg: /town rank add/remove [resident] [rank]");

		// Make sure the target resident is part of the Player's town.
		Resident target = getResidentOrThrow(split[1]);
		if (!getTownFromPlayerOrThrow(player).hasResident(target))
			throw new TownyException(Translatable.of("msg_resident_not_your_town"));

		// Match casing to an existing rank, returns null if Town rank doesn't exist.
		String rank = TownyPerms.matchTownRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

		// Only allow the player to assign ranks if they have the grant perm for it.
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank.toLowerCase(Locale.ROOT)), Translatable.of("msg_no_permission_to_give_rank"));

		// Try to apply the rank to the resident.
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add" -> rankAdd(player, target, rank);
		case "remove" -> rankRemove(player, target, rank);
		default -> HelpMenu.TOWN_RANK.send(player);
		}
	}

	private void rankAdd(Player player, Resident target, String rank) throws TownyException {
		Translatable townWord = Translatable.of("town_sing");
		if (target.hasTownRank(rank)) 
			throw new TownyException(Translatable.of("msg_resident_already_has_rank", target.getName(), townWord));

		if (TownyPerms.ranksWithTownLevelRequirementPresent()) {
			int rankLevelReq = TownyPerms.getRankTownLevelReq(rank);
			int levelNumber = target.getTownOrNull().getLevelNumber();
			if (rankLevelReq > levelNumber)
				throw new TownyException(Translatable.of("msg_town_or_nation_level_not_high_enough_for_this_rank", townWord, rank, townWord, levelNumber, rankLevelReq));
		}

		BukkitTools.ifCancelledThenThrow(new TownAddResidentRankEvent(target, rank, target.getTownOrNull()));

		target.addTownRank(rank);
		target.save();

		// Feedback
		TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", townWord, rank, target.getName()));
		if (target.isOnline()) {
			TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", townWord, rank));
			plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
		}
	}

	private void rankRemove(Player player, Resident target, String rank) throws TownyException {
		Translatable townWord = Translatable.of("town_sing");
		if (!target.hasTownRank(rank))
			throw new TownyException(Translatable.of("msg_resident_doesnt_have_rank", target.getName(), townWord));

		BukkitTools.ifCancelledThenThrow(new TownRemoveResidentRankEvent(target, rank, target.getTownOrNull()));

		target.removeTownRank(rank);
		target.save();

		// Feedback
		TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", townWord, rank, target.getName()));
		if (target.isOnline()) {
			TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", townWord, rank));
			plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
		}
	}

	private void parseTownSayCommand(final Player player, String[] split) throws NoPermissionException, TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SAY.getNode());
		catchRuinedTown(player);
		TownyMessaging.sendPrefixedTownMessage(getTownFromPlayerOrThrow(player), TownyComponents.stripClickTags(StringMgmt.join(StringMgmt.remFirstArg(split))));
	}

	public static void townSet(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {

		if (split.length == 0) {
			HelpMenu.TOWN_SET.send(sender);
			return;
		}
		Resident resident;
		Player player = null;
		if (sender instanceof Player p) {
			catchRuinedTown(p);
			player = p;
		}
		
		if (!admin && player != null) {
			resident = getResidentOrThrow(player);
			town = resident.getTown();
		} else // Have the resident being tested be the mayor.
			resident = town.getMayor();

		String[] subArgs = StringMgmt.remFirstArg(split);
		String arg = split[0].toLowerCase(Locale.ROOT);
		switch (arg) {
		case "board" -> townSetBoard(sender, String.join(" ", subArgs), town);
		case "title" -> townSetTitle(sender, subArgs, admin);
		case "taxpercentcap" -> townSetTaxPercent(sender, subArgs, town);
		case "surname" -> townSetSurname(sender, subArgs, admin);
		case "mayor" -> townSetMayor(sender, subArgs, admin, town, resident);
		case "taxes" -> townSetTaxes(sender, subArgs, admin, town);
		case "plottax" -> townSetPlotTax(sender, subArgs, admin, town);
		case "shoptax" -> townSetShopTax(sender, subArgs, admin, town);
		case "embassytax" -> townSetEmbassyTax(sender, subArgs, admin, town);
		case "plotprice" -> townSetPlotPrice(sender, subArgs, admin, town);
		case "shopprice" -> townSetShopPrice(sender, subArgs, admin, town);
		case "embassyprice" -> townSetEmbassyPrice(sender, subArgs, admin, town);
		case "spawncost" -> townSetSpawnCost(sender, subArgs, admin, town);
		case "name" -> townSetName(sender, subArgs, town);
		case "tag" -> townSetTag(sender, subArgs, admin, town);
		case "homeblock" -> townSetHomeblock(sender, town);
		case "spawn" -> townSetSpawn(sender, town, admin);
		case "outpost" -> townSetOutpost(sender, town);
		case "perm" -> setTownBlockOwnerPermissions(sender, town, subArgs);
		case "primaryjail" -> townSetPrimaryJail(sender, town);
		case "mapcolor" -> townSetMapColor(sender, subArgs, town);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_SET, arg)) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_SET, arg).execute(sender, "town", split);
				return;
			}
			HelpMenu.TOWN_SET.send(sender);
		}}
	}

	public static void townSetBoard(CommandSender sender, String board, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode());
		if (board.isEmpty())
			throw new TownyException("Eg: /town set board " + Translatable.of("town_help_9").forLocale(sender));

		if ("reset".equalsIgnoreCase(board)) {
			board = TownySettings.getTownDefaultBoard();
			
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_board_reset"));
		} else if ("none".equalsIgnoreCase(board) || "clear".equalsIgnoreCase(board)) {
			board = "";
		} else {
			if (!NameValidation.isValidBoardString(board)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_string_board_not_set"));
				return;
			}
			
			// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
			if (board.length() > 159)
				board = board.substring(0, 159);
		}
		
		town.setBoard(board);
		town.save();
		
		TownyMessaging.sendTownBoard(sender, town);
	}

	public static void townSetTitle(@NotNull CommandSender sender, @NotNull String[] split, boolean admin) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
		// Give the resident a title
		if (split.length == 0)
			throw new TownyException("Eg: /town set title bilbo Jester");

		Resident resident = getResidentOrThrow(split[0]);
		final boolean sameTown = sender instanceof Player player && CombatUtil.isSameTown(getResidentOrThrow(player), resident);

		if (!admin && !sameTown)
			throw new TownyException(Translatable.of("msg_err_not_same_town", resident.getName()));

		String title = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 1));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(title))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

		resident.setTitle(title);
		resident.save();

		Translatable message = resident.hasTitle()
			? Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle()))
			: Translatable.of("msg_clear_title_surname", "Title", resident.getName());

		TownyMessaging.sendPrefixedTownMessage(resident, message);

		if (admin && !sameTown)
			TownyMessaging.sendMsg(sender, message);
	}

	public static void townSetSurname(CommandSender sender, String[] split, boolean admin) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
		// Give the resident a surname
		if (split.length == 0)
			throw new TownyException("Eg: /town set surname bilbo the dwarf ");

		Resident resident = getResidentOrThrow(split[0]);
		final boolean sameTown = sender instanceof Player player && CombatUtil.isSameTown(getResidentOrThrow(player), resident);

		if (!admin && !sameTown)
			throw new TownyException(Translatable.of("msg_err_not_same_town", resident.getName()));

		String surname = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 1));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(surname))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

		resident.setSurname(surname);
		resident.save();

		Translatable message = resident.hasSurname()
			? Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname()))
			: Translatable.of("msg_clear_title_surname", "Surname", resident.getName());

		TownyMessaging.sendPrefixedTownMessage(resident, message);

		if (admin && !sameTown)
			TownyMessaging.sendMsg(sender, message);
	}

	public static void townSetMayor(CommandSender sender, String[] split, boolean admin, Town town, Resident resident) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_MAYOR.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set mayor Dumbo");

		if (!admin && !resident.isMayor()) // We already know the resident is a mayor if admin if true, prevents an NPE in rare cases of resident being null.
			throw new TownyException(Translatable.of("msg_not_mayor"));

		Resident oldMayor = town.getMayor();
		Resident newMayor = getResidentOrThrow(split[0]);

		Confirmation.runOnAccept(() -> {
			try {
				if (!admin && !resident.isMayor()) // We already know the resident is a mayor if admin if true, prevents an NPE in rare cases of resident being null.
					throw new TownyException(Translatable.of("msg_not_mayor"));

				if (!town.hasResident(newMayor))
					throw new TownyException(Translatable.of("msg_err_mayor_doesnt_belong_to_town"));

				TownMayorChangeEvent townMayorChangeEvent = new TownMayorChangeEvent(sender, oldMayor, newMayor);
				if (BukkitTools.isEventCancelled(townMayorChangeEvent) && !admin)
					throw new TownyException(townMayorChangeEvent.getCancelMessage());

				if (town.isCapital()) {
					NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldMayor, newMayor);
					if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin)
						throw new TownyException(nationKingChangeEvent.getCancelMessage());
				}
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
				return;
			}

			town.setMayor(newMayor);
			plugin.deleteCache(newMayor);

			if (oldMayor != null) {
				TownyPerms.assignPermissions(oldMayor, null);
				plugin.deleteCache(oldMayor);
			}

			if (admin) {
				town.setHasUpkeep(!newMayor.isNPC());
				TownyMessaging.sendMsg(sender, Translatable.of("msg_new_mayor", newMayor.getName()));
			}

			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", newMayor.getName()));
			town.save();
		})
		.sendTo(sender);
	}

	public static void townSetTaxes(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXES.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set taxes 7");
		Double amount = MathUtil.getDoubleOrThrow(split[0]);
		if (amount < 0 && !TownySettings.isNegativeTownTaxAllowed())
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		if (town.isTaxPercentage() && (amount > 100 || amount < 0.0))
			throw new TownyException(Translatable.of("msg_err_not_percentage"));
		if (!TownySettings.isNegativeTownTaxAllowed() && TownySettings.getTownDefaultTaxMinimumTax() > amount)
			throw new TownyException(Translatable.of("msg_err_tax_minimum_not_met", TownySettings.getTownDefaultTaxMinimumTax()));
		town.setTaxes(amount);
		town.save();
		String slug = town.isTaxPercentage() ? amount.toString() + "%" : prettyMoney(town.getTaxes());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_tax", sender.getName(), slug));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_tax", sender.getName(), slug));
	}

	public static void townSetPlotTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PLOTTAX.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set plottax 10");
		double amount = MathUtil.getDoubleOrThrow(split[0]);
		if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		town.setPlotTax(amount);
		town.save();
		String price = prettyMoney(town.getPlotTax());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_plottax", sender.getName(), price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_plottax", sender.getName(), price));
	}

	public static void townSetShopTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SHOPTAX.getNode());
		if (split.length == 0) 
			throw new TownyException("Eg: /town set shoptax 10");
		double amount = MathUtil.getDoubleOrThrow(split[0]);
		if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		town.setCommercialPlotTax(amount);
		town.save();
		String price = prettyMoney(town.getCommercialPlotTax());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_alttax", sender.getName(), "shop", price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_alttax", sender.getName(), "shop", price));
	}

	public static void townSetEmbassyTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_EMBASSYTAX.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set embassytax 10");
		double amount = MathUtil.getDoubleOrThrow(split[0]);
		if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		town.setEmbassyPlotTax(amount);
		town.save();
		String price = prettyMoney(town.getEmbassyPlotTax());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_alttax", sender.getName(), "embassy", price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_alttax", sender.getName(), "embassy", price));
	}

	public static void townSetPlotPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PLOTPRICE.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set plotprice 50");
		
		double amount = MathUtil.getDoubleOrThrow(split[0]);

		town.setPlotPrice(amount);
		town.save();
		String price = prettyMoney(town.getPlotPrice());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_plotprice", sender.getName(), price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_plotprice", sender.getName(), price));
	}

	public static void townSetShopPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SHOPPRICE.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set shopprice 50");
		
		double amount = MathUtil.getDoubleOrThrow(split[0]);

		town.setCommercialPlotPrice(amount);
		town.save();
		String price = prettyMoney(town.getCommercialPlotPrice());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_altprice", sender.getName(), "shop", price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_altprice", sender.getName(), "shop", price));
	}

	public static void townSetEmbassyPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_EMBASSYPRICE.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set embassyprice 50");
		
		double amount = MathUtil.getDoubleOrThrow(split[0]);

		town.setEmbassyPlotPrice(amount);
		town.save();
		String price = prettyMoney(town.getEmbassyPlotPrice());
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_altprice", sender.getName(), "embassy", price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_altprice", sender.getName(), "embassy", price));
	}

	public static void townSetSpawnCost(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWNCOST.getNode());
		if (split.length == 0) 
			throw new TownyException("Eg: /town set spawncost 50");
		double amount = MathUtil.getDoubleOrThrow(split[0]);
		if (amount < 0)
			throw new TownyException(Translatable.of("msg_err_negative_money"));

		if (TownySettings.getSpawnTravelCost() < amount) 
			throw new TownyException(Translatable.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));

		town.setSpawnCost(amount);
		town.save();
		String price = prettyMoney(amount);
		if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_spawn_cost_set_to", sender.getName(), Translatable.of("town_sing"), price));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_spawn_cost_set_to", sender.getName(), Translatable.of("town_sing"), price));
	}

	public static void townSetName(CommandSender sender, String[] split, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode());
		if (split.length == 0) 
			throw new TownyException("Eg: /town set name BillyBobTown");

		String name = String.join("_", split);

		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			name = StringMgmt.capitalizeStrings(name);
		name = NameValidation.checkAndFilterGovernmentNameOrThrow(name, town);
		if (TownyUniverse.getInstance().hasTown(name))
			throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));

		if(TownyEconomyHandler.isActive() && TownySettings.getTownRenameCost() > 0) {
			if (!town.getAccount().canPayFromHoldings(TownySettings.getTownRenameCost()))
				throw new TownyException(Translatable.of("msg_err_no_money", prettyMoney(TownySettings.getTownRenameCost())));
			
			final Town finalTown = town;
			final String finalName = name;
			Confirmation.runOnAccept(() -> townRename(sender, finalTown, finalName))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(TownySettings.getTownRenameCost())))
				.sendTo(sender);
		} else {
			townRename(sender, town, name);
		}
	}

	public static void townSetTag(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAG.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set tag PLTC");
		else if (split[0].equalsIgnoreCase("clear")) {
			town.setTag(" ");
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_reset_town_tag", sender.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_reset_town_tag", sender.getName()));
		} else {
			String tag = NameValidation.checkAndFilterTagOrThrow(split[0]);
			town.setTag(tag);
			town.save();
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_set_town_tag", sender.getName(), town.getTag()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_set_town_tag", sender.getName(), town.getTag()));
		}
	}

	public static void townSetHomeblock(CommandSender sender, final Town town) throws TownyException {
		Player player = catchConsole(sender);
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_HOMEBLOCK.getNode());
		final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

		if (world == null || townBlock == null || !townBlock.hasTown() || townBlock.getTownOrNull() != town)
			throw new TownyException(Translatable.of("msg_area_not_own"));

		if (TownySettings.getHomeBlockMovementCooldownHours() > 0 
			&& town.getMovedHomeBlockAt() > 0
			&& TimeTools.getHours(System.currentTimeMillis() - town.getMovedHomeBlockAt()) < TownySettings.getHomeBlockMovementCooldownHours()) {
			long timeRemaining = ((town.getMovedHomeBlockAt() + TimeTools.getMillis(TownySettings.getHomeBlockMovementCooldownHours() + "h")) - System.currentTimeMillis());
			throw new TownyException(Translatable.of("msg_err_you_have_moved_your_homeblock_too_recently_wait_x", TimeMgmt.getFormattedTimeValue(timeRemaining)));
		}
		
		if (town.hasHomeBlock() && town.getHomeBlock().getWorldCoord().equals(townBlock.getWorldCoord()))
			throw new TownyException(Translatable.of("msg_err_homeblock_already_set_here"));

		ProximityUtil.allowTownHomeBlockOrThrow(world, townBlock.getCoord(), town, false);

		BukkitTools.ifCancelledThenThrow(new TownPreSetHomeBlockEvent(town, townBlock, player));

		// Test whether towns will be removed from the nation
		if (town.isCapital() && TownySettings.getNationProximityToCapital() > 0) {
			final Nation nation = town.getNationOrNull();
			// Determine if some of the nation's towns' homeblocks will be out of range.
			List<Town> removedTowns = ProximityUtil.gatherOutOfRangeTowns(nation);

			// Oh no, some the nation will lose at least one town, better make a confirmation.
			if (!removedTowns.isEmpty()) {
				final Location playerLocation = player.getLocation();
				Confirmation.runOnAccept(() -> {
					// Set town homeblock and remove the out of range towns.
					town.playerSetsHomeBlock(townBlock, playerLocation, player);
					town.save();
					ProximityUtil.removeOutOfRangeTowns(nation);
				})
				.setTitle(Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
				.sendTo(player);
				return;
			}
			// Phew, the nation won't lose any towns, let's do this.
		}
		town.playerSetsHomeBlock(townBlock, player.getLocation(), player);
		town.save();
	}

	public static void townSetSpawn(CommandSender sender, Town town, boolean admin) throws TownyException {
		Player player = catchConsole(sender);
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode());
		// Towns can only set their spawn if they have a homeblock.
		if (!town.hasHomeBlock())
			throw new TownyException(Translatable.of("msg_err_homeblock_has_not_been_set"));

		TownSetSpawnEvent event = new TownSetSpawnEvent(town, player, player.getLocation());
		if (BukkitTools.isEventCancelled(event) && 
			!admin && 
			!event.getCancelMessage().isEmpty())
				throw new TownyException(event.getCancelMessage());

		Location newSpawn = admin ? player.getLocation() : event.getNewSpawn();

		TownBlock tb = TownyAPI.getInstance().getTownBlock(newSpawn);

		// The townblock needs to exist, belong to the town and also be inside of the homeblock.
		if (tb == null || !tb.hasTown() || !tb.getTownOrNull().equals(town) || !town.getHomeBlock().getWorldCoord().equals(tb.getWorldCoord()))
			throw new TownyException(Translatable.of("msg_err_spawn_not_within_homeblock"));

		// Throw unset event, for SpawnPoint particles.
		if (town.getSpawnOrNull() != null)
			TownyUniverse.getInstance().removeSpawnPoint(town.getSpawnOrNull());

		// Set the spawn point and send feedback message.
		town.setSpawn(newSpawn);
		town.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_spawn"));
	}

	public static void townSetOutpost(CommandSender sender, Town town) throws TownyException {
		Player player = catchConsole(sender);
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_OUTPOST.getNode());
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		if (townBlock == null || !townBlock.hasTown() || !townBlock.isOutpost())
			throw new TownyException(Translatable.of("msg_err_location_is_not_within_an_outpost_plot"));
		
		if (!townBlock.getTownOrNull().equals(town))
			throw new TownyException(Translatable.of("msg_not_own_area"));

		TownSetOutpostSpawnEvent event = new TownSetOutpostSpawnEvent(town, player, player.getLocation());
		BukkitTools.ifCancelledThenThrow(event);

		town.addOutpostSpawn(event.getNewSpawn());
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_set_outpost_spawn"));
	}

	public static void townSetPrimaryJail(CommandSender sender, Town town) throws TownyException {
		Player player = catchConsole(sender);
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PRIMARYJAIL.getNode());
		TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
		if (tb == null || !tb.isJail())
			throw new TownyException(Translatable.of("msg_err_location_is_not_within_a_jail_plot"));
		
		Jail jail = tb.getJail();
		town.setPrimaryJail(jail);
		town.save();
		TownyMessaging.sendMsg(player, Translatable.of("msg_primary_jail_set_for_town"));
	}

	public static void townSetMapColor(CommandSender sender, String[] split, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_MAPCOLOR.getNode());
		if (split.length == 0)
			throw new TownyException("Eg: /town set mapcolor brown.");

		String color = StringMgmt.join(split, " ").toLowerCase(Locale.ROOT);

		if (!TownySettings.getTownColorsMap().containsKey(color))
			throw new TownyException(Translatable.of("msg_err_invalid_nation_map_color", TownySettings.getTownColorsMap().keySet().toString()));

		if (TownySettings.getTownSetMapColourCost() > 0)
			Confirmation
				.runOnAccept(()-> setTownMapColor(town, color))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(TownySettings.getTownSetMapColourCost())))
				.setCost(new ConfirmationTransaction(TownySettings::getTownSetMapColourCost, town, "Cost of setting town map color."))
				.sendTo(sender);
		else 
			setTownMapColor(town, color);
	}

	private static void setTownMapColor(Town town, String color) {
		town.setMapColorHexCode(TownySettings.getTownColorsMap().get(color));
		town.save();
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_map_color_changed", color));
	}

	public static void townSetTaxPercent(CommandSender sender, String[] split, Town town) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXPERCENTCAP.getNode());
		if (!town.isTaxPercentage())
			throw new TownyException(Translatable.of("msg_max_tax_amount_only_for_percent"));

		if (split.length == 0) 
			throw new TownyException("Eg. /town set taxpercentcap 10000");

		town.setMaxPercentTaxAmount(MathUtil.getDoubleOrThrow(split[0]));
		town.save();

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_tax_max_percent_amount", sender.getName(), prettyMoney(town.getMaxPercentTaxAmount())));
	}

	private static void parseTownBaltop(Player player, Town town) throws TownyException {
		catchRuinedTown(player);
		plugin.getScheduler().runAsync(() -> {
			StringBuilder sb = new StringBuilder();
			List<Resident> residents = new ArrayList<>(town.getResidents());
			residents.sort(Comparator.<Resident>comparingDouble(res -> res.getAccount().getCachedBalance()).reversed());
	
			int i = 0;
			for (Resident res : residents)
				sb.append(Translatable.of("msg_baltop_book_format", ++i, res.getName(), prettyMoney(res.getAccount().getCachedBalance())).forLocale(player) + "\n");

			ItemStack book = BookFactory.makeBook("Town Baltop", town.getName(), sb.toString());
			plugin.getScheduler().run(player, () -> player.openBook(book));
		});
	}

	public static void townBuy(CommandSender sender, String[] split, @Nullable Town town, boolean admin) throws TownyException {
		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TOWN_BUY.send(sender);
			return;
		}

		if (town == null && sender instanceof Player player) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode());
			catchRuinedTown(player);
			town = getTownFromPlayerOrThrow(player);
		}

		switch(split[0].toLowerCase(Locale.ROOT)) {
			case "bonus" -> townBuyBonus(town, split, sender);
			default -> {
				if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_BUY, split[0])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_BUY, split[0]).execute(sender, "town", split);
					return;
				}
				HelpMenu.TOWN_BUY.send(sender);
			}
		}
	}

	/**
	 * Town tries to buy bonus blocks or checks the cost and increase.
	 *
	 * @param town - Towm object.
	 * @param split - List of command arguments.
	 * @param sender - Player.
	 * @throws TownyException - Exception.
	 */
	public static void townBuyBonus(Town town, String[] split, CommandSender sender) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_BUY_BONUS.getNode());
		
		if (!TownySettings.isSellingBonusBlocks(town) && !TownySettings.isBonusBlocksPerTownLevel())
			throw new TownyException("Config.yml has bonus blocks disabled at max_purchased_blocks: '0' ");
		else if (TownySettings.isBonusBlocksPerTownLevel() && TownySettings.getMaxBonusBlocks(town) == 0)
			throw new TownyException("Config.yml has bonus blocks disabled at town_level section: townBlockBuyBonusLimit: 0");
		
		if (split.length < 2) {
			String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
			TownyMessaging.sendMessage(sender, String.format(line, prettyMoney(town.getBonusBlockCost()), TownySettings.getMaxPurchasedBlocks(town)));
			if (TownySettings.getPurchasedBonusBlocksIncreaseValue() != 1.0)
				TownyMessaging.sendMessage(sender, Colors.Green + "Cost Increase per TownBlock: " + Colors.LightGreen + "+" +  new DecimalFormat("##.##%").format(TownySettings.getPurchasedBonusBlocksIncreaseValue()-1));
			return;
		}
				
		townBuyBonusTownBlocks(town, MathUtil.getIntOrThrow(split[1].trim()), sender);
	}

	/**
	 * Town buys bonus blocks after checking the configured maximum.
	 *
	 * @param town - Towm object.
	 * @param inputN - Number of townblocks being bought.
	 * @param sender - Player.
	 * @throws TownyException - Exception.
	 */
	public static void townBuyBonusTownBlocks(Town town, int inputN, CommandSender sender) throws TownyException {

		if (inputN < 0)
			throw new TownyException(Translatable.of("msg_err_negative"));

		int current = town.getPurchasedBlocks();

		int n;
		if (current + inputN > TownySettings.getMaxPurchasedBlocks(town)) {
			n = TownySettings.getMaxPurchasedBlocks(town) - current;
		} else {
			n = inputN;
		}

		if (n == 0)
			throw new TownyException(Translatable.of("msg_err_you_cannot_purchase_any_more_bonus_blocks"));

		double cost = town.getBonusBlockCostN(n);
		// Test if the town can pay and throw economy exception if not.
		if (!town.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_no_funds_to_buy", n, Translatable.of("bonus_townblocks"), prettyMoney(cost)));
		
		Confirmation.runOnAccept(() -> {
			town.addPurchasedBlocks(n);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_buy", n, Translatable.of("bonus_townblocks"), prettyMoney(cost)));
			town.save();
		})
			.setCost(new ConfirmationTransaction(() -> cost, town, String.format("Town Buy Bonus (%d)", n),
					Translatable.of("msg_no_funds_to_buy", n, Translatable.of("bonus_townblocks"), prettyMoney(cost))))
			.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
			.sendTo(sender); 
	}

	private void parseTownNewCommand(final Player player, String[] split) throws NoPermissionException, TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode());
		if (split.length == 1) {
			throw new TownyException(Translatable.of("msg_specify_name"));
		} else {
			String townName = String.join("_", StringMgmt.remFirstArg(split));
			boolean noCharge = TownySettings.getNewTownPrice() == 0.0 || !TownyEconomyHandler.isActive();
			newTown(player, townName, getResidentOrThrow(player), noCharge);
		}
	}

	/**
	 * Create a new town. Command: /town new [town]
	 *
	 * @param player - Player.
	 * @param name - name of town
	 * @param resident - The resident in charge of the town.
	 * @param noCharge - charging for creation - /ta town new NAME MAYOR has no charge.
	 * @throws TownyException when a new town isn't allowed.
	 */
	public static void newTown(Player player, String name, Resident resident, boolean noCharge) throws TownyException {
		newTown(player, name, resident, noCharge, false);
	}

	/**
	 * Create a new town. Command: /town new [townname] or /ta town new [townname]
	 *
	 * @param player Player using the command.
	 * @param name Name of town
	 * @param resident The resident in charge of the town.
	 * @param noCharge Charging for creation - /ta town new NAME MAYOR has no charge.
	 * @param adminCreated true when an admin has used /ta town new [NAME].
	 * @throws TownyException when a new town isn't allowed.
	 */
	public static void newTown(Player player, String name, Resident resident, boolean noCharge, boolean adminCreated) throws TownyException {
		if (TownySettings.hasTownLimit() && TownyUniverse.getInstance().getTowns().size() >= TownySettings.getTownLimit())
			throw new TownyException(Translatable.of("msg_err_universe_limit"));

		// Check if the player has a cooldown since deleting their town.
		if (!resident.isAdmin() && CooldownTimerTask.hasCooldown(player.getName(), CooldownType.TOWN_DELETE))
			throw new TownyException(Translatable.of("msg_err_cannot_create_new_town_x_seconds_remaining",
					CooldownTimerTask.getCooldownRemaining(player.getName(), CooldownType.TOWN_DELETE)));

		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			name = StringMgmt.capitalizeStrings(name);

		name = NameValidation.checkAndFilterTownNameOrThrow(name);
		if (TownyUniverse.getInstance().hasTown(name))
			throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));

		if (resident.hasTown())
			throw new TownyException(Translatable.of("msg_err_already_res", resident.getName()));

		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

		if (world == null || !world.isUsingTowny())
			throw new TownyException(Translatable.of("msg_set_use_towny_off"));

		if (!world.isClaimable())
			throw new TownyException(Translatable.of("msg_not_claimable"));

		Location spawnLocation = player.getLocation();
		Coord key = Coord.parseCoord(player);

		if (!TownyAPI.getInstance().isWilderness(spawnLocation))
			throw new TownyException(Translatable.of("msg_already_claimed_1", key));

		if (!adminCreated) {
			// Check that a town isn't being formed inside of a biome that isn't allowed to be claimed.
			if (TownySettings.isUnwantedBiomeClaimingEnabled() || TownySettings.isOceanClaimingBlocked()) {
				List<WorldCoord> selection = Arrays.asList(WorldCoord.parseWorldCoord(spawnLocation));
				selection = AreaSelectionUtil.filterOutUnwantedBiomeWorldCoords(player, selection);
				selection = AreaSelectionUtil.filterOutOceanBiomeWorldCoords(player, selection);
				if (selection.isEmpty())
					throw new TownyException(Translatable.of("msg_err_cannot_begin_town_in_this_biome"));
			}
	
			ProximityUtil.allowTownHomeBlockOrThrow(world, key, null, true);
		}

		// If the town doesn't cost money to create, just make the Town.
		if (noCharge || !TownyEconomyHandler.isActive()) {
			BukkitTools.ifCancelledThenThrow(new PreNewTownEvent(player, name, spawnLocation, 0));
			try {
				newTown(world, name, resident, key, spawnLocation, player);
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(name)));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				if (!(e instanceof CancelledEventException)) {
					plugin.getLogger().log(Level.WARNING, "An exception occurred while creating a new town", e);
				}
			}
			return;
		}

		// Fire a cancellable event that allows allows plugins to alter the price of a town.
		PreNewTownEvent pnte = new PreNewTownEvent(player, name, spawnLocation, TownySettings.getNewTownPrice());
		BukkitTools.ifCancelledThenThrow(pnte);

		// Test if the resident can afford the town.
		double cost = pnte.getPrice();
		if (!resident.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translatable.of("msg_you") : resident.getName()), cost));

		// Send a confirmation before taking their money and throwing the PreNewTownEvent.
		final String finalName = name;
		Confirmation.runOnAccept(() -> {
			try {
				// Make town.
				newTown(world, finalName, resident, key, spawnLocation, player, cost);
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(finalName)));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				if (!(e instanceof CancelledEventException)) {
					plugin.getLogger().log(Level.WARNING, "An exception occurred while creating a new town", e);
				}
			}
		})
		.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
		.setCost(new ConfirmationTransaction(() -> cost, resident, "New Town Cost",
			Translatable.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translatable.of("msg_you") : resident.getName()), prettyMoney(cost))))
		.sendTo(player);
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn, Player player) throws TownyException {
		return newTown(world, name, resident, key, spawn, player, 0);
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn, Player player, double cost) throws TownyException {

		TownyUniverse.getInstance().newTown(name);
		Town town = TownyUniverse.getInstance().getTown(name);
		
		// This should never happen
		if (town == null)
			throw new TownyException(String.format("Error fetching new town from name '%s'", name));

		TownBlock townBlock = new TownBlock(key.getX(), key.getZ(), world);
		townBlock.setTown(town);
		TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, townBlock, player, false, true, false);
		preClaimEvent.setCancelMessage(Translation.of("msg_claim_error", 1, 1));
		
		if (BukkitTools.isEventCancelled(preClaimEvent)) {
			TownyUniverse.getInstance().removeTownBlock(townBlock);
			TownyUniverse.getInstance().unregisterTown(town);
			if (TownyEconomyHandler.isActive() && cost > 0)
				resident.getAccount().deposit(cost, "Cancelled town creation refund.");
			
			throw new CancelledEventException(preClaimEvent);
		}

		town.setRegistered(System.currentTimeMillis());
		town.setMapColorHexCode(TownySettings.getDefaultTownMapColor());
		resident.setTown(town);
		town.setMayor(resident, false);
		town.setFounder(resident.getName());

		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());
		town.setSpawn(spawn);
		
		// Disable upkeep if the mayor is an npc
		if (resident.isNPC())
			town.setHasUpkeep(false);

		if (world.isUsingPlotManagementRevert()) {
			PlotBlockData plotChunk = TownyRegenAPI.getPlotChunk(townBlock);
			if (plotChunk != null && TownyRegenAPI.getRegenQueueList().contains(townBlock.getWorldCoord())) {
				// This plot is in the regeneration queue.
				TownyRegenAPI.removeFromActiveRegeneration(plotChunk); // just claimed so stop regeneration.
				TownyRegenAPI.removeFromRegenQueueList(townBlock.getWorldCoord()); // Remove the WorldCoord from the regenqueue.
				TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
			} else {
				TownyRegenAPI.handleNewSnapshot(townBlock);
			}
		}
		
		if (TownyEconomyHandler.isActive()) {
			TownyMessaging.sendDebugMsg("Creating new Town account: " + TownySettings.getTownAccountPrefix() + name);
			try {
				town.getAccount().setBalance(0, "Setting 0 balance for Town");
			} catch (NullPointerException e1) {
				throw new TownyException("The server economy plugin " + TownyEconomyHandler.getVersion() + " could not return the Town account!");
			}
		}
		
		if (TownySettings.isTownTagSetAutomatically())
			town.setTag(NameUtil.getTagFromName(name));
		
		resident.save();
		townBlock.save();
		town.save();
		world.save();

		// Reset cache permissions for anyone in this TownBlock
		plugin.updateCache(townBlock.getWorldCoord());

		BukkitTools.fireEvent(new NewTownEvent(town));

		return town;
	}

	public static void townRename(CommandSender sender, Town town, String newName) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		// Check if the player has to wait because of recently renaming their town.
		String uuid = town.getUUID().toString();
		if (CooldownTimerTask.hasCooldown(uuid, CooldownType.TOWN_RENAME)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_you_must_wait_x_seconds_before_renaming_your_town", CooldownTimerTask.getCooldownRemaining(uuid, CooldownType.TOWN_RENAME)));
			return;
		}

		// Fire a cancellable event.
		TownPreRenameEvent event = new TownPreRenameEvent(town, newName);
		if (BukkitTools.isEventCancelled(event)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_rename_cancelled"));
			return;
		}

		double renameCost = TownySettings.getTownRenameCost();
		if (TownyEconomyHandler.isActive() && renameCost > 0 && !town.getAccount().withdraw(renameCost, String.format("Town renamed to: %s", newName))) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_no_money", prettyMoney(renameCost)));
			return;
		}

		// Put a cooldown on renaming the town. 
		CooldownTimerTask.addCooldownTimer(uuid, CooldownType.TOWN_RENAME);

		// Rename the town.
		try {
			townyUniverse.getDataSource().renameTown(town, newName);
			town = townyUniverse.getTown(newName);
			// This should never happen
			if (town == null)
				throw new TownyException("Error renaming town! Cannot fetch town with new name " + newName);

			town.save();
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_name", sender.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
	}

	public void townLeave(Player player, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode());
		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);

		if (resident.isMayor())
			throw new TownyException(Translatable.of("msg_mayor_abandon"));

		if (resident.isJailed() && TownySettings.JailDeniesTownLeave() && resident.getJailTown().getName().equals(town.getName()))
			throw new TownyException(Translatable.of("msg_cannot_abandon_town_while_jailed"));

		if (args.length > 0 && args[0].equalsIgnoreCase("-ignore")) {
			townLeave(player, resident, town);
			return;
		}

		Confirmation.runOnAccept(() -> townLeave(player, resident, town))
		.setCancellableEvent(new TownLeaveEvent(resident, town))
		.sendTo(player);
	}

	private void townLeave(Player player, Resident resident, Town town) {
		if (resident.isJailed() && resident.getJailTown().getUUID().equals(town.getUUID()))
			JailUtil.unJailResident(resident, UnJailReason.LEFT_TOWN);

		if (town.hasResident(resident))
			resident.removeTown();

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_left_town", resident.getName()));
		TownyMessaging.sendMsg(player, Translatable.of("msg_left_town", resident.getName()));

		town.checkTownHasEnoughResidentsForNationRequirements();
	}

	/**
	 * Wrapper for the townSpawn() method. All calls should be through here
	 * unless bypassing for admins.
	 *
	 * @param player - Player.
	 * @param split  - Current command arguments.
	 * @param outpost - Whether this in an outpost or not.
	 * @param ignoreWarning - Whether to ignore cost warning and pay automatically.
	 * @throws TownyException - Exception.
	 */
	public static void townSpawn(Player player, String[] split, Boolean outpost, boolean ignoreWarning) throws TownyException{

		if ((split.length == 1 && split[0].equals("-ignore")) || (split.length > 1 && split[1].equals("-ignore"))) {
			ignoreWarning = true;
		}
		
		Town town;
		String notAffordMSG;

		// Set target town and affiliated messages.
		if (split.length == 0 || outpost || split[0].equals("-ignore")) {

			town = getTownFromPlayerOrThrow(player);
			notAffordMSG = Translatable.of("msg_err_cant_afford_tp").forLocale(player);
		} else {
			// split.length > 1
			town = getTownOrThrow(split[0]);
			notAffordMSG = Translatable.of("msg_err_cant_afford_tp_town", town.getName()).forLocale(player);
		}
			
		SpawnUtil.sendToTownySpawn(player, split, town, notAffordMSG, outpost, ignoreWarning, SpawnType.TOWN);

	}

	public void townDelete(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode());
		catchRuinedTown(player);

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		final Town town = split.length == 0
				? getTownFromPlayerOrThrow(player)
				: getTownOrThrow(split[0]);

		if (split.length == 0 // No args, self deleting town. OR player supplied the town name unnecessarily.
			|| (town.hasResident(player) && townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode()))) {
			// Send information about ruining if enabled.
			if (TownySettings.getTownRuinsEnabled()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_warning_town_ruined_if_deleted", TownySettings.getTownRuinsMaxDurationHours()));
				if (TownySettings.getTownRuinsReclaimEnabled())
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_warning_town_ruined_if_deleted2", TownySettings.getTownRuinsMinDurationHours()));
			}
			Confirmation.runOnAccept(() -> {
				if (townyUniverse.getDataSource().removeTown(town, DeleteTownEvent.Cause.COMMAND, player)) {
					if (TownySettings.getTownUnclaimCoolDownTime() > 0)
						CooldownTimerTask.addCooldownTimer(player.getName(), CooldownType.TOWN_DELETE);
				}
			}).sendTo(player);
			return;
		}

		// An argument has been passed in the command, and the command sender is not a member of the town and able to delete it.
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode(), Translatable.of("msg_err_admin_only_delete_town"));

		Confirmation.runOnAccept(() -> {
			TownyMessaging.sendMsg(player, Translatable.of("town_deleted_by_admin", town.getName()));
			townyUniverse.getDataSource().removeTown(town, DeleteTownEvent.Cause.ADMIN_COMMAND, player);
		}).sendTo(player);
	}

	public static void townAddResidents(CommandSender sender, Town town, List<Resident> invited) throws TownyException {
		List<String> invitedResidents = invited.stream()
				.filter(res -> !res.hasMode("ignoreinvites"))
				.filter(res -> inviteResidentToTownOrThrow(sender, res, town))
				.map(Resident::getName)
				.collect(Collectors.toList());

		if (invitedResidents.isEmpty()) 
			throw new TownyException(Translatable.of("msg_invalid_name"));

		String name = sender instanceof Player player ? player.getName() : "Console";
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_invited_join_town", name, StringMgmt.join(invitedResidents, ", ")));
		town.save();
	}

	private static boolean inviteResidentToTownOrThrow(CommandSender sender, Resident newMember, Town town) {
		final boolean admin = !(sender instanceof Player player) || TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player);
		try {
			if (!admin)
				BukkitTools.ifCancelledThenThrow(new TownPreAddResidentEvent(town, newMember));

			// Not online
			if (!newMember.isOnline()) 
				throw new TownyException(Translatable.of("msg_player_is_not_online", newMember.getName()));

			// only add players with the right permissions.
			if (!newMember.hasPermissionNode(PermissionNodes.TOWNY_TOWN_RESIDENT.getNode()))
				throw new TownyException(Translatable.of("msg_not_allowed_join", newMember.getName()));

			if (!town.isAllowedThisAmountOfResidents(town.getNumResidents() + 1, town.isCapital()))
				throw new TownyException(Translatable.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsForTown(town)));

			if (town.hasNation() && !town.getNationOrNull().canAddResidents(1))
				throw new TownyException(Translatable.of("msg_err_cannot_add_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation(), newMember.getName()));

			if (!admin && TownySettings.getTownInviteCooldown() > 0 && (System.currentTimeMillis()/1000 - newMember.getRegistered()/1000) < TownySettings.getTownInviteCooldown())
				throw new TownyException(Translatable.of("msg_err_resident_doesnt_meet_invite_cooldown", newMember));

			// Throws when the player has a town or is in this town already.
			town.addResidentCheck(newMember);

			// Throws when the player cannot use the invite. Otherwise this will send an invite to the resident.
			townInviteResident(sender, town, newMember);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			return false;
		}
		return true;
	}

	public static void townAddResident(Town town, Resident resident) throws AlreadyRegisteredException {
		// If player is outlawed in target town, remove them from outlaw list.
		if (town.hasOutlaw(resident))
			town.removeOutlaw(resident);

		resident.setTown(town);
		plugin.deleteCache(resident);
		resident.save();
		town.save();
	}

	private static void townInviteResident(CommandSender sender,Town town, Resident newMember) throws TownyException {

		PlayerJoinTownInvite invite = new PlayerJoinTownInvite(sender, newMember, town);

		BukkitTools.ifCancelledThenThrow(new TownPreInvitePlayerEvent(invite));

		try {
			if (!InviteHandler.inviteIsActive(invite)) {
				newMember.newReceivedInvite(invite);
				town.newSentInvite(invite);
				InviteHandler.addInvite(invite);
				Player player = TownyAPI.getInstance().getPlayer(newMember);
				if (player != null)
					TownyMessaging.sendRequestMessage(player,invite);
				BukkitTools.fireEvent(new TownInvitePlayerEvent(invite));
			} else {
				throw new TownyException(Translatable.of("msg_err_player_already_invited", newMember.getName()));
			}
		} catch (TooManyInvitesException e) {
			newMember.deleteReceivedInvite(invite);
			town.deleteSentInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	/**
	 * Transforms a list of names into a list of residents to be kicked.
	 * Command: /town kick [resident] .. [resident]
	 *
	 * @param player - Player who initiated the kick command.
	 * @param names - List of names to kick.
	 * @throws TownyException on error.
	 */
	public static void townKick(Player player, String[] names) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode());
		catchRuinedTown(player);
		Resident resident = getResidentOrThrow(player);
		Town town = resident.getTown();

		townKickResidents(player, resident, town, ResidentUtil.getValidatedResidentsOfTown(player, town, names));

		// Reset everyones cache permissions as this player leaving can affect
		// multiple areas.
		plugin.resetCache();
	}

	/**
	 * Method for kicking residents from a town.
	 * 
	 * @param sender   CommandSender who initiated the kick.
	 * @param resident Resident who initiated the kick.
	 * @param town     Town the list of Residents are being kicked from.
	 * @param kicking  List of Residents being kicked from Towny.
	 * @throws TownyException when there is no one to kick.
	 */
	private static void townKickResidents(CommandSender sender, Resident resident, Town town, List<Resident> kicking) throws TownyException {
		List<String> kickedResidents = kicking.stream()
				.filter(res -> kickResidentFromTownOrThrow(sender, resident, res, town))
				.map(Resident::getName)
				.collect(Collectors.toList());

		if (kickedResidents.isEmpty())
			throw new TownyException(Translatable.of("msg_invalid_name"));

		town.checkTownHasEnoughResidentsForNationRequirements();
		town.save();

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_kicked",  resident.getName(), StringMgmt.join(kickedResidents, ", ")));
	}

	private static boolean kickResidentFromTownOrThrow(CommandSender sender, Resident senderResident, Resident resToKick, Town town) {
		try {
			if (!town.hasResident(resToKick))
				throw new TownyException(Translatable.of("msg_resident_not_your_town"));

			if (senderResident.equals(resToKick))
				throw new TownyException(Translatable.of("msg_you_cannot_kick_yourself"));

			if (resToKick.isMayor())
				throw new TownyException(Translatable.of("msg_you_cannot_kick_this_resident", resToKick));

			if (!senderResident.isMayor() && TownySettings.getTownUnkickableRanks().stream().anyMatch(resToKick::hasTownRank))
				throw new TownyException(Translatable.of("msg_you_cannot_kick_this_resident", resToKick));

			BukkitTools.ifCancelledThenThrow(new TownKickEvent(resToKick, sender));

			// Remove any trust they have in the town or town's plots.
			resToKick.removeTrustInTown(town);

			// Finally kick the resident.
			resToKick.removeTown();
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			return false;
		}
		return true;
	}

	private void parseTownRanklistCommand(final Player player, String[] split) throws NoPermissionException, TownyException {
		Town town;
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode());
		catchRuinedTown(player);
		town = split.length > 1 ? getTownOrThrow(split[1]) : getTownFromPlayerOrThrow(player);
		TownyMessaging.sendMessage(player, TownyFormatter.getRanksForTown(town, Translator.locale(player)));
	}

	private void parseTownReclaimCommand(final Player player) throws NoPermissionException, TownyException {
		if (!TownySettings.getTownRuinsReclaimEnabled())
			throw new TownyException(Translatable.of("msg_err_command_disable"));

		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RECLAIM.getNode());
		TownRuinUtil.processRuinedTownReclaimRequest(player);
	}

	private static void parseTownJoin(Player player, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode());
		Resident resident = getResidentOrThrow(player);
		if (resident.hasTown())
			throw new TownyException(Translatable.of("msg_err_already_res2", "You"));

		Town town = getTownOrThrow(args[0]);

		// Check if town is town is free to join.
		if (!town.isOpen())
			throw new TownyException(Translatable.of("msg_err_not_open", town.getFormattedName()));

		if (town.hasNation() && !town.getNationOrNull().canAddResidents(1))
			throw new TownyException(Translatable.of("msg_err_cannot_join_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation()));

		if (!town.isAllowedThisAmountOfResidents(town.getNumResidents() + 1, town.isCapital()))
			throw new TownyException(Translatable.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsForTown(town)));

		if (town.hasOutlaw(resident))
			throw new TownyException(Translatable.of("msg_err_outlaw_in_open_town"));

		BukkitTools.ifCancelledThenThrow(new TownPreAddResidentEvent(town, resident));

		townAddResident(town, resident);

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_join_town", resident.getName()));
	}

	/**
	 * Checks if the player is allowed to handle adding invites, then checks through
	 * a list of names to be invited, or to have their invites revoked. Will
	 * dispatch invites and/or revoke invites from names beginning with -.
	 *
	 * @param sender        Sender.
	 * @param specifiedTown Town to add to if not null.
	 * @param names         Names to add.
	 * @throws TownyException Thrown when an the invites are not allowed because of
	 *                        lack of permissions, ruined/bankruptcy, or the town
	 *                        having no spawn set.
	 */
	public static void townAdd(CommandSender sender, Town specifiedTown, String[] names) throws TownyException {
		Player player = null;
		if (sender instanceof Player p) {
			player = p;
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());
			catchRuinedTown(player);
		}

		// The /ta command can specify a town, the /t command cannot.
		Town town = specifiedTown != null ? specifiedTown : getTownFromPlayerOrThrow(player);
		if (town.isBankrupt())
			throw new TownyException(Translatable.of("msg_err_bankrupt_town_cannot_invite"));

		List<String> nameList = new ArrayList<>(Arrays.asList(names));
		if (TownySettings.getMaxDistanceFromTownSpawnForInvite() > 0)
			nameList = gatherNearbyPlayerNames(sender, nameList, town);

		// Get a list of valid names to invite.
		List<String> toInviteList = nameList.stream()
				.filter(name -> !name.startsWith("-") && !town.hasResident(name))
				.collect(Collectors.toList());

		// Get a list of negated names to revoke an invite from. 
		// (Legacy code also included anyone that is already a resident of the town for some reason.)
		List<String> toRevokeInviteList = nameList.stream()
				.filter(name -> name.startsWith("-") || town.hasResident(name))
				.map(name -> name.startsWith("-") ? name.substring(1) : name)
				.collect(Collectors.toList());

		// There's a special permission node to prevent invite spam.
		if (toInviteList.size() + toRevokeInviteList.size() > 1)
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD_MULTIPLE.getNode());

		// Begin process of sending invites.
		if (!toInviteList.isEmpty())
			townAddResidents(sender, town, ResidentUtil.getValidatedResidents(sender, toInviteList));

		// Pare down revoke list to names that have received invites, then revoke the invite.
		if (!toRevokeInviteList.isEmpty()) {
			townRevokeResidentInvites(sender, town, 
				ResidentUtil.getValidatedResidents(sender, town.getSentInvites().stream()          // Take the towns sent invites.
					.filter(invite -> toRevokeInviteList.contains(invite.getReceiver().getName())) // Find an invite which matches one of our names.
					.map(invite -> invite.getReceiver().getName())                                 // Get the name of that person who was invited.
					.collect(Collectors.toList())));
		}
	}

	private static List<String> gatherNearbyPlayerNames(CommandSender sender, List<String> names, Town town) throws TownyException {
		if (!town.hasSpawn())
			throw new TownyException(Translatable.of("msg_err_townspawn_has_not_been_set"));

		Location spawnLoc = town.getSpawn();
		int maxDistance = TownySettings.getMaxDistanceFromTownSpawnForInvite();
		return names.stream()
			.filter(name -> playerIsNearEnoughToTownSpawn(sender, name, spawnLoc, maxDistance))
			.collect(Collectors.toList());
	}

	private static boolean playerIsNearEnoughToTownSpawn(CommandSender sender, String name, Location spawnLoc, int maxDistance) {
		try {
			Player player = BukkitTools.getPlayerExact(name);
			if (player == null)
				return false;

			Location playerLoc = player.getLocation();
			if (!playerLoc.getWorld().equals(spawnLoc.getWorld()))
				throw new TownyException(Translatable.of("msg_err_player_too_far_from_town_spawn", name, maxDistance));

			double distance = spawnLoc.distance(playerLoc);
			if (distance > maxDistance)
				throw new TownyException(Translatable.of("msg_err_player_too_far_from_town_spawn", name, maxDistance));

			return true;
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			return false;
		}
	}

	private static void townRevokeResidentInvites(CommandSender sender, Town town, List<Resident> residents) {
		residents.forEach((resident) ->
			InviteHandler.getActiveInvitesFor(town, resident).forEach(invite -> {
				try {
					InviteHandler.declineInvite(invite, true);
					TownyMessaging.sendMsg(sender, Translatable.of("town_revoke_invite_successful", resident.getName()));
				} catch (InvalidObjectException e) {
					plugin.getLogger().log(Level.WARNING, "An exception occurred while revoking invite for resident " + resident.getName() + " from town " + town.getName(), e);
				}
			}));
	}

	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(CommandSender sender, Town town, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PERM.getNode());
		// Make sure we are allowed to set these permissions.
		toggleTest(town, StringMgmt.join(split, " "));
		setTownBlockPermissions(sender, town, town.getPermissions(), split, false);
		town.save();
	}

	public static void setTownBlockPermissions(CommandSender sender, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split.length > 3) {
			displaySetPlotPermissionsHelp(sender, townBlockOwner);
			return;
		}

		// /t set perm reset has been run.
		if (split[0].equalsIgnoreCase("reset")) {
			resetTownBlockOwnersTownBlocks(sender, townBlockOwner);
			return;
		}

		// Permissions commands for residents use Friend instead of Resident and Town
		// instead of Nation, we must set these values to their "true" value for
		// permissions to be set correctly.
		if (split[0].equalsIgnoreCase("friend"))
			split[0] = "resident";
		else if (split[0].equalsIgnoreCase("town"))
			split[0] = "nation";
		if (split[0].equalsIgnoreCase("itemuse"))
			split[0] = "item_use";
		if (split.length > 1 && split[1].equalsIgnoreCase("itemuse"))
			split[1] = "item_use";

		// A more complex command has been run for setting permissions.
		TownyPermissionChange permChange = null;
		switch (split.length) {
		case 1: { // Set all perms to On or Off ie: /town set perm off'
			try {
				boolean b = StringMgmt.parseOnOff(split[0]);
				permChange = new TownyPermissionChange(TownyPermissionChange.Action.ALL_PERMS, b);
				perm.change(permChange);
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "on/off."));
				return;
			}
			break;
		}
		case 2: { // Either /t set perm PERMLEVEL on|off or /t set perm ACTIONTYPE on|off
			boolean b;
			try {
				b = StringMgmt.parseOnOff(split[1]);
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "on/off."));
				return;
			}

			// Check if it is a perm level first
			try {
				TownyPermission.PermLevel permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));
				permChange = new TownyPermissionChange(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				perm.change(permChange);
			} catch (IllegalArgumentException permLevelException) {
				// If it is not a perm level, then check if it is a action type
				try {
					TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase(Locale.ROOT));
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					perm.change(permChange);
				} catch (IllegalArgumentException actionTypeException) {
					// It isn't either perm level or action type, it's a syntax error.
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
					return;
				}
			}
			break;
		}
		case 3: { // /t set perm PERMLEVEL ACTIONTYPE on|off
			TownyPermission.PermLevel permLevel;
			TownyPermission.ActionType actionType;

			try {
				permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));
				actionType = TownyPermission.ActionType.valueOf(split[1].toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException exception) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
				return;
			}

			try {
				boolean b = StringMgmt.parseOnOff(split[2]);
				permChange = new TownyPermissionChange(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);
				perm.change(permChange);
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_input", "on/off."));
				return;
			}
			break;
		}
		}

		// Propagate perms to all unchanged townblocks
		for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {
			// The townBlock has custom permissions compared to what the townBlockOwner has as default.
			if (townBlock.isChanged())
				continue;

			try {
				BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(townBlock, permChange));
			} catch (CancelledEventException e) {
				TownyMessaging.sendErrorMsg(sender, e.getCancelMessage());
				continue;
			}

			if (townBlockOwner instanceof Town && !townBlock.hasResident()) {
				townBlock.setType(townBlock.getType());
				townBlock.save();
			} else if (townBlockOwner instanceof Resident) {
				townBlock.setType(townBlock.getType());
				townBlock.save();
			}
		}

		Translator translator = Translator.locale(sender);
		TownyMessaging.sendMsg(sender, translator.of("msg_set_perms"));
		TownyMessaging.sendMessage(sender, (Colors.Green + translator.of("status_perm") + " " + ((townBlockOwner instanceof Resident) ? perm.getColourString().replace("n", "t") : perm.getColourString().replace("f", "r"))));
		String on = translator.of("status_on");
		String off = translator.of("status_off");
		TownyMessaging.sendMessage(sender, Colors.Green + translator.of("status_pvp") + " " + (perm.pvp ? on : off) + " " +
										   Colors.Green + translator.of("explosions") + " " + (perm.explosion ? on : off) + " " +
										   Colors.Green + translator.of("firespread") + " " + (perm.fire ? on : off) + " " +
										   Colors.Green + translator.of("mobspawns") + " " + (perm.mobs ? on : off));

		// Reset all caches as this can affect everyone.
		plugin.resetCache();

	}

	private static void displaySetPlotPermissionsHelp(CommandSender sender, TownBlockOwner townBlockOwner) {
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/... set perm"));
		if (townBlockOwner instanceof Town)
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Level", "[resident/nation/ally/outsider]", "", ""));
		if (townBlockOwner instanceof Resident)
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Level", "[friend/town/ally/outsider]", "", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Type", "[build/destroy/switch/itemuse]", "", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "set perm", "[on/off]", "Toggle all permissions"));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "set perm", "[level/type] [on/off]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "set perm", "[level] [type] [on/off]", ""));
		TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "set perm", "reset", ""));
		if (townBlockOwner instanceof Town)
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/town set perm", "ally off", ""));
		if (townBlockOwner instanceof Resident)
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand("Eg", "/resident set perm", "friend build on", ""));
	}

	private static void resetTownBlockOwnersTownBlocks(CommandSender sender, TownBlockOwner townBlockOwner) {
		// reset all townBlock permissions (by town/resident)
		for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {

			if ((townBlockOwner instanceof Town && !townBlock.hasResident()) || 
				(townBlockOwner instanceof Resident && townBlock.hasResident())) {

				TownyPermissionChange permChange = new TownyPermissionChange(TownyPermissionChange.Action.RESET, true, townBlock);
				try {
					BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(townBlock, permChange));
				} catch (CancelledEventException e) {
					TownyMessaging.sendErrorMsg(sender, e.getCancelMessage());
					return;
				}
				// Reset permissions
				townBlock.setType(townBlock.getType());
				townBlock.save();
			}
		}
		if (townBlockOwner instanceof Town)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_perms_reset", "Town owned"));
		else
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_perms_reset", "your"));

		// Reset all caches as this can affect everyone.
		plugin.resetCache();
	}

	private void parseTownCedeCommand(Player player, String[] args) throws TownyException {
		catchConsole(player);
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CEDE_PLOT.getNode());

		if (args.length < 2 || !args[0].equalsIgnoreCase("plot")) {
			HelpMenu.TOWN_CEDE.send(player);
			return;
		}

		Town playerTown = getTownFromPlayerOrThrow(player);
		WorldCoord worldCoord = WorldCoord.parseWorldCoord(player);
		Town wcTown = worldCoord.getTownOrNull();
		if (wcTown == null || !wcTown.equals(playerTown))
			throw new TownyException(Translatable.of("msg_not_own_area"));

		if (worldCoord.getTownBlock().isHomeBlock())
			throw new TownyException(Translatable.of("msg_err_town_cede_you_cannot_cede_your_homeblock"));

		Town townGainingPlot = getTownOrThrow(args[1]);
		if (playerTown.equals(townGainingPlot))
			throw new TownyException(Translatable.of("msg_err_town_cede_you_cannot_cede_a_plot_to_your_own_town"));

		ProximityUtil.allowTownClaimOrThrow(worldCoord.getTownyWorld(), worldCoord, townGainingPlot, false, true);

		if (!townGainingPlot.getMayor().isOnline())
			throw new TownyException(Translatable.of("msg_err_town_cede_the_mayor_is_not_online", townGainingPlot, townGainingPlot.getMayor()));
		Player otherMayor = townGainingPlot.getMayor().getPlayer();

		Confirmation.runOnAccept(() -> offerPlotToTown(playerTown, townGainingPlot, player, otherMayor, worldCoord))
		.setCancellableEvent(new TownCedePlotEvent(playerTown, townGainingPlot, worldCoord.getTownBlock()))
		.setTitle(Translatable.of("msg_town_cede_plot_confirmation_give_plot", townGainingPlot))
		.sendTo(player);
	}

	private void offerPlotToTown(Town townLosingPlot, Town townGainingPlot, Player playerGivingPlot, Player playerGainingPlot, WorldCoord worldCoord) {
		Confirmation.runOnAccept(() -> {
			try {
				ProximityUtil.allowTownClaimOrThrow(worldCoord.getTownyWorld(), worldCoord, townGainingPlot, false, true);

				TownBlock tb = worldCoord.getTownBlockOrNull();
				if (tb == null)
					throw new TownyException(Translatable.of("msg_not_claimed_1"));
				if (!tb.getTownOrNull().equals(townLosingPlot))
					throw new TownyException(Translatable.of("msg_err_town_no_longer_owns_this_plot", townLosingPlot));
				if (tb.isHomeBlock())
					throw new TownyException(Translatable.of("msg_err_town_cede_town_cannot_cede_their_homeblock", townLosingPlot));

				tb.setTown(townGainingPlot);
				tb.save();

				TownyMessaging.sendMsg(playerGainingPlot, Translatable.of("msg_town_cede_plot_you_have_accepted", worldCoord.toString()));
				TownyMessaging.sendMsg(playerGivingPlot, Translatable.of("msg_town_cede_plot_accepted", playerGainingPlot.getName()));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(playerGainingPlot, e.getMessage(playerGainingPlot));
				TownyMessaging.sendErrorMsg(playerGivingPlot, Translatable.of("msg_town_cede_plot_unable_to_accept", playerGainingPlot.getName()));
			}
		})
		.setCancellableEvent(new TownPreClaimEvent(townGainingPlot, worldCoord.getTownBlockOrNull(), playerGainingPlot, false, false, false))
		.runOnCancel(() -> TownyMessaging.sendErrorMsg(playerGivingPlot, Translatable.of("msg_town_cede_plot_denied", playerGainingPlot.getName())))
		.setTitle(Translatable.of("msg_town_cede_plot_confirmation_accept_plot", playerGivingPlot.getName(), worldCoord.toString()))
		.sendTo(playerGainingPlot);
	}

	public static void parseTownClaimCommand(Player player, String[] split) throws TownyException {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TOWN_CLAIM.send(player);
			return;
		}

		catchRuinedTown(player);
		Town town = getTownFromPlayerOrThrow(player);
		catchBankruptTownWithLand(town);
		final boolean outpost = split.length == 1 && split[0].equalsIgnoreCase("outpost");

		// Make initial selection of WorldCoord(s), test whether the player has the
		// required permission node, and vet the selection for proximity, biome,
		// wilderness rules.
		final List<WorldCoord> selection = getTownClaimSelectionOrThrow(player, split, town);

		// Check the Town can claim the vetted selection, available claimblocks/adjacent
		// claims/edge blocks, etc.
		vetTownAllowedTheseClaims(town, outpost, selection);

		// Allow other plugins to have a say in whether the claim is allowed.
		fireTownPreClaimEventOrThrow(player, town, outpost, selection);

		// See if the Town can pay (if required.)
		vetTheTownCanPayIfRequired(player, town, outpost, selection);

		// Begin the actual TownClaim task.
		plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, selection, outpost, true, false));
	}

	private static void catchBankruptTownWithLand(Town town) throws TownyException {
		// Allow a bankrupt town to claim a single plot.
		if (town.isBankrupt() && !town.getTownBlocks().isEmpty())
			throw new TownyException(Translatable.of("msg_err_bankrupt_town_cannot_claim"));
	}

	private static List<WorldCoord> getTownClaimSelectionOrThrow(Player player, String[] split, Town town) throws TownyException {
		List<WorldCoord> selection;
		WorldCoord playerWorldCoord = WorldCoord.parseWorldCoord(player);
		final TownyWorld world = playerWorldCoord.getTownyWorld();

		if (world == null || !world.isUsingTowny())
			throw new TownyException(Translatable.of("msg_set_use_towny_off"));

		if (!world.isClaimable())
			throw new TownyException(Translatable.of("msg_not_claimable"));

		// Prevent someone manually running /t claim world x z (a command which should only be run via /plot claim world x z)
		if (split.length != 0 && TownyAPI.getInstance().getTownyWorld(split[0]) != null)
			throw new TownyException(Translatable.of("tc_err_invalid_command"));

		if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {
			if (!TownySettings.isAllowingOutposts())
				throw new TownyException(Translatable.of("msg_outpost_disable"));

			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode());

			Resident resident = getResidentOrThrow(player);
			// Run various tests required by configuration/permissions through Util.
			OutpostUtil.OutpostTests(town, resident, world, playerWorldCoord, resident.isAdmin(), false);

			if (playerWorldCoord.hasTownBlock())
				throw new TownyException(Translatable.of("msg_already_claimed", playerWorldCoord.getTownOrNull()));

			// Select a single WorldCoord using the AreaSelectionUtil.
			selection = new ArrayList<>();
			selection.add(playerWorldCoord);

		} else if (split.length == 1 && "fill".equalsIgnoreCase(split[0])) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_FILL.getNode());

			final BorderUtil.FloodfillResult result = BorderUtil.getFloodFillableCoords(town, playerWorldCoord);
			if (result.type() != BorderUtil.FloodfillResult.Type.SUCCESS)
				throw result.feedback() != null ? new TownyException(result.feedback()) : new TownyException();
			else if (result.feedback() != null)
				TownyMessaging.sendMsg(player, result.feedback());

			selection = new ArrayList<>(result.coords());
		} else {
			// Standard /t claim [args] behaviour
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode());

			// Select the area, can be one or many.
			selection = AreaSelectionUtil.selectWorldCoordArea(town, playerWorldCoord, split, true);

			// Fast fail when we're claiming a single worldcoord and it is already claimed.
			if (selection.size() == 1 && playerWorldCoord.hasTownBlock())
				throw new TownyException(Translatable.of("msg_already_claimed", playerWorldCoord.getTownOrNull()));

			// If selection is greater than 1 check for the multiclaim node.
			if (selection.size() > 1)
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE.getNode());	
		}

		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		// Vet the selection for all sorts of proximity, biome, already-claimed, etc rules.
		return vetTownClaimSelection(player, town, selection);
	}

	private static List<WorldCoord> vetTownClaimSelection(Player player, Town town, List<WorldCoord> selection) throws TownyException {
		/*
		 * Filter out any unallowed claims.
		 */
		TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));

		// Filter out any TownBlocks which aren't Wilderness. 
		selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);

		// Filter out any TownBlocks which have too much of the unwanted biomes, when enabled.
		selection = AreaSelectionUtil.filterOutUnwantedBiomeWorldCoords(player, selection);

		// Filter out any TownBlocks which have too much ocean biomes, when enabled.
		selection = AreaSelectionUtil.filterOutOceanBiomeWorldCoords(player, selection);

		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		// Filter out townblocks too close to another Town's homeblock.
		selection = AreaSelectionUtil.filterInvalidProximityToHomeblock(selection, town);
		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

		// Filter out townblocks too close to other Towns' normal townblocks.
		selection = AreaSelectionUtil.filterInvalidProximityTownBlocks(selection, town);
		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));

		TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection [" + selection.size() + "] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
		return selection;
	}

	private static void vetTownAllowedTheseClaims(Town town, boolean outpost, List<WorldCoord> selection) throws TownyException {
		// Not enough available claims.
		if (!town.hasUnlimitedClaims() && selection.size() > town.availableTownBlocks())
			throw new TownyException(Translatable.of("msg_err_not_enough_blocks"));

		// Prevent straight line claims if configured, and the town has enough
		// townblocks claimed, and this is not an outpost.
		ProximityUtil.testAdjacentClaimsRulesOrThrow(selection.get(0), town, outpost);

		// When not claiming an outpost, make sure at least one of the selection is
		// attached to a claimed plot.
		if (!outpost && !isEdgeBlock(town, selection) && !town.getTownBlocks().isEmpty())
			throw new TownyException(Translatable.of("msg_err_not_attached_edge"));
	}

	private static void fireTownPreClaimEventOrThrow(Player player, Town town, boolean outpost, List<WorldCoord> selection) throws TownyException {
		int blockedClaims = 0;
		String cancelMessage = "";
		boolean isHomeblock = town.getTownBlocks().size() == 0;
		for (WorldCoord coord : selection) {
			TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, new TownBlock(coord), player, outpost, isHomeblock, false);
			if(BukkitTools.isEventCancelled(preClaimEvent)) {
				blockedClaims++;
				cancelMessage = preClaimEvent.getCancelMessage();
			}
		}
		if (blockedClaims > 0)
			throw new TownyException(String.format(cancelMessage, blockedClaims, selection.size()));
	}

	private static void vetTheTownCanPayIfRequired(Player player, Town town, final boolean outpost, final List<WorldCoord> selection) throws TownyException {
		if (TownyEconomyHandler.isActive()) {
			Account townAccount = town.getAccount();
			if (townAccount == null) // This is usually right about the time when admins find out their Economy plugin doesn't work well with Towny. 
				throw new TownyException("The server economy plugin " + TownyEconomyHandler.getVersion() + " could not return the Town account!");

			int selectionSize = selection.size();
			double blockCost = getSelectionCost(town, outpost, selectionSize);

			if (!townAccount.canPayFromHoldings(blockCost)) {
				double missingAmount = blockCost - townAccount.getHoldingBalance();
				throw new TownyException(Translatable.of("msg_no_funds_claim2", selectionSize, prettyMoney(blockCost), prettyMoney(missingAmount), new DecimalFormat("#").format(missingAmount)));
			}

			// Charge the town for the claim.
			TownyEconomyHandler.economyExecutor().execute(() ->
				town.getAccount().withdraw(blockCost, String.format("Town Claim (%d) by %s", selectionSize, player.getName())));
		}
	}

	private static double getSelectionCost(Town town, final boolean outpost, int selectionSize) throws TownyException {
		return outpost ? TownySettings.getOutpostCost()
				: selectionSize == 1 ? town.getTownBlockCost() : town.getTownBlockCostN(selectionSize);
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) throws TownyException {
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TOWN_UNCLAIM.send(player);
			return;
		}
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode());
		catchRuinedTown(player);

		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

		if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
			/* The Player is trying to unclaim all of their land. */
			parseTownUnclaimAllCommand(player, town, resident, world);
			return;
		}

		// Check permissions here because of the townunclaim mode.
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode());
		
		// Prevent someone manually running /t unclaim world x z (a command which should only be run via /plot claim world x z)
		if (split.length == 3 && TownyAPI.getInstance().getTownyWorld(split[0]) != null)
			throw new TownyException(Translatable.of("tc_err_invalid_command"));
		
		List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(town, WorldCoord.parseWorldCoord(player), split);
		selection = AreaSelectionUtil.filterOwnedBlocks(town, selection);
		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		if (selection.get(0).getTownBlock().isHomeBlock())
			throw new TownyException(Translatable.of("msg_err_cannot_unclaim_homeblock"));

		if (AreaSelectionUtil.filterHomeBlock(town, selection)) {
			// Do not stop the entire unclaim, just warn that the homeblock cannot be unclaimed
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_unclaim_homeblock"));
		}

		// Prevent unclaiming land that would reduce the number of adjacent claims of neighbouring plots below the threshold.
		ProximityUtil.testAdjacentUnclaimsRulesOrThrow(selection.get(0), town);

		BukkitTools.ifCancelledThenThrow(new TownPreUnclaimCmdEvent(town, resident, world, selection));

		// Handle a negative unclaim refund (yes, where someone is being charged money to unclaim their land. It's a thing.)
		if (TownyEconomyHandler.isActive() && TownySettings.getClaimRefundPrice() < 0) {
			double cost = Math.abs(TownySettings.getClaimRefundPrice() * selection.size());
			if (!town.getAccount().canPayFromHoldings(cost)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", prettyMoney(cost)));
				return;
			}
			List<WorldCoord> finalSelection = selection;
			Confirmation.runOnAccept(()-> {
				if (!town.getAccount().canPayFromHoldings(cost)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", prettyMoney(cost)));
					return;
				}
				// Set the area to unclaim
				plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, finalSelection, false, false, false));
			})
			.setTitle(Translatable.of("confirmation_unclaiming_costs", prettyMoney(cost)))
			.sendTo(player);
			return;
		}
		// Set the area to unclaim without a unclaim refund.
		plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, selection, false, false, false));
	}

	private static void parseTownUnclaimAllCommand(Player player, Town town, Resident resident, TownyWorld world) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM_ALL.getNode());
		List<WorldCoord> selection = new ArrayList<>();
		selection.addAll(town.getTownBlocks().stream().map(TownBlock::getWorldCoord).collect(Collectors.toList()));
		if (town.hasHomeBlock())
			selection.remove(town.getHomeBlock().getWorldCoord());
		BukkitTools.ifCancelledThenThrow(new TownPreUnclaimCmdEvent(town, resident, world, selection));

		if (TownyEconomyHandler.isActive() && TownySettings.getClaimRefundPrice() < 0) {
			int numTownBlocks = town.getTownBlocks().size() - (town.hasHomeBlock() ? 1 : 0); 
			String formattedCost = prettyMoney(Math.abs(TownySettings.getClaimRefundPrice() * numTownBlocks));
			// Unclaiming will cost the player money because of a negative refund price. Have them confirm the cost.
			Confirmation
				.runOnAcceptAsync(new TownClaim(plugin, player, town, null, false, false, false)) 
				.setTitle(Translatable.of("confirmation_unclaiming_costs", formattedCost))
				.sendTo(player);
			return;
		}
		// No cost to unclaim the land.
		plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, null, false, false, false));
	}

	private void parseTownTakeoverClaimCommand(Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_TAKEOVERCLAIM.getNode());
		catchRuinedTown(player);

		if (!TownySettings.isOverClaimingAllowingStolenLand())
			throw new TownyException(Translatable.of("msg_err_taking_over_claims_is_not_enabled"));

		Town town = getTownFromPlayerOrThrow(player);

		long ageRequirement = TownySettings.getOverclaimingTownAgeRequirement();
		if (ageRequirement > 0L) {
			long ageNeeded = System.currentTimeMillis() - ageRequirement;
			if (ageNeeded < town.getRegistered())
				throw new TownyException(Translatable.of("msg_err_your_town_is_not_old_enough_to_overclaim", TimeMgmt.getFormattedTimeValue(town.getRegistered() - ageNeeded)));
		}

		// Make sure this wouldn't end up becoming a homeblock.
		if (town.getTownBlocks().size() == 0)
			throw new TownyException(Translatable.of("msg_err_you_cannot_make_this_your_homeblock"));

		WorldCoord wc = WorldCoord.parseWorldCoord(player);
		if (wc.isWilderness())
			throw new TownyException(Translatable.of("msg_not_own_place"));

		Town overclaimedTown = wc.getTownOrNull();

		// Make sure the town doesn't already own this land.
		if (overclaimedTown.equals(town))
			throw new TownyException(Translatable.of("msg_already_claimed_1"));

		// Make sure this is in a town which is overclaimed, allowing for stealing land.
		if (!wc.canBeStolen())
			throw new TownyException(Translatable.of("msg_err_this_townblock_cannot_be_taken_over"));

		// Make sure that if nations are involved and the setting is true, that the
		// overclaiming nation has the other declared as an enemy.
		if (TownySettings.isOverclaimingWithNationsRequiringEnemy()) {
			Nation overclaimingNation = town.getNationOrNull();
			Nation overclaimedNation = overclaimedTown.getNationOrNull();
			if (overclaimingNation != null && overclaimedNation != null && !overclaimingNation.hasEnemy(overclaimedNation)) {
				throw new TownyException(Translatable.of("msg_err_cannot_overclaim_this_town_because_they_arent_your_enemy"));
			}
		}

		// Not enough available claims.
		if (!town.hasUnlimitedClaims() && town.availableTownBlocks() < 1)
			throw new TownyException(Translatable.of("msg_err_not_enough_blocks"));

		// Not connected to the town stealing the land.
		if (!isEdgeBlock(town, wc, new ArrayList<>()))
			throw new TownyException(Translatable.of("msg_err_not_attached_edge"));

		// Prevent straight line claims if configured, and the town has enough townblocks claimed, and this is not an outpost.
		ProximityUtil.testAdjacentClaimsRulesOrThrow(wc, town, false);

		// Prevent claiming that would cut off a section of a town from the main body. 
		if (takeoverWouldCutATownIntoTwoSections(wc, town))
			throw new TownyException(Translatable.of("msg_err_you_cannot_over_claim_would_cut_into_two"));

		// Filter out stealing land that is too close to another Town's homeblock.
		if (TownySettings.isOverClaimingPreventedByHomeBlockRadius() && AreaSelectionUtil.isTooCloseToHomeBlock(wc, town))
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

		if(BukkitTools.isEventCancelled(new TownPreClaimEvent(town, wc.getTownBlockOrNull(), player, false, false, true)))
			throw new TownyException(Translatable.of("msg_err_another_plugin_cancelled_takeover"));

		double cost = TownySettings.getTakeoverClaimPrice();
		String costSlug = !TownyEconomyHandler.isActive() || cost <= 0 ? Translatable.of("msg_spawn_cost_free").forLocale(player) : prettyMoney(cost);
		String townName = overclaimedTown.getName();
		Confirmation.runOnAcceptAsync(new TownClaim(plugin, player, town, Arrays.asList(wc), false, true, false, true))
			.setTitle(Translatable.of("confirmation_you_are_about_to_take_over_a_claim", townName, costSlug))
			.setCost(new ConfirmationTransaction(() -> cost, town, "Takeover Claim (" + wc.toString() + ") from " + townName + "."))
			.sendTo(player);
	}

	private boolean takeoverWouldCutATownIntoTwoSections(WorldCoord worldCoord, Town townOverClaiming) {
		// If the surrounding townblocks has at least 2 townblocks owned by the
		// overclaimed town and 1 plot that belongs to the wilderness or a third-town,
		// we can assume that it will cause an orphaned townblock.
		Town overclaimed = worldCoord.getTownOrNull();
		List<WorldCoord> surroundingClaims = worldCoord.getCardinallyAdjacentWorldCoords(true);
		long townOwned = surroundingClaims.stream().filter(wc -> wc.hasTown(overclaimed)).count();
		if (townOwned < 2)
			return false;

		long wildOr3rdPartyOwned = surroundingClaims.stream()
				.filter(wc -> !wc.hasTown(overclaimed) && !wc.hasTown(townOverClaiming))
				.count();
		return wildOr3rdPartyOwned > 0;
	}

	private void parseTownBankHistoryCommand(final Player player, String[] split)
			throws NoPermissionException, TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode());
		catchRuinedTown(player);
		int pages = 10;
		if (split.length > 1)
			pages = MathUtil.getIntOrThrow(split[1]);
		getTownFromPlayerOrThrow(player).generateBankHistoryBook(player, pages);
	}

	public static void parseTownMergeCommand(Player player, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_MERGE.getNode());
		catchRuinedTown(player);
		parseTownMergeCommand(player, args, getTownFromPlayerOrThrow(player), false);
	}

	public static void parseTownMergeCommand(CommandSender sender, String[] args, @NotNull final Town remainingTown, boolean admin) throws TownyException {

		if (args.length <= 0) // /t merge
			throw new TownyException(Translatable.of("msg_specify_name"));

		if (!admin && sender instanceof Player player && !getResidentOrThrow(player).isMayor())
			throw new TownyException(Translatable.of("msg_town_merge_err_mayor_only"));

		final Town succumbingTown = getTownOrThrow(args[0]);

		vetTownsForMergeAndThrow(remainingTown, succumbingTown);

		// An array that keeps Merge costs separate, so the individual prices can be used later on in messaging.
		final double[] mergeCost = getMergeCosts(remainingTown, succumbingTown, admin);
		final double cost = Arrays.stream(mergeCost).sum();

		if (cost > 0) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_warning", succumbingTown.getName(), prettyMoney(cost)));
			if (mergeCost[2] > 0) // If the succumbing town was bankrupt.
				TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_debt_warning", succumbingTown.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_cost_breakdown", 
					prettyMoney(mergeCost[0]), // Base Cost
					prettyMoney(mergeCost[1]), // TownBlock Cost
					prettyMoney(mergeCost[2]), // Bankruptcy Cost
					prettyMoney(mergeCost[3]))); // Purchased BonusBlock Cost

			Confirmation.runOnAccept(() -> {
				sendTownMergeRequest(sender, remainingTown, succumbingTown, cost);
			}).runOnCancel(() -> {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_cancelled"));
				return;
			}).sendTo(sender);
		} else
			sendTownMergeRequest(sender, remainingTown, succumbingTown, cost);
	}

	private static void vetTownsForMergeAndThrow(Town remainingTown, Town succumbingTown) throws TownyException {
		// A lot of checks.
		if (succumbingTown.getName().equals(remainingTown.getName()))
			throw new TownyException(Translatable.of("msg_err_invalid_name", succumbingTown.getName()));

		if (TownySettings.getMaxDistanceForTownMerge() > 0 && homeBlockDistance(remainingTown, succumbingTown) > TownySettings.getMaxDistanceForTownMerge())
			throw new TownyException(Translatable.of("msg_town_merge_err_not_close", succumbingTown.getName(), TownySettings.getMaxDistanceForTownMerge()));

		int newResidentsAmount = remainingTown.getNumResidents() + succumbingTown.getNumResidents();

		if (!remainingTown.isAllowedThisAmountOfResidents(newResidentsAmount, remainingTown.isCapital()))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_residents", TownySettings.getMaxResidentsForTown(remainingTown)));

		if (!remainingTown.hasUnlimitedClaims())
			vetTownWouldHaveTooManyTownBlocksOrThrow(remainingTown, succumbingTown, newResidentsAmount);

		if ((remainingTown.getPurchasedBlocks() + succumbingTown.getPurchasedBlocks()) > TownySettings.getMaxPurchasedBlocks(remainingTown, newResidentsAmount))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_purchased_townblocks", TownySettings.getMaxPurchasedBlocks(remainingTown, newResidentsAmount)));

		if (TownySettings.isAllowingOutposts() && TownySettings.isOutpostsLimitedByLevels()) {
			// When determining the future-max outposts we have to determine how many towns the potential nation might have,
			// nations can add bonus outposts to towns and the nation_level can be determined using the amount of towns a nation has.
			int nationTownsAmount = !remainingTown.hasNation() ? 0 : remainingTown.getNationOrNull().getTowns().size();
			if (remainingTown.hasNation() && remainingTown.getNationOrNull().hasTown(succumbingTown))
				nationTownsAmount--;

			int maxOutposts = TownySettings.getMaxOutposts(remainingTown, newResidentsAmount, nationTownsAmount);
			int combinedOutposts = remainingTown.getOutpostSpawns().size() + succumbingTown.getOutpostSpawns().size();
			if (combinedOutposts > maxOutposts)
				throw new TownyException(Translatable.of("msg_town_merge_err_too_many_outposts", maxOutposts));
		}

		if (!BukkitTools.isOnline(succumbingTown.getMayor().getName()) || succumbingTown.getMayor().isNPC())
			throw new TownyException(Translatable.of("msg_town_merge_other_offline", succumbingTown.getName(), succumbingTown.getMayor().getName()));
	}

	private static void vetTownWouldHaveTooManyTownBlocksOrThrow(Town remainingTown, Town succumbingTown, int newResidentsAmount) throws TownyException {
		int newTownBonus = TownySettings.getNewTownBonusBlocks();
		int succumbingTownTBAmount = succumbingTown.getNumTownBlocks();
		if (newTownBonus > 0 && succumbingTown.getBonusBlocks() >= newTownBonus)
			succumbingTownTBAmount = succumbingTownTBAmount - newTownBonus;

		int succumbingBonusBlocks = succumbingTown.getBonusBlocks() + (2 * newTownBonus);
		int succumbingPurchasedBlocks = succumbingTown.getPurchasedBlocks();
		int maxAllowedTownBlocks = TownySettings.getMaxTownBlocks(remainingTown, newResidentsAmount) + succumbingBonusBlocks + succumbingPurchasedBlocks;

		if ((remainingTown.getNumTownBlocks() + succumbingTownTBAmount) > maxAllowedTownBlocks)
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_townblocks", maxAllowedTownBlocks));
	}

	private static double[] getMergeCosts(Town remainingTown, Town succumbingTown, boolean admin) throws TownyException {
		// An array that keeps Merge costs separate, so the individual prices can be used later on in messaging.
		double[] mergeCost = new double[] {TownySettings.getBaseCostForTownMerge(), // mergeCost[0] Base cost of merging.
											0,  // mergeCost[1] TownblockCost
											0,  // mergeCost[2] BankruptcyCost,
											0}; // mergeCost[3] Purchased BonusBlock costs.

		if (admin || !TownyEconomyHandler.isActive())
			return mergeCost;

		// There is a configurable price that is applied as a percent, that the remaining town will pay.
		mergeCost[1] =  remainingTown.getTownBlockCostN(succumbingTown.getNumTownBlocks()) * (TownySettings.getPercentageCostPerPlot() * 0.01);

		// Remaining town has to wipe out the succumbing town's debt if any is present.
		if (succumbingTown.isBankrupt())
			mergeCost[2] = Math.abs(succumbingTown.getAccount().getHoldingBalance());

		// When purchased bonus townblocks have a price increase we have to make sure the new town is able to pay the difference
		// otherwise towns will farm small towns that buy up bonus blocks at a cheap rate, then merge.
		if (succumbingTown.getPurchasedBlocks() > 0 && TownySettings.getPurchasedBonusBlocksIncreaseValue() != 1.0) {
			int purchasedBlocks = succumbingTown.getPurchasedBlocks();
			double priceAlreadyPaid = MoneyUtil.returnPurchasedBlocksCost(0, purchasedBlocks, succumbingTown);
			mergeCost[3] = remainingTown.getBonusBlockCostN(purchasedBlocks) - priceAlreadyPaid;
		}

		double cost = Arrays.stream(mergeCost).sum();

		if (!remainingTown.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_town_merge_err_not_enough_money", prettyMoney(remainingTown.getAccount().getHoldingBalance()), prettyMoney(cost)));

		return mergeCost;
	}

	private static void sendTownMergeRequest(CommandSender sender, Town remainingTown, Town succumbingTown, double cost) {
		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_request_sent", succumbingTown.getName()));
		TownyMessaging.sendMsg(succumbingTown.getMayor(), Translatable.of("msg_town_merge_request_received", remainingTown.getName(), sender.getName(), remainingTown.getName()));

		Confirmation.runOnAccept(() -> {
			try {
				vetTownsForMergeAndThrow(remainingTown, succumbingTown);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
				return;
			}

			TownPreMergeEvent townPreMergeEvent = new TownPreMergeEvent(remainingTown, succumbingTown);
			if (BukkitTools.isEventCancelled(townPreMergeEvent)) {
				TownyMessaging.sendErrorMsg(succumbingTown.getMayor().getPlayer(), townPreMergeEvent.getCancelMessage());
				TownyMessaging.sendErrorMsg(sender, townPreMergeEvent.getCancelMessage());
				return;
			}

			if (TownyEconomyHandler.isActive() && cost > 0 &&
				!remainingTown.getAccount().withdraw(cost, Translation.of("msg_town_merge_cost_withdraw"))) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_merge_err_not_enough_money", (int) remainingTown.getAccount().getHoldingBalance(), (int) cost));
				return;
			}

			UUID succumbingTownUUID = succumbingTown.getUUID();
			String succumbingTownName = succumbingTown.getName();

			// Start merge
			TownyUniverse.getInstance().getDataSource().mergeTown(remainingTown, succumbingTown);
			TownyMessaging.sendGlobalMessage(Translatable.of("town1_has_merged_with_town2", succumbingTown, remainingTown));

			TownMergeEvent townMergeEvent = new TownMergeEvent(remainingTown, succumbingTownName, succumbingTownUUID);
			BukkitTools.fireEvent(townMergeEvent);
		}).runOnCancel(() -> {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_request_denied"));
			TownyMessaging.sendMsg(succumbingTown.getMayor(), Translatable.of("msg_town_merge_cancelled"));
		}).sendTo(BukkitTools.getPlayerExact(succumbingTown.getMayor().getName()));
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, List<WorldCoord> worldCoords) {

		List<WorldCoord> visited = new ArrayList<>();
		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord, visited))
				return true;
		return false;
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord, List<WorldCoord> visited) {

		for (WorldCoord wc : worldCoord.getCardinallyAdjacentWorldCoords()) {
			if (visited.contains(wc))
				continue;
			if (wc.isWilderness()) {
				visited.add(wc);
				continue;
			}
			if (!wc.getTownBlockOrNull().isOwner(owner)) {
				visited.add(wc);
				continue;
			}
			return true;
		}
		return false;
	}

	private static void townTransaction(Player player, String[] args, boolean withdraw) {
		try {
			if (args.length == 0)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", withdraw ? "/town withdraw" : "/town deposit"));

			Resident resident = getResidentOrThrow(player);
			Town town = null;

			// Check if this is a case of someone supplying a town name, to deposit to another town.
			if (!withdraw && args.length == 2) {
				town = getTownOrThrow(args[1]);
				if (!town.hasResident(player))
					checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT_OTHERTOWN.getNode());
			}

			// Catch if town is still null, check if the resident has a town.
			if (town == null && !resident.hasTown())
				throw new TownyException(Translatable.of("msg_err_dont_belong_town"));

			// If the town is still null, the resident has to have a town.
			if (town == null)
				town = resident.getTownOrNull();

			// Figure out how much to deposit or withdraw.
			int amount;
			if ("all".equalsIgnoreCase(args[0].trim()))
				amount = (int) Math.floor(withdraw ? town.getAccount().getHoldingBalance() : resident.getAccount().getHoldingBalance());
			else
				amount = MathUtil.getIntOrThrow(args[0].trim());

			// Stop 0 amounts being supplied.
			if (amount == 0)
				throw new TownyException(Translatable.of("msg_err_amount_must_be_greater_than_zero"));

			// Attempt to do the actual bank transaction.
			if (withdraw)
				MoneyUtil.townWithdraw(player, resident, town, amount);
			else
				MoneyUtil.townDeposit(player, resident, town, null, amount);

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	private static void townOutpost(Player player, String[] args) throws TownyException {
		catchRuinedTown(player);
		if (args.length >= 1) {
			if (args[0].equalsIgnoreCase("list")) {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTPOST_LIST.getNode());

				Town town = getTownFromPlayerOrThrow(player);
				List<Location> outposts = town.getAllOutpostSpawns();
				int page = 1;
				int total = (int) Math.ceil(((double) outposts.size()) / ((double) 10));
				if (args.length == 2) {
					page = MathUtil.getPositiveIntOrThrow(args[1]);
					if (page == 0)
						throw new TownyException(Translatable.of("msg_error_must_be_int"));
				}
				if (page > total)
					throw new TownyException(Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));

				TownyMessaging.sendOutpostList(player, town, page, total);
			} else {
				boolean ignoreWarning = args.length == 1 && args[0].equals("-ignore");
				townSpawn(player, args, true, ignoreWarning);
			}
		} else {
			townSpawn(player, args, true, false);
		}
	}
	
	private void townStatusScreen(CommandSender sender, Town town) {
		/*
		 * This is run async because it will ping the economy plugin for the town bank value.
		 */
		TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(town, sender)));
	}

	private void townResList(CommandSender sender, String[] args) throws TownyException {

		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;

		if (player != null)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode());

		Town town = null;
		if (args.length == 1 && player != null) {
			catchRuinedTown(player);
			town = getTownFromPlayerOrThrow(player);
		} else if (args.length == 2){
			town = getTownOrThrow(args[1]);
		}

		if (town == null)
			throw new TownyException(Translatable.of("msg_specify_name"));
		
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(town.getName() + " " + Translatable.of("res_list").forLocale(sender)));
		TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(Translatable.of("res_list").forLocale(sender), new ArrayList<>(town.getResidents())));
	}
	
	private void townPlotGroupList(CommandSender sender, String[] args) throws TownyException {
		// args: plotgrouplist townname pagenumber 
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		if (player != null) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTGROUPLIST.getNode());
			catchRuinedTown(player);
		}

		Town town = null;
		if (args.length > 1) // not just /town plotgrouplist
			town = getTownOrThrow(args[1]);

		if (town == null && player != null) // Probably a number and not a town name.
			town = getTownFromPlayerOrThrow(player);
		
		if (!town.hasPlotGroups())
			throw new TownyException(Translatable.of("msg_err_this_town_has_no_plot_groups"));

		int page = 1;		
		int total = (int) Math.ceil(((double) town.getPlotGroups().size()) / ((double) 10));
		if (args.length > 1) {
			page = MathUtil.getPositiveIntOrThrow(args[args.length - 1]);
			if (page == 0)
				throw new TownyException(Translatable.of("msg_error_must_be_int"));
			// Page will continue to be one.
		}
		if (page > total)
			throw new TownyException(Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
		
		TownyMessaging.sendPlotGroupList(sender, town, page, total);
	}

	private void townOutlawList(CommandSender sender, String[] args) throws TownyException {
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		Town town = null;
		if (args.length == 1 && player != null) {
			catchRuinedTown(player);
			town = getTownFromPlayerOrThrow(player);
		} else if (args.length == 2){
			town = getTownOrThrow(args[1]);
		}
		
		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("outlaws").forLocale(sender), new ArrayList<>(town.getOutlaws())));
	}
	
	public static void parseTownTrustCommand(CommandSender sender, String[] args, @Nullable Town town) throws TownyException {
		
		if (args.length < 1) {
			HelpMenu.TOWN_TRUST_HELP.send(sender);
			return;
		}

		if (town == null && sender instanceof Player player) {
			town = getTownFromPlayerOrThrow(player);
			catchRuinedTown(player);
		}

		if (args[0].equalsIgnoreCase("list")) {
			parseTownTrustListCommand(sender, town);
			return;
		}

		if (args.length < 2) {
			HelpMenu.TOWN_TRUST_HELP.send(sender);
			return;
		}

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TRUST.getNode());
		Resident resident = getResidentOrThrow(args[1]);
		catchNPCResident(resident);

		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "add" -> parseTownTrustAddCommand(sender, town, resident);
		case "remove" -> parseTownTrustRemoveCommand(sender, town, resident);
		default -> HelpMenu.TOWN_TRUST_HELP.send(sender);
		}
	}

	private static void parseTownTrustListCommand(CommandSender sender, Town town) {
		List<String> output = town.getTrustedResidents().isEmpty()
				? Collections.singletonList(Translatable.of("status_no_town").forLocale(sender)) // String which is "None".
				: town.getTrustedResidents().stream().map(Resident::getName).collect(Collectors.toList());
		TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedStrings(Translatable.of("status_trustedlist").forLocale(sender), output));
	}

	private static void parseTownTrustAddCommand(CommandSender sender, Town town, Resident resident) throws TownyException {
		if (town.hasTrustedResident(resident))
			throw new TownyException(Translatable.of("msg_already_trusted", resident.getName(), Translatable.of("town_sing")));

		if (town.hasOutlaw(resident))
			throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_outlaw"));

		if (resident.hasNation() && town.hasNation() && town.getNationOrNull().hasEnemy(resident.getNationOrNull()))
			throw new TownyException(Translatable.of("msg_err_you_cannot_add_trust_on_enemy"));

		BukkitTools.ifCancelledThenThrow(new TownTrustAddEvent(sender, resident, town));

		town.addTrustedResident(resident);
		town.save();
		plugin.deleteCache(resident);

		TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("town_sing")));
		if (resident.isOnline())
			TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_added_2", sender instanceof Player player ? player.getName() : "Console", Translatable.of("town_sing"), town.getName()));
	}

	private static void parseTownTrustRemoveCommand(CommandSender sender, Town town, Resident resident) throws TownyException {
		if (!town.hasTrustedResident(resident))
			throw new TownyException(Translatable.of("msg_not_trusted", resident.getName(), Translatable.of("town_sing")));

		BukkitTools.ifCancelledThenThrow(new TownTrustRemoveEvent(sender, resident, town));

		town.removeTrustedResident(resident);
		town.save();
		plugin.deleteCache(resident);

		TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_removed", resident.getName(), Translatable.of("town_sing")));
		if (resident.isOnline())
			TownyMessaging.sendMsg(resident, Translatable.of("msg_trusted_removed_2", sender instanceof Player player ? player.getName() : "Console", Translatable.of("town_sing"), town.getName()));
	}

	public static void parseTownTrustTownCommand(CommandSender sender, String[] args, @Nullable Town town) throws TownyException {

		if (args.length < 1) {
			HelpMenu.TOWN_TRUSTTOWN_HELP.send(sender);
			return;
		}

		if (town == null && sender instanceof Player player) {
			town = getTownFromPlayerOrThrow(player);
			catchRuinedTown(player);
		}

		if ("list".equalsIgnoreCase(args[0])) {
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_trustedlist").forLocale(sender), new ArrayList<>(town.getTrustedTowns())));
			return;
		}

		if (args.length < 2) {
			HelpMenu.TOWN_TRUSTTOWN_HELP.send(sender);
			return;
		}

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TRUSTTOWN.getNode());

		Town trustTown = getTownOrThrow(args[1]);

		switch (args[0].toLowerCase(Locale.ROOT)) {
		case "add" -> parseTownTrustTownAddCommand(sender, town, trustTown);
		case "remove" -> parseTownTrustTownRemoveCommand(sender, town, trustTown);
		default -> HelpMenu.TOWN_TRUSTTOWN_HELP.send(sender);
		}
	}

	private static void parseTownTrustTownAddCommand(CommandSender sender, Town town, Town trustTown) throws TownyException {
		if (town.hasTrustedTown(trustTown))
			throw new TownyException(Translatable.of("msg_already_trusted", trustTown.getName(), Translatable.of("town_sing")));

		if (trustTown.getResidents().stream().filter(res -> town.hasOutlaw(res)).findAny().isPresent())
			throw new TownyException(Translatable.of("msg_err_you_cannot_add_towntrust_on_outlaw"));

		if (trustTown.hasNation() && town.hasNation() && town.getNationOrNull().hasEnemy(trustTown.getNationOrNull()))
			throw new TownyException(Translatable.of("msg_err_you_cannot_add_towntrust_on_enemy"));

		if (town == trustTown)
			throw new TownyException(Translatable.of("msg_cannot_trust_your_own_town"));

		BukkitTools.ifCancelledThenThrow(new TownTrustTownAddEvent(sender, trustTown, town));
		Confirmation.runOnAccept(()-> {
				town.addTrustedTown(trustTown);
				town.save();
				TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_added", trustTown.getName(), Translatable.of("town_sing")));
				trustTown.getResidents().forEach(res -> plugin.deleteCache(res));
			})
			.setTitle(Translatable.of("confirmation_msg_trusttown_consequences"))
			.sendTo(sender);
	}

	private static void parseTownTrustTownRemoveCommand(CommandSender sender, Town town, Town trustTown) throws TownyException {
		if (!town.hasTrustedTown(trustTown))
			throw new TownyException(Translatable.of("msg_not_trusted", trustTown.getName(), Translatable.of("town_sing")));

		BukkitTools.ifCancelledThenThrow(new TownTrustTownRemoveEvent(sender, trustTown, town));
		town.removeTrustedTown(trustTown);
		town.save();
		trustTown.getResidents().forEach(res -> plugin.deleteCache(res));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_removed", trustTown.getName(), Translatable.of("town_sing")));
	}

	public static List<String> getTrustedResidentsFromResident(Player player){
		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (res != null && res.hasTown()) {
			return res.getTownOrNull().getTrustedResidents().stream().map(TownyObject::getName)
				.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	public static List<String> getTrustedTownsFromResident(Player player){
		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (res != null && res.hasTown()) {
			return res.getTownOrNull().getTrustedTowns().stream().map(TownyObject::getName)
				.collect(Collectors.toList());
		}

		return Collections.emptyList();
	}

	private static int homeBlockDistance(Town town1, Town town2) {
		if (!town1.hasHomeBlock() || !town2.hasHomeBlock())
			return Integer.MAX_VALUE;

		return (int) MathUtil.distance(town1.getHomeBlockOrNull().getCoord(), town2.getHomeBlockOrNull().getCoord());
	}

	private void parseTownForSaleCommand(Player player, String[] args) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_FORSALE.getNode());

		if (args.length == 0)
			throw new TownyException(Translatable.of("msg_error_must_be_num"));

		double forSalePrice = Math.min(MathUtil.getPositiveDoubleOrThrow(args[0]), TownySettings.maxBuyTownPrice());
		Town town = getTownFromPlayerOrThrow(player);

		Confirmation
			.runOnAccept(() -> {
				setTownForSale(town, forSalePrice, false);
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_forsale", town.getName(), prettyMoney(forSalePrice)));
			})
			.setTitle(Translatable.of("msg_town_sell_confirmation", prettyMoney(forSalePrice)))
			.sendTo(player);
	}

	private void parseTownNotForSaleCommand(Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_NOTFORSALE.getNode());
		Town town = getTownFromPlayerOrThrow(player);

		if (!town.isForSale())
			throw new TownyException(Translatable.of("msg_town_buytown_not_forsale"));
		
		setTownNotForSale(town, false);
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_notforsale", town.getName()));
	}

	public static void setTownForSale(Town town, double price, boolean admin, long time) {
		if (town != null) {
			town.setForSale(true);
			town.setForSalePrice(price);
			town.setForSaleTime(time);
			town.save();
		}
	}

	public static void setTownForSale(Town town, double price, boolean admin) {
		setTownForSale(town, price, admin, System.currentTimeMillis());
	}

	public static void setTownNotForSale(Town town, boolean admin) {
		if (town != null) {
			town.setForSale(false);
			town.save();
		}
	}

	private void parseTownBuyTownCommand(CommandSender sender, String[] args) throws TownyException {
		catchConsole(sender);
		Player player = (Player) sender;
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUYTOWN.getNode());

		if (args.length == 0) {
			throw new TownyException(Translatable.of("msg_specify_name"));
		}

		Town town = getTownOrThrow(args[0]);

		if (!town.isForSale()) {
			throw new TownyException(Translatable.of("msg_town_buytown_not_forsale"));
		}

		if (town.isRuined()) {
			throw new TownyException("msg_town_buytown_ruined");
		}
		
		Resident resident = getResidentOrThrow(player);
		if (resident.isMayor()) {
			throw new TownyException(Translatable.of("msg_mayor_abandon"));
		}

		Confirmation
			.runOnAccept(() -> {
				if (!town.isForSale()) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_buytown_not_forsale"));
					return;
				}

				Resident currentMayor = town.getMayor();
				if (resident.equals(currentMayor)) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_buytown_already_mayor", resident.getTownOrNull().getName()));
					return;
				}

				if (resident.isMayor()) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_mayor_abandon"));
					return;
				}
				
				if (!resident.getAccount().withdraw(town.getForSalePrice(), "Town purchase cost.")) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_you_need_x_to_pay", town.getForSalePrice()));
					return;
				}
				
				try {
					if (resident.hasTown())
						resident.removeTown();
					
					townAddResident(town, resident);
					town.setMayor(resident);
				} catch (AlreadyRegisteredException e) {
					town.setMayor(resident);
				}

				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", resident.getName()));

				if (currentMayor != null)
					currentMayor.getAccount().deposit(town.getForSalePrice(), "Payment for town sale");

				town.setForSale(false);
				town.save();
			})
		.setTitle(Translatable.of("msg_town_buytown_confirmation", town.getName(), prettyMoney(town.getForSalePrice())))
		.setCancellableEvent(new TownPreAddResidentEvent(town, resident))
		.sendTo(player);
	}
}
