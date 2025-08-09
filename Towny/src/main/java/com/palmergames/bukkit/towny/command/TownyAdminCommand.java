package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.conversation.SetupConversation;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownAddResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownRemoveResidentRankEvent;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.economy.NationTransactionEvent;
import com.palmergames.bukkit.towny.event.economy.TownTransactionEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.exceptions.NoPermissionException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.adapter.EconomyAdapter;
import com.palmergames.bukkit.towny.object.economy.provider.EconomyProvider;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.BackupTask;
import com.palmergames.bukkit.towny.tasks.NewDayScheduler;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.conversations.ConversationContext;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private final Towny plugin;
	
	private static final List<String> adminTabCompletes = Arrays.asList(
		"plot",
		"resident",
		"town",
		"nation",
		"reset",
		"toggle",
		"set",
		"givebonus",
		"reload",
		"backup",
		"checkperm",
		"checkoutposts",
		"newday",
		"newhour",
		"unclaim",
		"purge",
		"mysqldump",
		"tpplot",
		"database",
		"townyperms",
		"install",
		"eco"
	);

	private static final List<String> adminTownTabCompletes = Arrays.asList(
		"new",
		"add",
		"kick",
		"rename",
		"spawn",
		"tpplot",
		"outpost",
		"delete",
		"rank",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"outlaw",
		"leavenation",
		"invite",
		"unruin",
		"trust",
		"trusttown",
		"checkoutposts",
		"settownlevel",
		"giveboughtblocks",
		"merge",
		"forcemerge",
		"forsale",
		"fs",
		"notforsale",
		"nfs"
	);

	private static final List<String> adminTownSetTabCompletes = Stream.concat(TownCommand.townSetTabCompletes.stream(),
		Stream.of("foundingdate")).collect(Collectors.toList());
	
	private static final List<String> adminTownToggleTabCompletes = Stream.concat(TownCommand.townToggleTabCompletes.stream(),
			Arrays.asList("forcemobs", "forcepvp", "forcedisablepvp", "unlimitedclaims", "upkeep", "allowedtowar", "conquered", "visibleontoplists").stream()).collect(Collectors.toList());

	private static final List<String> adminNationTabCompletes = Arrays.asList(
		"add",
		"kick",
		"meta",
		"rename",
		"delete",
		"toggle",
		"sanctiontown",
		"set",
		"meta",
		"deposit",
		"withdraw",
		"bankhistory",
		"rank",
		"enemy",
		"ally",
		"merge",
		"transfer",
		"forcemerge",
		"recheck"
	);
	private static final List<String> adminNationSetTabCompletes = Stream.concat(NationCommand.nationSetTabCompletes.stream(),
		Stream.of("foundingdate")).collect(Collectors.toList());
	private static final List<String> adminToggleTabCompletes = Arrays.asList(
		"wildernessuse",
		"regenerations",
		"neutral",
		"npc",
		"debug",
		"devmode",
		"townwithdraw",
		"nationwithdraw"
	);
	
	private static final List<String> adminPlotTabCompletes = Arrays.asList(
		"claim",
		"meta",
		"claimedat",
		"trust"
	);
	
	private static final List<String> adminMetaTabCompletes = Arrays.asList(
		"set",
		"add",
		"remove"
	);
	
	private static final List<String> adminDatabaseTabCompletes = Arrays.asList(
		"save",
		"load",
		"remove"
	);
	
	private static final List<String> adminResidentTabCompletes = Arrays.asList(
		"rename",
		"friend",
		"meta",
		"unjail",
		"delete"
	);
	
	private static final List<String> adminResidentFriendTabCompletes = Arrays.asList(
		"add",
		"remove",
		"list",
		"clear"
	);
	
	private static final List<String> adminSetCompletes = Arrays.asList(
		"about",
		"mayor",
		"capital",
		"title",
		"founder",
		"surname",
		"nationzoneoverride",
		"plot"
	);
	
	private static final List<String> adminAboutTabCompletes = Arrays.asList(
		"reset",
		"none",
		"clear"
	);
	
	private static final List<String> adminTownyPermsCompletes = Arrays.asList(
		"listgroups",
		"group",
		"townrank",
		"nationrank"
	);
	
	private static final List<String> adminReloadTabCompletes = Arrays.asList(
		"database",
		"db",
		"config",
		"perms",
		"permissions",
		"language",
		"lang",
		"townyperms",
		"all"
	);
	
	private static final List<String> adminEcoTabCompletes = Arrays.asList(
		"convert",
		"depositall",
		"depositalltowns",
		"depositallnations",
		"info",
		"resetbanks"
	);

	private static final List<String> adminEcoInfoTabCompletes = Arrays.asList(
			"nation",
			"resident",
			"serveraccount",
			"town"
		);
	
	public TownyAdminCommand(Towny towny) {
		this.plugin = towny;
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {

		try {
			parseTownyAdminCommand(sender, args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "reload":
				if (args.length > 1)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_RELOAD, adminReloadTabCompletes), args[1]);
			case "purge":
				if (args.length == 3)
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("townless"), args[2], "+t");
				break;
			case "set":
				if (args.length > 1) {
					switch (args[1].toLowerCase(Locale.ROOT)) {
						case "mayor":
							switch (args.length) {
								case 3:
									return getTownyStartingWith(args[2], "t");
								case 4:
									return filterByStartOrGetTownyStartingWith(Collections.singletonList("npc"), args[3], "+r");
								default:
									return Collections.emptyList();
							}
						case "capital":
						case "founder":
						case "nationzoneoverride":
						case "plot":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "t");
						case "about":
						case "title":
						case "surname":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "r");
							if (args.length == 4 && args[1].equalsIgnoreCase("about"))
								return NameUtil.filterByStart(adminAboutTabCompletes, args[args.length - 1]);
						default:
							if (args.length == 2)
								return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_SET, adminSetCompletes), args[1]);
							else if (args.length > 2 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_SET, args[1]))
								return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
					}
				}
				break;
			case "plot":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminPlotTabCompletes, args[1]);
				} else if (args.length > 2) {
					switch (args[1].toLowerCase(Locale.ROOT)) {
						case "claim":
							return getTownyStartingWith(args[2], "r");
						case "meta":
							if (args.length == 3)
								return NameUtil.filterByStart(adminMetaTabCompletes, args[2]);
						case "trust":
							if (args.length == 3)
								return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[2]);
							if (args.length == 4)
								return getTownyStartingWith(args[3], "r");
						default:
							return Collections.emptyList();
					}
				}
				break;
			case "givebonus":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "rt");
				break;
			case "toggle":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_TOGGLE, adminToggleTabCompletes), args[1]);
				} else if (args.length >= 3 && args[1].equalsIgnoreCase("npc")) {
					if (args.length == 3) {
						return getTownyStartingWith(args[2], "r");
					} else if (args.length == 4) {
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[3]);
					}
				} else if (args.length == 3) {
					return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
				}
				break;
			case "tpplot":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyUniverse.getInstance().getTownyWorlds()
						.stream()
						.map(TownyWorld::getName)
						.collect(Collectors.toList()), args[1]);
				}
				break;
			case "checkperm":
				if (args.length == 2)
					return NameUtil.filterByStart(BukkitTools.getVisibleOnlinePlayers(sender)
						.stream()
						.map(Player::getName)
						.collect(Collectors.toList()), args[1]);
				if (args.length == 3)
					return NameUtil.filterByStart(BukkitTools.getPluginManager().getPermissions()
						.stream()
						.map(Permission::getName)
						.collect(Collectors.toList()), args[2]);
			case "database":
				if (args.length == 2)
					return NameUtil.filterByStart(adminDatabaseTabCompletes, args[1]);
				if (args.length == 3 && args[1].equalsIgnoreCase("remove"))
					return Collections.singletonList("titles");
				break;
			case "resident":
				switch (args.length) {
					case 2:
						return getTownyStartingWith(args[1], "r");
					case 3:
						return NameUtil.filterByStart(adminResidentTabCompletes, args[2]);
					case 4:
						if (args[2].equalsIgnoreCase("about"))
							return Collections.singletonList("clear");
						if (args[2].equalsIgnoreCase("friend"))
							return NameUtil.filterByStart(adminResidentFriendTabCompletes, args[3]);
						if (args[2].equalsIgnoreCase("meta"))
							return NameUtil.filterByStart(adminMetaTabCompletes, args[3]);
					default:
						return Collections.emptyList();
				}
			case "town":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+t");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase(Locale.ROOT)) {
						case "add":
							if (args.length == 4)
								return null;
						case "kick":
							if (args.length == 4)
								return getResidentsOfTownStartingWith(args[1], args[3]);
						case "rank":
							switch (args.length) {
								case 4:
									return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
								case 5:
									return getResidentsOfTownStartingWith(args[1], args[4]);
								case 6:
									switch (args[3].toLowerCase(Locale.ROOT)) {
										case "add":
											return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[5]);
										case "remove": {
											Resident res = TownyUniverse.getInstance().getResident(args[4]);
											if (res != null)
												return NameUtil.filterByStart(res.getTownRanks(), args[5]);
											break;
										}
										default:
											return Collections.emptyList();
									}
								default:
									return Collections.emptyList();
							}
						case "set": {
							final Town town = TownyUniverse.getInstance().getTown(args[1]);
							if (town != null) {
								if (args.length == 4) {
									return NameUtil.filterByStart(adminTownSetTabCompletes, args[3]);
                                }
                            }
							break;
						}
						case "toggle":
							if (args.length == 4)
								return NameUtil.filterByStart(adminTownToggleTabCompletes, args[3]);
							else if (args.length == 5 && !args[3].equalsIgnoreCase("jail"))
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "outlaw":
							switch (args.length) {
							case 4:
								return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
							case 5:
								switch (args[3].toLowerCase(Locale.ROOT)) {
									case "add":
										return getTownyStartingWith(args[4], "r");
									case "remove": {
										final Town town = TownyUniverse.getInstance().getTown(args[1]);
										
										if (town != null)
											return NameUtil.filterByStart(NameUtil.getNames(town.getOutlaws()), args[4]);
										
										break;
									}
									default:
										return Collections.emptyList();
								}
							default:
								return Collections.emptyList();
							}
						case "invite":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "r");
						case "meta":
							if (args.length == 4) {
								return NameUtil.filterByStart(adminMetaTabCompletes, args[3]);
							}
							break;
						case "giveboughtblocks":
						case "settownlevel":
							if (args.length == 4)
								return NameUtil.filterByStart(StringMgmt.addToList(numbers, "unset"), args[3]);
							break;
						case "trust":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add", "remove"), args[3]);
							if (args.length == 5)
								return getTownyStartingWith(args[4], "r");
						case "merge", "forcemerge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_TOWN, adminTownTabCompletes), args[2]);
							else if (args.length > 3 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_TOWN, args[2]))
								return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_TOWN, args[2]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "r");
				}
				break;
			case "nation":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+n");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					Nation nation = TownyUniverse.getInstance().getNation(args[1]);
					if (nation == null)
						return Collections.emptyList();
					switch (args[2].toLowerCase(Locale.ROOT)) {
						case "add":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "toggle":
							if (args.length == 4) 
								return NameUtil.filterByStart(NationCommand.nationToggleTabCompletes, args[3]);
							else if (args.length == 5)
								return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[4]);
						case "set": {
							if (args.length == 4) {
								return NameUtil.filterByStart(adminNationSetTabCompletes, args[3]);
							}
						}
						case "meta":
							if (args.length == 4) {
								return NameUtil.filterByStart(adminMetaTabCompletes, args[3]);
							}
							break;
						case "merge", "forcemerge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "n");
						case "transfer":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "rank":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add","remove"), args[3]);
							else if (args.length == 5)
								return getTownyStartingWith(args[4], "r");
							else if (args.length == 6)
								return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[5]);
						case "sanctiontown":
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("add","remove","list"), args[3]);
							if (args.length == 5 && args[3].equalsIgnoreCase("add") || args[4].equalsIgnoreCase("remove"))
								return NameUtil.filterByStart(TownyUniverse.getInstance().getTowns()
										.stream()
										.filter(t -> !nation.hasTown(t))
										.map(Town::getName)
										.collect(Collectors.toList()), args[4]);
							break;
						case "enemy":
						case "ally":
							if (args.length == 4)
								return Arrays.asList("add", "remove");
							if (args.length == 5)
								return getTownyStartingWith(args[4], "n");
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN_NATION, adminNationTabCompletes), args[2]);
							else if (args.length > 3 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_NATION, args[2]))
								return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_NATION, args[2]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "t");
				}
				break;
			case "unclaim":
				if (args.length == 2)
					return NameUtil.filterByStart(TownCommand.townUnclaimTabCompletes, args[1]);
			case "townyperms":
				if (args.length == 2)
					return NameUtil.filterByStart(adminTownyPermsCompletes, args[1]);
				if (args.length > 2) {
					switch (args[1].toLowerCase(Locale.ROOT)) {
						case "group":
							if (args.length == 3)
								return NameUtil.filterByStart(TownyPerms.getGroupList(), args[2]);
							if (args.length == 4)
								return NameUtil.filterByStart(Arrays.asList("addperm","removeperm"), args[3]);
							break;
						case "townrank":
						case "nationrank":
							if (args.length == 3) 
								return NameUtil.filterByStart(Arrays.asList("addrank","removerank", "renamerank"), args[2]);
							if (args.length == 4 && (args[2].equalsIgnoreCase("removerank") || args[2].equalsIgnoreCase("renamerank"))) {
								if (args[1].equalsIgnoreCase("nationrank"))
									return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[3]);
								if (args[1].equalsIgnoreCase("townrank"))
									return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[3]);
							}
							break;
						default:
							return Collections.emptyList();
					}
				}
				break;
			case "eco":
				if (args.length == 2)
					return NameUtil.filterByStart(adminEcoTabCompletes, args[1]);
				
				if (args.length > 2) {
					if ("convert".equalsIgnoreCase(args[1])) {
						List<String> convertChoices = new ArrayList<>();
						if (!TownyEconomyHandler.isActive())
							return convertChoices;
						
						for (EconomyAdapter adapter : TownyEconomyHandler.getProvider().economyAdapters())
							convertChoices.add(adapter.name());
						
						EconomyAdapter active = TownyEconomyHandler.activeAdapter();
						if (active != null)
							convertChoices.remove(active.name());
						
						if (TownyEconomyHandler.getProvider().isLegacy() && args.length < 4)
							convertChoices.add("modern");
						
						return NameUtil.filterByStart(convertChoices, args[2]);
					}
					if ("info".equalsIgnoreCase(args[1])) {
						if (args.length == 3)
							return NameUtil.filterByStart(adminEcoInfoTabCompletes, args[2]);
						if (args.length == 4) {
							switch (args[2].toLowerCase(Locale.ROOT)) {
							case "nation": return BaseCommand.getTownyStartingWith(args[3], "n");
							case "resident": return BaseCommand.getTownyStartingWith(args[3], "r");
							case "town": return BaseCommand.getTownyStartingWith(args[3], "t");
							default: return Collections.emptyList();
							}
						}
					}
				}
				
				break;
			default:
				if (args.length == 1)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNYADMIN, adminTabCompletes), args[0]);
				else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN, args[0]).getTabCompletion(sender, args), args[args.length-1]);
		}
		
		return Collections.emptyList();
	}

	public void parseTownyAdminCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SCREEN.getNode());
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.TA_HELP.send(sender);
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "set" -> adminSet(sender, StringMgmt.remFirstArg(split));
		case "resident" -> parseAdminResidentCommand(sender, StringMgmt.remFirstArg(split));
		case "town" -> parseAdminTownCommand(sender, StringMgmt.remFirstArg(split));
		case "nation" -> parseAdminNationCommand(sender, StringMgmt.remFirstArg(split));
		case "toggle" -> parseToggleCommand(sender, StringMgmt.remFirstArg(split));
		case "plot" -> parseAdminPlotCommand(catchConsole(sender), StringMgmt.remFirstArg(split));
		case "givebonus", "giveplots" -> giveBonus(sender, StringMgmt.remFirstArg(split));
		case "reload" -> parseAdminReload(sender, StringMgmt.remFirstArg(split));
		case "reset" -> parseAdminReset(sender);
		case "backup" -> parseAdminBackup(sender);
		case "database" -> parseAdminDatabaseCommand(sender, StringMgmt.remFirstArg(split));
		case "mysqldump" -> parseAdminMySQLDump(sender);
		case "newday" -> parseAdminNewDay(sender);
		case "newhour" -> parseAdminNewHour(sender);
		case "purge" -> purge(sender, StringMgmt.remFirstArg(split));
		case "unclaim" -> parseAdminUnclaimCommand(catchConsole(sender), StringMgmt.remFirstArg(split));
		case "checkperm" -> parseAdminCheckPermCommand(sender, StringMgmt.remFirstArg(split));
		case "checkoutposts" -> parseAdminCheckOutpostsCommand(sender, null);
		case "townyperms" -> parseAdminTownyPermsCommand(sender, StringMgmt.remFirstArg(split));
		case "tpplot" -> parseAdminTpPlotCommand(catchConsole(sender), StringMgmt.remFirstArg(split));
		case "eco" -> parseAdminEcoCommand(sender, StringMgmt.remFirstArg(split));
		case "install" -> parseAdminInstall(sender);
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN, split[0]).execute(sender, "townyadmin", split);
				return;
			}
			HelpMenu.TA_HELP.send(sender);
		}}
	}

	private boolean parseBoolean(Object object) {
		if (!(object instanceof String string))
			return false;

		return switch (string.toLowerCase(Locale.ROOT)) {
			case "y", "yes", "true" -> true;
			default -> false;
		};
	}

	private void parseAdminTownyPermsCommand(CommandSender sender, String[] args) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWNYPERMS.getNode());
		if (args.length == 0 || args[0].equalsIgnoreCase("?") || !adminTownyPermsCompletes.contains(args[0])) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}
		switch (args[0]) {
		case "listgroups":
			// Send an alphabetized list of ranks.
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Groups"));
			TownyMessaging.sendMessage(sender, ChatTools.list(TownyPerms.getGroupList().stream().sorted().collect(Collectors.toList())));
			break;
		case "group":
			// Alter a groups permission nodes.
			parseAdminTownypermsGroupCommand(sender, StringMgmt.remFirstArg(args));
			break;
		case "townrank":
		case "nationrank":
			// Create and remove town and nation ranks.
			parseAdminTownypermsRankCommand(sender, args);
			break;
		default:
		}
	}

	private void parseAdminTownypermsGroupCommand(CommandSender sender, String[] args) throws TownyException {
		//ta townyperms group GROUPNAME add|remove NODE
		//                    ^ args[0]
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}
		if (!TownyPerms.getGroupList().contains(args[0].toLowerCase(Locale.ROOT)))
			throw new TownyException(Translatable.of("msg_err_group_not_found", args[0]));

		String group = args[0];
		List<String> groupNodes = TownyPerms.getPermsOfGroup(group);

		// /ta townyperms group GROUPNAME: display nodes held by group.
		if (args.length == 1) {
			displayNodesHelpByGroup(sender, group, groupNodes);
			return;
		}
		
		if ((!args[1].equalsIgnoreCase("addperm") && !args[1].equalsIgnoreCase("removeperm")) || 
			args.length != 3)
			throw new TownyException(Translatable.of("msg_err_expected_command_format", "/ta townyperms group add|remove node"));
		
		boolean add = args[1].equalsIgnoreCase("addperm");
		String node = args[2];
		boolean changed = false;
		switch (args[1].toLowerCase(Locale.ROOT)) {
		case "addperm":
			if (groupNodes.contains(node))
				throw new TownyException(Translatable.of("msg_err_group_already_has_node", group, node));
			changed = groupNodes.add(node);
			break;
		case "removeperm":
			if (!groupNodes.contains(node))
				throw new TownyException(Translatable.of("msg_err_group_doesnt_have_node", group, node));
			changed = groupNodes.remove(node);
			break;
		default:
		}
		
		if (!changed)
			return;
		
		TownyPerms.getTownyPermsFile().set(group, groupNodes);
		TownyPerms.getTownyPermsFile().save();
		if (add)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_added_node_to_group", node, group));
		else 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_removed_node_from_group", node, group));
		
		reloadPerms(sender);
	}
	
	private void displayNodesHelpByGroup(CommandSender sender, String group, List<String> groupNodes) {
		if (groupNodes.size() > 0) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle(Translatable.of("msg_title_group_permissions", StringMgmt.capitalize(group)).forLocale(sender)));
			for (String node : groupNodes)
				TownyMessaging.sendMessage(sender, " - " + node);

		} else {
			TownyMessaging.sendErrorMsg(Translatable.of("msg_err_group_has_no_nodes", group));
		}
	}

	private void parseAdminTownypermsRankCommand(CommandSender sender, String[] args) throws TownyException {
		//ta townyperms townrank|nationrank addrank|removerank|renamerank RANKNAME
		//              ^ args[0]
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWNYPERMS.send(sender);
			return;
		}

		if (args.length < 3
			|| (!args[0].equalsIgnoreCase("townrank") && !args[0].equalsIgnoreCase("nationrank"))
			|| (!args[1].equalsIgnoreCase("addrank") && !args[1].equalsIgnoreCase("removerank") && !args[1].equalsIgnoreCase("renamerank"))
			|| ((args[1].equalsIgnoreCase("addrank") || args[1].equalsIgnoreCase("removerank")) && args.length != 3)
			|| (args[1].equalsIgnoreCase("renamerank") && args.length != 4))
			throw new TownyException(Translatable.of("msg_err_expected_command_format", "/ta townyperms townrank|nationrank addrank|removerank|renamerank [rank]"));

		if (args[1].equalsIgnoreCase("addrank"))
			parseAdminTownypermsAddRank(sender, args[0], args[2], true);
		else if (args[1].equalsIgnoreCase("removerank"))
			parseAdminTownypermsRemoveRank(sender, args[0], args[2], true);
		else
			parseAdminTownyPermsRenameRank(sender, args[0], args[2], args[3]);

		TownyPerms.getTownyPermsFile().save();
		reloadPerms(sender);
	}

	private void parseAdminTownypermsAddRank(CommandSender sender, String rankType, String rank, boolean notify) throws TownyException {
		// rankType will equal "townrank" or "nationrank"
		// Adding to town ranks.
		if (rankType.equalsIgnoreCase("townrank")) {
			if (TownyPerms.getTownRanks().contains(rank))
				throw new TownyException(Translatable.of("msg_err_there_is_already_a_town_or_nationrank_called_x", rankType, rank));
			TownyPerms.getTownyPermsFile().createSection("towns.ranks." + rank);
			
		// Adding to nation ranks.
		} else {
			if (TownyPerms.getNationRanks().contains(rank))
				throw new TownyException(Translatable.of("msg_err_there_is_already_a_town_or_nationrank_called_x", rankType, rank));
			TownyPerms.getTownyPermsFile().createSection("nations.ranks." + rank);
		}
		if (notify)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_add_rank_to_the_town_or_nation_rank", rank, rankType));
	}

	private void parseAdminTownypermsRemoveRank(CommandSender sender, String rankType, String rank, boolean notify) throws TownyException {
		// rankType will equal "townrank" or "nationrank"
		// Removing from town ranks.
		if (rankType.equalsIgnoreCase("townrank")) {
			if (!TownyPerms.getTownRanks().contains(rank))
				throw new TownyException(Translatable.of("msg_err_there_is_no_town_or_nationrank_called_x", rankType, rank));
			TownyPerms.getTownyPermsFile().set("towns.ranks." + rank, null);
			
		// Removing from nation ranks.
		} else {
			if (!TownyPerms.getNationRanks().contains(rank))
				throw new TownyException(Translatable.of("msg_err_there_is_no_town_or_nationrank_called_x", rankType, rank));
			TownyPerms.getTownyPermsFile().set("nations.ranks." + rank, null);
		}
		if (notify)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_removed_rank_from_the_town_or_nation_rank", rank, rankType));
	}
	
	private void parseAdminTownyPermsRenameRank(CommandSender sender, String rankType, String oldRank, String newRank) throws TownyException {

		if (!TownyPerms.mapHasGroup(oldRank))
			throw new TownyException(Translatable.of("msg_err_there_is_no_town_or_nationrank_called_x", rankType, oldRank));
		
		parseAdminTownypermsAddRank(sender, rankType, newRank, false);
		
		// rankType will equal "townrank" or "nationrank"
		// Renaming town rank.
		if (rankType.equalsIgnoreCase("townrank")) {
			// Remove residents from old rank and add them to new rank
			TownyUniverse.getInstance().getResidents()
				.stream()
				.filter(resident -> resident.hasTownRank(oldRank))
				.forEach(resident -> {
					resident.removeTownRank(oldRank);
					resident.addTownRank(newRank);
					resident.save();
				});

			// Transfer permissions from old rank to new rank
			List<String> oldRankPerms = TownyPerms.getPermsOfGroup("towns.ranks." + oldRank);
			List<String> newRankPerms = TownyPerms.getPermsOfGroup("towns.ranks." + newRank);
			newRankPerms.addAll(oldRankPerms);
			TownyPerms.getTownyPermsFile().set("towns.ranks." + newRank, newRankPerms);
			
		// Renaming nation rank.
		} else {
			// Remove residents from old rank and add them to the new rank
			TownyUniverse.getInstance().getResidents()
				.stream()
				.filter(resident -> resident.hasNationRank(oldRank))
				.forEach(resident -> {
					resident.removeNationRank(oldRank);
					resident.addNationRank(newRank);
					resident.save();
				});

			// Transfer permissions from old rank to new rank
			List<String> oldRankPerms = TownyPerms.getPermsOfGroup("nations.ranks." + oldRank);
			List<String> newRankPerms = TownyPerms.getPermsOfGroup("nations.ranks." + newRank);
			newRankPerms.addAll(oldRankPerms);
			TownyPerms.getTownyPermsFile().set("nations.ranks." + newRank, newRankPerms);
		}
		parseAdminTownypermsRemoveRank(sender, rankType, oldRank, false);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_successfully_renamed_rank_for_the_town_or_nation_rank", oldRank, newRank, rankType));
	}

	private void parseAdminReload(CommandSender sender, String[] split) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RELOAD.getNode());
		if (split.length != 1) {
			HelpMenu.TA_RELOAD.send(sender);
			return;
		}
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "db", "database" -> reloadDatabase(sender);
		case "config" -> reloadConfig(sender, false);
		case "perms", "townyperms", "permissions" -> reloadPerms(sender);
		case "language",  "lang" -> reloadLangs(sender);
		case "all" -> {
			// reloadDatabase() already reloads lang & config.
			reloadPerms(sender);
			reloadDatabase(sender);
		}
		default -> {
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_RELOAD, split[0]))
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_RELOAD, split[0]).execute(sender, split);
			else
				HelpMenu.TA_RELOAD.send(sender);
		}}
		Towny.getPlugin().resetCache();
	}

	private void parseAdminReset(CommandSender sender) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESET.getNode());
		Confirmation.runOnAccept(()-> reloadConfig(sender, true))
			.setTitle(Translatable.of("this_will_reset_your_config"))
			.sendTo(sender);
	}

	private void parseAdminBackup(CommandSender sender) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_BACKUP.getNode());
		CompletableFuture.runAsync(new BackupTask())
			.thenRun(()-> TownyMessaging.sendMsg(sender, Translatable.of("mag_backup_success")));
	}

	private void parseAdminDatabaseCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_DATABASE.getNode());
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_DATABASE.send(sender);
			return;
		}
		
		if (split[0].equalsIgnoreCase("save")) {
			if (TownyUniverse.getInstance().getDataSource().saveAll())
				TownyMessaging.sendMsg(sender, Translatable.of("msg_save_success"));
	
		} else if (split[0].equalsIgnoreCase("load")) {
			TownyUniverse.getInstance().clearAllObjects();			
			if (TownyUniverse.getInstance().getDataSource().loadAll()) {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_load_success"));
				BukkitTools.fireEvent(new TownyLoadedDatabaseEvent());
			}
		} else if (split[0].equalsIgnoreCase("remove")) {
			parseAdminDatabaseRemoveCommand(sender, StringMgmt.remFirstArg(split));
		}
	}

	private void parseAdminDatabaseRemoveCommand(CommandSender sender, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_DATABASE.send(sender);
			return;
		}
		
		if (split[0].equalsIgnoreCase("titles")) {
			TownyUniverse.getInstance().getResidents()
				.forEach(resident -> {
					resident.setTitle("");
					resident.setSurname("");
					resident.save();
				});
			TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_removed_all_titles_and_surnames_removed"));
		}
		
	}

	private void parseAdminMySQLDump(CommandSender sender) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_MYSQLDUMP.getNode());
		if (TownySettings.getSaveDatabase().equalsIgnoreCase("mysql") && TownySettings.getLoadDatabase().equalsIgnoreCase("mysql")) {
			TownyDataSource dataSource = new TownyFlatFileSource(plugin, TownyUniverse.getInstance());
			dataSource.saveAll();
			dataSource.finishTasks();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_mysql_dump_success"));
			return;
		}
		throw new TownyException(Translatable.of("msg_err_mysql_not_being_used"));
	}

	private void parseAdminNewDay(CommandSender sender) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NEWDAY.getNode());
		if (NewDayScheduler.isNewDayRunning())
			throw new TownyException(Translatable.of("msg_newday_already_running"));

		// Turn the daily timer scheduler on if it wasn't running already.
		if (!NewDayScheduler.isNewDaySchedulerRunning())
			TownyTimerHandler.toggleDailyTimer(true);
		// Start a newday.
		NewDayScheduler.newDay();
	}

	private void parseAdminNewHour(CommandSender sender) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NEWHOUR.getNode());
		TownyTimerHandler.newHour();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_newhour_success"));
	}

	private void parseAdminPlotCommand(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT.getNode());

		if (split.length < 1 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_PLOT.send(player);
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "meta" -> handlePlotMetaCommand(player, split);
		case "claim" -> parseAdminPlotClaim(player, split);
		case "claimedat" -> parseAdminPlotClaimedAt(player);
		case "trust" -> parseAdminPlotTrust(player, StringMgmt.remFirstArg(split));
		default -> HelpMenu.TA_PLOT.send(player);
		}
	}

	private void parseAdminPlotClaim(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIM.getNode());

		if (split.length == 1)
			throw new TownyException(Translatable.of("msg_error_ta_plot_claim"));

		Optional<Resident> resOpt = TownyUniverse.getInstance().getResidentOpt(split[1]);
		
		if (!resOpt.isPresent())
			throw new TownyException(Translatable.of("msg_error_no_player_with_that_name", split[1]));

		String world = player.getWorld().getName();
		List<WorldCoord> selection = new ArrayList<>();
		selection.add(new WorldCoord(world, Coord.parseCoord(player)));
		plugin.getScheduler().runAsync(new PlotClaim(plugin, player, resOpt.get(), selection, true, true, false));
	}

	private void parseAdminPlotClaimedAt(Player player) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIMEDAT.getNode());

		WorldCoord wc = WorldCoord.parseWorldCoord(player);
		if (!wc.hasTownBlock())
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		
		long claimedAt = wc.getTownBlockOrNull().getClaimedAt(); 
		if (claimedAt == 0)
			throw new TownyException(Translatable.of("msg_err_townblock_has_no_claimedat_data"));

		TownyMessaging.sendMsg(player, Translatable.of("msg_plot_perm_claimed_at").append(" " + TownyFormatter.fullDateFormat.format(claimedAt)));
	}

	private void parseAdminPlotTrust(Player player, String[] split) throws NoPermissionException, TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_TRUST.getNode());

		PlotCommand.parsePlotTrustCommand(player, split);
	}

	private void parseAdminCheckPermCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_CHECKPERM.getNode());
		if (split.length !=2 ) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta checkperm {name} {node}"));
		}
		Player player = BukkitTools.getPlayerExact(split[0]);
		if (player == null) {
			throw new TownyException("Player couldn't be found");
		}
		String node = split[1];
		if (TownyUniverse.getInstance().getPermissionSource().testPermission(player, node))
			TownyMessaging.sendMsg(sender, Translatable.of("msg_perm_true"));
		else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_perm_false"));
	}

	private void parseAdminCheckOutpostsCommand(CommandSender sender, @Nullable Town specificTown) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_CHECKOUTPOSTS.getNode());
		List<Town> towns = new ArrayList<>();
		if (specificTown == null) 
			towns.addAll(TownyAPI.getInstance().getTowns());
		else 
			towns.add(specificTown);
		int removed = 0;
		for (Town town : towns) {
			for (Location loc : town.getAllOutpostSpawns()) {
				boolean save = false;
				if (TownyAPI.getInstance().isWilderness(loc) || !TownyAPI.getInstance().getTown(loc).getUUID().equals(town.getUUID())) {
					town.removeOutpostSpawn(loc);
					save = true;
					removed++;
				}
				if (save)
					town.save();
			}
		}
		TownyMessaging.sendMsg(sender, Translatable.of("msg_removed_x_invalid_outpost_spawns", removed));
	}

	private void parseAdminTpPlotCommand(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TPPLOT.getNode());
		if (split.length != 3) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		}
		
		World world = Bukkit.getWorld(split[0]);

		if (world == null)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: /ta tpplot world x z"));
		
		int x, z;
		try {
			x = Integer.parseInt(split[1]) * TownySettings.getTownBlockSize();
			z = Integer.parseInt(split[2]) * TownySettings.getTownBlockSize();
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_input_must_be_int", "x and z"));
		}
		
		world.getChunkAtAsync(x, z).thenAccept(chunk -> {
			int y = world.getHighestBlockYAt(x, z) + 1;
			
			plugin.getScheduler().run(player, () -> player.teleportAsync(new Location(world, x, y, z)));
		});
	}

	private void giveBonus(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_GIVEBONUS.getNode());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Town town;
		Resident target = null;
		boolean isTown = false;

		if (split.length != 2)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: givebonus [town/player] [n]"));

		if ((town = townyUniverse.getTown(split[0])) != null) {
			isTown = true;
		} else {
			target = getResidentOrThrow(split[0]);

			if (!target.hasTown())
				throw new TownyException(Translatable.of("msg_err_resident_doesnt_belong_to_any_town"));

			town = target.getTownOrNull();
		}
		
		int extraBlocks = MathUtil.getIntOrThrow(split[1]);

		town.setBonusBlocks(town.getBonusBlocks() + extraBlocks);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_give_total", town.getName(), split[1], town.getBonusBlocks()));
		
		boolean isConsole = sender instanceof ConsoleCommandSender;
		if (!isConsole || isTown)
			TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_you_have_been_given_bonus_blocks", extraBlocks)); 
		if (isConsole && !isTown) {
			TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_bonus_blocks", extraBlocks)); 
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_1").forLocale(target));
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_2").forLocale(target));
			TownyMessaging.sendMessage(target, Translatable.of("msg_ptw_warning_3").forLocale(target));
		}
		town.save();

	}

	public void parseAdminUnclaimCommand(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_UNCLAIM.getNode());
		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_UNCLAIM.send(player);
			return;
		} 
		List<WorldCoord> selection;
		selection = AreaSelectionUtil.selectWorldCoordArea(null, new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)), split);
		selection = AreaSelectionUtil.filterOutWildernessBlocks(selection);

		if (selection.isEmpty())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		Town town = selection.get(0).getTownOrNull();
		if (town == null)
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, selection, false, false, true));
	}

	public void parseAdminResidentCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode());
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_RESIDENT.send(sender);
			return;
		}
		Resident resident = getResidentOrThrow(split[0]);

		if (split.length == 1) {
			TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(resident, sender)));
			return;
		}
		
		switch (split[1].toLowerCase(Locale.ROOT)) {
		case "rename" -> residentRename(sender, split, resident);
		case "friend" -> residentFriend(sender, split, resident);
		case "meta" -> handleResidentMetaCommand(sender, resident, split);
		case "unjail" -> residentUnjail(sender, resident);
		case "delete" -> residentDelete(sender, resident);
		case "about" -> residentAbout(sender, split, resident); 
		default -> throw new TownyException(Translatable.of("msg_err_invalid_property", split[1]));
		}
	}

	private void residentAbout(CommandSender sender, String[] split, Resident resident) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_ABOUT.getNode());
		if (split.length < 3 || !split[2].equalsIgnoreCase("clear"))
			throw new TownyException("Eg: /townyadmin resident [resident] about clear");
		resident.setAbout("");
		TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_about", resident.getName()));
	}

	private void residentRename(CommandSender sender, String[] split, Resident resident) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_RENAME.getNode());
		if (split.length != 3)
			throw new TownyException("Eg: /townyadmin resident [resident] rename [newname]");

		String name = NameValidation.checkAndFilterPlayerName(split[2]);
