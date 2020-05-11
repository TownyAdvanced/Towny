package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.TownPreRenameEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.InvalidMetadataTypeException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.PlotClaim;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import javax.naming.InvalidNameException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> ta_help = new ArrayList<>();
	private static final List<String> ta_panel = new ArrayList<>();
	private static final List<String> ta_unclaim = new ArrayList<>();
	private static final List<String> adminTabCompletes = Arrays.asList(
		"delete",
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
		"newday",
		"newhour",
		"unclaim",
		"purge",
		"mysqldump",
		"tpplot",
		"database",
		"depositall"
	);

	private static final List<String> adminTownTabCompletes = Arrays.asList(
		"new",
		"add",
		"remove",
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
		"outlaw"
	);

	private static final List<String> adminNationTabCompletes = Arrays.asList(
		"add",
		"rename",
		"delete",
		"toggle",
		"set",
		"meta",
		"deposit",
		"withdraw"
	);

	private static final List<String> adminToggleTabCompletes = Arrays.asList(
		"war",
		"neutral",
		"npc",
		"debug",
		"devmode",
		"townwithdraw",
		"nationwithdraw"
	);
	
	private static final List<String> adminPlotTabCompletes = Arrays.asList(
		"claim",
		"meta"
	);
	
	private static final List<String> adminPlotMetaTabCompletes = Arrays.asList(
		"set",
		"add",
		"remove"
	);
	
	private static final List<String> adminDatabaseTabCompletes = Arrays.asList(
		"save",
		"load"
	);
	
	private static final List<String> adminResidentTabCompletes = Arrays.asList(
		"rename",
		"friend",
		"unjail"
	);
	
	private static final List<String> adminResidentFriendTabCompletes = Arrays.asList(
		"add",
		"remove",
		"list",
		"clear"
	);
	
	private static final List<String> adminSetCompletes = Arrays.asList(
		"mayor",
		"capital",
		"title",
		"surname",
		"plot"
	);
	

	private boolean isConsole;
	private Player player;
	private CommandSender sender;

	static {
		ta_help.add(ChatTools.formatTitle("/townyadmin"));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "", TownySettings.getLangString("admin_panel_1")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' " + TownySettings.getLangString("res_5")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "unclaim [radius]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "town/nation", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "plot", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "givebonus [town/player] [num]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "toggle peaceful/war/debug/devmode", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "resident/town/nation", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "tpplot {world} {x} {z}", ""));

		// TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin",
		// "npc rename [old name] [new name]", ""));
		// TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin",
		// "npc list", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "checkperm {name} {node}", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reload", TownySettings.getLangString("admin_panel_2")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reset", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "backup", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "mysqldump", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "database [save/load]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newday", TownySettings.getLangString("admin_panel_3")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newhour", TownySettings.getLangString("admin_panel_4")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "purge [number of days]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "delete [] .. []", "delete a residents data files."));

		ta_unclaim.add(ChatTools.formatTitle("/townyadmin unclaim"));
		ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin unclaim", "", TownySettings.getLangString("townyadmin_help_1")));
		ta_unclaim.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin unclaim", "[radius]", TownySettings.getLangString("townyadmin_help_2")));

	}

	public TownyAdminCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		this.sender = sender;

		if (sender instanceof Player) {
			player = (Player) sender;
			isConsole = false;

		} else {
			isConsole = true;
			this.player = null;
		}

		try {
			return parseTownyAdminCommand(args);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}

		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "reload":
				if (args.length > 1)
					return NameUtil.filterByStart(Arrays.asList("database", "db", "config", "perms", "permissions", "language", "lang", "townyperms", "all"), args[1]);
			case "set":
				if (args.length > 1) {
					switch (args[1].toLowerCase()) {
						case "mayor":
							switch (args.length) {
								case 3:
									return getTownyStartingWith(args[2], "t");
								case 4:
									return filterByStartOrGetTownyStartingWith(Collections.singletonList("npc"), args[3], "+r");
							}
						case "capital":
						case "plot":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "t");
						case "title":
						case "surname":
							if (args.length == 3)
								return getTownyStartingWith(args[2], "r");
						default:
							if (args.length == 2)
								return NameUtil.filterByStart(adminSetCompletes, args[1]);
					}
				}
				break;
			case "plot":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminPlotTabCompletes, args[1]);
				} else if (args.length > 2) {
					switch (args[1].toLowerCase()) {
						case "claim":
							return getTownyStartingWith(args[2], "r");
						case "meta":
							if (args.length == 3)
								return NameUtil.filterByStart(adminPlotMetaTabCompletes, args[2]);
					}
				}
				break;
			case "givebonus":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "rt");
				break;
			case "toggle":
				if (args.length == 2) {
					return NameUtil.filterByStart(adminToggleTabCompletes, args[1]);
				} else if (args.length == 3 && args[1].equalsIgnoreCase("npc")) {
					return getTownyStartingWith(args[2], "r");
				}
				break;
			case "tpplot":
				if (args.length == 2) {
					return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getWorlds()
						.stream()
						.map(TownyWorld::getName)
						.collect(Collectors.toList()), args[1]);
				}
				break;
			case "checkperm":
			case "delete":
				if (args.length == 2)
					return getTownyStartingWith(args[1], "r");
				break;
			case "database":
				if (args.length == 2)
					return NameUtil.filterByStart(adminDatabaseTabCompletes, args[1]);
				break;
			case "resident":
				switch (args.length) {
					case 2:
						return getTownyStartingWith(args[1], "r");
					case 3:
						return NameUtil.filterByStart(adminResidentTabCompletes, args[2]);
					case 4:
						if (args[2].equalsIgnoreCase("friend"))
							return NameUtil.filterByStart(adminResidentFriendTabCompletes, args[3]);
				}
				break;
			case "town":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+t");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
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
									switch (args[3].toLowerCase()) {
										case "add":
											return NameUtil.filterByStart(TownyPerms.getTownRanks(), args[5]);
										case "remove":
											try {
												return NameUtil.filterByStart(TownyUniverse.getInstance().getDataSource().getResident(args[4]).getTownRanks(), args[5]);
											} catch (NotRegisteredException ignored) {}
									}
							}
							break;
						case "set":
							try {
								return TownCommand.townSetTabComplete(TownyUniverse.getInstance().getDataSource().getTown(args[1]), StringMgmt.remArgs(args, 2));
							} catch (NotRegisteredException ignored) {}
						case "toggle":
							if (args.length == 4)
								return NameUtil.filterByStart(TownCommand.townToggleTabCompletes, args[3]);
						case "outlaw":
							switch (args.length) {
							case 4:
								return NameUtil.filterByStart(TownCommand.townAddRemoveTabCompletes, args[3]);
							case 5:
								switch (args[3].toLowerCase()) {
									case "add":
										return getTownyStartingWith(args[4], "r");
									case "remove":
										try {
											return NameUtil.filterByStart(NameUtil.getNames(TownyUniverse.getInstance().getDataSource().getTown(args[1]).getOutlaws()), args[4]);
										} catch (TownyException ignore) {}
								}
							}
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(adminTownTabCompletes, args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "r");
				}
				break;
			case "nation":
				if (args.length == 2) {
					return filterByStartOrGetTownyStartingWith(Collections.singletonList("new"), args[1], "+n");
				} else if (args.length > 2 && !args[1].equalsIgnoreCase("new")) {
					switch (args[2].toLowerCase()) {
						case "add":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "t");
						case "toggle":
							if (args.length == 4) 
								return NameUtil.filterByStart(NationCommand.nationToggleTabCompletes, args[3]);
						case "set":
							try {
								return NationCommand.nationSetTabComplete(TownyUniverse.getInstance().getDataSource().getNation(args[1]), StringMgmt.remArgs(args, 2));
							} catch (NotRegisteredException e) {
								return Collections.emptyList();
							}
						case "merge":
							if (args.length == 4)
								return getTownyStartingWith(args[3], "n");
						default:
							if (args.length == 3)
								return NameUtil.filterByStart(adminNationTabCompletes, args[2]);
					}
				} else if (args.length == 4 && args[1].equalsIgnoreCase("new")) {
					return getTownyStartingWith(args[3], "t");
				}
				break;
			case "unclaim":
				if (args.length == 2)
					return NameUtil.filterByStart(TownCommand.townUnclaimTabCompletes, args[1]);
			default:
				if (args.length == 1)
					return NameUtil.filterByStart(adminTabCompletes, args[0]);
		}
		
		return Collections.emptyList();
	}

	private Object getSender() {

		if (isConsole)
			return sender;
		else
			return player;
	}

	public boolean parseTownyAdminCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (getSender()==player && !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SCREEN.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
		if (split.length == 0) {
			buildTAPanel();
			for (String line : ta_panel) {
				sender.sendMessage(line);
			}

		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			for (String line : ta_help) {
				sender.sendMessage(line);
			}
		} else {

			if (split[0].equalsIgnoreCase("set")) {

				adminSet(StringMgmt.remFirstArg(split));
				return true;
			} else if (split[0].equalsIgnoreCase("resident")){
				
				parseAdminResidentCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("town")) {

				parseAdminTownCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("nation")) {

				parseAdminNationCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("toggle")) {

				parseToggleCommand(StringMgmt.remFirstArg(split));
				return true;

			} else if (split[0].equalsIgnoreCase("plot")) {

				parseAdminPlotCommand(StringMgmt.remFirstArg(split));
				return true;

			}

			if ((!isConsole) && (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode(split[0].toLowerCase()))))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("givebonus") || split[0].equalsIgnoreCase("giveplots")) {

				giveBonus(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("reload")) {
				if (split.length == 2) {
					switch (split[1]) {
						case "db":
						case "database":
							reloadDatabase();
							break;
						case "config":
							reloadConfig(false);
							break;
						case "perms":
						case "townyperms":
						case "permissions":
							reloadPerms();
							break;
						case "language":
						case "lang":
							reloadLangs();
							break;
						case "all":
							reloadConfig(false);
							reloadLangs();
							reloadDatabase();
							reloadPerms();
							break;
						default:
							showReloadHelp();
					}
				} else {
					showReloadHelp();
					return false;
				}
			} else if (split[0].equalsIgnoreCase("reset")) {

				reloadConfig(true);

			} else if (split[0].equalsIgnoreCase("backup")) {

				try {
					townyUniverse.getDataSource().backup();
					TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("mag_backup_success"));

				} catch (IOException e) {
					TownyMessaging.sendErrorMsg(getSender(), "Error: " + e.getMessage());

				}
			} else if (split[0].equalsIgnoreCase("database")) {

				parseAdminDatabaseCommand(StringMgmt.remFirstArg(split));
				return true;				
				
			} else if (split[0].equalsIgnoreCase("mysqldump")) {
				if (TownySettings.getSaveDatabase().equalsIgnoreCase("mysql") && TownySettings.getLoadDatabase().equalsIgnoreCase("mysql")) {
					TownyDataSource dataSource = new TownyFlatFileSource(plugin, townyUniverse);
					dataSource.saveAll();
					TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_mysql_dump_success"));
					return true;
				} else 
					throw new TownyException(TownySettings.getLangString("msg_err_mysql_not_being_used"));

			} else if (split[0].equalsIgnoreCase("newday")) {

				TownyTimerHandler.newDay();

			} else if (split[0].equalsIgnoreCase("newhour")) {

				TownyTimerHandler.newHour();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_newhour_success"));

			} else if (split[0].equalsIgnoreCase("purge")) {

				purge(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("delete")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentDelete(player, newSplit);
			} else if (split[0].equalsIgnoreCase("unclaim")) {

				parseAdminUnclaimCommand(StringMgmt.remFirstArg(split));
				/*
				 * else if (split[0].equalsIgnoreCase("seed") &&
				 * TownySettings.getDebug()) seedTowny(); else if
				 * (split[0].equalsIgnoreCase("warseed") &&
				 * TownySettings.getDebug()) warSeed(player);
				 */
				
			} else if (split[0].equalsIgnoreCase("checkperm")) {
				
				parseAdminCheckPermCommand(StringMgmt.remFirstArg(split));
				
			} else if (split[0].equalsIgnoreCase("tpplot")) {
				
				parseAdminTpPlotCommand(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("depositall")) {
				
				parseAdminDepositAllCommand(StringMgmt.remFirstArg(split));
				
			}  else {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_sub"));
				return false;
			}
		}

		return true;
	}

	private void parseAdminDatabaseCommand(String[] split) {
	
		if (split.length == 0 || split.length > 2 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin database"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin database", "save", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin database", "load", ""));
			return;
		}
		
		if (split[0].equalsIgnoreCase("save")) {
			TownyUniverse.getInstance().getDataSource().saveAll();
			TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_save_success"));
	
		} else if (split[0].equalsIgnoreCase("load")) {
			TownyUniverse.getInstance().clearAll();			
			TownyUniverse.getInstance().getDataSource().loadAll();
			TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_load_success"));			
		}
	}

	private void parseAdminPlotCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (isConsole) {
			sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
			return;
		}

		if (split.length == 0 || split.length < 1 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin plot"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin plot claim", "[player]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin plot meta", "", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin plot meta", "set [key] [value]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin plot meta", "[add|remove] [key]", ""));
			return;
		}

		if (split[0].equalsIgnoreCase("meta")) {
			handlePlotMetaCommand(player, split);
			return;
		}
		
		if (split[0].equalsIgnoreCase("claim")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_CLAIM.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split.length == 1) {
				TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_ta_plot_claim"));
				return;
			}			
			Resident resident = null;
			try {
				resident = townyUniverse.getDataSource().getResident(split[1]);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(sender, String.format(TownySettings.getLangString("msg_error_no_player_with_that_name"), split[1]));
			}

			Player player = BukkitTools.getPlayer(sender.getName());
			String world = player.getWorld().getName();
			List<WorldCoord> selection = new ArrayList<>();
			selection.add(new WorldCoord(world, Coord.parseCoord(player)));

			if (resident != null) {
				new PlotClaim(plugin, player, resident, selection, true, true, false).start();
			}
		}
		
		
	}

	private void parseAdminCheckPermCommand(String[] split) throws TownyException {
		
		if (split.length !=2 ) {
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: /ta checkperm {name} {node}"));
		}
		Player player = TownyAPI.getInstance().getPlayer(TownyUniverse.getInstance().getDataSource().getResident(split[0]));
		if (player == null) {
			throw new TownyException("Player couldn't be found");
		}
		String node = split[1];
		if (player.hasPermission(node))
			TownyMessaging.sendMessage(sender, "Permission true");
		else
			TownyMessaging.sendErrorMsg(sender, "Permission false");
	}

	private void parseAdminTpPlotCommand(String[] split) throws TownyException {

		if (split.length != 3) {
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: /ta tpplot world x z"));
		}
		
		Player player = (Player) sender;
		World world;
		double x;
		double y = 1.0;
		double z;
		Location loc;
		if (Bukkit.getServer().getWorld(split[0]) != null ) {
			world =  Bukkit.getServer().getWorld(split[0]);
			x = Double.parseDouble(split[1]) * TownySettings.getTownBlockSize();
			z = Double.parseDouble(split[2]) * TownySettings.getTownBlockSize();
		} else {
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: /ta tpplot world x z"));
		}
		y = Bukkit.getWorld(world.getName()).getHighestBlockYAt(new Location(world, x, y, z));
		loc = new Location(world, x, y, z);
		player.teleport(loc, TeleportCause.PLUGIN);
	}

	private void giveBonus(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Town town;
		boolean isTown = false;

		try {
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: givebonus [town/player] [n]"));
			try {
				town = townyUniverse.getDataSource().getTown(split[0]);
				isTown = true;
			} catch (NotRegisteredException e) {
				town = townyUniverse.getDataSource().getResident(split[0]).getTown();
			}
			try {
				town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[1].trim()));
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_give_total"), town.getName(), split[1], town.getBonusBlocks()));
				if (!isConsole || isTown)
					TownyMessaging.sendTownMessagePrefixed(town, "You have been given " + Integer.parseInt(split[1].trim()) + " bonus townblocks.");
				if (isConsole && !isTown) {
					TownyMessaging.sendMessage(town, "You have been given " + Integer.parseInt(split[1].trim()) + " bonus townblocks.");
					TownyMessaging.sendMessage(town, "If you have paid any real-life money for these townblocks please understand: the creators of Towny do not condone this transaction, the server you play on breaks the Minecraft EULA and, worse, is selling a part of Towny which the developers did not intend to be sold.");
					TownyMessaging.sendMessage(town, "If you did pay real money you should consider playing on a Towny server that respects the wishes of the Towny Team.");
				}
			} catch (NumberFormatException nfe) {
				throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
			}
			townyUniverse.getDataSource().saveTown(town);
		} catch (TownyException e) {
			throw new TownyException(e.getMessage());
		}

	}

	private void buildTAPanel() {

		ta_panel.clear();
		Runtime run = Runtime.getRuntime();
		ta_panel.add(ChatTools.formatTitle(TownySettings.getLangString("ta_panel_1")));
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_2") + Colors.LightGreen + TownyAPI.getInstance().isWarTime() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_3") + (TownyTimerHandler.isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off") + Colors.Gray + " | " + (Colors.Green + TownySettings.getLangString("ta_panel_5") + (TownyTimerHandler.isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")));
		/*
		 * ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" +
		 * Colors.Blue + "] " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_4") +
		 * (TownySettings.isRemovingWorldMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off") + Colors.Gray + " | " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_4_1") +
		 * (TownySettings.isRemovingTownMobs() ? Colors.LightGreen + "On" :
		 * Colors.Rose + "Off"));
		 *
		 * try { TownyEconomyObject.checkEconomy(); ta_panel.add(Colors.Blue +
		 * "[" + Colors.LightBlue + "Economy" + Colors.Blue + "] " +
		 * Colors.Green + TownySettings.getLangString("ta_panel_6") +
		 * Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) +
		 * Colors.Gray + " | " + Colors.Green +
		 * TownySettings.getLangString("ta_panel_7") + Colors.LightGreen +
		 * getNumBankAccounts()); } catch (Exception e) { }
		 */
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + TownySettings.getLangString("ta_panel_8") + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_9") + Colors.LightGreen + MemMgmt.getMemSize(run.totalMemory()) + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_10") + Colors.LightGreen + Thread.getAllStackTraces().keySet().size() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_11") + Colors.LightGreen + TownyFormatter.getTime());
		ta_panel.add(Colors.Yellow + MemMgmt.getMemoryBar(50, run));

	}

	public void parseAdminUnclaimCommand(String[] split) {

		if (split.length == 1 && split[0].equalsIgnoreCase("?")) {
			for (String line : ta_unclaim)
				((CommandSender) getSender()).sendMessage(line);
		} else {

			if (isConsole) {
				sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
				return;
			}

			try {
				if (TownyAPI.getInstance().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				List<WorldCoord> selection;
				selection = AreaSelectionUtil.selectWorldCoordArea(null, new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)), split);
				selection = AreaSelectionUtil.filterWildernessBlocks(selection);

				new TownClaim(plugin, player, null, selection, false, false, true).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		}
	}

	public void parseAdminResidentCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin resident"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] friend... [add|remove] [resident]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] friend... |list|clear]", ""));
			
			return;
		}

		try	{
			Resident resident = townyUniverse.getDataSource().getResident(split[0]);

			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(resident, player));
				return;
			}
						
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(split[1].equalsIgnoreCase("rename"))	{
				
				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renamePlayer(resident, split[2]);
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
				
			} else if(split[1].equalsIgnoreCase("friend"))	{
				
				if (split.length == 2) {
					sender.sendMessage(ChatTools.formatTitle("/townyadmin resident {resident} friend"));
					sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] friend... [add|remove] [resident]", ""));
					sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] friend... |list|clear]", ""));
					return;
				}
				if (isConsole)
					throw new TownyException("/ta resident {resident} friend cannot be run from console.");

				ResidentCommand.residentFriend(BukkitTools.getPlayer(sender.getName()), StringMgmt.remArgs(split, 2), true, resident);

			} else if(split[1].equalsIgnoreCase("unjail")) {
				
				Player jailedPlayer = TownyAPI.getInstance().getPlayer(resident);
				if (player == null) {
					throw new TownyException(String.format("%s is not online", resident.getName()));
				}

				if(resident.isJailed())	{
					resident.setJailed(false);
					final String town = resident.getJailTown();
					final int index = resident.getJailSpawn();
					try	{
						final Location loc = Bukkit.getWorld(TownyAPI.getInstance().getDataSource().getTown(town).getHomeblockWorld().getName()).getSpawnLocation();

						// Use teleport warmup
						jailedPlayer.sendMessage(String.format(TownySettings.getLangString("msg_town_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
						TownyAPI.getInstance().jailTeleport(jailedPlayer, loc);

						resident.removeJailSpawn();
						resident.setJailTown(" ");
						TownyMessaging.sendMsg(player, "You have been freed from jail.");
						TownyMessaging.sendPrefixedTownMessage(townyUniverse.getDataSource().getTown(town), jailedPlayer.getName() + " has been freed from jail number " + index);
					} catch (TownyException e) {
						e.printStackTrace();
					}
				} else {
					throw new TownyException(TownySettings.getLangString("msg_player_not_jailed_in_your_town"));
				}
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}
	
	public void parseAdminTownCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "new [name] [mayor]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] add/kick [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] delete", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] spawn", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] outpost #", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rank", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] set", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] toggle", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] meta", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] deposit [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] withdraw [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] outlaw [add|remove] [name]", ""));
			
			return;
		}

		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				/*
				 * Moved from TownCommand as of 0.92.0.13
				 */
				if (split.length != 3)
					throw new TownyException(TownySettings.getLangString("msg_err_not_enough_variables") + "/ta town new [name] [mayor]");

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_NEW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				TownCommand.newTown(player, split[1], split[2], true);
				return;
			}
			
			Town town = townyUniverse.getDataSource().getTown(split[0]);
			
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(town));
				return;
			}

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			if (split[1].equalsIgnoreCase("invite")) {
				// Give admins the ability to invite a player to town, invite still requires acceptance.
				TownCommand.townAdd(getSender(), town, StringMgmt.remArgs(split, 2));
				
			} else if (split[1].equalsIgnoreCase("add")) {
				// Force-join command for admins to use to bypass invites system.
				Resident resident;
				try {
					resident = townyUniverse.getDataSource().getResident(split[2]);
				} catch (NotRegisteredException e) {
					TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_error_no_player_with_that_name"), split[2]));
					return;
				}
				TownCommand.townAddResident(town, resident);
				TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_join_town"), resident.getName()));
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_join_town"), resident.getName()));
				
			} else if (split[1].equalsIgnoreCase("kick")) {

				TownCommand.townKickResidents(getSender(), town.getMayor(), town, ResidentUtil.getValidatedResidents(getSender(), StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {
				if (!isConsole) {
					TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("town_deleted_by_admin"), town.getName()));
					TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_town"), town.getName()));
					townyUniverse.getDataSource().removeTown(town, false);
				} else { //isConsole
					Confirmation confirmation = new Confirmation(() -> {
						TownyMessaging.sendGlobalMessage(TownySettings.getDelTownMsg(town));
						TownyUniverse.getInstance().getDataSource().removeTown(town);
					});
					ConfirmationHandler.sendConfirmation(sender, confirmation);
				}

			} else if (split[1].equalsIgnoreCase("rename")) {
				
				TownPreRenameEvent event = new TownPreRenameEvent(town, split[2]);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_err_rename_cancelled"));
					return;
				}

				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renameTown(town, split[2]);
					TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
					TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_town_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
				} else {
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
				}
				
			} else if (split[1].equalsIgnoreCase("spawn")) {

				SpawnUtil.sendToTownySpawn(player, StringMgmt.remArgs(split, 2), town, "", false, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("outpost")) {

				SpawnUtil.sendToTownySpawn(player, StringMgmt.remArgs(split, 2), town, "", true, false, SpawnType.TOWN);

			} else if (split[1].equalsIgnoreCase("rank")) {
				
				parseAdminTownRankCommand(player, town, StringMgmt.remArgs(split, 2));
			} else if (split[1].equalsIgnoreCase("toggle")) {
				
				if (split.length == 2 || split[2].equalsIgnoreCase("?")) {
					sender.sendMessage(ChatTools.formatTitle("/ta town {townname} toggle"));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "pvp", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "forcepvp", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "public", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "explosion", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "fire", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "mobs", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "taxpercent", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "open", ""));					
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "jail [number] [resident]", ""));
					sender.sendMessage(ChatTools.formatCommand("", "/ta town {townname} toggle", "forcepvp", ""));

					return;
				}
				
				if (split[2].equalsIgnoreCase("forcepvp")) {
					
					if (town.isAdminEnabledPVP())
						town.setAdminEnabledPVP(false);
					else 
						town.setAdminEnabledPVP(true);
					
					townyUniverse.getDataSource().saveTown(town);
					TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_town_forcepvp_setting_set_to"), town.getName(), town.isAdminEnabledPVP()));
					
				} else
					TownCommand.townToggle(sender, StringMgmt.remArgs(split, 2), true, town);
				
			} else if (split[1].equalsIgnoreCase("set")) {
				
				TownCommand.townSet(player, StringMgmt.remArgs(split, 2), true, town);
			} else if (split[1].equalsIgnoreCase("meta")) {
				handleTownMetaCommand(player, town, split);
			} else if (split[1].equalsIgnoreCase("deposit")) {
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "deposit [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}
				
				town.getAccount().collect(amount, "Admin Deposit");
				
				// Send notifications
				String depositMessage = String.format(TownySettings.getLangString("msg_xx_deposited_xx"), (isConsole ? "Console" : player.getName()), amount,  TownySettings.getLangString("town_sing"));
				TownyMessaging.sendMessage(sender, depositMessage);
				TownyMessaging.sendPrefixedTownMessage(town, depositMessage);
			} else if (split[1].equalsIgnoreCase("withdraw")) {
				int amount;

				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "withdraw [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}

				town.getAccount().pay(amount, "Admin Withdraw");
				
				// Send notifications
				String withdrawMessage = String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), (isConsole ? "Console" : player.getName()), amount,  TownySettings.getLangString("town_sing"));
				TownyMessaging.sendMessage(sender, withdrawMessage);
				TownyMessaging.sendPrefixedTownMessage(town, withdrawMessage);
			} else if (split[1].equalsIgnoreCase("outlaw")) {
				TownCommand.parseTownOutlawCommand(sender, StringMgmt.remArgs(split, 2), true, town);				
			} else {
				sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "new [name] [mayor]", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town]", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] add/kick [] .. []", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rename [newname]", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] delete", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] spawn", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] outpost #", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rank", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] set", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] toggle", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] meta", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] deposit [amount]", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] withdraw [amount]", ""));
				sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] outlaw [add|remove] [name]", ""));
				
				return;
			}

		} catch (TownyException | EconomyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
		
	}

	private void parseAdminTownRankCommand(Player player, Town town, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (split.length < 3) {
			throw new TownyException("Eg: /townyadmin town [townname] rank add/remove [resident] [rank]");
		}

		Resident target;
		
		try {

			target = townyUniverse.getDataSource().getResident(split[1]);
			if (!target.hasTown()) {
				throw new TownyException(TownySettings.getLangString("msg_resident_not_your_town"));
			}
			if (target.getTown() != town) {
				throw new TownyException(TownySettings.getLangString("msg_err_townadmintownrank_wrong_town"));
			}
			
		} catch (TownyException x) {
			throw new TownyException(x.getMessage());
		}

		String rank = split[2];
		/*
		 * Is this a known rank?
		 */
		if (!TownyPerms.getTownRanks().contains(rank))
			throw new TownyException(String.format(TownySettings.getLangString("msg_unknown_rank_available_ranks"), rank, StringMgmt.join(TownyPerms.getTownRanks(), ",") ));

		if (split[0].equalsIgnoreCase("add")) {
			try {
				if (target.addTownRank(rank)) {
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_given_rank"), "Town", rank));
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_given_rank"), "Town", rank, target.getName()));
				} else {
					// Not in a town or Rank doesn't exist
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_resident_not_your_town"));
					return;
				}
			} catch (AlreadyRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_already_has_rank"), target.getName(), "Town"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("remove")) {
			try {
				if (target.removeTownRank(rank)) {
					TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_had_rank_taken"), "Town", rank));
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_taken_rank_from"), "Town", rank, target.getName()));
				}
			} catch (NotRegisteredException e) {
				// Must already have this rank
				TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_doesnt_have_rank"), target.getName(), "Town"));
				return;
			}

		} else {
			TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
			return;
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		townyUniverse.getDataSource().saveResident(target);
		
	}

	public void parseAdminNationCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			sender.sendMessage(ChatTools.formatTitle("/townyadmin nation"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "new", "[name] [capital]"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] add [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] delete", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] recheck", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] toggle", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] set", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] deposit [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] withdraw [amount]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[oldnation] merge [newnation]", ""));

			return;
		}
		try {
			
			if (split[0].equalsIgnoreCase("new")) {
				/*
				 * Moved from TownCommand as of 0.92.0.13
				 */
				if (split.length != 3)
					throw new TownyException(TownySettings.getLangString("msg_err_not_enough_variables") + "/ta town new [name] [mayor]");

				if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_NEW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				NationCommand.newNation(player, split[1], split[2], true);
				return;
			}
			
			Nation nation = townyUniverse.getDataSource().getNation(split[0]);
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(nation));
				return;
			}

			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				NationCommand.nationAdd(nation, townyUniverse.getDataSource().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {
				if (!isConsole) {
					TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("nation_deleted_by_admin"), nation.getName()));
					TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), nation.getName()));
					townyUniverse.getDataSource().removeNation(nation);
				} else {
					Confirmation confirmation = new Confirmation(() -> {
						TownyUniverse.getInstance().getDataSource().removeNation(nation);
						TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
					});
					ConfirmationHandler.sendConfirmation(sender, confirmation); // It takes the nation, an admin deleting another town has no confirmation.
				}

			} else if(split[1].equalsIgnoreCase("recheck")) {
				
				nation.recheckTownDistance();
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("nation_rechecked_by_admin"), nation.getName()));

			} else if (split[1].equalsIgnoreCase("rename")) {

				NationPreRenameEvent event = new NationPreRenameEvent(nation, split[2]);
				Bukkit.getServer().getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_err_rename_cancelled"));
					return;
				}
				
				if (!NameValidation.isBlacklistName(split[2])) {
					townyUniverse.getDataSource().renameNation(nation, split[2]);
					TownyMessaging.sendPrefixedNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), nation.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));

			} else if (split[1].equalsIgnoreCase("merge")) {
				
				Nation remainingNation = null;
				try {
					remainingNation = townyUniverse.getDataSource().getNation(split[2]);
				} catch (NotRegisteredException e) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), split[2]));
				}
				if (remainingNation.equals(nation))
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), split[2]));
				townyUniverse.getDataSource().mergeNation(nation, remainingNation);
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("nation1_has_merged_with_nation2"), nation, remainingNation));

			} else if(split[1].equalsIgnoreCase("set")) {
				
				NationCommand.nationSet(player, StringMgmt.remArgs(split, 2), true, nation);

			} else if(split[1].equalsIgnoreCase("toggle")) {
				
				NationCommand.nationToggle(player, StringMgmt.remArgs(split, 2), true, nation);
			} else if (split[1].equalsIgnoreCase("deposit")) {
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "deposit [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().collect(amount, "Admin Deposit");
				
				// Send notifications
				String depositMessage = String.format(TownySettings.getLangString("msg_xx_deposited_xx"), (isConsole ? "Console" : player.getName()), amount,  TownySettings.getLangString("nation_sing"));
				TownyMessaging.sendMessage(sender, depositMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, depositMessage);
			}
			else if (split[1].equalsIgnoreCase("withdraw")) {
				int amount;
				
				// Handle incorrect number of arguments
				if (split.length != 3)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "withdraw [amount]"));
				
				try {
					amount = Integer.parseInt(split[2]);
				} catch (NumberFormatException ex) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}

				nation.getAccount().pay(amount, "Admin Withdraw");
				
				// Send notifications
				String withdrawMessage = String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), (isConsole ? "Console" : player.getName()), amount,  TownySettings.getLangString("nation_sing"));
				TownyMessaging.sendMessage(sender, withdrawMessage);
				TownyMessaging.sendPrefixedNationMessage(nation, withdrawMessage);
			}

		} catch (NotRegisteredException | AlreadyRegisteredException | InvalidNameException | EconomyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}

	public void adminSet(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin set"));
			// TODO: player.sendMessage(ChatTools.formatCommand("",
			// "/townyadmin set", "king [nation] [king]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] " + TownySettings.getLangString("town_help_2"), ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "capital [town]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "title [resident] [title]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "surname [resident] [surname]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "plot [town]", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("mayor")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_MAYOR.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			if (split.length < 3) {

				sender.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] " + TownySettings.getLangString("town_help_2"), ""));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));

			} else
				try {
					Resident newMayor;
					Town town = townyUniverse.getDataSource().getTown(split[1]);

					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						townyUniverse.getDataSource().newResident(name);

						newMayor = townyUniverse.getDataSource().getResident(name);

						newMayor.setRegistered(System.currentTimeMillis());
						newMayor.setLastOnline(0);
						newMayor.setNPC(true);

						townyUniverse.getDataSource().saveResident(newMayor);
						townyUniverse.getDataSource().saveResidentList();

						// set for no upkeep as an NPC mayor is assigned
						town.setHasUpkeep(false);

					} else {
						newMayor = townyUniverse.getDataSource().getResident(split[2]);
					}

					if (!town.hasResident(newMayor)) {
						TownCommand.townAddResident(town, newMayor);
					}
					// Delete the resident if the old mayor was an NPC.
					Resident oldMayor = town.getMayor();

					town.setMayor(newMayor);

					if (oldMayor.isNPC()) {
						try {
							town.removeResident(oldMayor);
							townyUniverse.getDataSource().removeResident(oldMayor);
							townyUniverse.getDataSource().removeResidentList(oldMayor);
							// set upkeep again
							town.setHasUpkeep(true);
						} catch (EmptyTownException e) {
							// Should never reach here as we are setting a new
							// mayor before removing the old one.
							e.printStackTrace();
						}
					}
					townyUniverse.getDataSource().saveTown(town);					
					TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_new_mayor"),newMayor.getName()));
					// TownyMessaging.sendMessage(player, msg);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
				}

		} else if (split[0].equalsIgnoreCase("capital")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_CAPITAL.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split.length < 2) {

				sender.sendMessage(ChatTools.formatTitle("/townyadmin set capital"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set capital", "[town name]", "[nation name]"));

			} else {
				
				try {
					Town newCapital = townyUniverse.getDataSource().getTown(split[1]);
					Nation nation = newCapital.getNation();
					NationCommand.nationSet(player, split, true, nation);
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

			}
		} else if (split[0].equalsIgnoreCase("title")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_TITLE.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			Resident resident = null;
			// Give the resident a title
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set title bilbo Jester");
			else
				resident = townyUniverse.getDataSource().getResident(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setTitle(title + " ");
			townyUniverse.getDataSource().saveResident(resident);

			if (resident.hasTitle()) {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Title", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Title", resident.getName()));
			}

		} else if (split[0].equalsIgnoreCase("surname")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_SURNAME.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			Resident resident = null;
			// Give the resident a surname
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set surname bilbo Jester");
			else
				resident = townyUniverse.getDataSource().getResident(split[1]);

			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setSurname(surname + " ");
			townyUniverse.getDataSource().saveResident(resident);

			if (resident.hasSurname()) {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Surname", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Surname", resident.getName()));
			}

		} else if (split[0].equalsIgnoreCase("plot")) {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET_PLOT.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			
			TownBlock tb = TownyAPI.getInstance().getTownBlock(player.getLocation());
			if (split.length < 2) {
				sender.sendMessage(ChatTools.formatTitle("/townyadmin set plot"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name]", TownySettings.getLangString("msg_admin_set_plot_help_1")));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name] {rect|circle} {radius}", TownySettings.getLangString("msg_admin_set_plot_help_2")));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set plot", "[town name] {rect|circle} auto", TownySettings.getLangString("msg_admin_set_plot_help_2")));
				return;
			}
			if (tb != null) {
				try {
					Town newTown = townyUniverse.getDataSource().getTown(split[1]);
					if (newTown != null) {
						tb.setResident(null);
						tb.setTown(newTown);
						tb.setType(TownBlockType.RESIDENTIAL);
						tb.setName("");
						TownyMessaging.sendMessage(player, String.format(TownySettings.getLangString("changed_plot_town"), newTown.getName()));
					}
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			} else {
				Town town = townyUniverse.getDataSource().getTown(split[1]);
				TownyWorld world = townyUniverse.getDataSource().getWorld(player.getWorld().getName());
				Coord key = Coord.parseCoord(plugin.getCache(player).getLastLocation());
				List<WorldCoord> selection;
				if (split.length == 2)
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), new String[0]);
				else  {
					String[] newSplit = StringMgmt.remFirstArg(split);
					newSplit = StringMgmt.remFirstArg(newSplit);
					selection = AreaSelectionUtil.selectWorldCoordArea(town, new WorldCoord(world.getName(), key), newSplit);
				}
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Pre-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				selection = AreaSelectionUtil.filterTownOwnedBlocks(selection);
				TownyMessaging.sendDebugMsg("Admin Initiated townClaim: Post-Filter Selection ["+selection.size()+"] " + Arrays.toString(selection.toArray(new WorldCoord[0])));
				
				new TownClaim(plugin, player, town, selection, false, true, false).start();
