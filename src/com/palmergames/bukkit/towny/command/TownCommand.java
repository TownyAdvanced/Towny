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
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
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
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.OutpostUtil;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
import java.util.stream.Collectors;

/**
 * Send a list of all town help commands to player Command: /town
 */

public class TownCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;

	private static final List<String> townTabCompletes = Arrays.asList(
		"here",
		"leave",
		"list",
		"online",
		"new",
		"plots",
		"add",
		"kick",
		"spawn",
		"claim",
		"unclaim",
		"withdraw",
		"delete",
		"outlawlist",
		"deposit",
		"outlaw",
		"ban",
		"outpost",
		"purge",
		"plotgrouplist",
		"ranklist",
		"rank",
		"reclaim",
		"reslist",
		"say",
		"set",
		"toggle",
		"join",
		"invite",
		"buy",
		"mayor",
		"bankhistory",
		"merge",
		"jail",
		"unjail",
		"trust",
		"trusttown",
		"allylist",
		"enemylist",
		"baltop"
		);
	private static final List<String> townSetTabCompletes = Arrays.asList(
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
		"list"
	);
	
	static final List<String> townAddRemoveTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> townClaimTabCompletes = Arrays.asList(
		"outpost",
		"auto",
		"circle",
		"rect"
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
		
		if (sender instanceof Player player) {
			
			switch (args[0].toLowerCase()) {
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
					if (args.length == 3) {
						List<String> ignore = Collections.singletonList("-ignore");
						return ignore;
					}
				case "rank":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
						case 3:
							return getTownResidentNamesOfPlayerStartingWith(player, args[2]);
						case 4:
							switch (args[1].toLowerCase()) {
								case "add":
									return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[3]);
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
				case "unjail":
					if (args.length == 2) {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null && res.hasTown()) {
							Town town = res.getTownOrNull();
							List<String> jailedResidents = new ArrayList<>();
							TownyUniverse.getInstance().getJailedResidentMap().stream()
									.filter(jailee -> jailee.hasJailTown(town.getName()))
									.forEach(jailee -> jailedResidents.add(jailee.getName()));
							return NameUtil.filterByStart(jailedResidents, args[1]);
						}
					}
				case "outpost":
					if (args.length == 2)
						return Collections.singletonList("list");
					break;
				case "outlaw":
				case "ban":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townAddRemoveTabCompletes, args[1]);
						case 3:
							switch (args[1].toLowerCase()) {
								case "add":
									return getTownyStartingWith(args[2], "r");
								case "remove": {
									Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
									if (resident != null) {
										try {
											return NameUtil.filterByStart(NameUtil.getNames(resident.getTown().getOutlaws()), args[2]);
										} catch (TownyException ignore) {
										}
									}
								}
								default:
									return Collections.emptyList();
							}
						default:
							return Collections.emptyList();
					}
				case "claim":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(townClaimTabCompletes, args[1]);
						case 3:
							if (!args[1].equalsIgnoreCase("outpost")) {
								return NameUtil.filterByStart(Collections.singletonList("auto"), args[2]);
							}
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
					try {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null)
							return townSetTabComplete(sender, res.getTown(), args);
					} catch (TownyException ignore) {}
					return Collections.emptyList();
				case "invite":
					switch (args.length) {
						case 2:
							List<String> returnValue = NameUtil.filterByStart(townInviteTabCompletes, args[1]);
							if (returnValue.size() > 0) {
								return returnValue;
							} else {
								if (args[1].startsWith("-")) {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									
									if (res == null)
										return null;
									
									try {
										return NameUtil.filterByStart(res.getTown().getSentInvites()
											// Get all sent invites
											.stream()
											.map(Invite::getReceiver)
											.map(InviteReceiver::getName)
											.collect(Collectors.toList()), args[1].substring(1))
												// Add the hyphen back to the front
												.stream()
												.map(e -> "-"+e)
												.collect(Collectors.toList());
									} catch (TownyException ignore) {}
								} else {
									return getVisibleResidentsForPlayerWithoutTownsStartingWith(args[1], sender);
								}
							}
						case 3:
							switch (args[1].toLowerCase()) {
								case "accept":
								case "deny": {
									Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
									if (res == null)
										return null;
									
									try {
										return NameUtil.filterByStart(res.getTown().getReceivedInvites()
											// Get the names of all received invites
											.stream()
											.map(Invite::getSender)
											.map(InviteSender::getName)
											.collect(Collectors.toList()),args[2]);
									} catch (TownyException ignore) {
									}
								}
								default:
									return Collections.emptyList();
							}
						default:
							return Collections.emptyList();
					}
				case "buy":
					if (args.length == 2)
						return NameUtil.filterByStart(Collections.singletonList("bonus"), args[1]);
					break;
				case "toggle":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_TOGGLE, townToggleTabCompletes), args[1]);
						case 3:
							return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
						case 4:
							return getTownResidentNamesOfPlayerStartingWith(player, args[3]);
						default:
							return Collections.emptyList();
					}
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(townListTabCompletes, args[2]);
						default:
							return Collections.emptyList();
					}
				case "trust":
					switch (args.length) {
						case 2:
							return NameUtil.filterByStart(Arrays.asList("add", "remove", "list"), args[1]);
						case 3:
							if (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove"))
								return getTownyStartingWith(args[2], "r");
							else 
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
							if (args[1].equalsIgnoreCase("remove")) {
								return NameUtil.filterByStart(getTrustedTownsFromResident(player), args[2]);
							}
							return Collections.emptyList();
						default:
							return Collections.emptyList();
					}
				default:
					if (args.length == 1)
						return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN, townTabCompletes), args[0], "t");
					else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.TOWN, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(townConsoleTabCompletes, args[0], "t");
		}
		
		return Collections.emptyList();
	}
	
	static List<String> townSetTabComplete(CommandSender sender, Town town, String[] args) {
		if (args.length == 2) {
			return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWN_SET, townSetTabCompletes), args[1]);
		} else if (args.length > 2) {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_SET, args[1]))
				return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
			
			switch (args[1].toLowerCase()) {
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
						return NameUtil.filterByStart(TownySettings.getTownColorsMap().keySet().stream().collect(Collectors.toList()), args[2]);
					break;
				default:
					return Collections.emptyList();
			}
		}
		
		return Collections.emptyList();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
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
		
		Town town;
		switch (split[0].toLowerCase(Locale.ROOT)) {
		
		case "mayor":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_MAYOR.getNode());
			HelpMenu.TOWN_MAYOR_HELP.send(player);
			break;
		case "here":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_HERE.getNode());
			town = TownyAPI.getInstance().getTown(player.getLocation());
			if (town == null)
				throw new TownyException(Translatable.of("msg_not_claimed", Coord.parseCoord(player.getLocation())));
			townStatusScreen(player, town);
			break;
		case "list":
			// Permission test is internal.
			listTowns(player, split);
			break;
		case "new", "create":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_NEW.getNode());
			if (split.length == 1) {
				throw new TownyException(Translatable.of("msg_specify_name"));
			} else {
				String townName = String.join("_", StringMgmt.remFirstArg(split));
				boolean noCharge = TownySettings.getNewTownPrice() == 0.0 || !TownyEconomyHandler.isActive();
				newTown(player, townName, getResidentOrThrow(player), noCharge);
			}
			break;
		case "reclaim":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RECLAIM.getNode());
			
			if(!TownySettings.getTownRuinsReclaimEnabled())
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			
			TownRuinUtil.processRuinedTownReclaimRequest(player);
			break;
		case "join":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_JOIN.getNode());
			parseTownJoin(player, StringMgmt.remFirstArg(split));
			break;
		case "leave":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_LEAVE.getNode());
			townLeave(player);
			break;
		case "withdraw":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_WITHDRAW.getNode());
			townTransaction(player, StringMgmt.remFirstArg(split), true);
			break;
		case "deposit":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_DEPOSIT.getNode());
			townTransaction(player, StringMgmt.remFirstArg(split), false);
			break;
		case "plots":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTS.getNode());
			townPlots(player, split);
			break;
		case "reslist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RESLIST.getNode());
			townResList(player, split);
			break;
		case "plotgrouplist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PLOTGROUPLIST.getNode());
			townPlotGroupList(player, split);
			break;
		case "outlawlist":
			townOutlawList(player, split);
			break;
		case "allylist":
			townAllyList(player, split);
			break;
		case "enemylist":
			townEnemyList(player, split);
			break;
		case "spawn":
			// Permission test is internal.
			boolean ignoreWarning = split.length > 2 && split[2].equals("-ignore");
			townSpawn(player, StringMgmt.remFirstArg(split), false, ignoreWarning);
			break;
		case "rank":
			// Permission test is internal.
			catchRuinedTown(player);
			townRank(player, StringMgmt.remFirstArg(split));
			break;
		case "set":
			// Permission test is internal.
			catchRuinedTown(player);
			townSet(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "buy":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_BUY.getNode());
			catchRuinedTown(player);
			townBuy(player, StringMgmt.remFirstArg(split), null, false);
			break;
		case "toggle":
			// Permission test is internal.
			catchRuinedTown(player);
			townToggle(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "outpost":
			// Permission test is internal.
			catchRuinedTown(player);
			townOutpost(player, StringMgmt.remFirstArg(split));
			break;
		case "delete":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_DELETE.getNode());
			catchRuinedTown(player);
			townDelete(player, StringMgmt.remFirstArg(split));
			break;
		case "ranklist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANKLIST.getNode());
			catchRuinedTown(player);
			town = split.length > 1 ? getTownOrThrow(split[1]) : getTownFromPlayerOrThrow(player);
			TownyMessaging.sendMessage(player, TownyFormatter.getRanksForTown(town, Translator.locale(player)));
			break;
		case "add":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());
			catchRuinedTown(player);
			townAdd(player, null, StringMgmt.remFirstArg(split));
			break;
		case "invite", "invites":
			// Permission test is internal.
			catchRuinedTown(player);
			parseInviteCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "kick":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_KICK.getNode());
			catchRuinedTown(player);
			townKick(player, StringMgmt.remFirstArg(split));
			break;
		case "claim":
			// Permission test is internal.
			catchRuinedTown(player);
			parseTownClaimCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "unclaim":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode());
			catchRuinedTown(player);
			parseTownUnclaimCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "online":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_ONLINE.getNode());
			catchRuinedTown(player);
			parseTownOnlineCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "say":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_SAY.getNode());
			catchRuinedTown(player);
			TownyMessaging.sendPrefixedTownMessage(getTownFromPlayerOrThrow(player), TownyComponents.stripClickTags(StringMgmt.join(StringMgmt.remFirstArg(split))));
			break;
		case "outlaw", "ban":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_OUTLAW.getNode());
			catchRuinedTown(player);
			parseTownOutlawCommand(player, StringMgmt.remFirstArg(split), false, getResidentOrThrow(player).getTown());
			break;
		case "bankhistory":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_BANKHISTORY.getNode());
			catchRuinedTown(player);
			int pages = 10;
			if (split.length > 1)
				pages = MathUtil.getIntOrThrow(split[1]);
			getTownFromPlayerOrThrow(player).generateBankHistoryBook(player, pages);
			break;
		case "merge":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_MERGE.getNode());
			catchRuinedTown(player);
			parseTownMergeCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "jail":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_JAIL.getNode());
			catchRuinedTown(player);
			parseJailCommand(player, null, StringMgmt.remFirstArg(split), false);
			break;
		case "unjail":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNJAIL.getNode());
			catchRuinedTown(player);
			parseUnJailCommand(player, null, StringMgmt.remFirstArg(split), false);
			break;
		case "purge":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_PURGE.getNode());
			catchRuinedTown(player);
			parseTownPurgeCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "trust":
			catchRuinedTown(player);
			parseTownTrustCommand(player, StringMgmt.remFirstArg(split), null);
			break;
		case "trusttown":
			catchRuinedTown(player);
			parseTownTrustTownCommand(player, StringMgmt.remFirstArg(split), null);
			break;
		case "baltop":
			catchRuinedTown(player);
			town = split.length > 1 ? getTownOrThrow(split[1]) : getTownFromPlayerOrThrow(player);
			parseTownBaltop(player, town);
			break;
		default:
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

	private void catchRuinedTown(Player player) throws TownyException {
		if (TownRuinUtil.isPlayersTownRuined(player))
			throw new TownyException(Translatable.of("msg_err_cannot_use_command_because_town_ruined"));
	}

	private void townEnemyList(Player player, String[] split) throws TownyException {
		Town town = split.length == 1 ? getTownFromPlayerOrThrow(player) : getTownOrThrow(split[1]);

		if (town.getEnemies().isEmpty())
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_town_has_no_enemies")); 
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(town.getName() + " " + Translatable.of("status_nation_enemies").forLocale(player)));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_enemies").forLocale(player), new ArrayList<>(town.getEnemies())));
		}
	}

	private void townAllyList(Player player, String[] split) throws TownyException {
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

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		Resident resident = getResidentOrThrow(player);

		String received = Translatable.of("town_received_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getTown().getReceivedInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown())));
		String sent = Translatable.of("town_sent_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getTown().getSentInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown())));

		if (newSplit.length == 0) { // (/town invite)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_SEE_HOME.getNode());

			HelpMenu.TOWN_INVITE.send(player);
			TownyMessaging.sendMessage(player, sent);
			TownyMessaging.sendMessage(player, received);
			return;
		}
		if (newSplit.length >= 1) { // /town invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				HelpMenu.TOWN_INVITE.send(player);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_SENT.getNode());

				List<Invite> sentinvites = resident.getTown().getSentInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				TownyMessaging.sendMessage(player, sent);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("received")) { // /town invite received
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_LIST_RECEIVED.getNode());

				List<Invite> receivedinvites = resident.getTown().getReceivedInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException ignored) {
					}
				}
				InviteCommand.sendInviteList(player, receivedinvites, page, false);
				TownyMessaging.sendMessage(player, received);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("accept")) {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ACCEPT.getNode());

				// /town (gone)
				// invite (gone)
				// args[0] = accept = length = 1
				// args[1] = [Nation] = length = 2
				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					nation = TownyUniverse.getInstance().getNation(newSplit[1]);
					
					if (nation == null) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}

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
						e.printStackTrace();
					}
				}
			}
			if (newSplit[0].equalsIgnoreCase("deny")) { // /town invite deny
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_DENY.getNode());

				Town town = resident.getTown();
				Nation nation;
				List<Invite> invites = town.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_no_invites"));
					return;
				}
				if (newSplit.length >= 2) { // /invite deny args[1]
					nation = TownyUniverse.getInstance().getNation(newSplit[1]);
					
					if (nation == null) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}
				
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
						return;
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}
			} else {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_INVITE_ADD.getNode());

				townAdd(player, null, newSplit);
				// It's none of those 4 subcommands, so it's a playername, I just expect it to be ok.
				// If it is invalid it is handled in townAdd() so, I'm good
			}
		}
	}

	public static void parseTownOutlawCommand(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			// Help output.
			if (!admin) {
				TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/town outlaw"));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/town outlaw", "add/remove [name]", ""));
			} else {
				TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/ta town [town] outlaw"));
				TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/ta town [town] outlaw", "add/remove [name]", ""));
			}

		} else {

			Resident resident;
			Resident target = null;
			Town targetTown = null;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 2)
				throw new TownyException(Translatable.of("msg_usage", "/town outlaw add/remove [name]"));

			if (!admin)
				resident = getResidentOrThrow(sender.getName());
			else
				resident = town.getMayor();	// if this is an Admin-initiated command, dupe the action as if it were done by the mayor.
			
			target = townyUniverse.getResident(split[1]);
			
			if (target == null) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_name", split[1]));
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {
				try {
					// Get the target resident's town if they have a town.
					if (target.hasTown())
						targetTown = TownyAPI.getInstance().getResidentTownOrNull(target);
					
					// Don't allow a resident to outlaw their own mayor.
					if (resident.getTown().getMayor().equals(target))
						return;

					// Call cancellable event.
					BukkitTools.ifCancelledThenThrow(new TownOutlawAddEvent(sender, target, town));

					// Kick outlaws from town if they are residents.
					if (targetTown != null && targetTown.getUUID().equals(town.getUUID())) {
						target.removeTown();
						Object outlawer = (admin ? Translatable.of("admin_sing") : sender.getName());
						TownyMessaging.sendMsg(target, Translatable.of("msg_kicked_by", outlawer));
						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_kicked", outlawer, target.getName()));
					}
					
					// Add the outlaw and save the town.
					town.addOutlaw(target);
					town.save();
					
					// Send feedback messages.
					if (target.getPlayer() != null && target.getPlayer().isOnline()) {
						TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_declared_outlaw", town.getName()));
						Location loc = target.getPlayer().getLocation();
						
						// If the newly-outlawed player is within the town's borders and is meant to be teleported away, 
						// send them using the outlaw teleport warmup time, potentially giving them the chance to escape
						// the borders.
						if (TownySettings.areNewOutlawsTeleportedAway() 
							&& TownyAPI.getInstance().getTownBlock(loc) != null
							&& TownyAPI.getInstance().getTown(loc) == town) {
							
							OutlawTeleportEvent event = new OutlawTeleportEvent(target, town, loc);
							if (BukkitTools.isEventCancelled(event))
								return;
							
							if (TownySettings.getOutlawTeleportWarmup() > 0)
								TownyMessaging.sendMsg(target, Translatable.of("msg_outlaw_kick_cooldown", town, TimeMgmt.formatCountdownTime(TownySettings.getOutlawTeleportWarmup())));
							
							final Resident outlawRes = target;
							new BukkitRunnable() {
								@Override
								public void run() {
									if (TownyAPI.getInstance().getTown(loc) != null && TownyAPI.getInstance().getTown(loc) == town) 
										SpawnUtil.outlawTeleport(town, outlawRes);
								}
							}.runTaskLater(plugin, TownySettings.getOutlawTeleportWarmup() * 20);
						}
					}
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
					if (admin)
						TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_declared_an_outlaw", target.getName(), town.getName()));
				} catch (AlreadyRegisteredException e) {
					// Must already be an outlaw
					TownyMessaging.sendMsg(sender, Translatable.of("msg_err_resident_already_an_outlaw"));
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				if (town.hasOutlaw(target)) {

					// Call cancellable event.
					BukkitTools.ifCancelledThenThrow(new TownOutlawRemoveEvent(sender, target, town));

					town.removeOutlaw(target);
					town.save();

					// Send feedback messages.
					if (target.getPlayer() != null && target.getPlayer().isOnline())
						TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_undeclared_outlaw", town.getName()));
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
					if (admin)
						TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_undeclared_an_outlaw", target.getName(), town.getName()));
				} else {
					// Must already not be an outlaw
					TownyMessaging.sendMsg(sender, Translatable.of("msg_err_player_not_an_outlaw"));
					return;
				}

			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
				return;
			}

			/*
			 * If we got here we have made a change Save the altered resident
			 * data.
			 */
			town.save();

		}

	}

	private void townPlots(CommandSender sender, String[] args) throws TownyException {
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		Town town = null;
		if (args.length == 1 && player != null) {
			catchRuinedTown(player);
			town = getTownFromPlayerOrThrow(player);
		} else {
			town = getTownOrThrow(args[1]);
		}
		
		Translator translator = Translator.locale(sender);
		
		if (town == null)
			throw new TownyException(translator.of("msg_specify_name"));

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
				plotTypeLine += translator.of("msg_town_plots_type_line_revenue", TownyEconomyHandler.getFormattedBalance(residentOwned * type.getTax(town)));
			out.add(plotTypeLine);
		}
		out.add(Translatable.of("msg_town_plots_revenue_disclaimer").forLocale(player));
		TownyMessaging.sendMessage(sender, out);

	}

	private void parseTownOnlineCommand(Player player, String[] split) throws TownyException {
		Translator translator = Translator.locale(player);
		if (split.length > 0) {
			Town town = getTownOrThrow(split[0]);

			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, town);
			if (onlineResidents.size() > 0) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_town_online"), town, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White + "0 " + translator.of("res_list") + " " + (translator.of("msg_town_online") + ": " + town));
			}
		} else {
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_town_online"), getTownFromPlayerOrThrow(player), player));
		}
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
					
					if (!townListTabCompletes.contains(split[i].toLowerCase()))
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
				Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
					TownyMessaging.sendTownList(sender, ComparatorCaches.getTownListCache(finalType), finalType, pageNumber, totalNumber);
				});
			} else { 
				// Make a randomly sorted output.
				List<TextComponent> output = new ArrayList<>();
				List<Town> towns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
				Collections.shuffle(towns);
				
				for (Town town : towns) {
					TextComponent townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA)
							.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
					townName = townName.append(Component.text(" - ", NamedTextColor.DARK_GRAY).append(Component.text("(" + town.getResidents().size() + ")", NamedTextColor.AQUA)));

					if (town.isOpen())
						townName = townName.append(Component.space()).append(Translatable.of("status_title_open").locale(sender).component());
					
					Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
					if (TownyEconomyHandler.isActive())
						spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

					townName = townName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", town).append("\n").append(spawnCost).locale(sender).component()));
					output.add(townName);
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
			Resident resident = getResidentOrThrow(sender.getName());
			town = resident.getTown();
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TOGGLE.getNode(split[0].toLowerCase()));
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
			throw new TownyException(Translatable.of("msg_town_cant_peaceful"));

		// Fire cancellable event directly before setting the toggle.
		TownToggleNeutralEvent preEvent = new TownToggleNeutralEvent(sender, town, admin, choice.orElse(!town.isNeutral()));
		BukkitTools.ifCancelledThenThrow(preEvent);

		// If they setting neutral status on send a message confirming they paid something, if they did.
		if (peacefulState && TownyEconomyHandler.isActive() && cost > 0) {
			town.getAccount().withdraw(cost, "Peaceful Town Cost");
			TownyMessaging.sendMsg(sender, Translatable.of("msg_you_paid", TownyEconomyHandler.getFormattedBalance(cost)));
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
		
		if (!admin)
			town = getTownFromPlayerOrThrow((Player) sender);
		
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
		
		if (!admin)
			town = getTownFromPlayerOrThrow((Player) sender);
			
		if (!town.hasJails())
			throw new TownyException(Translatable.of("msg_town_has_no_jails"));

		if (split.length == 0) {
			if (TownySettings.isAllowingBail() && TownyEconomyHandler.isActive())
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
		double bail = TownySettings.isAllowingBail() && TownyEconomyHandler.isActive() ? TownySettings.getBailAmount() : 0.0;
		int jailNum = 1;
		int cell = 1;
		Jail jail = town.getPrimaryJail();
		double initialJailFee = TownyEconomyHandler.isActive() && TownySettings.initialJailFee() > 0 ? TownySettings.initialJailFee() : 0;

		try {
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

			// Make sure the town can afford to jail. 
			if (initialJailFee > 0 && !town.getAccount().canPayFromHoldings(initialJailFee))
				throw new TownyException(Translatable.of("msg_not_enough_money_in_bank_to_jail_x_fee_is_x", jailedResident, initialJailFee));

			// Make sure the to-be-jailed resident is online.
			if (!jailedResident.isOnline())
				throw new TownyException(Translatable.of("msg_player_is_not_online", jailedResident.getName()));

			// Make sure this isn't someone jailing themselves to get a free teleport.
			Player jailedPlayer = jailedResident.getPlayer();
			if (!admin && jailedPlayer.getUniqueId().equals(((Player) sender).getUniqueId()))
				throw new TownyException(Translatable.of("msg_no_self_jailing"));

			// Test if a player is located where they are outlawed/enemied and unable to teleport.
			Town jaileeLocTown = TownyAPI.getInstance().getTown(jailedPlayer.getLocation());
			if (jaileeLocTown != null) {
				if (!TownySettings.canOutlawsTeleportOutOfTowns() && jaileeLocTown.hasOutlaw(jailedResident))
					throw new TownyException(Translatable.of("msg_err_resident_cannot_be_jailed_because_they_are_outlawed_there"));

				if (jaileeLocTown.hasNation() && 
					jailedResident.hasNation() &&
					TownySettings.getDisallowedTownSpawnZones().contains("enemy") &&
					jaileeLocTown.getNationOrNull().hasEnemy(jailedResident.getNationOrNull()))
						throw new TownyException(Translatable.of("msg_err_resident_cannot_be_jailed_because_they_are_enemied_there"));
			}

			// Begin getting hours, bail, jail and cell numbers from the inputted arguments.
			if (split.length > 1) {
				// offset is used to determine what argument in split is used for bail, jail # and cell #. 
				int offset = TownySettings.isAllowingBail() && TownyEconomyHandler.isActive() ? 1 : 0;

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
					jail = town.getJail(Integer.valueOf(split[2 + offset]));
					if (jail == null) 
						throw new TownyException(Translatable.of("msg_err_the_town_does_not_have_that_many_jails"));
				}

				// Set the jail cell if the argument is given.
				if (split.length == 4 + offset) {
					cell = Integer.valueOf(split[3 + offset]);
					if (!jail.hasJailCell(cell))
						throw new TownyException(Translatable.of("msg_err_that_jail_plot_does_not_have_that_many_cells"));
				}
			}

			// Check if Town has reached max potential jailed and react according to maxJailedNewJailBehavior in config
			if (TownySettings.getMaxJailedPlayerCount() > 0 && town.getJailedPlayerCount() >= TownySettings.getMaxJailedPlayerCount()) {
				if (TownySettings.getMaxJailedNewJailBehavior() == 0)
					// simple mode, rejects new jailed people outright
					throw new TownyException(Translatable.of("msg_town_has_no_jailslots"));
				//Pass to JailUtil method
				JailUtil.maxJailedUnjail(town);
			}

			// Jail the resident.
			JailUtil.jailResidentWithBail(jailedResident, jail, cell, hours, bail, JailReason.MAYOR, sender);

			// Send an admin a message if the player was jailed via Admin.
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_player_has_been_sent_to_jail_number", jailedPlayer.getName(), jailNum));

			// If fee exists (already sanitised for) deduct it from Town bank and inform in chat
			if (initialJailFee > 0) {
				town.getAccount().withdraw(initialJailFee, "New Prisoner fee for " + jailedResident.getName());
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_x_has_been_withdrawn_for_jailing_of_prisoner_x", initialJailFee,jailedResident));
			}

		} catch (NullPointerException e) {
			e.printStackTrace();
			return;
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

	private static int setJailHours(CommandSender sender, String[] split) {
		int hours = Math.min(2, Integer.valueOf(split[1]));
		if (hours > TownySettings.getJailedMaxHours()) {
			hours = TownySettings.getJailedMaxHours();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_higher_than_max_allowed_hours_x", TownySettings.getJailedMaxHours()));
		}
		return hours;
	}

	private static double setBail(CommandSender sender, String[] split) {
		double bail = Math.min(1, Double.valueOf(split[2]));
		if (bail > TownySettings.getBailMaxAmount()) {
			bail = TownySettings.getBailMaxAmount();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_higher_than_max_allowed_bail_x", TownySettings.getBailMaxAmount()));
		}
		return bail;
	}

	private static void parseJailListCommand(CommandSender sender, Town town, String[] args) {
		try {
			Player player = null;
			if (sender instanceof Player) {
				player = (Player) sender;
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_JAIL_LIST.getNode());
				
				Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
				if (resident == null || !resident.hasTown())
					throw new TownyException(Translatable.of("msg_err_must_belong_town"));
			}

			int page = 1;
			int total = (int) Math.ceil(((double) town.getJails().size()) / ((double) 10));
			if (args.length == 1) {
				page = MathUtil.getPositiveIntOrThrow(args[0]);
				if (page == 0)
					throw new TownyException(Translatable.of("msg_error_must_be_int"));

			}
			if (page > total)
				throw new TownyException(Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));

			TownyMessaging.sendJailList(player, town, page, total);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}

		
	}

	private static void toggleTest(Town town, String split) throws TownyException {

		// Make sure we are allowed to set these permissions.

		split = split.toLowerCase();

		if (split.contains("mobs") && town.getHomeblockWorld().isForceTownMobs())
			throw new TownyException(Translatable.of("msg_world_mobs"));

		if (split.contains("fire") && town.getHomeblockWorld().isForceFire())
			throw new TownyException(Translatable.of("msg_world_fire"));

		if (split.contains("explosion") && town.getHomeblockWorld().isForceExpl())
			throw new TownyException(Translatable.of("msg_world_expl"));

		if (split.contains("pvp") && town.getHomeblockWorld().isForcePVP())
			throw new TownyException(Translatable.of("msg_world_pvp"));

	}

	public void townRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			TownyMessaging.sendMessage(player, ChatTools.formatTitle("/town rank"));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/town rank", "add/remove [resident] rank", ""));
			return;
		}

		Resident target;
		Town town = null;
		String rank;

		/*
		 * Does the command have enough arguments?
		 */
		if (split.length < 3)
			throw new TownyException("Eg: /town rank add/remove [resident] [rank]");

		target = getResidentOrThrow(split[1]);
		town = getTownFromPlayerOrThrow(player);

		if (town != target.getTown())
			throw new TownyException(Translatable.of("msg_resident_not_your_town"));

		/*
		 * Match casing to an existing rank, returns null if Town rank doesn't exist.
		 */
		rank = TownyPerms.matchTownRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

		/*
		 * Only allow the player to assign ranks if they have the grant perm
		 * for it.
		 */
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank.toLowerCase()), Translatable.of("msg_no_permission_to_give_rank"));

		if (split[0].equalsIgnoreCase("add")) {

			if (!target.hasTownRank(rank)) {
				BukkitTools.ifCancelledThenThrow(new TownAddResidentRankEvent(target, rank, town));

				target.addTownRank(rank);
				if (target.isOnline()) {
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", Translatable.of("town_sing"), rank));
					plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
				}
				TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", Translatable.of("town_sing"), rank, target.getName()));
			} else {
				// Must already have this rank
				TownyMessaging.sendMsg(player, Translatable.of("msg_resident_already_has_rank", target.getName(), Translatable.of("town_sing")));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove")) {

			if (target.hasTownRank(rank)) {
				BukkitTools.ifCancelledThenThrow(new TownRemoveResidentRankEvent(target, rank, town));

				target.removeTownRank(rank);
				if (target.isOnline()) {
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", Translatable.of("town_sing"), rank));
					plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
				}
				TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", Translatable.of("town_sing"), rank, target.getName()));
			} else {
				// Doesn't have this rank
				TownyMessaging.sendMsg(player, Translatable.of("msg_resident_doesnt_have_rank", target.getName(), Translatable.of("town_sing")));
				return;
			}

		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		target.save();

	}

	public static void townSet(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {

		if (split.length == 0) {
			HelpMenu.TOWN_SET.send(sender);
			return;
		}
		Resident resident;
		Nation nation = null;
		Player player = null;
		if (sender instanceof Player p)
			player = p;
		
		if (!admin && player != null) {
			resident = getResidentOrThrow(player);
			town = resident.getTown();
		} else // Have the resident being tested be the mayor.
			resident = town.getMayor();

		if (town.hasNation())
			nation = town.getNationOrNull();

		if (split[0].equalsIgnoreCase("board")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_BOARD.getNode());
			townSetBoard(sender, String.join(" ", StringMgmt.remFirstArg(split)), town);

		} else if (split[0].equalsIgnoreCase("title")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());
			townSetTitle(sender, split, admin);

		} else if (split[0].equalsIgnoreCase("taxpercentcap")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXPERCENTCAP.getNode());
			townSetTaxPercent(sender, split, town);

		} else if (split[0].equalsIgnoreCase("surname")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
			townSetSurname(sender, split, admin);

		} else if (split[0].equalsIgnoreCase("mayor")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_MAYOR.getNode());
			townSetMayor(sender, split, admin, town, resident);

		} else if (split[0].equalsIgnoreCase("taxes")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAXES.getNode());
			townSetTaxes(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("plottax")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PLOTTAX.getNode());
			townSetPlotTax(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("shoptax")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SHOPTAX.getNode());
			townSetShopTax(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("embassytax")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_EMBASSYTAX.getNode());
			townSetEmbassyTax(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("plotprice")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PLOTPRICE.getNode());
			townSetPlotPrice(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("shopprice")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SHOPPRICE.getNode());
			townSetShopPrice(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("embassyprice")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_EMBASSYPRICE.getNode());
			townSetEmbassyPrice(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("spawncost")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWNCOST.getNode());
			townSetSpawnCost(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("name")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_NAME.getNode());
			townSetName(sender, split, town);

		} else if (split[0].equalsIgnoreCase("tag")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TAG.getNode());
			townSetTag(sender, split, admin, town);

		} else if (split[0].equalsIgnoreCase("homeblock")) {

			catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_HOMEBLOCK.getNode());
			townSetHomeblock(player, town, nation);
			
		} else if (split[0].equalsIgnoreCase("spawn")) {

			catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SPAWN.getNode());
			townSetSpawn(player, town, admin);

		} else if (split[0].equalsIgnoreCase("outpost")) {

			catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_OUTPOST.getNode());
			townSetOutpost(sender, town, player);

		} else if (split[0].equalsIgnoreCase("perm")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PERM.getNode());
			// Make sure we are allowed to set these permissions.
			toggleTest(town, StringMgmt.join(split, " "));
			setTownBlockOwnerPermissions(sender, town, StringMgmt.remFirstArg(split));

		} else if (split[0].equalsIgnoreCase("primaryjail")) {

			catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_PRIMARYJAIL.getNode());
			townSetPrimaryJail(player, town);
			
		} else if (split[0].equalsIgnoreCase("mapcolor")) {

			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_MAPCOLOR.getNode());
			townSetMapColor(sender, split, town);

		} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWN_SET, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.TOWN_SET, split[0]).execute(sender, "town", split);
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "town"));
			return;
		}
		
		town.save();

		if (nation != null)
			nation.save();

	}

	public static void townSetBoard(CommandSender sender, String board, Town town) throws TownyException {

		if (board.isEmpty())
			throw new TownyException("Eg: /town set board " + Translatable.of("town_help_9").forLocale(sender));

		if ("reset".equalsIgnoreCase(board)) {
			board = TownySettings.getTownDefaultBoard();
			
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_board_reset"));
		} else if ("none".equalsIgnoreCase(board) || "clear".equalsIgnoreCase(board)) {
			board = "";
		} else {
			if (!NameValidation.isValidString(board)) {
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

	@Deprecated
	public static void townSetTitle(CommandSender sender, String[] split, boolean admin, Town town, Resident resident, Player player) throws TownyException {
		townSetTitle(sender, split, admin);
	}

	public static void townSetTitle(@NotNull CommandSender sender, @NotNull String[] split, boolean admin) throws TownyException {
		// Give the resident a title
		if (split.length < 2)
			throw new TownyException("Eg: /town set title bilbo Jester");

		Resident resident = getResidentOrThrow(split[1]);
		String title = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
		
		townSetTitle(sender, resident, title, admin);
	}

	/**
	 * @param sender The command sender who initiated the command.
	 * @param resident The resident to set the title for.
	 * @param title The title to set.
	 * @param admin Whether to skip the same-town test.
	 * @throws TownyException If the title wasn't able to be set.
	 */
	public static void townSetTitle(@NotNull CommandSender sender, @NotNull Resident resident, @NotNull String title, boolean admin) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_TITLE.getNode());

		final boolean sameTown = sender instanceof Player player && CombatUtil.isSameTown(getResidentOrThrow(player), resident);
		
		if (!admin && !sameTown)
			throw new TownyException(Translatable.of("msg_err_not_same_town", resident.getName()));
		
		title = NameValidation.filterName(title);
		
		if (title.length() > TownySettings.getMaxTitleLength())
			throw new TownyException(Translatable.of("msg_err_input_too_long"));

		if (NameValidation.isConfigBlacklistedName(title))
			throw new TownyException(Translatable.of("msg_invalid_name"));

		resident.setTitle(title);
		resident.save();

		Translatable message = resident.hasTitle()
			? Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle()))
			: Translatable.of("msg_clear_title_surname", "Title", resident.getName());
		
		TownyMessaging.sendPrefixedTownMessage(resident, message);

		if (admin && !sameTown)
			TownyMessaging.sendMsg(sender, message);
	}

	@Deprecated
	public static void townSetSurname(CommandSender sender, String[] split, boolean admin, Town town, Resident resident, Player player) throws TownyException {
		townSetSurname(sender, split, admin);
	}
	
	public static void townSetSurname(CommandSender sender, String[] split, boolean admin) throws TownyException {
		// Give the resident a surname
		if (split.length < 2)
			throw new TownyException("Eg: /town set surname bilbo the dwarf ");
		
		Resident resident = getResidentOrThrow(split[1]);
		String surname = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
		
		townSetSurname(sender, resident, surname, admin);
	}

	/**
	 * @param sender The command sender who initiated the command.
	 * @param resident The resident to set the surname for.
	 * @param surname The surname to set.
	 * @param admin Whether to skip the same-town test.
	 * @throws TownyException If the surname wasn't able to be set.
	 */
	public static void townSetSurname(@NotNull CommandSender sender, @NotNull Resident resident, @NotNull String surname, boolean admin) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_SET_SURNAME.getNode());
		
		final boolean sameTown = sender instanceof Player player && CombatUtil.isSameTown(getResidentOrThrow(player), resident);
		
		if (!admin && !sameTown)
			throw new TownyException(Translatable.of("msg_err_not_same_town", resident.getName()));
		
		surname = NameValidation.filterName(surname);

		if (surname.length() > TownySettings.getMaxTitleLength())
			throw new TownyException(Translatable.of("msg_err_input_too_long"));

		if (NameValidation.isConfigBlacklistedName(surname))
			throw new TownyException(Translatable.of("msg_invalid_name"));
		
		resident.setSurname(surname);
		resident.save();

		Translatable message = resident.hasSurname()
			? Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname()))
			: Translatable.of("msg_clear_title_surname", "Surname", resident.getName());

		TownyMessaging.sendPrefixedTownMessage(resident, message);
		
		if (admin && !sameTown)
			TownyMessaging.sendMsg(sender, message);
	}

	public static void townSetMayor(CommandSender sender, String[] split, boolean admin, Town town, Resident resident) throws TownyException, NotRegisteredException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set mayor Dumbo");

		if (!admin && !resident.isMayor()) // We already know the resident is a mayor if admin if true, prevents an NPE in rare cases of resident being null.
			throw new TownyException(Translatable.of("msg_not_mayor"));

		Resident oldMayor = town.getMayor();
		Resident newMayor = getResidentOrThrow(split[1]);
		if (!town.hasResident(split[1]))
			throw new TownyException(Translatable.of("msg_err_mayor_doesnt_belong_to_town"));

		TownMayorChangeEvent townMayorChangeEvent = new TownMayorChangeEvent(oldMayor, newMayor);
		if (BukkitTools.isEventCancelled(townMayorChangeEvent) && !admin)
			throw new TownyException(townMayorChangeEvent.getCancelMessage());

		if (town.isCapital()) {
			NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldMayor, newMayor);
			if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin)
				throw new TownyException(nationKingChangeEvent.getCancelMessage());
		}
		
		town.setMayor(newMayor);
		
		if (oldMayor != null) {
			TownyPerms.assignPermissions(oldMayor, null);
			plugin.deleteCache(oldMayor);
		}

		plugin.deleteCache(newMayor);
		if (admin) {
			town.setHasUpkeep(!newMayor.isNPC());
			TownyMessaging.sendMsg(sender, Translatable.of("msg_new_mayor", newMayor.getName()));
		}
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", newMayor.getName()));
	}

	public static void townSetTaxes(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set taxes 7");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));
			if (town.isTaxPercentage() && amount > 100)
				throw new TownyException(Translatable.of("msg_err_not_percentage"));
			if (TownySettings.getTownDefaultTaxMinimumTax() > amount)
				throw new TownyException(Translatable.of("msg_err_tax_minimum_not_met", TownySettings.getTownDefaultTaxMinimumTax()));
			town.setTaxes(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_tax", sender.getName(), town.getTaxes()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_tax", sender.getName(), town.getTaxes()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetPlotTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set plottax 10");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));
			town.setPlotTax(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_plottax", sender.getName(), town.getPlotTax()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_plottax", sender.getName(), town.getPlotTax()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetShopTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2) 
			throw new TownyException("Eg: /town set shoptax 10");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));
			town.setCommercialPlotTax(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_alttax", sender.getName(), "shop", town.getCommercialPlotTax()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_alttax", sender.getName(), "shop", town.getCommercialPlotTax()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetEmbassyTax(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set embassytax 10");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (!TownySettings.isNegativePlotTaxAllowed() && amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));
			town.setEmbassyPlotTax(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_alttax", sender.getName(), "embassy", town.getEmbassyPlotTax()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_alttax", sender.getName(), "embassy", town.getEmbassyPlotTax()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetPlotPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set plotprice 50");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (amount < 0) 
				throw new TownyException(Translatable.of("msg_err_negative_money"));

			town.setPlotPrice(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_plotprice", sender.getName(), town.getPlotPrice()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_plotprice", sender.getName(), town.getPlotPrice()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetShopPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set shopprice 50");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));

			town.setCommercialPlotPrice(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_altprice", sender.getName(), "shop", town.getCommercialPlotPrice()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_altprice", sender.getName(), "shop", town.getCommercialPlotPrice()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetEmbassyPrice(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set embassyprice 50");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));

			town.setEmbassyPlotPrice(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_altprice", sender.getName(), "embassy", town.getEmbassyPlotPrice()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_altprice", sender.getName(), "embassy", town.getEmbassyPlotPrice()));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetSpawnCost(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException {
		if (split.length < 2) 
			throw new TownyException("Eg: /town set spawncost 50");
		try {
			Double amount = Double.parseDouble(split[1]);
			if (amount < 0)
				throw new TownyException(Translatable.of("msg_err_negative_money"));

			if (TownySettings.getSpawnTravelCost() < amount) 
				throw new TownyException(Translatable.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));

			town.setSpawnCost(amount);
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_spawn_cost_set_to", sender.getName(), Translatable.of("town_sing"), split[1]));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_spawn_cost_set_to", sender.getName(), Translatable.of("town_sing"), split[1]));
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_num"));
		}
	}

	public static void townSetName(CommandSender sender, String[] split, Town town) throws TownyException {
		if (split.length < 2) 
			throw new TownyException("Eg: /town set name BillyBobTown");

		String name = String.join("_", StringMgmt.remFirstArg(split));
		
		if (NameValidation.isBlacklistName(name) 
			|| TownyUniverse.getInstance().hasTown(name)
			|| (!TownySettings.areNumbersAllowedInTownNames() && NameValidation.containsNumbers(name)))
			throw new TownyException(Translatable.of("msg_invalid_name"));

		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			name = StringMgmt.capitalizeStrings(name);
		
		if(TownyEconomyHandler.isActive() && TownySettings.getTownRenameCost() > 0) {
			if (!town.getAccount().canPayFromHoldings(TownySettings.getTownRenameCost()))
				throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())));
			
			final Town finalTown = town;
			final String finalName = name;
			Confirmation.runOnAccept(() -> townRename(sender, finalTown, finalName))
				.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getTownRenameCost())))
				.sendTo(sender);
		} else {
			townRename(sender, town, name);
		}
	}

	public static void townSetTag(CommandSender sender, String[] split, boolean admin, Town town) throws TownyException, InvalidNameException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set tag PLTC");
		else if (split[1].equalsIgnoreCase("clear")) {
			town.setTag(" ");
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_reset_town_tag", sender.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_reset_town_tag", sender.getName()));
		} else {
			if (split[1].length() > TownySettings.getMaxTagLength())
				throw new TownyException(Translatable.of("msg_err_tag_too_long"));
			
			town.setTag(NameValidation.checkAndFilterName(split[1]));
			if (admin) TownyMessaging.sendMsg(sender, Translatable.of("msg_set_town_tag", sender.getName(), town.getTag()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_set_town_tag", sender.getName(), town.getTag()));
		}
	}

	public static void townSetHomeblock(Player player, Town town, @Nullable Nation nation) throws TownyException {
		Coord coord = Coord.parseCoord(player);
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

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

		if (world.hasTowns() &&
			TownySettings.getMinDistanceFromTownHomeblocks() > 0 || 
			TownySettings.getMaxDistanceBetweenHomeblocks() > 0 ||
			TownySettings.getMinDistanceBetweenHomeblocks() > 0) {
				
			final int distanceToNextNearestHomeblock = world.getMinDistanceFromOtherTowns(coord, town);
			if (distanceToNextNearestHomeblock < TownySettings.getMinDistanceFromTownHomeblocks() ||
				distanceToNextNearestHomeblock < TownySettings.getMinDistanceBetweenHomeblocks()) 
				throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0 &&
				distanceToNextNearestHomeblock > TownySettings.getMaxDistanceBetweenHomeblocks())
				throw new TownyException(Translatable.of("msg_too_far"));
		}
		
		if (TownySettings.getHomeBlockMovementDistanceInTownBlocks() > 0) {
			double distance = MathUtil.distance(town.getHomeBlock().getX(), townBlock.getX(), town.getHomeBlock().getZ(), townBlock.getZ());
			if (distance > TownySettings.getHomeBlockMovementDistanceInTownBlocks())
				throw new TownyException(Translatable.of("msg_err_you_cannot_move_your_homeblock_this_far_limit_is_x_you_are_x", TownySettings.getHomeBlockMovementDistanceInTownBlocks(), Math.floor(distance)));
		}

		BukkitTools.ifCancelledThenThrow(new TownPreSetHomeBlockEvent(town, townBlock, player));

		// Test whether towns will be removed from the nation
		if (nation != null && TownySettings.getNationRequiresProximity() > 0 && town.isCapital()) {
			// Determine if some of the nation's towns' homeblocks will be out of range.
			List<Town> removedTowns = nation.gatherOutOfRangeTowns(nation.getTowns(), town);
			
			// Oh no, some the nation will lose at least one town, better make a confirmation.
			if (!removedTowns.isEmpty()) {
				final Town finalTown = town;
				final TownBlock finalTB = townBlock;
				final Nation finalNation = nation;
				final Location playerLocation = player.getLocation();
				Confirmation.runOnAccept(() -> {
					// Set town homeblock and remove the out of range towns.
					finalTown.setHomeBlock(finalTB);
					finalTown.setSpawn(playerLocation);
					town.setMovedHomeBlockAt(System.currentTimeMillis());
					finalNation.removeOutOfRangeTowns();
					TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_home", coord.toString()));
				}).setTitle(Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
				  .sendTo(player);

			// Phew, the nation won't lose any towns, let's do this.
			} else {
				town.setHomeBlock(townBlock);
				town.setSpawn(player.getLocation());		
				town.setMovedHomeBlockAt(System.currentTimeMillis());
				TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_home", coord.toString()));
			}
		// No nation to check proximity for/proximity isn't tested anyways.
		} else {
			town.setHomeBlock(townBlock);
			town.setSpawn(player.getLocation());
			town.setMovedHomeBlockAt(System.currentTimeMillis());
			TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_home", coord.toString()));
		}
	}

	public static void townSetSpawn(Player player, Town town, boolean admin) throws TownyException {
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
		TownyMessaging.sendMsg(player, Translatable.of("msg_set_town_spawn"));
	}

	public static void townSetOutpost(CommandSender sender, Town town, Player player) throws TownyException {
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		if (townBlock == null || !townBlock.hasTown() || !townBlock.isOutpost())
			throw new TownyException(Translatable.of("msg_err_location_is_not_within_an_outpost_plot"));
		
		if (townBlock.getTownOrNull().equals(town)) {
			town.addOutpostSpawn(player.getLocation());
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_outpost_spawn"));
		} else
			throw new TownyException(Translatable.of("msg_not_own_area"));
	}

	public static void townSetPrimaryJail(Player player, Town town) throws TownyException {

		TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
		if (tb == null || !tb.isJail())
			throw new TownyException(Translatable.of("msg_err_location_is_not_within_a_jail_plot"));
		
		Jail jail = tb.getJail();
		town.setPrimaryJail(jail);
		TownyMessaging.sendMsg(player, Translatable.of("msg_primary_jail_set_for_town"));
	}

	public static void townSetMapColor(CommandSender sender, String[] split, Town town) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /town set mapcolor brown.");

		String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

		if (!TownySettings.getTownColorsMap().containsKey(line.toLowerCase()))
			throw new TownyException(Translatable.of("msg_err_invalid_nation_map_color", TownySettings.getTownColorsMap().keySet().toString()));

		town.setMapColorHexCode(TownySettings.getTownColorsMap().get(line.toLowerCase()));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_map_color_changed", line.toLowerCase()));
	}

	public static void townSetTaxPercent(CommandSender sender, String[] split, Town town) throws TownyException {
		if (!town.isTaxPercentage())
			throw new TownyException(Translatable.of("msg_max_tax_amount_only_for_percent"));

		if (split.length < 2) 
			throw new TownyException("Eg. /town set taxpercentcap 10000");

		town.setMaxPercentTaxAmount(Double.parseDouble(split[1]));

		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_tax_max_percent_amount", sender.getName(), TownyEconomyHandler.getFormattedBalance(town.getMaxPercentTaxAmount())));
	}

	private static void parseTownBaltop(Player player, Town town) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			StringBuilder sb = new StringBuilder();
			List<Resident> residents = new ArrayList<>(town.getResidents());
			residents.sort(Comparator.<Resident>comparingDouble(res -> res.getAccount().getCachedBalance()).reversed());
	
			int i = 0;
			for (Resident res : residents)
				sb.append(Translatable.of("msg_baltop_book_format", ++i, res.getName(), TownyEconomyHandler.getFormattedBalance(res.getAccount().getCachedBalance())).forLocale(player) + "\n");

			ItemStack book = BookFactory.makeBook("Town Baltop", town.getName(), sb.toString());
			Bukkit.getScheduler().runTask(plugin, () -> player.openBook(book));
		});
	}

	public static void townBuy(CommandSender sender, String[] split, @Nullable Town town, boolean admin) throws TownyException {
		
		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));

		if (town == null && sender instanceof Player player)
			town = getTownFromPlayerOrThrow(player);

		if (!TownySettings.isSellingBonusBlocks(town) && !TownySettings.isBonusBlocksPerTownLevel())
			throw new TownyException("Config.yml has bonus blocks diabled at max_purchased_blocks: '0' ");
		else if (TownySettings.isBonusBlocksPerTownLevel() && TownySettings.getMaxBonusBlocks(town) == 0)
			throw new TownyException("Config.yml has bonus blocks disabled at town_level section: townBlockBonusBuyAmount: 0");

		if (split.length == 0 || !split[0].equalsIgnoreCase("bonus")) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle("/town buy"));
			String line = Colors.Yellow + "[Purchased Bonus] " + Colors.Green + "Cost: " + Colors.LightGreen + "%s" + Colors.Gray + " | " + Colors.Green + "Max: " + Colors.LightGreen + "%d";
			TownyMessaging.sendMessage(sender, String.format(line, TownyEconomyHandler.getFormattedBalance(town.getBonusBlockCost()), TownySettings.getMaxPurchasedBlocks(town)));
			if (TownySettings.getPurchasedBonusBlocksIncreaseValue() != 1.0)
				TownyMessaging.sendMessage(sender, Colors.Green + "Cost Increase per TownBlock: " + Colors.LightGreen + "+" +  new DecimalFormat("##.##%").format(TownySettings.getPurchasedBonusBlocksIncreaseValue()-1));
			TownyMessaging.sendMessage(sender, ChatTools.formatCommand("", "/town buy", "bonus [n]", ""));
			return;
		}
		
		// They have used `/t buy bonus`, check that they have specified an amount to purchase.
		if (split.length == 2)
			townBuyBonusTownBlocks(town, MathUtil.getIntOrThrow(split[1].trim()), sender);
		else
			throw new TownyException(Translatable.of("msg_must_specify_amnt", "/town buy bonus"));
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
			throw new TownyException(Translatable.of("msg_no_funds_to_buy", n, Translatable.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
		
		Confirmation.runOnAccept(() -> {
			town.addPurchasedBlocks(n);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_buy", n, Translatable.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost)));
			town.save();
		})
			.setCost(new ConfirmationTransaction(() -> cost, town.getAccount(), String.format("Town Buy Bonus (%d)", n),
					Translatable.of("msg_no_funds_to_buy", n, Translatable.of("bonus_townblocks"), TownyEconomyHandler.getFormattedBalance(cost))))
			.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(cost)))
			.sendTo(sender); 
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
		if (TownySettings.hasTownLimit() && TownyUniverse.getInstance().getTowns().size() >= TownySettings.getTownLimit())
			throw new TownyException(Translatable.of("msg_err_universe_limit"));

		// Check if the player has a cooldown since deleting their town.
		if (!resident.isAdmin() && CooldownTimerTask.hasCooldown(player.getName(), CooldownType.TOWN_DELETE))
			throw new TownyException(Translatable.of("msg_err_cannot_create_new_town_x_seconds_remaining",
					CooldownTimerTask.getCooldownRemaining(player.getName(), CooldownType.TOWN_DELETE)));

		name = filterNameOrThrow(name);

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

		if (world.hasTowns())
			testDistancesOrThrow(world, key);

		// If the town doesn't cost money to create, just make the Town.
		if (noCharge || !TownyEconomyHandler.isActive()) {
			BukkitTools.ifCancelledThenThrow(new PreNewTownEvent(player, name, spawnLocation));
			newTown(world, name, resident, key, spawnLocation, player);
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(name)));
			return;
		}

		// Test if the resident can afford the town.
		if (!resident.getAccount().canPayFromHoldings(TownySettings.getNewTownPrice()))
			throw new TownyException(Translatable.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translatable.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice()));

		// Send a confirmation before taking their money and throwing the PreNewTownEvent.
		final String finalName = name;
		Confirmation.runOnAccept(() -> {
			try {
				// Make town.
				newTown(world, finalName, resident, key, spawnLocation, player);
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_town", player.getName(), StringMgmt.remUnderscore(finalName)));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				e.printStackTrace();
			}
		})
		.setCancellableEvent(new PreNewTownEvent(player, name, spawnLocation))
		.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewTownPrice())))
		.setCost(new ConfirmationTransaction(TownySettings::getNewTownPrice, resident.getAccount(), "New Town Cost",
			Translatable.of("msg_no_funds_new_town2", (resident.getName().equals(player.getName()) ? Translatable.of("msg_you") : resident.getName()), TownySettings.getNewTownPrice())))
		.sendTo(player);
	}

	public static Town newTown(TownyWorld world, String name, Resident resident, Coord key, Location spawn, Player player) throws TownyException {

		TownyUniverse.getInstance().newTown(name);
		Town town = TownyUniverse.getInstance().getTown(name);
		
		// This should never happen
		if (town == null)
			throw new TownyException(String.format("Error fetching new town from name '%s'", name));

		TownBlock townBlock = new TownBlock(key.getX(), key.getZ(), world);
		townBlock.setTown(town);
		TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, townBlock, player, false, true);
		preClaimEvent.setCancelMessage(Translation.of("msg_claim_error", 1, 1));
		
		if (BukkitTools.isEventCancelled(preClaimEvent)) {
			TownyUniverse.getInstance().removeTownBlock(townBlock);
			TownyUniverse.getInstance().unregisterTown(town);
			town = null;
			townBlock = null;
			throw new TownyException(preClaimEvent.getCancelMessage());
		}

		town.setRegistered(System.currentTimeMillis());
		town.setMapColorHexCode(MapUtil.generateRandomTownColourAsHexCode());
		resident.setTown(town);
		town.setMayor(resident);
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
			} else {

				plotChunk = new PlotBlockData(townBlock); // Not regenerating so create a new snapshot.
				plotChunk.initialize();

			}
			TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save a snapshot.
			plotChunk = null;
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
			town.setTag(name.substring(0, Math.min(name.length(), TownySettings.getMaxTagLength())).replace("_","").replace("-", ""));
		
		resident.save();
		townBlock.save();
		town.save();
		world.save();

		// Reset cache permissions for anyone in this TownBlock
		plugin.updateCache(townBlock.getWorldCoord());

		BukkitTools.fireEvent(new NewTownEvent(town));

		return town;
	}

	private static void testDistancesOrThrow(TownyWorld world, Coord key) throws TownyException {
		if (TownySettings.getMinDistanceFromTownPlotblocks() > 0 || TownySettings.getNewTownMinDistanceFromTownPlots() > 0) {
			int minDistance = TownySettings.getNewTownMinDistanceFromTownPlots();
			if (minDistance <= 0)
				minDistance = TownySettings.getMinDistanceFromTownPlotblocks();
			
			if (world.getMinDistanceFromOtherTownsPlots(key) < minDistance)
				throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));
		}

		if (TownySettings.getMinDistanceFromTownHomeblocks() > 0 ||
			TownySettings.getMaxDistanceBetweenHomeblocks() > 0 ||
			TownySettings.getMinDistanceBetweenHomeblocks() > 0 ||
			TownySettings.getNewTownMinDistanceFromTownHomeblocks() > 0) {
			
			final int distanceToNextNearestHomeblock = world.getMinDistanceFromOtherTowns(key);
			
			int minDistance = TownySettings.getNewTownMinDistanceFromTownHomeblocks();
			if (minDistance <= 0)
				minDistance = TownySettings.getMinDistanceFromTownHomeblocks();
			
			if (distanceToNextNearestHomeblock < minDistance || distanceToNextNearestHomeblock < TownySettings.getMinDistanceBetweenHomeblocks()) 
				throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0 &&
				TownyUniverse.getInstance().getTowns().size() > 0 &&
				distanceToNextNearestHomeblock > TownySettings.getMaxDistanceBetweenHomeblocks())
				throw new TownyException(Translatable.of("msg_too_far"));
		}
	}

	private static String filterNameOrThrow(String name) throws TownyException {
		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			name = StringMgmt.capitalizeStrings(name);
		
		// Check the name is valid and doesn't already exist.
		String filteredName;
		try {
			filteredName = NameValidation.checkAndFilterName(name);
		} catch (InvalidNameException e) {
			filteredName = null;
		}

		if (filteredName == null || TownyUniverse.getInstance().hasTown(filteredName) || (!TownySettings.areNumbersAllowedInTownNames() && NameValidation.containsNumbers(filteredName)))
			throw new TownyException(Translatable.of("msg_err_invalid_name", name));
		
		name = filteredName;
		return name;
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
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(renameCost)));
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
			
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_name", sender.getName(), town.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}
	}

	public void townLeave(Player player) throws TownyException {

		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);

		if (resident.isMayor())
			throw new TownyException(Translatable.of("msg_mayor_abandon"));

		if (resident.isJailed() && TownySettings.JailDeniesTownLeave() && resident.getJailTown().getName().equals(town.getName()))
			throw new TownyException(Translatable.of("msg_cannot_abandon_town_while_jailed"));

		Confirmation.runOnAccept(() -> {
			if (resident.isJailed() && resident.getJailTown().getUUID().equals(town.getUUID()))
				JailUtil.unJailResident(resident, UnJailReason.LEFT_TOWN);

			if (town.hasResident(resident))
				resident.removeTown();

			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_left_town", resident.getName()));
			TownyMessaging.sendMsg(player, Translatable.of("msg_left_town", resident.getName()));

			checkTownResidents(town);
		})
		.setCancellableEvent(new TownLeaveEvent(resident, town))
		.sendTo(player);
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
				townyUniverse.getDataSource().removeTown(town);
				if (TownySettings.getTownUnclaimCoolDownTime() > 0)
					CooldownTimerTask.addCooldownTimer(player.getName(), CooldownType.TOWN_DELETE);
			}).sendTo(player);
			return;
		}

		// An argument has been passed in the command, and the command sender is not a member of the town and able to delete it.
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode(), Translatable.of("msg_err_admin_only_delete_town"));

		Confirmation.runOnAccept(() -> {
			TownyMessaging.sendMsg(player, Translatable.of("town_deleted_by_admin", town.getName()));
			townyUniverse.getDataSource().removeTown(town);
		}).sendTo(player);
	}

	/**
	 * Transforms a list of names into a list of residents to be kicked.
	 * Command: /town kick [resident] .. [resident]
	 *
	 * @param player - Player who initiated the kick command.
	 * @param names - List of names to kick.
	 */
	public static void townKick(Player player, String[] names) {

		Resident resident;
		Town town;
		try {
			resident = getResidentOrThrow(player);
			town = resident.getTown();
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage(player));
			return;
		}

		townKickResidents(player, resident, town, ResidentUtil.getValidatedResidentsOfTown(player, town, names));

		// Reset everyones cache permissions as this player leaving can affect
		// multiple areas.
		plugin.resetCache();
	}

	public static void townAddResidents(CommandSender sender, Town town, List<Resident> invited) {
		String name;
		boolean admin = false;
		if (sender instanceof Player) {
			name = ((Player) sender).getName();
			if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin((Player) sender))
				admin = true;				
		} else {
			name = "Console";
			admin = true;
		}

		for (Resident newMember : new ArrayList<>(invited)) {
			try {

				if (!admin)
					BukkitTools.ifCancelledThenThrow(new TownPreAddResidentEvent(town, newMember));

				// only add players with the right permissions.
				if (BukkitTools.matchPlayer(newMember.getName()).isEmpty()) { // Not online
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_offline_no_join", newMember.getName()));
					invited.remove(newMember);
				} else if (!newMember.hasPermissionNode(PermissionNodes.TOWNY_TOWN_RESIDENT.getNode())) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_allowed_join", newMember.getName()));
					invited.remove(newMember);
				} else if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsForTown(town)){
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsForTown(town)));
					invited.remove(newMember);
				} else if (town.hasNation() && TownySettings.getMaxResidentsPerNation() > 0 && town.getNationOrNull().getResidents().size() >= TownySettings.getMaxResidentsPerNation()) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_cannot_add_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation(), newMember.getName()));
					invited.remove(newMember);
				} else if (!admin && TownySettings.getTownInviteCooldown() > 0 && ( (System.currentTimeMillis()/1000 - newMember.getRegistered()/1000) < (TownySettings.getTownInviteCooldown()) )) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_resident_doesnt_meet_invite_cooldown", newMember));
					invited.remove(newMember);
				} else if (TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() >= TownySettings.getMaxNumResidentsWithoutNation()) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_unable_to_add_more_residents_without_nation", TownySettings.getMaxNumResidentsWithoutNation()));
					invited.remove(newMember);
				} else {
					town.addResidentCheck(newMember);
					townInviteResident(sender, town, newMember);
				}
			} catch (TownyException e) {
				invited.remove(newMember);
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
		}

		if (invited.size() > 0) {
			StringBuilder msg = new StringBuilder();
			for (Resident newMember : invited)
				msg.append(newMember.getName()).append(", ");

			msg = new StringBuilder(msg.substring(0, msg.length() - 2));
			
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_invited_join_town", name, msg.toString()));
			town.save();
		} else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
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

	private static void townRevokeInviteResident(CommandSender sender, Town town, List<Resident> residents) {

		for (Resident invited : residents) {
			if (InviteHandler.inviteIsActive(town, invited)) {
				for (Invite invite : invited.getReceivedInvites()) {
					if (invite.getSender().equals(town)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMsg(sender, Translatable.of("town_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	/**
	 * Method for kicking residents from a town.
	 * 
	 * @param sender - CommandSender who initiated the kick.
	 * @param resident - Resident who initiated the kick.
	 * @param town - Town the list of Residents are being kicked from.
	 * @param kicking - List of Residents being kicked from Towny.
	 */
	public static void townKickResidents(CommandSender sender, Resident resident, Town town, List<Resident> kicking) {

		Resident senderResident = sender instanceof Player player ? TownyAPI.getInstance().getResident(player) : null;
		
		for (Resident member : new ArrayList<>(kicking)) {
			if (!town.hasResident(member)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_resident_not_your_town"));
				kicking.remove(member);
				continue;
			}

			if (resident == member) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_you_cannot_kick_yourself"));
				kicking.remove(member);
				continue;
			}

			// The player being kicked is either the mayor or has an 'unkickable' rank (usually an assistant)
			// The rank check is bypassed if the sender is either not a player or not in the same town as the player being kicked, for townyadmin purposes
			if (member.isMayor() || (senderResident != null && !senderResident.isMayor() && town.hasResident(senderResident) && TownySettings.getTownUnkickableRanks().stream().anyMatch(member::hasTownRank))) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_you_cannot_kick_this_resident", member));
				kicking.remove(member);
				continue;
			}

			TownKickEvent townKickEvent = new TownKickEvent(member, sender);
			if (BukkitTools.isEventCancelled(townKickEvent)) {
				TownyMessaging.sendErrorMsg(sender, townKickEvent.getCancelMessage());
				kicking.remove(member);
			} else
				member.removeTown();
		}
		
		if (kicking.size() > 0) {
			String message = kicking.stream().map(Resident::getName).collect(Collectors.joining(", "));
			String kickerName = sender instanceof Player player ? player.getName() : "CONSOLE";

			for (Resident member : kicking)
				TownyMessaging.sendMsg(member, Translatable.of("msg_kicked_by", kickerName));

			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_kicked", kickerName, message));

			if (!(sender instanceof Player kickingPlayer) || !town.hasResident(kickingPlayer)) {
				// For when the an admin uses /ta town {name} kick {residents}
				TownyMessaging.sendMessage(sender, Translation.translateTranslatables(sender, "", Translatable.of("default_town_prefix", StringMgmt.remUnderscore(town.getName())), Translatable.of("msg_kicked", kickerName, message)));
			}
			town.save();
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
		}

		checkTownResidents(town);
	}

	public static void checkTownResidents(Town town) {
		if (!town.hasNation())
			return;
		Nation nation = town.getNationOrNull();
		if (town.isCapital() && TownySettings.getNumResidentsCreateNation() > 0 && town.getNumResidents() < TownySettings.getNumResidentsCreateNation()) {
			for (Town newCapital : nation.getTowns())
				if (newCapital.getNumResidents() >= TownySettings.getNumResidentsCreateNation()) {
					nation.setCapital(newCapital);
					if (TownySettings.getNumResidentsJoinNation() > 0 && town.getNumResidents() < TownySettings.getNumResidentsJoinNation()) {
						town.removeNation();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_capital_not_enough_residents_left_nation", town.getName()));
					}
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_not_enough_residents_no_longer_capital", newCapital.getName()));
					return;
				}
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_disbanded_town_not_enough_residents", town.getName()));
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation));
			TownyUniverse.getInstance().getDataSource().removeNation(nation);

			if (TownyEconomyHandler.isActive() && TownySettings.isRefundNationDisbandLowResidents()) {
				town.getAccount().deposit(TownySettings.getNewNationPrice(), "nation refund");
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_not_enough_residents_refunded", TownySettings.getNewNationPrice()));
			}
		} else if (!town.isCapital() && TownySettings.getNumResidentsJoinNation() > 0 && town.getNumResidents() < TownySettings.getNumResidentsJoinNation()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_not_enough_residents_left_nation", town.getName()));
			town.removeNation();
		}
	}

	/**
	 * If no arguments are given (or error), send usage of command. If sender is
	 * a player: args = [town]. Elsewise: args = [resident] [town]
	 *
	 * @param sender - Sender of command.
	 * @param args - Current command arguments.
	 */
	public static void parseTownJoin(CommandSender sender, String[] args) {

		try {
			Resident resident;
			Town town;
			String residentName, townName, contextualResidentName;
			boolean console = false;
			String exceptionMsg;

			if (sender instanceof Player) {
				// Player
				if (args.length < 1)
					throw new TownyException(Translatable.of("msg_usage", "/town join [town]"));

				Player player = (Player) sender;
				residentName = player.getName();
				townName = args[0];
				contextualResidentName = "You";
				exceptionMsg = "msg_err_already_res2";
			} else {
				// Console
				if (args.length < 2)
					throw new TownyException(Translatable.of("msg_usage", "town join [resident] [town]"));

				residentName = args[0];
				townName = args[1];
				contextualResidentName = residentName;
				exceptionMsg = "msg_err_already_res";
			}
			
			resident = getResidentOrThrow(residentName);
			town = getTownOrThrow(townName);

			// Check if resident is currently in a town.
			if (resident.hasTown())
				throw new TownyException(Translatable.of(exceptionMsg, contextualResidentName));

			if (!console) {
				// Check if town is town is free to join.
				if (!town.isOpen())
					throw new TownyException(Translatable.of("msg_err_not_open", town.getFormattedName()));
				if (town.hasNation() && TownySettings.getMaxResidentsPerNation() > 0 && town.getNationOrNull().getResidents().size() >= TownySettings.getMaxResidentsPerNation())
					throw new TownyException(Translatable.of("msg_err_cannot_join_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation()));
				if (TownySettings.getMaxResidentsPerTown() > 0 && town.getResidents().size() >= TownySettings.getMaxResidentsForTown(town))
					throw new TownyException(Translatable.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsForTown(town)));
				if (TownySettings.getMaxNumResidentsWithoutNation() > 0 && !town.hasNation() && town.getResidents().size() >= TownySettings.getMaxNumResidentsWithoutNation())
					throw new TownyException(Translatable.of("msg_err_unable_to_add_more_residents_without_nation", TownySettings.getMaxNumResidentsWithoutNation()));
				if (town.hasOutlaw(resident))
					throw new TownyException(Translatable.of("msg_err_outlaw_in_open_town"));
			}

			BukkitTools.ifCancelledThenThrow(new TownPreAddResidentEvent(town, resident));

			// Check if player is already in selected town (Pointless)
			// Then add player to town.
			townAddResident(town, resident);

			// Resident was added successfully.
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_join_town", resident.getName()));

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 *
	 * @param sender - Sender.
	 * @param specifiedTown - Town to add to if not null.
	 * @param names - Names to add.
	 * @throws TownyException - General Exception, or if Town's spawn has not been set
	 */
	public static void townAdd(CommandSender sender, Town specifiedTown, String[] names) throws TownyException {

		String name = sender instanceof Player player ? player.getName() : "Console";
		boolean console = !(sender instanceof Player);

		Resident resident;
		Town town;
		try {
			if (console) {
				town = specifiedTown;
			} else {
				resident = getResidentOrThrow(name);
				if (specifiedTown == null)
					town = resident.getTown();
				else
					town = specifiedTown;
			}

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage(sender));
			return;
		}

		if (town.isBankrupt())
			throw new TownyException(Translatable.of("msg_err_bankrupt_town_cannot_invite"));

		if (TownySettings.getMaxDistanceFromTownSpawnForInvite() > 0) {

			if (!town.hasSpawn())
				throw new TownyException(Translatable.of("msg_err_townspawn_has_not_been_set"));
		
			Location spawnLoc = town.getSpawn();
			ArrayList<String> newNames = new ArrayList<>();
			int maxDistance = TownySettings.getMaxDistanceFromTownSpawnForInvite();

			for (String nameForDistanceTest : names) {
				Player player = BukkitTools.getPlayerExact(nameForDistanceTest);
				if (player == null)
					continue;
				
				Location playerLoc = player.getLocation();
				
				double distance;
				try {
					distance = spawnLoc.distance(playerLoc);
				} catch (Exception e) {
					// Can throw an exception if the player is in another world
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_player_too_far_from_town_spawn", nameForDistanceTest, maxDistance));
					continue;
				}

				if (distance <= maxDistance)
					newNames.add(nameForDistanceTest);
				else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_player_too_far_from_town_spawn", nameForDistanceTest, maxDistance));
				}
			}
			names = newNames.toArray(new String[0]);
		}
		List<String> resList = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newResList = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeInvites = new ArrayList<>();
		// List of invites to be removed;
		for (String resName : resList) {
			if (resName.startsWith("-")) {
				removeInvites.add(resName.substring(1));
				// Add to removing them, remove the "-"
			} else {
				if (!town.hasResident(resName))
					newResList.add(resName);// add to adding them,
				else 
					removeInvites.add(resName);
			}
		}
		names = newResList.toArray(new String[0]);
		String[] namesToRemove = removeInvites.toArray(new String[0]);
		if (namesToRemove.length != 0) {
			List<Resident> toRevoke = getValidatedResidentsForInviteRevoke(sender, namesToRemove, town);
			if (!toRevoke.isEmpty())
				townRevokeInviteResident(sender, town, toRevoke);
		}

		if (names.length != 0) {
			townAddResidents(sender, town, ResidentUtil.getValidatedResidents(sender, names));
		}

		// Reset this players cached permissions
		if (!console)
			plugin.resetCache(BukkitTools.getPlayerExact(name));
	}

	// wrapper function for non friend setting of perms
	public static void setTownBlockOwnerPermissions(CommandSender sender, TownBlockOwner townBlockOwner, String[] split) {

		setTownBlockPermissions(sender, townBlockOwner, townBlockOwner.getPermissions(), split, false);
	}

	public static void setTownBlockPermissions(CommandSender sender, TownBlockOwner townBlockOwner, TownyPermission perm, String[] split, boolean friend) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split.length > 3) {

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

			return;
		}

		// reset the friend to resident so the perm settings don't fail
		if (friend && split[0].equalsIgnoreCase("friend"))
			split[0] = "resident";
		// reset the town to nation so the perm settings don't fail
		if (friend && split[0].equalsIgnoreCase("town"))
			split[0] = "nation";

		TownyPermissionChange permChange;

		if (split.length == 1) {

			if (split[0].equalsIgnoreCase("reset")) {

				// reset all townBlock permissions (by town/resident)
				for (TownBlock townBlock : townBlockOwner.getTownBlocks()) {

					if ((townBlockOwner instanceof Town && !townBlock.hasResident()) || 
						(townBlockOwner instanceof Resident && townBlock.hasResident())) {

						permChange = new TownyPermissionChange(TownyPermissionChange.Action.RESET, true, townBlock);
						try {
							BukkitTools.ifCancelledThenThrow(new TownBlockPermissionChangeEvent(townBlock, permChange));
						} catch (CancelledEventException e) {
							sender.sendMessage(e.getCancelMessage());
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
				return;

			} else {
				// Set all perms to On or Off
				// '/town set perm off'

				try {
					boolean b = StringMgmt.parseOnOff(split[0]);
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.ALL_PERMS, b);
					perm.change(permChange);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
					return;
				}
			}

		} else if (split.length == 2) {
			boolean b;

			try {
				b = StringMgmt.parseOnOff(split[1]);
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
				return;
			}

			if (split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";
			else if (split[0].equalsIgnoreCase("town"))
				split[0] = "nation";
			else if (split[0].equalsIgnoreCase("itemuse"))
				split[0] = "item_use";

			// Check if it is a perm level first
			try {
				TownyPermission.PermLevel permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));
				permChange = new TownyPermissionChange(TownyPermissionChange.Action.PERM_LEVEL, b, permLevel);
				perm.change(permChange);
			}
			catch (IllegalArgumentException permLevelException) {
				// If it is not a perm level, then check if it is a action type
				try {
					TownyPermission.ActionType actionType = TownyPermission.ActionType.valueOf(split[0].toUpperCase(Locale.ROOT));
					permChange = new TownyPermissionChange(TownyPermissionChange.Action.ACTION_TYPE, b, actionType);
					perm.change(permChange);
				} catch (IllegalArgumentException actionTypeException) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
					return;
				}
			}

		} else {
			// Reset the friend to resident so the perm settings don't fail
			if (split[0].equalsIgnoreCase("friend"))
				split[0] = "resident";
			// reset the town to nation so the perm settings don't fail
			else if (split[0].equalsIgnoreCase("town"))
				split[0] = "nation";
			if (split[1].equalsIgnoreCase("itemuse"))
				split[1] = "item_use";

			TownyPermission.PermLevel permLevel;
			TownyPermission.ActionType actionType;

			try {
				permLevel = TownyPermission.PermLevel.valueOf(split[0].toUpperCase(Locale.ROOT));
				actionType = TownyPermission.ActionType.valueOf(split[1].toUpperCase(Locale.ROOT));
			} catch (IllegalArgumentException ignore) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
				return;
			}
			
			try {
				boolean b = StringMgmt.parseOnOff(split[2]);

				permChange = new TownyPermissionChange(TownyPermissionChange.Action.SINGLE_PERM, b, permLevel, actionType);
				perm.change(permChange);

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_town_set_perm_syntax_error"));
				return;
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
				sender.sendMessage(e.getCancelMessage());
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
		TownyMessaging.sendMessage(sender, Colors.Green + translator.of("status_pvp") + " " + (perm.pvp ? translator.of("status_on") : translator.of("status_off")) + " " +
										   Colors.Green + translator.of("explosions") + " " + (perm.explosion ? translator.of("status_on") : translator.of("status_off")) + " " +
										   Colors.Green + translator.of("firespread") + " " + (perm.fire ? translator.of("status_on") : translator.of("status_off")) + " " +
										   Colors.Green + translator.of("mobspawns") + " " + (perm.mobs ? translator.of("status_on") : translator.of("status_off")));

		// Reset all caches as this can affect everyone.
		plugin.resetCache();

	}

	public static void parseTownClaimCommand(Player player, String[] split) throws TownyException {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TOWN_CLAIM.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);

		// Allow a bankrupt town to claim a single plot.
		if (town.isBankrupt() && town.getTownBlocks().size() != 0)
			throw new TownyException(Translatable.of("msg_err_bankrupt_town_cannot_claim"));

		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

		if (world == null || !world.isUsingTowny())
			throw new TownyException(Translatable.of("msg_set_use_towny_off"));
		
		if (!world.isClaimable())
			throw new TownyException(Translatable.of("msg_not_claimable"));

		List<WorldCoord> selection;
		boolean outpost = false;
		boolean isAdmin = resident.isAdmin();
		Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());

		/*
		 * Make initial selection of WorldCoord(s)
		 */
		if (split.length == 1 && split[0].equalsIgnoreCase("outpost")) {

			if (TownySettings.isAllowingOutposts()) {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_OUTPOST.getNode());
				
				// Run various tests required by configuration/permissions through Util.
				OutpostUtil.OutpostTests(town, resident, world, key, isAdmin, false);
				
				if (!TownyAPI.getInstance().isWilderness(plugin.getCache(player).getLastLocation()))
					throw new TownyException(Translatable.of("msg_already_claimed_1", key));

				// Select a single WorldCoord using the AreaSelectionUtil.
				selection = new ArrayList<>();
				selection.add(new WorldCoord(world.getName(), key));
				outpost = true;
			} else
				throw new TownyException(Translatable.of("msg_outpost_disable"));
		} else {
			
			// Prevent someone manually running /t claim world x z (a command which should only be run via /plot claim world x z)
			if (split.length != 0 && TownyAPI.getInstance().getTownyWorld(split[0]) != null)
				throw new TownyException(Translatable.of("tc_err_invalid_command"));

			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN.getNode());

			// Select the area, can be one or many.
			selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), split, true);
			
			if (selection.size() > 1) 
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_CLAIM_TOWN_MULTIPLE.getNode());

			// Filter out any TownBlocks which aren't Wilderness. 
			selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
		}

		// Not enough available claims.
		if (!town.hasUnlimitedClaims() && selection.size() > town.availableTownBlocks())
			throw new TownyException(Translatable.of("msg_err_not_enough_blocks"));

		// If this is a single claim and it is already claimed, by someone else.
		if (selection.size() == 1 && selection.get(0).getTownOrNull() != null)
			throw new TownyException(Translatable.of("msg_already_claimed", selection.get(0).getTownOrNull()));
		
		/*
		 * Filter out any unallowed claims.
		 */
		TownyMessaging.sendDebugMsg("townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
		
		// Filter out townblocks already owned.
		selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
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

		// Prevent straight line claims if configured, and the town has enough townblocks claimed, and this is not an outpost.
		int minAdjacentBlocks = TownySettings.getMinAdjacentBlocks();
		if (!outpost && minAdjacentBlocks > 0 && town.getTownBlocks().size() > minAdjacentBlocks) {
			// Only consider the first worldCoord, larger selection-claims will automatically "bubble" anyways.
			WorldCoord firstWorldCoord = selection.get(0);
			int numAdjacent = numAdjacentTownOwnedTownBlocks(town, firstWorldCoord);
			// The number of adjacement TBs is not enough and there is not a nearby outpost.
			if (numAdjacent < minAdjacentBlocks && numAdjacentOutposts(town, firstWorldCoord) == 0)
				throw new TownyException(Translatable.of("msg_min_adjacent_blocks", minAdjacentBlocks, numAdjacent));
		}
		
		TownyMessaging.sendDebugMsg("townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
		
		// When not claiming an outpost, make sure at least one of the selection is attached to a claimed plot.
		if (!outpost && !isEdgeBlock(town, selection) && !town.getTownBlocks().isEmpty())
			throw new TownyException(Translatable.of("msg_err_not_attached_edge"));
						
		/*
		 * Allow other plugins to have a say in whether the claim is allowed.
		 */
		int blockedClaims = 0;

		String cancelMessage = "";
		boolean isHomeblock = town.getTownBlocks().size() == 0;
		for (WorldCoord coord : selection) {
			//Use the user's current world
			TownPreClaimEvent preClaimEvent = new TownPreClaimEvent(town, new TownBlock(coord.getX(), coord.getZ(), world), player, outpost, isHomeblock);
			if(BukkitTools.isEventCancelled(preClaimEvent)) {
				blockedClaims++;
				cancelMessage = preClaimEvent.getCancelMessage();
			}
		}

		if (blockedClaims > 0) {
			throw new TownyException(String.format(cancelMessage, blockedClaims, selection.size()));
		}
		
		/*
		 * See if the Town can pay (if required.)
		 */
		if (TownyEconomyHandler.isActive()) {
			final boolean isOutpost = outpost;
			final List<WorldCoord> finalSelection = selection;
			
			final Runnable withdrawAndStart = () -> {
				double blockCost;
				try {
					if (isOutpost)
						blockCost = TownySettings.getOutpostCost();
					else if (finalSelection.size() == 1)
						blockCost = town.getTownBlockCost();
					else
						blockCost = town.getTownBlockCostN(finalSelection.size());

					if (!town.getAccount().canPayFromHoldings(blockCost)) {
						double missingAmount = blockCost - town.getAccount().getHoldingBalance();
						throw new TownyException(Translatable.of("msg_no_funds_claim2", finalSelection.size(), TownyEconomyHandler.getFormattedBalance(blockCost), TownyEconomyHandler.getFormattedBalance(missingAmount), new DecimalFormat("#").format(missingAmount)));
					}

					town.getAccount().withdraw(blockCost, String.format("Town Claim (%d) by %s", finalSelection.size(), player.getName()));
					
					// Start the claiming process after a successful withdraw.
					Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, finalSelection, isOutpost, true, false));
				} catch (NullPointerException e2) {
					TownyMessaging.sendErrorMsg(player, "The server economy plugin " + TownyEconomyHandler.getVersion() + " could not return the Town account!");
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				}
			};
			
			if (TownySettings.isEconomyAsync())
				Bukkit.getScheduler().runTaskAsynchronously(plugin, withdrawAndStart);
			else 
				Bukkit.getScheduler().runTask(plugin, withdrawAndStart);
		} else {
			// Economy isn't enabled, start the claiming process immediately.
			Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, selection, outpost, true, false));
		}
	}

	public static void parseTownUnclaimCommand(Player player, String[] split) throws TownyException {
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TOWN_UNCLAIM.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());

		BukkitTools.ifCancelledThenThrow(new TownPreUnclaimCmdEvent(town, resident, world));

		if (split.length == 1 && split[0].equalsIgnoreCase("all")) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM_ALL.getNode());

			if (TownyEconomyHandler.isActive() && TownySettings.getClaimRefundPrice() < 0) {
				// Unclaiming will cost the player money because of a negative refund price. Have them confirm the cost.
				Confirmation
					.runOnAccept(() -> Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, null, false, false, false))) 
					.setTitle(Translatable.of("confirmation_unclaiming_costs",
						TownyEconomyHandler.getFormattedBalance(Math.abs(TownySettings.getClaimRefundPrice() * town.getTownBlocks().size() - 1))))
					.sendTo(player);
				return;
			}
			// No cost to unclaim the land.
			Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, null, false, false, false));
			return;
		}
		
		/*
		 * We are not unclaiming the entire town.
		 */
		
		// Check permissions here because of the townunclaim mode.
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWN_UNCLAIM.getNode());
		
		// Prevent someone manually running /t unclaim world x z (a command which should only be run via /plot claim world x z)
		if (split.length == 3 && TownyAPI.getInstance().getTownyWorld(split[0]) != null)
			throw new TownyException(Translatable.of("tc_err_invalid_command"));
		
		List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), Coord.parseCoord(plugin.getCache(player).getLastLocation())), split);
		selection = AreaSelectionUtil.filterOwnedBlocks(town, selection);
		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		if (selection.get(0).getTownBlock().isHomeBlock())
			throw new TownyException(Translatable.of("msg_err_cannot_unclaim_homeblock"));
		
		if (AreaSelectionUtil.filterHomeBlock(town, selection)) {
			// Do not stop the entire unclaim, just warn that the homeblock cannot be unclaimed
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_cannot_unclaim_homeblock"));
		}
		
		// Handle a negative unclaim refund (yes, where someone is being charged money to unclaim their land. It's a thing.)
		if (TownyEconomyHandler.isActive() && TownySettings.getClaimRefundPrice() < 0) {
			double cost = Math.abs(TownySettings.getClaimRefundPrice() * selection.size());
			if (!town.getAccount().canPayFromHoldings(cost)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", TownyEconomyHandler.getFormattedBalance(cost)));
				return;
			}
			List<WorldCoord> finalSelection = selection;
			Confirmation.runOnAccept(()-> {
				if (!town.getAccount().canPayFromHoldings(cost)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_your_town_cannot_afford_unclaim", TownyEconomyHandler.getFormattedBalance(cost)));
					return;
				}
				// Set the area to unclaim
				Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, finalSelection, false, false, false));
			})
			.setTitle(Translatable.of("confirmation_unclaiming_costs", TownyEconomyHandler.getFormattedBalance(cost)))
			.sendTo(player);
			return;
		}
		// Set the area to unclaim without a unclaim refund.
		Bukkit.getScheduler().runTask(plugin, new TownClaim(plugin, player, town, selection, false, false, false));
	}
	
	public static void parseTownMergeCommand(Player player, String[] args) throws TownyException {
		parseTownMergeCommand(player, args, getTownFromPlayerOrThrow(player), false);
	}

	public static void parseTownMergeCommand(CommandSender sender, String[] args, @NotNull Town remainingTown, boolean admin) throws TownyException {

		if (args.length <= 0) // /t merge
			throw new TownyException(Translatable.of("msg_specify_name"));

		if (!admin && sender instanceof Player player && !getResidentOrThrow(player).isMayor())
			throw new TownyException(Translatable.of("msg_town_merge_err_mayor_only"));

		Town succumbingTown = getTownOrThrow(args[0]);

		// A lot of checks.
		if (succumbingTown.getName().equals(remainingTown.getName()))
			throw new TownyException(Translatable.of("msg_err_invalid_name", args[0]));

		if (TownySettings.getMaxDistanceForTownMerge() > 0 && homeBlockDistance(remainingTown, succumbingTown) > TownySettings.getMaxDistanceForTownMerge())
			throw new TownyException(Translatable.of("msg_town_merge_err_not_close", succumbingTown.getName(), TownySettings.getMaxDistanceForTownMerge()));

		int newResidentsAmount = remainingTown.getNumResidents() + succumbingTown.getNumResidents();

		if (TownySettings.getMaxResidentsPerTown() > 0 && 
			newResidentsAmount > TownySettings.getMaxResidentsForTown(remainingTown))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_residents", TownySettings.getMaxResidentsForTown(remainingTown)));

		if (!remainingTown.hasUnlimitedClaims() && 
			(remainingTown.getNumTownBlocks() + succumbingTown.getNumTownBlocks()) > TownySettings.getMaxTownBlocks(remainingTown, newResidentsAmount))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_townblocks", TownySettings.getMaxTownBlocks(remainingTown, newResidentsAmount)));

		if ((remainingTown.getPurchasedBlocks() + succumbingTown.getPurchasedBlocks()) > TownySettings.getMaxPurchasedBlocks(remainingTown, newResidentsAmount))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_purchased_townblocks", TownySettings.getMaxPurchasedBlocks(remainingTown, newResidentsAmount)));

		if (TownySettings.isAllowingOutposts() &&
			TownySettings.isOutpostsLimitedByLevels() && 
			(remainingTown.getMaxOutpostSpawn() + succumbingTown.getMaxOutpostSpawn()) > TownySettings.getMaxOutposts(remainingTown, newResidentsAmount))
			throw new TownyException(Translatable.of("msg_town_merge_err_too_many_outposts", TownySettings.getMaxOutposts(remainingTown, newResidentsAmount)));

		if (!BukkitTools.isOnline(succumbingTown.getMayor().getName()) || succumbingTown.getMayor().isNPC())
			throw new TownyException(Translatable.of("msg_town_merge_other_offline", succumbingTown.getName(), succumbingTown.getMayor().getName()));

		double baseCost = TownySettings.getBaseCostForTownMerge();
		double townblockCost = 0;
		double bankruptcyCost = 0;
		double cost = 0;
		if (!admin && TownyEconomyHandler.isActive()) {
			townblockCost = remainingTown.getTownBlockCostN(succumbingTown.getNumTownBlocks()) * (TownySettings.getPercentageCostPerPlot() * 0.01);
			if (succumbingTown.isBankrupt())
				bankruptcyCost = Math.abs(succumbingTown.getAccount().getHoldingBalance());
			
			cost = baseCost + townblockCost + bankruptcyCost;

			if (!remainingTown.getAccount().canPayFromHoldings(cost))
				throw new TownyException(Translatable.of("msg_town_merge_err_not_enough_money", TownyEconomyHandler.getFormattedBalance(remainingTown.getAccount().getHoldingBalance()), TownyEconomyHandler.getFormattedBalance(cost)));
		}
		
		if (cost > 0) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_warning", succumbingTown.getName(), TownyEconomyHandler.getFormattedBalance(cost)));
			if (bankruptcyCost > 0)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_debt_warning", succumbingTown.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_cost_breakdown", TownyEconomyHandler.getFormattedBalance(baseCost), TownyEconomyHandler.getFormattedBalance(townblockCost), TownyEconomyHandler.getFormattedBalance(bankruptcyCost)));
			final Town finalSuccumbingTown = succumbingTown;
			final Town finalRemainingTown = remainingTown;
			final double finalCost = cost;
			Confirmation.runOnAccept(() -> {
				sendTownMergeRequest(sender, finalRemainingTown, finalSuccumbingTown, finalCost);
			}).runOnCancel(() -> {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_cancelled"));
				return;
			}).sendTo(sender);
		} else
			sendTownMergeRequest(sender, remainingTown, succumbingTown, cost);
	}

	private static void sendTownMergeRequest(CommandSender sender, Town remainingTown, Town succumbingTown, double cost) {
		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_request_sent", succumbingTown.getName()));
		TownyMessaging.sendMsg(succumbingTown.getMayor(), Translatable.of("msg_town_merge_request_received", remainingTown.getName(), sender.getName(), remainingTown.getName()));

		Confirmation.runOnAccept(() -> {
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

		// TODO: Better algorithm that doesn't duplicates checks.
		for (WorldCoord worldCoord : worldCoords)
			if (isEdgeBlock(owner, worldCoord))
				return true;
		return false;
	}

	public static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {

		for (WorldCoord wc : worldCoord.getCardinallyAdjacentWorldCoords()) {
			if (wc.isWilderness())
				continue;
			if (!wc.getTownBlockOrNull().isOwner(owner))
				continue;
			return true;
		}
		return false;
	}

	public static int numAdjacentTownOwnedTownBlocks(Town town, WorldCoord worldCoord) {
		return (int) worldCoord.getCardinallyAdjacentWorldCoords(true).stream()
			.filter(wc -> wc.hasTown(town))
			.count();
	}

	public static int numAdjacentOutposts(Town town, WorldCoord worldCoord) {
		return (int) worldCoord.getCardinallyAdjacentWorldCoords(true).stream()
			.filter(wc -> wc.hasTown(town))
			.map(wc -> wc.getTownBlockOrNull())
			.filter(TownBlock::isOutpost)
			.count();
	}

	public static List<Resident> getValidatedResidentsForInviteRevoke(Object sender, String[] names, Town town) {
		List<Resident> toRevoke = new ArrayList<>();
		for (Invite invite : town.getSentInvites()) {
			for (String name : names) {
				if (invite.getReceiver().getName().equalsIgnoreCase(name)) {
					Resident revokeRes = TownyUniverse.getInstance().getResident(name);
					if (revokeRes != null) {
						toRevoke.add(revokeRes);
					}
				}
			}
			
		}
		return toRevoke;		
	}
	
	private static void townTransaction(Player player, String[] args, boolean withdraw) {
		if (TownySettings.isEconomyAsync() && Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> townTransaction(player, args, withdraw));
			return;
		}

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
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(town, sender)));
	}

	private void townResList(CommandSender sender, String[] args) throws TownyException {

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
		
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(town.getName() + " " + Translatable.of("res_list").forLocale(sender)));
		TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(Translatable.of("res_list").forLocale(sender), new ArrayList<>(town.getResidents())));
	}
	
	private void townPlotGroupList(CommandSender sender, String[] args) throws TownyException {
		// args: plotgrouplist townname pagenumber 
		
		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;
		
		if (player != null)
			catchRuinedTown(player);

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
		
		if (args.length < 1
			|| args.length < 2 && (args[0].equalsIgnoreCase("add") || args[0].equalsIgnoreCase("remove"))
			|| args.length == 1 && !args[0].equalsIgnoreCase("list")) {
			HelpMenu.TOWN_TRUST_HELP.send(sender);
			return;
		}
		
		if (town == null && sender instanceof Player player)
			town = getTownFromPlayerOrThrow(player);

		if (args[0].equalsIgnoreCase("list")) {
			List<String> output = town.getTrustedResidents().isEmpty()
					? Collections.singletonList(Translatable.of("status_no_town").forLocale(sender)) // String which is "None".
					: town.getTrustedResidents().stream().map(res -> res.getName()).collect(Collectors.toList());
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedStrings(Translatable.of("status_trustedlist").forLocale(sender), output));
			return;
		}

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TRUST.getNode());

		Resident resident = TownyAPI.getInstance().getResident(args[1]);
		if (resident == null || resident.isNPC()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", args[1]));
			return;
		}
		
		if (args[0].equalsIgnoreCase("add")) {
			if (town.hasTrustedResident(resident)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_already_trusted", resident.getName(), Translatable.of("town_sing")));
				return;
			}

			BukkitTools.ifCancelledThenThrow(new TownTrustAddEvent(sender, resident, town));

			town.addTrustedResident(resident);
			plugin.deleteCache(resident);
			
			TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_added", resident.getName(), Translatable.of("town_sing")));
			if (BukkitTools.isOnline(resident.getName()))
				TownyMessaging.sendMsg(resident.getPlayer(), Translatable.of("msg_trusted_added_2", sender instanceof Player player ? player.getName() : "Console", Translatable.of("town_sing"), town.getName()));
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (!town.hasTrustedResident(resident)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_trusted", resident.getName(), Translatable.of("town_sing")));
				return;
			}

			BukkitTools.ifCancelledThenThrow(new TownTrustRemoveEvent(sender, resident, town));

			town.removeTrustedResident(resident);
			plugin.deleteCache(resident);
			
			TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_removed", resident.getName(), Translatable.of("town_sing")));
			if (BukkitTools.isOnline(resident.getName()))
				TownyMessaging.sendMsg(resident.getPlayer(), Translatable.of("msg_trusted_removed_2", sender instanceof Player player ? player.getName() : "Console", Translatable.of("town_sing"), town.getName()));
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", args[0]));
			return;
		}
		
		town.save();
	}

	public static void parseTownTrustTownCommand(CommandSender sender, String[] args, @Nullable Town town) throws TownyException {

		if (args.length < 1
			|| args.length < 2 && ("add".equalsIgnoreCase(args[0]) || "remove".equalsIgnoreCase(args[0]))
			|| args.length == 1 && !"list".equalsIgnoreCase(args[0])) {
			HelpMenu.TOWN_TRUSTTOWN_HELP.send(sender);
			return;
		}
		
		if (town == null && sender instanceof Player player) {
			town = getTownFromPlayerOrThrow(player);
		}
		if ("list".equalsIgnoreCase(args[0])) {
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_trustedlist").forLocale(sender), new ArrayList<>(town.getTrustedTowns())));
			return;
		}

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWN_TRUSTTOWN.getNode());

		Town trustTown = getTownOrThrow(args[1]);

		if (args[0].equalsIgnoreCase("add")) {
			if (town.hasTrustedTown(trustTown)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_already_trusted", trustTown.getName(), Translatable.of("town_sing")));
				return;
			}
			else if (town == trustTown) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_already_trusted", trustTown.getName(), Translatable.of("town_sing")));
				return;
			}
			BukkitTools.ifCancelledThenThrow(new TownTrustTownAddEvent(sender, trustTown, town));
			@Nullable Town finalTown = town;
			Confirmation.runOnAccept(()-> {
					trustTown.getResidents().forEach(res -> plugin.deleteCache(res));

					TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_added", trustTown.getName(), Translatable.of("town_sing")));
					finalTown.addTrustedTown(trustTown);
				})
				.setTitle(Translatable.of("confirmation_msg_trusttown_consequences"))
				.sendTo(sender);
		} else if (args[0].equalsIgnoreCase("remove")) {
			if (!town.hasTrustedTown(trustTown)) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_trusted", trustTown.getName(), Translatable.of("town_sing")));
				return;
			}
			BukkitTools.ifCancelledThenThrow(new TownTrustTownRemoveEvent(sender, trustTown, town));
			town.removeTrustedTown(trustTown);
			trustTown.getResidents().forEach(res -> plugin.deleteCache(res));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_trusted_removed", trustTown.getName(), Translatable.of("town_sing")));
		} else {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", args[0]));
			return;
		}

		town.save();
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
		try {
			return (int) Math.sqrt((Math.abs(town1.getHomeBlock().getX() - town2.getHomeBlock().getX())^2) + (Math.abs(town1.getHomeBlock().getZ() - town2.getHomeBlock().getZ())^2));
		} catch (TownyException e) {
			return Integer.MAX_VALUE;
		}
	}
}