//		if (NameValidation.isBlacklistName(split[2]))
//			throw new TownyException(Translatable.of("msg_invalid_name"));

		TownyUniverse.getInstance().getDataSource().renamePlayer(resident, name);
	}

	private void residentFriend(CommandSender sender, String[] split, Resident resident) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_FRIEND.getNode());
		if (split.length == 2) {
			HelpMenu.TA_RESIDENT_FRIEND.send(sender);
			return;
		}

		ResidentCommand.residentFriend(catchConsole(sender), StringMgmt.remArgs(split, 2), true, resident);
	}

	private void residentUnjail(CommandSender sender, Resident resident) throws NoPermissionException, TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_UNJAIL.getNode());
		if (!resident.isJailed())
			throw new TownyException(Translatable.of("msg_err_player_is_not_jailed"));

		JailUtil.unJailResident(resident, UnJailReason.ADMIN);
	}

	/**
	 * Delete a resident and their data file afer a successful confirmation.
	 * 
	 * @param sender Sender who ran the command.
	 * @param resident Resident to delete.
	 * @throws NoPermissionException 
	 */
	public void residentDelete(CommandSender sender, Resident resident) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_DELETE.getNode());
		Confirmation.runOnAccept(()-> {
			Player player = resident.isOnline() ? resident.getPlayer() : null;
			TownyUniverse.getInstance().getDataSource().removeResident(resident);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_del_resident", resident.getName()));

			if (player != null)
				plugin.getScheduler().run(player, new OnPlayerLogin(plugin, player));
		}).sendTo(sender);
	}

	public void parseAdminTownCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWN.send(sender);
			return;
		}

		// Special case where we can create a new Town before we use split[0] to get a Town.
		if (split[0].equalsIgnoreCase("new")) {
			parseAdminNewTownCommand(sender, StringMgmt.remFirstArg(split));
			return;
		}

		Town town = getTownOrThrow(split[0]);
		if (split.length == 1) {
			//This is run async because it will ping the economy plugin for the town bank value.
			TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(town, sender)));
			return;
		}

		switch (split[1].toLowerCase(Locale.ROOT)) {
		case "invite":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_INVITE.getNode());
			// Give admins the ability to invite a player to town, invite still requires acceptance.
			if (split.length < 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME invite PLAYERNAME"));
			TownCommand.townAdd(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "add":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_ADD.getNode());
			// Force-join command for admins to use to bypass invites system.
			if (split.length < 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME add PLAYERNAME"));
			Resident resident = getResidentOrThrow(split[2]);
			TownCommand.townAddResident(town, resident);
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_join_town", resident.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_join_town", resident.getName()));
			break;
		case "kick":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_KICK.getNode());
			// Force-kick command for admins.
			if (split.length < 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME kick PLAYERNAME"));
			resident = getResidentOrThrow(split[2]);
			if (!town.hasResident(resident))
				throw new TownyException(Translatable.of("msg_err_townadmintownrank_wrong_town"));
			if (resident.isMayor())
				throw new TownyException(Translatable.of("msg_you_cannot_kick_this_resident", resident.getName()));
			resident.removeTown();
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_kicked", "Console", resident.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_kicked", "You", resident.getName()));
			if (resident.isOnline())
				TownyMessaging.sendMsg(resident, Translatable.of("msg_kicked_by", "Console"));
			break;
		case "delete":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DELETE.getNode());
			Confirmation.runOnAccept(() -> {
				TownyMessaging.sendMsg(sender, Translatable.of("town_deleted_by_admin", town.getName()));
				TownyUniverse.getInstance().getDataSource().removeTown(town, DeleteTownEvent.Cause.ADMIN_COMMAND, sender);
			}).sendTo(sender);
			break;
		case "rename":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_RENAME.getNode());
			if (split.length < 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME rename NEWNAME"));
			String name = NameValidation.checkAndFilterTownNameOrThrow(String.join("_", StringMgmt.remArgs(split, 2)));
			if (TownyUniverse.getInstance().hasTown(name))
				throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));
			BukkitTools.ifCancelledThenThrow(new TownPreRenameEvent(town, name));
			townyUniverse.getDataSource().renameTown(town, name);
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_set_name", getSenderFormatted(sender), town.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_name", getSenderFormatted(sender), town.getName()));
			break;
		case "spawn":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN.getNode());
			SpawnUtil.sendToTownySpawn(catchConsole(sender), StringMgmt.remArgs(split, 2), town, "", false, false, SpawnType.TOWN);
			break;
		case "outpost":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_OUTPOST.getNode());
			SpawnUtil.sendToTownySpawn(catchConsole(sender), StringMgmt.remArgs(split, 2), town, "", true, false, SpawnType.TOWN);
			break;
		case "rank":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_RANK.getNode());
			parseAdminTownRankCommand(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "toggle":
			parseAdminTownToggle(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "set":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SET.getNode());
			parseAdminTownSet(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "meta":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode());
			handleTownMetaCommand(sender, town, split);
			break;
		case "giveboughtblocks":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_GIVEBOUGHTBLOCKS.getNode());
			giveTownBoughtTownBlocks(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "settownlevel":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SETTOWNLEVEL.getNode());
			setTownLevel(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "bankhistory":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_BANKHISTORY.getNode());
			int pages = 10;
			if (split.length > 2)
				pages = MathUtil.getPositiveIntOrThrow(split[2]);

			town.generateBankHistoryBook(catchConsole(sender), pages);
			break;
		case "deposit":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_DEPOSIT.getNode());
			if (!TownyEconomyHandler.isActive())
				throw new TownyException(Translatable.of("msg_err_no_economy"));

			// Handle incorrect number of arguments
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "deposit [amount]"));

			int deposit = MathUtil.getIntOrThrow(split[2]);
			if (town.getAccount().deposit(deposit, "Admin Deposit")) {
				BukkitTools.fireEvent(new TownTransactionEvent(town, Transaction.deposit(deposit).paidTo(town).build()));
				// Send notifications
				Translatable depositMessage = Translatable.of("msg_xx_deposited_xx", getSenderFormatted(sender), deposit, Translatable.of("town_sing"));
				TownyMessaging.sendMsg(sender, depositMessage);
				TownyMessaging.sendPrefixedTownMessage(town, depositMessage);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_unable_to_deposit_x", deposit));
			}

			break;
		case "withdraw":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_WITHDRAW.getNode());
			if (!TownyEconomyHandler.isActive())
				throw new TownyException(Translatable.of("msg_err_no_economy"));

			// Handle incorrect number of arguments
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "withdraw [amount]"));

			int withdraw = MathUtil.getIntOrThrow(split[2]);
			if (town.getAccount().withdraw(withdraw, "Admin Withdraw")) {
				BukkitTools.fireEvent(new TownTransactionEvent(town, Transaction.withdraw(withdraw).paidBy(town).build()));
				// Send notifications
				Translatable withdrawMessage = Translatable.of("msg_xx_withdrew_xx", getSenderFormatted(sender), withdraw, Translatable.of("town_sing"));
				TownyMessaging.sendMsg(sender, withdrawMessage);
				TownyMessaging.sendPrefixedTownMessage(town, withdrawMessage);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_unable_to_withdraw_x", withdraw));
			}
			break;
		case "outlaw":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_OUTLAW.getNode());
			TownCommand.parseTownOutlawCommand(sender, StringMgmt.remArgs(split, 2), true, town);
			break;
		case "leavenation":
			if (!town.hasNation())
				throw new TownyException(Translatable.of("That town does not belong to a nation."));

			Nation nation = town.getNationOrNull();
			town.removeNation();
			plugin.resetCache();

			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));
			break;
		case "unruin":
			// Sets the town to unruined with the existing NPC mayor still in place.
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_UNRUIN.getNode());
			TownRuinUtil.reclaimTown(town.getMayor(), town);
			town.save();
			break;
		case "checkoutposts":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_CHECKOUTPOSTS.getNode());
			parseAdminCheckOutpostsCommand(sender, town);
			break;
		case "trust":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TRUST.getNode());
			TownCommand.parseTownTrustCommand(sender, StringMgmt.remArgs(split, 2), town);
			break;
		case "trusttown":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TRUSTTOWN.getNode());
			TownCommand.parseTownTrustTownCommand(sender, StringMgmt.remArgs(split, 2), town);
			break;
		case "merge":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_MERGE.getNode());
			TownCommand.parseTownMergeCommand(sender, StringMgmt.remArgs(split, 2), town, true);
			break;
		case "forcemerge":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_FORCEMERGE.getNode());
			if (split.length < 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta town TOWNNAME forcemerge OTHERTOWN"));
			Town succumbingTown = getTownOrThrow(split[2]);
			if (succumbingTown.equals(town))
				throw new TownyException(Translatable.of("msg_err_invalid_name", split[2]));
			Confirmation.runOnAccept(() -> {
				townyUniverse.getDataSource().mergeTown(town, succumbingTown);
				TownyMessaging.sendGlobalMessage(Translatable.of("town1_has_merged_with_town2", succumbingTown, town));
			}).sendTo(sender);
			break;
		case "buy":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_FORCEMERGE.getNode());
			TownCommand.townBuy(sender, StringMgmt.remArgs(split, 2), town, true);
			break;
		case "forsale":
		case "fs":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_FORSALE.getNode());
			parseAdminTownForSaleCommand(sender, town, StringMgmt.remArgs(split, 2));
			break;
		case "notforsale":
		case "nfs":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NOTFORSALE.getNode());
			parseAdminTownNotForSaleCommand(sender, town);
			break;
		default:
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_TOWN, split[1])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_TOWN, split[1]).execute(sender, split, town);
				return;
			}

			HelpMenu.TA_TOWN.send(sender);
			return;
		}
	}

	private void parseAdminNewTownCommand(CommandSender sender, String[] split) throws TownyException {
		if (split.length < 2)
			throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta town new [townname] [mayor]");

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW.getNode());

		String mayorName = split[split.length - 1];
		String townName = StringMgmt.join(StringMgmt.remLastArg(split), "_");
		Player player = sender instanceof Player p ? p : null;
		Resident resident;
		if ("npc".equalsIgnoreCase(mayorName) && player != null) // Avoid creating a new npc resident if command is ran from console.
			resident = ResidentUtil.createAndGetNPCResident();
		else
			resident = getResidentOrThrow(mayorName);

		// If the command is being run from console, try to sub in the specfied player.
		if (player == null) {
			if (!resident.isOnline())
				throw new TownyException(Translatable.of("msg_player_is_not_online", mayorName));
			player = resident.getPlayer();
		}
		TownCommand.newTown(player, townName, resident, true, true);
	}

	private void parseAdminTownSet(CommandSender sender, Town town, String[] split) throws TownyException {
		if (split.length == 0 || split[0].equals("?")) {
			// Empty Case: /townyadmin town [town] set
			HelpMenu.TA_TOWN_SET.send(sender);
        } else if (split[0].equalsIgnoreCase("foundingdate")) {
			parseAdminTownSetFoundingDate(sender, town, split);
		} else {
			TownCommand.townSet(sender, split, true, town);
        }
	}

	private static void parseAdminTownSetFoundingDate(CommandSender sender, Town town, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SETFOUNDINGDATE.getNode());

		// Handle incorrect number of arguments
		if (split.length == 1) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Use /townyadmin town [townname] set foundingdate [unix_timestamp]"));
		}

		long timestamp;
		try {
			// They will input a UNIX timestamp in seconds (e.g. https://www.unixtimestamp.com/)
			// We use a UNIX timestamp in milliseconds, hence * 1000.
			timestamp = Long.parseLong(split[1]) * 1000;
		} catch (NumberFormatException ife) {
			throw new TownyException(Translatable.of("msg_error_must_be_int"));
		}
		if (timestamp <= 0) {
			throw new TownyException(Translatable.of("msg_err_amount_must_be_greater_than_zero"));
		}
		if (timestamp > System.currentTimeMillis()) {
			throw new TownyException(Translatable.of("msg_err_time_cannot_be_in_the_future"));
		}
		town.setRegistered(timestamp);
		town.save();

		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_registered_set", town, TownyFormatter.registeredFormat.format(town.getRegistered())));
	}

	private void parseAdminTownToggle(CommandSender sender, Town town, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_TOGGLE.getNode());
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_TOWN_TOGGLE.send(sender);
			return;
		}
		
		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2) {
			choice = BaseCommand.parseToggleChoice(split[1]);
		}
		
		if (split[0].equalsIgnoreCase("forcepvp")) {
			
			town.setAdminEnabledPVP(choice.orElse(!town.isAdminEnabledPVP()));
			
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forcepvp_setting_set_to", town.getName(), town.isAdminEnabledPVP()));
		} else if (split[0].equalsIgnoreCase("forcedisablepvp")) {
			
			town.setAdminDisabledPVP(choice.orElse(!town.isAdminDisabledPVP()));
			
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forcedisabledpvp_setting_set_to", town.getName(), town.isAdminDisabledPVP()));
		} else if (split[0].equalsIgnoreCase("forcemobs")) {

			town.setAdminEnabledMobs(choice.orElse(!town.isAdminEnabledMobs()));
			
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forcemobs_setting_set_to", town.getName(), town.isAdminEnabledMobs()));

		} else if (split[0].equalsIgnoreCase("unlimitedclaims")) {
			
			town.setHasUnlimitedClaims(choice.orElse(!town.hasUnlimitedClaims()));
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_unlimitedclaims_setting_set_to", town.getName(), town.hasUnlimitedClaims()));
		} else if (split[0].equalsIgnoreCase("upkeep")) {
			
			town.setHasUpkeep(choice.orElse(!town.hasUpkeep()));
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_upkeep_setting_set_to", town.getName(), town.hasUpkeep()));
		} else if (split[0].equalsIgnoreCase("allowedtowar")) {
			
			town.setAllowedToWar(choice.orElse(!town.isAllowedToWar()));
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_allowedtowar_setting_set_to", town.getName(), town.isAllowedToWar()));
		} else if (split[0].equalsIgnoreCase("conquered")) {
			if (!town.isConquered())
				throw new TownyException(Translatable.of("msg_err_that_town_is_not_conquered", town.getName()));

			town.setConquered(false);
			town.setConqueredDays(0);
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_conquered_status_removed", town.getName()));
		} else if (split[0].equalsIgnoreCase("visibleontoplists")) {

			town.setVisibleOnTopLists(choice.orElse(!town.isVisibleOnTopLists()));
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_visibleontoplist_setting_set_to", town.getName(), town.isVisibleOnTopLists()));
		} else
			TownCommand.townToggle(sender, split, true, town);
	}

	private void giveTownBoughtTownBlocks(CommandSender sender, Town town, String[] split) throws TownyException {
		// The number is missing.
		if (split.length == 0)
			throw new TownyException("Eg: /townyadmin town [townname] giveboughtblocks 2");

		if (TownySettings.getMaxBonusBlocks(town) == 0)
			throw new TownyException(Translatable.of("msg_err_town_not_allowed_purchased_blocks", town));

		// Handle removing all bought blocks
		if (split[0].equalsIgnoreCase("unset")) {
			town.setPurchasedBlocks(0);
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_purchased_blocks_unset", town));
			return;
		}

		// Handle modifying the town's bought blocks.
		int blocks = MathUtil.getIntOrThrow(split[0]);
		town.setPurchasedBlocks(Math.max(town.getPurchasedBlocks() + blocks, 0)); // Don't go below 0 or something will probably break.
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_purchased_blocks_changed", town, blocks, town.getPurchasedBlocks()));
	}

	private void setTownLevel(CommandSender sender, Town town, String[] split) throws TownyException {
		// The number is missing.
		if (split.length == 0)
			throw new TownyException("Eg: /townyadmin town [townname] settownlevel 2");
		
		// Handle un-setting the manual override.
		if (split[0].equalsIgnoreCase("unset")) {
			town.setManualTownLevel(-1);
			town.save();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_level_unset", town, town.getLevelNumber()));
			return;
		}

		// Handle applying a manual override.
		int level = MathUtil.getPositiveIntOrThrow(split[0]);
		if (level > TownySettings.getTownLevelMax() - 1)
			level = TownySettings.getTownLevelMax() - 1;
		town.setManualTownLevel(level);
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_level_overridden_with", town, level));
	}

	private void parseAdminTownRankCommand(CommandSender sender, Town town, String[] split) throws TownyException {
		if (split.length < 3) {
			throw new TownyException("Eg: /townyadmin town [townname] rank add/remove [resident] [rank]");
		}

		Resident target = getResidentOrThrow(split[1]);

		if (!target.hasTown()) {
			throw new TownyException(Translatable.of("msg_err_resident_doesnt_belong_to_any_town"));
		}
		if (!town.hasResident(target)) {
			throw new TownyException(Translatable.of("msg_err_townadmintownrank_wrong_town"));
		}

		/*
		 * Match casing to an existing rank, returns null if Town rank doesn't exist.
		 */
		String rank = TownyPerms.matchTownRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getTownRanks(), ", ")));

		if (split[0].equalsIgnoreCase("add")) {
			if (!target.hasTownRank(rank)) {
				BukkitTools.fireEvent(new TownAddResidentRankEvent(target, rank, town));
				target.addTownRank(rank);
				if (target.isOnline())
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", Translatable.of("town_sing"), rank));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_given_rank", Translatable.of("town_sing"), rank, target.getName()));
			} else {
				// Must already have this rank
				TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_already_has_rank", target.getName(), Translatable.of("town_sing")));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove")) {
			if (target.hasTownRank(rank)) {
				BukkitTools.fireEvent(new TownRemoveResidentRankEvent(target, rank, town));
				target.removeTownRank(rank);
				if (target.isOnline())
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", Translatable.of("town_sing"), rank));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_taken_rank_from", Translatable.of("town_sing"), rank, target.getName()));
			} else {
				// Doesn't have this rank
				TownyMessaging.sendMsg(sender, Translatable.of("msg_resident_doesnt_have_rank", target.getName(), Translatable.of("town_sing")));
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
		target.save();
		
	}

	public void parseAdminNationCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode());
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			HelpMenu.TA_NATION.send(sender);
			return;
		}
		// Special case where we check for a new nation being made before we use split[0] to set the Nation.
		if (split[0].equalsIgnoreCase("new")) {
			parseAdminNewNationCommand(sender, split);
			return;
		}

		Nation nation = getNationOrThrow(split[0]);
		if (split.length == 1) {
			//This is run async because it will ping the economy plugin for the town bank value.
			TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(nation, sender)));
			return;
		}

		switch (split[1].toLowerCase(Locale.ROOT)) {
		case "add":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ADD.getNode());
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation [nationname] add [townname]");
			townyAdminNationAddTown(sender, nation, StringMgmt.remArgs(split, 2));
			break;
		case "transfer":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_TRANSFER.getNode());
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation [nationname] transfer [townname]");
			townyAdminNationTransfterTown(sender, nation, StringMgmt.remArgs(split, 2));
			break;
		case "kick":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_KICK.getNode());
			NationCommand.nationKick(sender, nation, TownyAPI.getInstance().getTowns(StringMgmt.remArgs(split, 2)));
			break;
		case "sanctiontown":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SANCTIONTOWN.getNode());
			NationCommand.nationSanctionTown(sender, nation, StringMgmt.remArgs(split, 2));
			break;
		case "delete":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode());
			Confirmation.runOnAccept(() -> {
				TownyUniverse.getInstance().getDataSource().removeNation(nation, DeleteNationEvent.Cause.ADMIN_COMMAND, sender);
				TownyMessaging.sendGlobalMessage(Translatable.of("MSG_DEL_NATION", nation.getName()));
				if (sender instanceof Player)
					TownyMessaging.sendMsg(sender, Translatable.of("nation_deleted_by_admin", nation.getName()));
			}).sendTo(sender);
			break;
		case "meta":
			handleNationMetaCommand(sender, nation, split);
			break;
		case "recheck":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RECHECK.getNode());
			ProximityUtil.removeOutOfRangeTowns(nation);
			TownyMessaging.sendMsg(sender, Translatable.of("nation_rechecked_by_admin", nation.getName()));
			break;
		case "rename":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RENAME.getNode());
			String name = NameValidation.checkAndFilterNationNameOrThrow(String.join("_", StringMgmt.remArgs(split, 2)));
			if (TownyUniverse.getInstance().hasNation(name))
				throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));
			BukkitTools.ifCancelledThenThrow(new NationPreRenameEvent(nation, name));
			townyUniverse.getDataSource().renameNation(nation, name);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_name", getSenderFormatted(sender), nation.getName()));
			TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_set_name", getSenderFormatted(sender), nation.getName()));
			break;
		case "merge":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_MERGE.getNode());
			NationCommand.mergeNation(sender, StringMgmt.remArgs(split, 2), nation, true);
			break;
		case "forcemerge":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_FORCEMERGE.getNode());
			Nation remainingNation = getNationOrThrow(split[2]);
			if (remainingNation.equals(nation))
				throw new TownyException(Translatable.of("msg_err_invalid_name", split[2]));
			Confirmation.runOnAccept(() -> {
				townyUniverse.getDataSource().mergeNation(nation, remainingNation);
				TownyMessaging.sendGlobalMessage(Translatable.of("nation1_has_merged_with_nation2", nation, remainingNation));
			}).sendTo(sender);
			break;
		case "set":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SET.getNode());
			parseAdminNationSet(sender, nation, StringMgmt.remArgs(split, 2));
			break;
		case "toggle":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_TOGGLE.getNode());
			NationCommand.nationToggle(sender, StringMgmt.remArgs(split, 2), true, nation);
			break;
		case "bankhistory":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_BANKHISTORY.getNode());
			int pages = 10;
			if (split.length > 2)
				pages = MathUtil.getPositiveIntOrThrow(split[2]);

			nation.generateBankHistoryBook(catchConsole(sender), pages);
			break;
		case "deposit":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DEPOSIT.getNode());
			if (!TownyEconomyHandler.isActive())
				throw new TownyException(Translatable.of("msg_err_no_economy"));
			// Handle incorrect number of arguments
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "deposit [amount]"));

			int deposit = MathUtil.getPositiveIntOrThrow(split[2]);

			BukkitTools.fireEvent(new NationTransactionEvent(nation, Transaction.deposit(deposit).paidTo(nation).build()));

			nation.getAccount().deposit(deposit, "Admin Deposit");
			// Send notifications
			Translatable depositMessage = Translatable.of("msg_xx_deposited_xx", getSenderFormatted(sender), deposit,  Translatable.of("nation_sing"));
			TownyMessaging.sendMsg(sender, depositMessage);
			TownyMessaging.sendPrefixedNationMessage(nation, depositMessage);
			break;
		case "withdraw":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_WITHDRAW.getNode());
			if (!TownyEconomyHandler.isActive())
				throw new TownyException(Translatable.of("msg_err_no_economy"));
			// Handle incorrect number of arguments
			if (split.length != 3)
				throw new TownyException(Translatable.of("msg_err_invalid_input", "withdraw [amount]"));

			int withdraw = MathUtil.getPositiveIntOrThrow(split[2]);

			BukkitTools.fireEvent(new NationTransactionEvent(nation, Transaction.withdraw(withdraw).paidBy(nation).build()));

			nation.getAccount().withdraw(withdraw, "Admin Withdraw");
			// Send notifications
			Translatable withdrawMessage = Translatable.of("msg_xx_withdrew_xx", getSenderFormatted(sender), withdraw,  Translatable.of("nation_sing"));
			TownyMessaging.sendMsg(sender, withdrawMessage);
			TownyMessaging.sendPrefixedNationMessage(nation, withdrawMessage);
			break;
		case "rank":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_RANK.getNode());
			parseAdminNationRankCommand(sender, StringMgmt.remArgs(split, 2), nation);
			break;
		case "ally":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ALLY.getNode());
			parseAdminNationAllyCommand(sender, StringMgmt.remArgs(split, 2), nation);
			break;
		case "enemy":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_ENEMY.getNode());
			parseAdminNationEnemyCommand(sender, StringMgmt.remArgs(split, 2), nation);
			break;
		default:
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_NATION, split[1])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_NATION, split[1]).execute(sender, split, nation);
				return;
			}
			HelpMenu.TA_NATION.send(sender);
		}
	}

	private void parseAdminNewNationCommand(CommandSender sender, String[] split) throws TownyException {
		if (split.length != 3)
			throw new TownyException(Translatable.of("msg_err_not_enough_variables") + "/ta nation new [name] [capital]");

		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_NEW.getNode());
		
		final Town capitalTown = getTownOrThrow(split[2]);
		NationCommand.newNation(sender, split[1], capitalTown, true);
	}

	/**
	 * Force-join command for admins which will bypass the invite system.
	 * This also bypasses other limits Towny imposes on towns and nations
	 * such as the max-towns-per-nation and nation-proximity, and doesn't
	 * fire a cancellable pre-join event either. Any admin who runs this
	 * can be assumed to know what they want. 
	 * @param sender CommandSender
	 * @param nation Nation which will have a town added.
	 * @param townName Name of Town to add to Nation.
	 * @throws TownyException when something has gone wrong.
	 */
	private void townyAdminNationAddTown(CommandSender sender, Nation nation, String[] townName) throws TownyException {

		Town town = getTownOrThrow(townName[0]);
		if (town.hasNation()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_already_nation"));
			TownyMessaging.sendMessage(sender, "Suggestion: /townyadmin town " + town + "leavenation, or /ta nation " + nation + " transfer " + town);
			return;
		}
		town.setNation(nation);
		town.save();
		TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_join_nation", town.getName()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_join_nation", town.getName()));
	}
	
	private void townyAdminNationTransfterTown(CommandSender sender, Nation nation, String[] townName) throws TownyException {
		Town town = getTownOrThrow(townName[0]);
		if (town.hasNation()) {
			nation = town.getNationOrNull();
			town.removeNation();
			plugin.resetCache();
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));
		}
		town.setNation(nation);
		town.save();
		TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_join_nation", town.getName()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_join_nation", town.getName()));
	}
	private void parseAdminNationSet(CommandSender sender, Nation nation, String[] split) throws TownyException {
		if (split.length == 0 || split[0].equals("?")) {
			// Empty Case: /townyadmin town [town] set
			HelpMenu.TA_NATION_SET.send(sender);
		} else if (split[0].equalsIgnoreCase("foundingdate")) {
			parseAdminNationSetFoundingDate(sender, nation, split);
		} else {
			NationCommand.nationSet(sender, split, true, nation);
		}
	}

	private static void parseAdminNationSetFoundingDate(CommandSender sender, Nation nation, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SETFOUNDINGDATE.getNode());

		// Handle incorrect number of arguments
		if (split.length == 1) {
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Use /townyadmin nation [nationname] set foundingdate [unix_timestamp]"));
		}

		long timestamp;
		try {
			// They will input a UNIX timestamp in seconds (e.g. https://www.unixtimestamp.com/)
			// We use a UNIX timestamp in milliseconds, hence * 1000.
			timestamp = Long.parseLong(split[1]) * 1000;
		} catch (NumberFormatException ife) {
			throw new TownyException(Translatable.of("msg_error_must_be_int"));
		}
		if (timestamp <= 0) {
			throw new TownyException(Translatable.of("msg_err_amount_must_be_greater_than_zero"));
		}
		if (timestamp > System.currentTimeMillis()) {
			throw new TownyException(Translatable.of("msg_err_time_cannot_be_in_the_future"));
		}
		nation.setRegistered(timestamp);
		nation.save();

		TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_registered_set", nation, TownyFormatter.registeredFormat.format(nation.getRegistered())));
	}

	private void parseAdminNationRankCommand(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (split.length < 3) {
			HelpMenu.TA_NATION_RANK.send(sender);
			return;
		}
		Resident target = getResidentOrThrow(split[1]);
		if (!target.hasTown() || !target.getTownOrNull().hasNation() || !target.getTownOrNull().getNationOrNull().getUUID().equals(nation.getUUID()))
			throw new TownyException(Translatable.of("msg_err_that_resident_doesnt_belong_to_that_nation"));

		String rank = TownyPerms.matchNationRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));

		switch(split[0].toLowerCase(Locale.ROOT)) {
			case "add":
				if (target.hasNationRank(rank))
					// Already has the rank.
					throw new TownyException(Translatable.of("msg_resident_already_has_rank", target.getName(), Translatable.of("nation_sing")));
				BukkitTools.fireEvent(new NationRankAddEvent(nation, rank, target));
				target.addNationRank(rank);
				if (target.isOnline()) {
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_been_given_rank", Translatable.of("nation_sing"), rank));
					plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
				}
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_given_rank", Translatable.of("nation_sing"), rank, target.getName()));
				target.save();
				return;
			case "remove":
				if (!target.hasNationRank(rank))
					// Doesn't have the rank.
					throw new TownyException(Translatable.of("msg_resident_doesnt_have_rank", target.getName(), Translatable.of("nation_sing")));
				BukkitTools.fireEvent(new NationRankRemoveEvent(nation, rank, target));
				target.removeNationRank(rank);
				if (target.isOnline()) {
					TownyMessaging.sendMsg(target, Translatable.of("msg_you_have_had_rank_taken", Translatable.of("nation_sing"), rank));
					plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
				}
				TownyMessaging.sendMsg(sender, Translatable.of("msg_you_have_taken_rank_from", Translatable.of("nation_sing"), rank, target.getName()));
				target.save();
				return;
			default:
				HelpMenu.TA_NATION_RANK.send(sender);
				return;
		}
	}

	private void parseAdminNationAllyCommand(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (split.length < 2)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta nation [nation] ally [add/remove] [nation]"));

		Nation ally = getNationOrThrow(split[1]);
		if (ally.equals(nation))
			throw new TownyException(Translatable.of("msg_err_invalid_name", split[1]));

		if (split[0].equalsIgnoreCase("add")) {
			if (!nation.hasAlly(ally)) {
				if (nation.hasEnemy(ally))
					nation.removeEnemy(ally);
				
				if (ally.hasEnemy(nation))
					ally.removeEnemy(nation);
				
				nation.addAlly(ally);
				nation.save();

				ally.addAlly(nation);
				ally.save();

				plugin.resetCache();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_added_ally", ally.getName()));
				TownyMessaging.sendPrefixedNationMessage(ally, Translatable.of("msg_added_ally", nation.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
			} else
				throw new TownyException(Translatable.of("msg_err_nation_already_allied_with_2", nation.getName(), ally.getName()));
		} else if (split[0].equalsIgnoreCase("remove")) {
			if (nation.hasAlly(ally)) {
				nation.removeAlly(ally);
				nation.save();

				ally.removeAlly(nation);
				ally.save();

				plugin.resetCache();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", ally.getName()));
				TownyMessaging.sendPrefixedNationMessage(ally, Translatable.of("msg_removed_ally", nation.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
			} else
				throw new TownyException(Translatable.of("msg_err_nation_not_allied_with_2", nation.getName(), ally.getName()));
		} else
			throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta nation [nation] ally [add/remove] [nation]"));
	}

	private void parseAdminNationEnemyCommand(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (split.length < 2)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta nation [nation] enemy [add/remove] [nation]"));

		Nation enemy = getNationOrThrow(split[1]);
		if (enemy.equals(nation))
			throw new TownyException(Translatable.of("msg_err_invalid_name", split[3]));

		if (split[0].equalsIgnoreCase("add")) {
			if (!nation.hasEnemy(enemy)) {
				if (nation.hasAlly(enemy)) {
					nation.removeAlly(enemy);
					enemy.removeAlly(nation);
					plugin.resetCache();
				}

				nation.addEnemy(enemy);
				nation.save();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_nations", getSenderFormatted(sender), enemy.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
			} else
				throw new TownyException(Translatable.of("msg_err_nation_already_enemies_with_2", nation.getName(), enemy.getName()));
		} else if (split[0].equalsIgnoreCase("remove")) {
			if (nation.hasEnemy(enemy)) {
				nation.removeEnemy(enemy);
				nation.save();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_to_neutral", getSenderFormatted(sender), enemy.getName()));
				TownyMessaging.sendPrefixedNationMessage(enemy, Translatable.of("msg_removed_enemy", nation.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_allies_enemies_updated", nation.getName()));
			} else
				throw new TownyException(Translatable.of("msg_err_nation_not_enemies_with_2", nation.getName(), enemy.getName()));
		} else
			throw new TownyException(Translatable.of("msg_err_invalid_input", "/ta nation [nation] enemy [add/remove] [nation]"));
	}

	private String getSenderFormatted(CommandSender sender) {
		return sender instanceof Player player ? player.getName() : "Console";
	}

	private void adminSet(CommandSender sender, String[] split) throws TownyException {
		if (split.length == 0) {
			HelpMenu.TA_SET.send(sender);
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "capital":
			adminSetCapital(sender, split);
			break;
		case "founder":
			adminSetFounder(sender, split);
			break;
		case "mayor":
			adminSetMayor(sender, split);
			break;
		case "nationzoneoverride":
			adminSetNationZoneOverride(sender, split);
			break;
		case "plot":
			adminSetPlot(sender, split);
			break;
		case "about":
			adminSetAbout(sender, split);
			break;
		case "surname":
			adminSetSurname(sender, split);
			break;
		case "title":
			adminSetTitle(sender, split);
			break;
		default:
			if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_SET, split[0]).execute(sender, "townyadmin", split);
			} else {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", "administrative"));
			}
		}
	}

	private void adminSetCapital(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_CAPITAL.getNode());

		if (split.length < 2) {
			HelpMenu.TA_SET_CAPITAL.send(sender);
			return;
		}
		final Nation nation = getNationFromTownOrThrow(getTownOrThrow(split[1]));
		NationCommand.nationSet(sender, split, true, nation);
	}

	private void adminSetFounder(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_FOUNDER.getNode());

		if (split.length < 3) {
			HelpMenu.TA_SET_FOUNDER.send(sender);
			return;
		}
		Town town = getTownOrThrow(split[1]);
		String founderName = split[2];
		town.setFounder(founderName);
		town.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_founder_set", town, founderName));
	}

	private void adminSetMayor(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR.getNode());

		if (split.length < 3) {
			HelpMenu.TA_SET_MAYOR.send(sender);
			return;
		}
		Town town = getTownOrThrow(split[1]);
		@Nullable Resident oldMayor = town.getMayor();
		boolean deleteOldMayor = oldMayor != null && oldMayor.isNPC();
		
		// New mayor is either an NPC resident or a resident by name from split[2].
		Resident newMayor = split[2].equalsIgnoreCase("npc") 
				? ResidentUtil.createAndGetNPCResident()
				: getResidentOrThrow(split[2]);

		// Add the new Mayor to the town if need be.
		if (!town.hasResident(newMayor))
			TownCommand.townAddResident(town, newMayor);

		// Set the new mayor.
		town.setMayor(newMayor);
		
		// Reset caches and permissions.
		if (!deleteOldMayor && oldMayor != null && oldMayor.isOnline()) {
			Towny.getPlugin().deleteCache(oldMayor);
			TownyPerms.assignPermissions(oldMayor, oldMayor.getPlayer());
		}
		
		if (newMayor.isOnline() && !newMayor.isNPC())
			Towny.getPlugin().deleteCache(newMayor);

		// If the previous mayor was an NPC make sure they're removed from the database.
		if (deleteOldMayor)
			TownyUniverse.getInstance().getDataSource().removeResident(oldMayor);

		// NPC mayors set their towns to not pay any upkeep.
		town.setHasUpkeep(!newMayor.isNPC());

		town.save();
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_new_mayor", newMayor));
	}

	private void adminSetNationZoneOverride(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_NATIONZONE.getNode());
		
		if (split.length < 2) {
			HelpMenu.TA_SET_NATIONZONE.send(sender);
			return;
		}
		Town town = getTownOrThrow(split[1]);
		int size = MathUtil.getPositiveIntOrThrow(split[2]);
		town.setNationZoneOverride(size);
		town.save();
		if (size == 0)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_nationzone_override_removed", town.getName()));
		else 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_nationzone_override_set_to", town.getName(), size));
	}

	private void adminSetPlot(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_PLOT.getNode());
		
		if (split.length < 2) {
			HelpMenu.TA_SET_PLOT.send(sender);
			return;
		}
		
		final Player player = catchConsole(sender);
		TownBlock tb = TownyAPI.getInstance().getTownBlock(player);
		if (tb != null) {
			Town newTown = getTownOrThrow(split[1]);
			tb.removeResident();
			tb.setTown(newTown);
			tb.setType(TownBlockType.RESIDENTIAL);
			tb.setName("");
			tb.save();
			TownyMessaging.sendMsg(player, Translatable.of("changed_plot_town", newTown.getName()));
		} else {
			Town town = getTownOrThrow(split[1]);
			WorldCoord key = WorldCoord.parseWorldCoord(player);
			String[] newSplit = StringMgmt.remArgs(split, 2);
			List<WorldCoord> selection = AreaSelectionUtil.selectWorldCoordArea(town, key, newSplit, true);
			
			TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
			selection = AreaSelectionUtil.filterOutTownOwnedBlocks(selection);
			TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
			
			plugin.getScheduler().runAsync(new TownClaim(plugin, player, town, selection, false, true, false));

		}
	}

	private void adminSetAbout(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_ABOUT.getNode());

		Resident resident = null;
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyadmin set about bilbo Just a humble farmer");
		} else {
			resident = getResidentOrThrow(split[1]);

			split = StringMgmt.remArgs(split, 2);
			String about = StringMgmt.join(split);

			if ("reset".equalsIgnoreCase(about))
				about = "/resident set about [msg]";

			else if ("none".equalsIgnoreCase(about) || "clear".equalsIgnoreCase(about)) {
				about = "";
			}
			
			if (about.length() > 159)
				about = about.substring(0, 159);

			resident.setAbout(about);
			resident.save();

			if (about.isEmpty()) {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_about", resident.getName()));
			} else {
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_about", resident.getName(), about));
			}
		}
	}

	private void adminSetSurname(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_SURNAME.getNode());
		
		Resident resident = null;
		// Give the resident a surname
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyadmin set surname bilbo Jester");
			return;
		} else
			resident = getResidentOrThrow(split[1]);

		String surname = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));
		resident.setSurname(surname + " ");
		resident.save();

		if (resident.hasSurname()) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
		} else {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
		}
	}

	private void adminSetTitle(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_TITLE.getNode());
		
		Resident resident = null;
		// Give the resident a title
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /townyadmin set title bilbo Jester");
			return;
		} else
			resident = getResidentOrThrow(split[1]);

		String title = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));
		resident.setTitle(title + " ");
		resident.save();

		if (resident.hasTitle()) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
		} else {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
		}
	}

	public void reloadLangs(CommandSender sender) {
		try {
			plugin.loadLocalization(true);
			TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_lang"));
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_reload_error"));
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
			plugin.getLogger().log(Level.WARNING, "Error occurred while reloading lang", e);
			plugin.addError(TownyInitException.TownyError.LOCALIZATION);
		}
	}
	
	public void reloadPerms(CommandSender sender) {
		try {
			plugin.loadPermissions(true);
			PluginIntegrations.getInstance().registerPermissionsProviders(plugin);
		} catch (TownyInitException tie) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_reload_error"));
			TownyMessaging.sendErrorMsg(sender, tie.getMessage());
			plugin.getLogger().log(Level.WARNING, "Error occurred while reloading townyperms.yml", tie);
			// Place Towny in Safe Mode while the townyperms.yml is unreadable.
			plugin.addError(tie.getError());
			return;
		}
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_perms"));
		
	}

	/**
	 * Reloads only the config
	 * 
	 * @param reset Whether or not to reset the config.
	 */
	public void reloadConfig(CommandSender sender, boolean reset) {

		if (reset) {
			TownyUniverse.getInstance().getDataSource().deleteFile(plugin.getConfigPath());
			TownyMessaging.sendMsg(sender, Translatable.of("msg_reset_config"));
		}
		
		try {
			plugin.loadConfig(true);
			TownySettings.loadTownLevelConfig();   // TownLevel and NationLevels are not loaded in the config,
			TownySettings.loadNationLevelConfig(); // but later so the config-migrator can do it's work on them if needed.
			plugin.loadLocalization(true);
			TownBlockTypeHandler.initialize();

			TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_config"));
		} catch (TownyException | TownyInitException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_reload_error"));
			plugin.getLogger().log(Level.WARNING, "An exception occurred while reloading the config", e);
			
			if (e instanceof TownyInitException tie)
				plugin.addError(tie.getError());
		}
		
	}

	/**
	 * Reloads both the database and the config. Used with a database reload command.
	 */
	public void reloadDatabase(CommandSender sender) {
		TownyUniverse.getInstance().getDataSource().finishTasks();
		try {
			plugin.loadFoundation(true);
		} catch (TownyInitException tie) {
			TownyMessaging.sendErrorMsg(sender, tie.getMessage());
			plugin.getLogger().log(Level.WARNING, "An exception occurred when reloading the database", tie);
			plugin.addError(tie.getError());
			return;
		}
		// Register all child permissions for ranks
		TownyPerms.registerPermissionNodes();

		// Update permissions for all online players
		TownyPerms.updateOnlinePerms();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_reloaded_db"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split - Current command arguments.
	 * @throws TownyException when an error message needs to be returned.
	 */
	public void purge(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode());
		if (split.length == 0) {
			// command was '/townyadmin purge'
			HelpMenu.TA_PURGE.send(sender);
			return;
		}

		checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode(), Translatable.of("msg_err_admin_only"));

		final boolean townless = split.length == 2 && split[1].equalsIgnoreCase("townless");
		final Town town = !townless && split.length == 2 ? getTownOrThrow(split[1]) : null;

		try {
			int days = Integer.parseInt(split[0]);
			Confirmation.runOnAccept(() -> 
				plugin.getScheduler().runAsync(new ResidentPurge(sender, TimeTools.getMillis(days + "d"), townless, town))
			).sendTo(sender);
		} catch (NumberFormatException e) {
			throw new TownyException(Translatable.of("msg_error_must_be_int"));
		}
	}

	public void parseToggleCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode());

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2) {
			choice = BaseCommand.parseToggleChoice(split[1]);
		} else if (split.length == 3 && split[1].equalsIgnoreCase("npc")) {
			choice = BaseCommand.parseToggleChoice(split[2]);
		}

		if (split.length == 0) {
			// command was '/townyadmin toggle'
			HelpMenu.TA_TOGGLE.send(sender);
			return;
		}

		switch(split[0].toLowerCase(Locale.ROOT)) {
			case "wildernessuse" -> adminToggleWildernessUse(sender, choice);
			case "regenerations" -> adminToggleRegenerations(sender, choice);
			case "devmode" -> adminToggleDevMode(sender, choice);
			case "debug" -> adminToggleDebug(sender, choice);
			case "townwithdraw" -> adminToggleTownWithDraw(sender, choice);
			case "nationwithdraw" -> adminToggleNationWithdraw(sender, choice);
			case "npc" -> parseAdminToggleNPC(sender, split);
			default -> {
				if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNYADMIN_TOGGLE, split[0])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNYADMIN_TOGGLE, split[0]).execute(sender, "townyadmin", split);
					return;
				}
				HelpMenu.TA_TOGGLE.send(sender);
			}
		}
	}

	private void adminToggleWildernessUse(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		// Toggles build/destroy/switch/itemuse on or off in all worlds. True is the default, for installation setup to alter the defaulted false.
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_WILDERNESSUSE.getNode());
		toggleWildernessUsage(choice.orElse(true));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_wilderness_use_x_in_all_worlds", choice.orElse(true)));
	}

	private void adminToggleRegenerations(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_REGENERATIONS.getNode());
		toggleRegenerations(choice.orElse(false));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_regenerations_use_x_in_all_worlds", choice.orElse(false)));
	}

	private void adminToggleDevMode(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEVMODE.getNode());
		TownySettings.setDevMode(choice.orElse(!TownySettings.isDevMode()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_devmode", (TownySettings.isDevMode() ? Translatable.literal(Colors.Green).append(Translatable.of("enabled")) : Translatable.literal(Colors.Red).append(Translatable.of("disabled")))));	}

	private void adminToggleDebug(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_DEBUG.getNode());
		TownySettings.setDebug(choice.orElse(!TownySettings.getDebug()));
		TownyLogger.refreshDebugLogger();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_debugmode", (TownySettings.getDebug() ? Translatable.literal(Colors.Green).append(Translatable.of("enabled")) : Translatable.literal(Colors.Red).append(Translatable.of("disabled")))));	}

	private void adminToggleTownWithDraw(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_TOWNWITHDRAW.getNode());
		TownySettings.SetTownBankAllowWithdrawls(choice.orElse(!TownySettings.getTownBankAllowWithdrawls()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_townwithdraw", (TownySettings.getTownBankAllowWithdrawls() ? Translatable.literal(Colors.Green).append(Translatable.of("enabled")) : Translatable.literal(Colors.Red).append(Translatable.of("disabled")))));	}

	private void adminToggleNationWithdraw(CommandSender sender, Optional<Boolean> choice) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NATIONWITHDRAW.getNode());
		TownySettings.SetNationBankAllowWithdrawls(choice.orElse(!TownySettings.getNationBankAllowWithdrawls()));
		TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_toggle_nationwithdraw", (TownySettings.getNationBankAllowWithdrawls() ? Translatable.literal(Colors.Green).append(Translatable.of("enabled")) : Translatable.literal(Colors.Red).append(Translatable.of("disabled")))));	}

	private void parseAdminToggleNPC(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE_NPC.getNode());
		if (split.length != 2)
			throw new TownyException(Translatable.of("msg_err_invalid_input", "Eg: toggle npc [resident]"));
		
		Resident resident = getResidentOrThrow(split[1]);
		resident.setNPC(!resident.isNPC());
		resident.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_npc_flag", resident.isNPC(), resident.getName()));
	}

	private void toggleRegenerations(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUsingPlotManagementRevert(choice);
			world.setUsingPlotManagementWildBlockRevert(choice);
			world.setUsingPlotManagementWildEntityRevert(choice);
			world.save();
		}
	}
	
	private void toggleRevertUnclaim(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUsingPlotManagementRevert(choice);
			world.save();
		}
	}

	private void toggleWildernessUsage(boolean choice) {
		for (TownyWorld world : new ArrayList<>(TownyUniverse.getInstance().getTownyWorlds())) {
			world.setUnclaimedZoneBuild(choice);
			world.setUnclaimedZoneDestroy(choice);
			world.setUnclaimedZoneSwitch(choice);
			world.setUnclaimedZoneItemUse(choice);
			world.save();
		}
	}

	public static void handleNationMetaCommand(CommandSender sender, Nation nation, String[] split) throws TownyException {
		
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_META.getNode());

		if (split.length == 2) {
			displayNationMeta(sender, nation);
			return;
		}

		if (split.length < 4) {
			HelpMenu.TA_NATION_META.send(sender);
			return;
		}

		final String mdKey = split[3];
		switch (split[2].toLowerCase(Locale.ROOT)) {
		case "set" -> handleNationMetaSet(sender, nation, split, mdKey);
		case "add" -> handleNationMetaAdd(sender, nation, mdKey);
		case "remove" -> handleNationMetaRemove(sender, nation, mdKey);
		default -> HelpMenu.TA_NATION_META.send(sender);
		}
	}

	private static void displayNationMeta(CommandSender sender, Nation nation) throws TownyException {
		if (!nation.hasMeta())
			throw new TownyException(Translatable.of("msg_err_this_nation_doesnt_have_any_associated_metadata"));

		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Custom Meta Data"));
		for (CustomDataField<?> field : nation.getMetadata()) {
			TownyMessaging.sendMessage(sender, field.getKey() + " = " + field.getValue());
		}
	}

	private static void handleNationMetaSet(CommandSender sender, Nation nation, String[] split, final String mdKey) throws TownyException {
		String val = split.length == 5 ? split[4] : null;

		if (!nation.hasMeta() || !nation.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_x_is_not_part_of_this_nation", mdKey));

		CustomDataField<?> cdf = nation.getMetadata(mdKey);

		// Check if the given value is valid for this field.
		try {
			if (val == null)
				throw new InvalidMetadataTypeException(cdf);

			cdf.isValidType(val);
		} catch (InvalidMetadataTypeException e) {
			throw new TownyException(e.getMessage());
		}

		// Change state
		cdf.setValueFromString(val);

		// Let user know that it was successful.
		TownyMessaging.sendMsg(sender, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

		// Save changes.
		nation.save();
	}

	private static void handleNationMetaAdd(CommandSender sender, Nation nation, final String mdKey) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey))
			throw new TownyException(Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
		
		CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

		if (nation.hasMeta() && nation.hasMeta(md.getKey()))
			throw new TownyException(Translatable.of("msg_err_key_x_already_exists", mdKey));

		TownyMessaging.sendMsg(sender, Translatable.of("msg_custom_data_was_successfully_added_to_nation"));
		
		nation.addMetaData(md.clone(), true);
	}

	private static void handleNationMetaRemove(CommandSender sender, Nation nation, final String mdKey) throws TownyException {
		if (!nation.hasMeta() || !nation.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_cannot_be_deleted"));
		nation.removeMetaData(mdKey, true);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_data_successfully_deleted"));
	}

	public static void handleTownMetaCommand(CommandSender sender, Town town, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode());
		if (split.length == 2) {
			displayTownMeta(sender, town);
			return;
		}

		if (split.length < 4) {
			HelpMenu.TA_TOWN_META.send(sender);
			return;
		}

		final String mdKey = split[3];
		switch (split[2].toLowerCase(Locale.ROOT)) {
		case "set" -> handleTownMetaSet(sender, town, split, mdKey);
		case "add" -> handleTownMetaAdd(sender, town, mdKey);
		case "remove" -> handleTownMetaRemove(sender, town, mdKey);
		default -> HelpMenu.TA_TOWN_META.send(sender);
		}
	}

	private static void displayTownMeta(CommandSender sender, Town town) throws TownyException {
		if (!town.hasMeta())
			throw new TownyException(Translatable.of("msg_err_this_town_doesnt_have_any_associated_metadata"));

		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Custom Meta Data"));
		for (CustomDataField<?> field : town.getMetadata()) {
			TownyMessaging.sendMessage(sender, field.getKey() + " = " + field.getValue());
		}
	}

	private static void handleTownMetaSet(CommandSender sender, Town town, String[] split, final String mdKey) throws TownyException {
		String val = split.length == 5 ? split[4] : null;

		if (!town.hasMeta() || !town.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_x_is_not_part_of_this_town", mdKey));

		CustomDataField<?> cdf = town.getMetadata(mdKey);

		// Check if the given value is valid for this field.
		try {
			if (val == null)
				throw new InvalidMetadataTypeException(cdf);

			cdf.isValidType(val);
		} catch (InvalidMetadataTypeException e) {
			throw new TownyException(e.getMessage());
		}

		// Change state
		cdf.setValueFromString(val);

		// Let user know that it was successful.
		TownyMessaging.sendMsg(sender, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

		// Save changes.
		town.save();
	}

	private static void handleTownMetaAdd(CommandSender sender, Town town, final String mdKey) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey))
			throw new TownyException(Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
		
		CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

		if (town.hasMeta() && town.hasMeta(md.getKey()))
			throw new TownyException(Translatable.of("msg_err_key_x_already_exists", mdKey));

		TownyMessaging.sendMsg(sender, Translatable.of("msg_custom_data_was_successfully_added_to_town"));
		
		town.addMetaData(md.clone(), true);
	}

	private static void handleTownMetaRemove(CommandSender sender, Town town, final String mdKey) throws TownyException {
		if (!town.hasMeta() || !town.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_cannot_be_deleted"));
		town.removeMetaData(mdKey, true);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_data_successfully_deleted"));
	}

	public static void handleResidentMetaCommand(CommandSender sender, Resident resident, String[] split) throws TownyException {
		
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT_META.getNode());

		if (split.length == 2) {
			displayResidentMeta(sender, resident);
			return;
		}

		if (split.length < 4) {
			HelpMenu.TA_RESIDENT_META.send(sender);
			return;
		}

		final String mdKey = split[3];
		switch (split[2].toLowerCase(Locale.ROOT)) {
		case "set" -> handleResidentMetaSet(sender, resident, split, mdKey);
		case "add" -> handleResidentMetaAdd(sender, resident, mdKey);
		case "remove" -> handleResidentMetaRemove(sender, resident, mdKey);
		default -> HelpMenu.TA_RESIDENT_META.send(sender);
		}
	}

	private static void displayResidentMeta(CommandSender sender, Resident resident) throws TownyException {
		if (!resident.hasMeta())
			throw new TownyException(Translatable.of("msg_err_this_resident_doesnt_have_any_associated_metadata"));

		TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Custom Meta Data"));
		for (CustomDataField<?> field : resident.getMetadata()) {
			TownyMessaging.sendMessage(sender, field.getKey() + " = " + field.getValue());
		}
	}

	private static void handleResidentMetaSet(CommandSender sender, Resident resident, String[] split, final String mdKey) throws TownyException {
		String val = split.length == 5 ? split[4] : null;

		if (!resident.hasMeta() || !resident.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_x_is_not_part_of_this_resident", mdKey));

		CustomDataField<?> cdf = resident.getMetadata(mdKey);

		// Check if the given value is valid for this field.
		try {
			if (val == null)
				throw new InvalidMetadataTypeException(cdf);

			cdf.isValidType(val);
		} catch (InvalidMetadataTypeException e) {
			throw new TownyException(e.getMessage());
		}

		// Change state
		cdf.setValueFromString(val);

		// Let user know that it was successful.
		TownyMessaging.sendMsg(sender, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

		// Save changes.
		resident.save();
	}

	private static void handleResidentMetaAdd(CommandSender sender, Resident resident, final String mdKey) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey))
			throw new TownyException(Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));
		
		CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

		if (resident.hasMeta() && resident.hasMeta(md.getKey()))
			throw new TownyException(Translatable.of("msg_err_key_x_already_exists", mdKey));

		TownyMessaging.sendMsg(sender, Translatable.of("msg_custom_data_was_successfully_added_to_resident"));
		
		resident.addMetaData(md.clone(), true);
	}

	private static void handleResidentMetaRemove(CommandSender sender, Resident resident, final String mdKey) throws TownyException {
		if (!resident.hasMeta() || !resident.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_cannot_be_deleted"));
		resident.removeMetaData(mdKey, true);
		TownyMessaging.sendMsg(sender, Translatable.of("msg_data_successfully_deleted"));
	}

	public static void handlePlotMetaCommand(Player player, String[] split) throws TownyException {
		
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_META.getNode());

		WorldCoord wc = WorldCoord.parseWorldCoord(player);
		if (wc.isWilderness())
			throw new TownyException(Translatable.of("msg_not_claimed_1"));
		TownBlock townBlock = wc.getTownBlockOrNull();

		if (split.length == 1) {
			displayPlotMeta(player, townBlock);
			return;
		}

		if (split.length < 3) {
			HelpMenu.TA_PLOT_META.send(player);
			return;
		}

		final String mdKey = split[2];
		switch(split[1].toLowerCase(Locale.ROOT)) {
		case "set" -> handlePlotMetaSet(player, split, townBlock, mdKey);
		case "add" -> handlePlotMetaAdd(player, townBlock, mdKey);
		case "remove" -> handlePlotMetaRemove(player, townBlock, mdKey);
		default -> HelpMenu.TA_PLOT.send(player);
		}
	}

	private static void displayPlotMeta(Player player, TownBlock townBlock) throws TownyException {
		if (!townBlock.hasMeta())
			throw new TownyException(Translatable.of("msg_err_this_plot_doesnt_have_any_associated_metadata"));

		TownyMessaging.sendMessage(player, ChatTools.formatTitle("Custom Meta Data"));
		for (CustomDataField<?> field : townBlock.getMetadata()) {
			TownyMessaging.sendMessage(player, field.getKey() + " = " + field.getValue());
		}
	}

	private static void handlePlotMetaSet(Player player, String[] split, TownBlock townBlock, final String mdKey) throws TownyException {
		String val = split.length == 4 ? split[3] : null;
		if (!townBlock.hasMeta() || !townBlock.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_x_is_not_part_of_this_plot", mdKey));

		CustomDataField<?> cdf = townBlock.getMetadata(mdKey);

		// Change state
		try {
			if (val == null)
				throw new InvalidMetadataTypeException(cdf); 
					
			cdf.isValidType(val);
		} catch (InvalidMetadataTypeException e) {
			throw new TownyException(e.getMessage());
		}

		cdf.setValueFromString(val);

		// Let user know that it was successful.
		TownyMessaging.sendMsg(player, Translatable.of("msg_key_x_was_successfully_updated_to_x", mdKey, cdf.getValue()));

		// Save changes.
		townBlock.save();
	}

	private static void handlePlotMetaAdd(Player player, TownBlock townBlock, final String mdKey) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey))
			throw new TownyException(Translatable.of("msg_err_the_metadata_for_key_is_not_registered", mdKey));

		CustomDataField<?> md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
		if (townBlock.hasMeta() && townBlock.hasMeta(md.getKey()))
			throw new TownyException(Translatable.of("msg_err_key_x_already_exists", mdKey));

		TownyMessaging.sendMsg(player, Translatable.of("msg_custom_data_was_successfully_added_to_townblock"));

		townBlock.addMetaData(md.clone());
	}

	private static void handlePlotMetaRemove(Player player, TownBlock townBlock, final String mdKey)
			throws TownyException {
		if (!townBlock.hasMeta() || !townBlock.hasMeta(mdKey))
			throw new TownyException(Translatable.of("msg_err_key_cannot_be_deleted"));
		CustomDataField<?> cdf = townBlock.getMetadata(mdKey);
		townBlock.removeMetaData(cdf);
		TownyMessaging.sendMsg(player, Translatable.of("msg_data_successfully_deleted"));
	}
	
	private void parseAdminEcoCommand(CommandSender sender, String[] split) throws TownyException {
		if (!TownyEconomyHandler.isActive())
			throw new TownyException(Translatable.of("msg_err_no_economy"));

		if (split.length < 1) {
			HelpMenu.TA_ECO.send(sender);
			return;
		} 

		switch (split[0].toLowerCase(Locale.ROOT)) {
			case "depositall" -> parseAdminDepositAllCommand(sender, StringMgmt.remFirstArg(split));
			case "depositalltowns" -> parseAdminDepositAllTownsCommand(sender, StringMgmt.remFirstArg(split));
			case "depositallnations" -> parseAdminDepositAllNationsCommand(sender, StringMgmt.remFirstArg(split));
			case "resetbanks" -> parseAdminResetBanksCommand(sender, StringMgmt.remFirstArg(split));
			case "convert" -> parseAdminConvertEconomyCommand(sender, StringMgmt.remFirstArg(split));
			case "info" -> parseAdminEcoInfoCommand(sender, StringMgmt.remFirstArg(split));
		}
	}

	private void parseAdminDepositAllCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_DEPOSITALL.getNode());
		if (split.length != 1) {
			HelpMenu.TA_DEPOSITALL.send(sender);
			return;
		}
		String reason = "townyadmin depositall";
		double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[0]);
		TownyEconomyHandler.economyExecutor().execute(() -> {
			for (Nation nation : TownyUniverse.getInstance().getNations())
				nation.getAccount().deposit(amount, reason);
			
			for (Town town : TownyUniverse.getInstance().getTowns())
				town.getAccount().deposit(amount, reason);
		});
		TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_deposit_all_success", TownyEconomyHandler.getFormattedBalance(amount)));
	}

	private void parseAdminDepositAllTownsCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_DEPOSITALLTOWNS.getNode());
		if (split.length != 1) {
			HelpMenu.TA_DEPOSITALLTOWNS.send(sender);
			return;
		}
		String reason = "townyadmin depositalltowns";
		double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[0]);
		TownyEconomyHandler.economyExecutor().execute(() -> {
			for (Town town : TownyUniverse.getInstance().getTowns())
				town.getAccount().deposit(amount, reason);
		});
		TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_deposit_all_towns_success", TownyEconomyHandler.getFormattedBalance(amount)));
	}

	private void parseAdminDepositAllNationsCommand(CommandSender sender, String[] split) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_DEPOSITALLNATIONS.getNode());
		if (split.length != 1) {
			HelpMenu.TA_DEPOSITALLNATIONS.send(sender);
			return;
		}
		String reason = "townyadmin depositallnations";
		double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[0]);
		TownyEconomyHandler.economyExecutor().execute(() -> {
			for (Nation nation : TownyUniverse.getInstance().getNations())
				nation.getAccount().deposit(amount, reason);
		});
		TownyMessaging.sendMsg(sender, Translatable.of("msg_ta_deposit_all_nations_success", TownyEconomyHandler.getFormattedBalance(amount)));
	}
	private void parseAdminResetBanksCommand(CommandSender sender, String[] args) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_RESETBANKS.getNode());
		final double amount = args.length == 1 ? MathUtil.getIntOrThrow(args[0]) : 0.0;
 		Confirmation.runOnAccept(() -> {
			 TownyEconomyHandler.economyExecutor().execute(() -> {
				 for (Town town : TownyUniverse.getInstance().getTowns())
					 town.getAccount().setBalance(amount, "Admin used /ta resetbanks");

				 for (Nation nation : TownyUniverse.getInstance().getNations())
					 nation.getAccount().setBalance(amount, "Admin used /ta resetbanks");
			 });
 		})
 		.setTitle(Translatable.of("confirmation_are_you_sure_you_want_to_reset_all_banks", TownyEconomyHandler.getFormattedBalance(amount)))
 		.sendTo(sender);
	}
	
	private void parseAdminConvertEconomyCommand(CommandSender sender, String[] args) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_CONVERT.getNode());
		
		EconomyProvider provider = TownyEconomyHandler.getProvider();
		if (provider == null)
			throw new TownyException(Translatable.of("msg_admin_eco_convert_no_providers"));
		
		if (args.length == 0)
			throw new TownyException(Translatable.of("msg_admin_eco_convert_no_target_specified"));
		
		EconomyAdapter target;
		
		final String targetName = String.join(" ", args);
		if ("modern".equalsIgnoreCase(targetName)) {
			if (!provider.isLegacy())
				throw new TownyException(Translatable.of("msg_admin_eco_convert_already_modern"));
			
			// Temporarily set to modern to create a non-legacy adapter as the target
			provider.setLegacy(false);
			try {
				target = provider.mainAdapter();
			} finally {
				provider.setLegacy(true);
			}
		} else {
			target = provider.getEconomyAdapter(targetName);
		}
		
		if (target == null)
			throw new TownyException(Translatable.of("msg_admin_eco_convert_target_not_found", targetName, provider.economyAdapters().stream().map(EconomyAdapter::name).collect(Collectors.joining(", "))));

		if (Bukkit.getOnlinePlayers().size() > 0) 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_eco_convert_online_players_warning"));

		Confirmation.runOnAccept(() -> TownyEconomyHandler.economyExecutor().execute(() -> {
			Map<Account, Double> balances = new HashMap<>();
			
			// Gather up all the balances
			for (Town town : TownyUniverse.getInstance().getTowns()) {
				balances.put(town.getAccount(), town.getAccount().getHoldingBalance());

				town.getAccount().setUUID(TownyEconomyHandler.modifyNPCUUID(town.getUUID()));
			}
			
			for (Nation nation : TownyUniverse.getInstance().getNations()) {
				balances.put(nation.getAccount(), nation.getAccount().getHoldingBalance());

				nation.getAccount().setUUID(TownyEconomyHandler.modifyNPCUUID(nation.getUUID()));
			}
			
			for (Resident resident : TownyUniverse.getInstance().getResidents()) {
				balances.put(resident.getAccount(), resident.getAccount().getHoldingBalance());

				if (resident.isNPC())
					resident.getAccount().setUUID(TownyEconomyHandler.modifyNPCUUID(resident.getUUID()));
			}
			
			balances.put(TownyServerAccount.ACCOUNT, TownyServerAccount.ACCOUNT.getHoldingBalance());

			TownyMessaging.sendMessage(sender, Translatable.of("msg_admin_eco_convert_x_accounts_found", balances.size()));
			HashMap<Integer, String> progressMap = new HashMap<>();
			int tenPercent = balances.size() / 10;
			for (int i = 1; i < 10; i++)
				progressMap.put(tenPercent * i, i + "0%");

			int i = 0;
			// And set them to the target economy adapter
			for (Map.Entry<Account, Double> entry : balances.entrySet()) {
				i++;
				if (progressMap.containsKey(i))
					TownyMessaging.sendMessage(sender, Translatable.of("msg_admin_eco_convert_conversion_progress", progressMap.get(i)));

				// Skip accounts with no balance
				if (entry.getValue() == 0)
					continue;
				
				if (!target.hasAccount(entry.getKey())) {
					TownyMessaging.sendDebugMsg("Creating new account for " + entry.getKey().getUUID());
					target.newAccount(entry.getKey());
				}

				target.setBalance(entry.getKey(), entry.getValue());
				TownyMessaging.sendDebugMsg("Setting balance of account " + entry.getKey().getUUID() + " to " + entry.getValue());
			}
			
			// Change config to modern & setup economy again
			if ("modern".equalsIgnoreCase(targetName)) {
				TownySettings.setProperty(ConfigNodes.ECO_ADVANCED_MODERN.getRoot(), true);
				TownySettings.saveConfig();
				TownyEconomyHandler.setupEconomy();
			}
			
			TownyMessaging.sendMsg(sender, Translatable.of("msg_admin_eco_convert_success"));
		})).sendTo(sender);
	}

	private void parseAdminEcoInfoCommand(CommandSender sender, String[] args) throws TownyException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ECO_INFO.getNode());
		if (args.length == 0 || (!args[0].equalsIgnoreCase("serveraccount") && args.length < 2)) {
			HelpMenu.TA_ECO_INFO.send(sender);
			return;
		}

		Account account = switch (args[0].toLowerCase(Locale.ROOT)) {
			case "serveraccount" -> TownyServerAccount.ACCOUNT;
			case "nation" -> getNationOrThrow(args[1]).getAccount();
			case "resident" -> getResidentOrThrow(args[1]).getAccount();
			case "town" -> getTownOrThrow(args[1]).getAccount();
			default -> null;
		};

		if (account == null)
			throw new TownyException("Account not found.");

		TownyEconomyHandler.economyExecutor().execute(() -> {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle("Account Info"));
			TownyMessaging.sendMessage(sender, "Name: " + account.getName());
			TownyMessaging.sendMessage(sender, "UUID: " + account.getUUID());
			TownyMessaging.sendMessage(sender, "Balance: " + account.getHoldingBalance());
		});
	}

	private void parseAdminInstall(CommandSender sender) throws NoPermissionException {
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_INSTALL.getNode());
		
		if (plugin.isFolia())
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_setup_command_folia"));
		
		sender.sendMessage(Component.text(Translatable.of("msg_setup_full_guide_link").forLocale(sender)).clickEvent(ClickEvent.openUrl("https://github.com/TownyAdvanced/Towny/wiki/Installation")));
		
		if (plugin.isFolia())
			return;
		
		new SetupConversation(sender).runOnResponse(response -> {
			ConversationContext context = (ConversationContext) response;
			
			toggleWildernessUsage(parseBoolean(context.getSessionData(0)));
			toggleRevertUnclaim(parseBoolean(context.getSessionData(1)));
			TownySettings.setProperty(ConfigNodes.CLAIMING_TOWN_BLOCK_RATIO.getRoot(), Integer.parseInt((String) context.getSessionData(2)));
			
			if (TownyEconomyHandler.isActive()) {
				TownySettings.setProperty(ConfigNodes.ECO_PRICE_NEW_TOWN.getRoot(), Integer.parseInt((String) context.getSessionData(3)));
				TownySettings.setProperty(ConfigNodes.ECO_PRICE_NEW_NATION.getRoot(), Integer.parseInt((String) context.getSessionData(4)));
				TownySettings.setProperty(ConfigNodes.ECO_PRICE_CLAIM_TOWNBLOCK.getRoot(), Integer.parseInt((String) context.getSessionData(5)));
			}
			
			TownySettings.saveConfig();
			TownyMessaging.sendMsg(sender, Translatable.of("msg_setup_success"));
		});
	}

	private void parseAdminTownForSaleCommand(CommandSender sender, Town town, String[] arg) throws TownyException {
		if (arg.length == 0)
			throw new TownyException(Translatable.of("msg_error_must_be_num"));

		double forSalePrice = MathUtil.getDoubleOrThrow(arg[0]);
		TownCommand.setTownForSale(town, forSalePrice, true);

		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_forsale", town.getName(), TownyEconomyHandler.getFormattedBalance(forSalePrice)));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_forsale", town.getName(), TownyEconomyHandler.getFormattedBalance(forSalePrice)));
	}

	private void parseAdminTownNotForSaleCommand(CommandSender sender, Town town) throws TownyException {
		if (!town.isForSale())
			throw new TownyException(Translatable.of("msg_town_buytown_not_forsale"));
		
		TownCommand.setTownNotForSale(town, true);

		TownyMessaging.sendMsg(sender, Translatable.of("msg_town_notforsale", town.getName()));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_notforsale", town.getName()));
	}
}