//				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("not_standing_in_plot"));
//				return;
			}
		} else {
			TownyMessaging.sendErrorMsg(getSender(), String.format(TownySettings.getLangString("msg_err_invalid_property"), "administrative"));
		}
	}

	public String nextNpcName() throws TownyException {

		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!TownyUniverse.getInstance().getDataSource().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException(TownySettings.getLangString("msg_err_too_many_npc"));
		} while (true);
	}
	
	public void reloadLangs() {
		String rootFolder = TownyUniverse.getInstance().getRootFolder();
		try {
			TownySettings.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_reload_error"));
			e.printStackTrace();
			return;
		}
		
		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded_lang"));
	}
	
	public void reloadPerms() {
		String rootFolder = TownyUniverse.getInstance().getRootFolder();
		TownyPerms.loadPerms(rootFolder + File.separator + "settings", "townyperms.yml");
		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded_perms"));
	}

	/**
	 * Reloads only the config
	 * 
	 * @param reset Whether or not to reset the config.
	 */
	public void reloadConfig(boolean reset) {

		if (reset) {
			TownyUniverse.getInstance().getDataSource().deleteFile(plugin.getConfigPath());
			TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reset_config"));
		}
		
		try {
			String rootFolder = TownyUniverse.getInstance().getRootFolder();
			TownySettings.loadConfig(rootFolder + File.separator + "settings" + File.separator + "config.yml", plugin.getVersion());
			TownySettings.loadLanguage(rootFolder + File.separator + "settings", "english.yml");
		} catch (IOException e) {
			TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_reload_error"));
			e.printStackTrace();
			return;
		}
		
		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded_config"));
	}

	/**
	 * Reloads both the database and the config. Used with a database reload command.
	 *
	 */
	public void reloadDatabase() {
		
		if (plugin.load()) {

			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();

			// Update permissions for all online players
			TownyPerms.updateOnlinePerms();

		}

		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded_db"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split - Current command arguments.
	 */
	public void purge(String[] split) {

		if (split.length == 0) {
			// command was '/townyadmin purge'
			sender.sendMessage(ChatTools.formatTitle("/townyadmin purge"));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin purge", "[number of days] {townless}", ""));
			sender.sendMessage(ChatTools.formatCommand("", "", "Removes offline residents not seen for this duration.", ""));
			sender.sendMessage(ChatTools.formatCommand("", "", "Optional {townless} flag limits purge to only people that have no town.", ""));
			return;
		}
		String days = "";
		if (split.length == 2 && split[1].equalsIgnoreCase("townless")) {
			days += "townless";
		}

		try {
			days += String.valueOf(split[0]);
		} catch (NumberFormatException e) {
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_error_must_be_int"));
			return;
		}

		if (!isConsole) {

			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PURGE.getNode())) {
				try {
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only"));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			}

			final String finalDays = days;

			Runnable purgeHandler = () -> {
				Player player = (Player) sender;
				if (player == null) {
					try {
						throw new TownyException("Player could not be found!");
					} catch (TownyException e) {
						e.printStackTrace();
					}
				}

				int numDays;
				boolean townless = false;
				if (finalDays.startsWith("townless")) {
					townless = true;
					numDays = Integer.parseInt(finalDays.substring(8));
				} else {
					numDays = Integer.parseInt(finalDays);
				}

				new ResidentPurge(plugin, player, TimeTools.getMillis(numDays + "d"), townless).start();
			};
			
			if (sender != null) {
				Confirmation confirmation = new Confirmation(purgeHandler);
				ConfirmationHandler.sendConfirmation(sender, confirmation);
			}
		} else { // isConsole
			final String finalDays = days;
			Confirmation confirmation = new Confirmation(() -> {
				int numDays;
				boolean townless = false;
				if (finalDays.startsWith("townless")) {
					townless = true;
					numDays = Integer.parseInt(finalDays.substring(8));
				} else {
					numDays = Integer.parseInt(finalDays);
				}

				new ResidentPurge(plugin, null, TimeTools.getMillis(numDays + "d"), townless).start();
			});
			
			ConfirmationHandler.sendConfirmation(sender, confirmation);
		}
	}

	/**
	 * Delete a resident and it's data file (if not online) Available Only to
	 * players with the 'towny.admin' permission node.
	 * 
	 * @param player - Player.
	 * @param split - Current command arguments.
	 */
	public void residentDelete(Player player, String[] split) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0)
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
		else
			try {
				if (!townyUniverse.getPermissionSource().isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete"));

				for (String name : split) {
					try {
						Resident resident = townyUniverse.getDataSource().getResident(name);
						if (!resident.isNPC() && !BukkitTools.isOnline(resident.getName())) {
							townyUniverse.getDataSource().removeResident(resident);
							townyUniverse.getDataSource().removeResidentList(resident);
							TownyMessaging.sendGlobalMessage(TownySettings.getDelResidentMsg(resident));
						} else
							TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_online_or_npc"), name));
					} catch (NotRegisteredException x) {
						// This name isn't registered as a resident
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
					}
				}
			} catch (TownyException x) {
				// Admin only escape
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
	}

	public void parseToggleCommand(String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		boolean choice;

		if (split.length == 0) {
			// command was '/townyadmin toggle'
			player.sendMessage(ChatTools.formatTitle("/townyadmin toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "war", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "peaceful", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "devmode", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "debug", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "townwithdraw/nationwithdraw", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle npc", "[resident]", ""));
			return;

		}

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode(split[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("war")) {
			choice = TownyAPI.getInstance().isWarTime();

			if (!choice) {
				townyUniverse.startWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_started"));
			} else {
				townyUniverse.endWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_ended"));
			}
		} else if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

			try {
				choice = !TownySettings.isDeclaringNeutral();
				TownySettings.setDeclaringNeutral(choice);
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_nation_allow_peaceful"), choice ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}

		} else if (split[0].equalsIgnoreCase("devmode")) {
			try {
				choice = !TownySettings.isDevMode();
				TownySettings.setDevMode(choice);
				TownyMessaging.sendMsg(getSender(), "Dev Mode " + (choice ? Colors.Green + TownySettings.getLangString("enabled") : Colors.Red + TownySettings.getLangString("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("debug")) {
			try {
				choice = !TownySettings.getDebug();
				TownySettings.setDebug(choice);
				TownyLogger.getInstance().refreshDebugLogger();
				TownyMessaging.sendMsg(getSender(), "Debug Mode " + (choice ? Colors.Green + TownySettings.getLangString("enabled") : Colors.Red + TownySettings.getLangString("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("townwithdraw")) {
			try {
				choice = !TownySettings.getTownBankAllowWithdrawls();
				TownySettings.SetTownBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "Town Withdrawls " + (choice ? Colors.Green + TownySettings.getLangString("enabled") : Colors.Red + TownySettings.getLangString("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("nationwithdraw")) {
			try {
				choice = !TownySettings.geNationBankAllowWithdrawls();
				TownySettings.SetNationBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "Nation Withdrawls " + (choice ? Colors.Green + TownySettings.getLangString("enabled") : Colors.Red + TownySettings.getLangString("disabled")));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
			
		} else if (split[0].equalsIgnoreCase("npc")) {
			
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: toggle npc [resident]"));
			
			try {
				Resident resident = townyUniverse.getDataSource().getResident(split[1]);
				resident.setNPC(!resident.isNPC());
				
				townyUniverse.getDataSource().saveResident(resident);
				
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_npc_flag"), resident.isNPC(), resident.getName()));
				
			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[1]));
			}
			
		} else {
			// parameter error message
			// peaceful/war/townmobs/worldmobs
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
		}
	}

	public static void handleTownMetaCommand(Player player, Town town, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_META.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split.length == 2) {
			if (town.hasMeta()) {
				player.sendMessage(ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField field : town.getMetadata()) {
					player.sendMessage(field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_this_town_doesnt_have_any_associated_metadata"));
			}

			return;
		}

		if (split.length < 4) {
			player.sendMessage(ChatTools.formatTitle("/townyadmin town {townname} meta"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "set", "The key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "add", "Add a key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "remove", "Remove a key from the town"));
			return;
		}

		if (split.length == 5) {
			String mdKey = split[3];
			String val = split[4];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)){
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			} else if (split[2].equalsIgnoreCase("set")) {
				CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
				if (town.hasMeta()) {
					for (CustomDataField cdf: town.getMetadata()) {
						if (cdf.equals(md)) {

							// Check if the given value is valid for this field.
							try {
								cdf.isValidType(val);
							} catch (InvalidMetadataTypeException e) {
								TownyMessaging.sendErrorMsg(player, e.getMessage());
								return;
							}
							
							// Change state TODO: Add type casting..
							cdf.setValue(val);

							// Let user know that it was successful.
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_key_x_was_successfully_updated_to_x"), mdKey, cdf.getValue()));

							// Save changes.
							townyUniverse.getDataSource().saveTown(town);

							return;
						}
					}
				}

				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_key_x_is_not_part_of_this_town"), mdKey));

			}
		} else if (split[2].equalsIgnoreCase("add")) {
			String mdKey = split[3];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			}

			CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (town.hasMeta()) {
				for (CustomDataField cdf : town.getMetadata()) {
					if (cdf.equals(md)) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_key_x_already_exists"), mdKey));
						return;
					}
				}
			}

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_custom_data_was_successfully_added_to_town"));
			
			
			town.addMetaData(md.newCopy());
			
		} else if (split[2].equalsIgnoreCase("remove")) {
			String mdKey = split[3];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return;
			}

			CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (town.hasMeta()) {
				for (CustomDataField cdf : town.getMetadata()) {
					if (cdf.equals(md)) {
						town.removeMetaData(cdf);
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_data_successfully_deleted"));
						return;
					}
				}
			}
			
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_key_cannot_be_deleted"));
		}
	}
	
	public static boolean handlePlotMetaCommand(Player player, String[] split) throws TownyException {
		
		String world = player.getWorld().getName();
		TownBlock townBlock = null;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		try {
			townBlock = new WorldCoord(world, Coord.parseCoord(player)).getTownBlock();
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
			return false;
		}

		if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_PLOT_META.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
		
		if (split.length == 1) {
			if (townBlock.hasMeta()) {
				player.sendMessage(ChatTools.formatTitle("Custom Meta Data"));
				for (CustomDataField field : townBlock.getMetadata()) {
					player.sendMessage(field.getKey() + " = " + field.getValue());
				}
			} else {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_this_plot_doesnt_have_any_associated_metadata"));
			}

			return true;
		}
		
		

		if (split.length < 3) {
			player.sendMessage(ChatTools.formatTitle("/townyadmin plot meta"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "set", "The key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "add", "Add a key of a registered data field"));
			player.sendMessage(ChatTools.formatCommand("", "meta", "remove", "Remove a key from the town"));
			return false;
		}

		if (split.length == 4) {
			String mdKey = split[2];
			String val = split[3];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)){
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return false;
			} else if (split[1].equalsIgnoreCase("set")) {
				CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
				if (townBlock.hasMeta())
				{
					for (CustomDataField cdf: townBlock.getMetadata()) {
						if (cdf.equals(md)) {

							// Change state
							try {
								cdf.isValidType(val);
							} catch (InvalidMetadataTypeException e) {
								TownyMessaging.sendErrorMsg(player, e.getMessage());
								return false;
							}

							cdf.setValue(val);

							// Let user know that it was successful.
							TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_key_x_was_successfully_updated_to_x"), mdKey, cdf.getValue()));

							// Save changes.
							townyUniverse.getDataSource().saveTownBlock(townBlock);

							return true;
						}
					}
				}

				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_key_x_is_not_part_of_this_plot"), mdKey));

				return false;

			}
		} else if (split[1].equalsIgnoreCase("add")) {
			String mdKey = split[2];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return false;
			}

			CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);
			if (townBlock.hasMeta()) {
				for (CustomDataField cdf: townBlock.getMetadata()) {
					if (cdf.equals(md)) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_key_x_already_exists"), mdKey));
						return false;
					}
				}
			}

			TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_custom_data_was_successfully_added_to_townblock"));

			townBlock.addMetaData(md.newCopy());
			
		} else if (split[1].equalsIgnoreCase("remove")) {
			String mdKey = split[2];

			if (!townyUniverse.getRegisteredMetadataMap().containsKey(mdKey)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_the_metadata_for_key_is_not_registered"), mdKey));
				return false;
			}

			CustomDataField md = townyUniverse.getRegisteredMetadataMap().get(mdKey);

			if (townBlock.hasMeta()) {
				for (CustomDataField cdf : townBlock.getMetadata()) {
					if (cdf.equals(md)) {
						townBlock.removeMetaData(cdf);
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_data_successfully_deleted"));
						return true;
					}
				}
			}

			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_key_cannot_be_deleted"));
			
			return false;
		}
		
		return true;
	}
	
	private void showReloadHelp() {
		sender.sendMessage(ChatTools.formatTitle("/ta reload"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "database", "Reloads database"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "config", "Reloads config"));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "lang", "Reloads language file."));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "perms", "Reloads Towny permissions."));
		sender.sendMessage(ChatTools.formatCommand("", "/ta reload", "all", "Reloads all components of towny."));
	}
	
	private void parseAdminDepositAllCommand(String[] split) {
		if (split.length == 0)
			showDepositAllHelp();
		else if (split.length > 1)
			showDepositAllHelp();
		else if (split.length == 1) {
			String reason = "townyadmin depositall";
			double amount = 0;
			try {
				amount = Double.parseDouble(split[0]);				
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_must_be_num"));
				return;
			}
			
			for (Nation nation : TownyUniverse.getInstance().getNationsMap().values()) {
				try {
					nation.getAccount().collect(amount, reason);
				} catch (EconomyException e) {
				}
			}
			
			for (Town town : TownyUniverse.getInstance().getTownsMap().values()) {
				try {
					town.getAccount().collect(amount, reason);
				} catch (EconomyException e) {
				}
			}
			TownyMessaging.sendMsg(sender, String.format(TownySettings.getLangString("msg_ta_deposit_all_success"), TownyEconomyHandler.getFormattedBalance(amount)));
		}
	}
	
	private void showDepositAllHelp() {
		sender.sendMessage(ChatTools.formatTitle("/townyadmin depositall"));
		sender.sendMessage(ChatTools.formatCommand("", "/townyadmin depositall", "[amount]", ""));		
	}

}
