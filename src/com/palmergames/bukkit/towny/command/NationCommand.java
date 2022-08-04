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
import com.palmergames.bukkit.towny.event.NationAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationInviteTownEvent;
import com.palmergames.bukkit.towny.event.NationPreAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationPreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationSetSpawnEvent;
import com.palmergames.bukkit.towny.event.nation.NationTownLeaveEvent;
import com.palmergames.bukkit.towny.event.NationRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.NationRequestAllyNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.nation.NationMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreAddAllyEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownKickEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleOpenEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationTogglePublicEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationDenyAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationAcceptAllyRequestEvent;
import com.palmergames.bukkit.towny.event.nation.NationKingChangeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.InvalidNameException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.MapUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;


public class NationCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> nationTabCompletes = Arrays.asList(
		"list",
		"online",
		"leave",
		"withdraw",
		"deposit",
		"new",
		"add",
		"kick",
		"delete",
		"enemy",
		"rank",
		"ranklist",
		"say",
		"set",
		"toggle",
		"invite",
		"join",
		"merge",
		"townlist",
		"allylist",
		"enemylist",
		"ally",
		"spawn",
		"king",
		"leader",
		"bankhistory",
		"baltop"
	);

	private static final List<String> nationSetTabCompletes = Arrays.asList(
		"king",
		"leader",
		"capital",
		"board",
		"taxes",
		"name",
		"spawn",
		"spawncost",
		"title",
		"surname",
		"tag",
		"mapcolor"
	);
	
	private static final List<String> nationListTabCompletes = Arrays.asList(
		"residents",
		"balance",
		"founded",
		"name",		
		"online",
		"open",
		"public",
		"townblocks",
		"towns",
		"upkeep"
	);
	
	static final List<String> nationToggleTabCompletes = Arrays.asList(
		"neutral",
		"peaceful",
		"public",
		"open"
	);
	
	private static final List<String> nationEnemyTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> nationAllyTabCompletes = Arrays.asList(
		"add",
		"remove",
		"sent",
		"received",
		"accept",
		"deny"
	);

	private static final List<String> nationKingTabCompletes = Collections.singletonList("?");
	
	private static final List<String> nationConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;

			switch (args[0].toLowerCase()) {
				case "toggle":
					if (args.length == 2)
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_TOGGLE, nationToggleTabCompletes), args[1]);
					else if (args.length == 3)
						return NameUtil.filterByStart(BaseCommand.setOnOffCompletes, args[2]);
					break;
				case "king":
				case "leader":
					if (args.length == 2)
						return NameUtil.filterByStart(nationKingTabCompletes, args[1]);
					break;
				case "townlist":
				case "allylist":
				case "enemylist":
				case "ranklist":
				case "online":
				case "join":
				case "delete":
				case "merge":
				case "baltop":
					if (args.length == 2)
						return getTownyStartingWith(args[1], "n");
					break;
				case "spawn":
					if (args.length == 2) {
						List<String> nationOrIgnore = getTownyStartingWith(args[1], "n");
						nationOrIgnore.add("-ignore");
						return NameUtil.filterByStart(nationOrIgnore, args[1]);
					}
					if (args.length == 3) {
						List<String> ignore = Collections.singletonList("-ignore");
						return ignore;
					}
				case "add":
					return getTownyStartingWith(args[args.length - 1], "t");
				case "kick":
					try {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null)
							return NameUtil.filterByStart(NameUtil.getNames(res.getTown().getNation().getTowns()), args[args.length - 1]);
					} catch (TownyException ignored) {}
				case "ally":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationAllyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase()) {
							case "add":
								if (args[args.length - 1].startsWith("-")) {
									// Return only sent invites to revoked because the nation name starts with a hyphen, e.g. -exampleNationName
									try {
										return NameUtil.filterByStart(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getSentAllyInvites()
											// Get names of sent invites
											.stream()
											.map(Invite::getReceiver)
											.map(InviteReceiver::getName)
											// Collect sent invite names and check with the last arg without the hyphen
											.collect(Collectors.toList()), args[args.length - 1].substring(1))
											// Add the hyphen back to the beginning
											.stream()
											.map(e -> "-" + e)
											.collect(Collectors.toList());
									} catch (TownyException ignored) {}
								} else {
									// Otherwise return possible nations to send invites to
									return getTownyStartingWith(args[args.length - 1], "n");
								}
							case "remove":
								// Return current allies to remove
								try {
									return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getAllies()), args[args.length - 1]);
								} catch (TownyException ignore) {}
							case "accept":
							case "deny":
								// Return sent ally invites to accept or deny
								try {
									return NameUtil.filterByStart(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getReceivedInvites()
										.stream()
										.map(Invite::getSender)
										.map(InviteSender::getName)
										.collect(Collectors.toList()), args[args.length - 1]);
								} catch (TownyException ignore) {}
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "rank":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase()) {
							case "add":
							case "remove":
								if (args.length == 3) {
									try {
										return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getResidents()), args[2]);
									} catch (NotRegisteredException e) {
										return Collections.emptyList();
									}
								} else if (args.length == 4) {
									return NameUtil.filterByStart(TownyPerms.getNationRanks(), args[3]);
								}
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "enemy":
					if (args.length == 2) {
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					} else if (args.length >= 3){
						switch (args[1].toLowerCase()) {
							case "add":
								return getTownyStartingWith(args[2], "n");
							case "remove":
								// Return enemies of nation
								try {
									return NameUtil.filterByStart(NameUtil.getNames(getResidentOrThrow(player.getUniqueId()).getTown().getNation().getEnemies()), args[2]);
								} catch (TownyException ignored) {}
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "set":
					try {
						return nationSetTabComplete(sender, getResidentOrThrow(player.getUniqueId()).getTown().getNation(), args);
					} catch (NotRegisteredException e) {
						return Collections.emptyList();
					}
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(nationListTabCompletes, args[2]);
						default:
							return Collections.emptyList();
					}
				default:
					if (args.length == 1) {
						List<String> nationNames = NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION, nationTabCompletes), args[0]);
						if (nationNames.size() > 0) {
							return nationNames;
						} else {
							return getTownyStartingWith(args[0], "n");
						}
					} else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.NATION, args[0]))
						return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.NATION, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(nationConsoleTabCompletes, args[0], "n");
		}

		return Collections.emptyList();
	}
	
	static List<String> nationSetTabComplete(CommandSender sender, Nation nation, String[] args) {
		if (args.length == 2) {
			return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_SET, nationSetTabCompletes), args[1]);
		} else if (args.length > 2){
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_SET, args[1]))
				return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_SET, args[1]).getTabCompletion(sender, StringMgmt.remFirstArg(args)), args[args.length-1]);
			
			switch (args[1].toLowerCase()) {
				case "king":
				case "leader":
				case "title":
				case "surname":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getResidents()), args[2]);
				case "capital":
					return NameUtil.filterByStart(NameUtil.getNames(nation.getTowns()), args[2]);
				case "tag":
					if (args.length == 3)
						return NameUtil.filterByStart(Collections.singletonList("clear"), args[2]);
				default:
					return Collections.emptyList();
			}
		}
		
		return Collections.emptyList();
	}

	public NationCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			try {
				parseNationCommand(player, args);
			} catch (TownyException te) {
				TownyMessaging.sendErrorMsg(player, te.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

		} else
			parseNationCommandForConsole(sender, args);

		return true;
	}

	/**
	 * Returns a nation from the player if args is empty or from the name supplied at arg[0].  
	 * @param player {@link Player} to try and get a nation from when args is empty.
	 * @param args {@link String[]} from which to try and get a nation name from.
	 * @return nation {@link Nation} from the Player or from the arg.
	 * @throws TownyException thrown when the player has no nation, or no nation exists by the name supplied in arg[0].
	 */
	private static Nation getPlayerNationOrNationFromArg(Player player, String[] args) throws TownyException {
		return args.length == 0 ? getNationFromPlayerOrThrow(player) : getNationOrThrow(args[0]);  
	}
	
	private void parseNationCommandForConsole(final CommandSender sender, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			HelpMenu.NATION_HELP_CONSOLE.send(sender);

		} else if (split[0].equalsIgnoreCase("list")) {

			try {
				listNations(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}

		} else {
			Nation nation = TownyUniverse.getInstance().getNation(split[0]);
			if (nation != null)
				nationStatusScreen(sender, nation);
			else 
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[0]));
		}
	}

	public void parseNationCommand(final Player player, String[] split) throws TownyException, Exception {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();

		if (split.length == 0) {
			nationStatusScreen(player, getNationFromPlayerOrThrow(player));
			return;
		} 

		if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_HELP.send(player);
			return;
		}

		if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION.getNode(split[0].toLowerCase()))) {
			// Test if this is an addon command
			if (tryNationAddonCommand(player, split))
				return;
			// Test if this is a town status screen lookup.
			if (tryNationStatusScreen(player, split))
				return;
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		}

		switch (split[0].toLowerCase()) {
		case "list":
			listNations(player, split);
			break;
		case "townlist":
			nationTownList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "allylist":
			nationAllyList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "enemylist":
			nationEnemyList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "new":
		case "create":
			newNation(player, split);
			break;
		case "join":
			parseNationJoin(player, StringMgmt.remFirstArg(split));
			break;
		case "merge":
			mergeNation(player, split);
			break;
		case "withdraw":
			nationTransaction(player, split, true);
			break;
		case "leave":
			nationLeave(player);
			break;
		case "spawn":
			boolean ignoreWarning = (split.length > 1 && split[1].equals("-ignore")) || (split.length > 2 && split[2].equals("-ignore"));
			nationSpawn(player, StringMgmt.remFirstArg(split), ignoreWarning);
			break;
		case "deposit":
			nationTransaction(player, split, false);
			break;
		case "rank":
			nationRank(player, StringMgmt.remFirstArg(split));
			break;
		case "ranklist":
			TownyMessaging.sendMessage(player, TownyFormatter.getRanksForNation(getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)), Translator.locale(player)));
			break;
		case "king":
		case "leader":
			nationKing(player, StringMgmt.remFirstArg(split));
			break;
		case "add":
			nationAdd(player, StringMgmt.remFirstArg(split));
			break;
		case "invite":
		case "invites":
			parseInviteCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "kick":
			nationKick(player, StringMgmt.remFirstArg(split));
			break;
		case "set":
			nationSet(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "toggle":
			nationToggle(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "ally":
			nationAlly(player, StringMgmt.remFirstArg(split));
			break;
		case "enemy":
			nationEnemy(player, StringMgmt.remFirstArg(split));
			break;
		case "delete":
			nationDelete(player, StringMgmt.remFirstArg(split));
			break;
		case "online":
			parseNationOnlineCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "say":
			nationSay(getNationFromPlayerOrThrow(player), StringMgmt.remFirstArg(split));
			break;
		case "bankhistory":
			nationBankHistory(player, StringMgmt.remFirstArg(split));
			break;
		case "baltop":
			parseNationBaltop(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		default:
			// Test if this is an addon command
			if (tryNationAddonCommand(player, split))
				return;
			// Test if this is a town status screen lookup.
			if (tryNationStatusScreen(player, split))
				return;
			
			// Alert the player that the subcommand doesn't exist.
			throw new TownyException(Translatable.of("msg_err_invalid_sub"));
		}
	}
	
	private boolean tryNationStatusScreen(Player player, String[] split) throws TownyException {
		Nation nation = TownyUniverse.getInstance().getNation(split[0]);
		if (nation != null) {
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode()) && !nation.hasResident(player.getName()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			nationStatusScreen(player, nation);
			return true;
		}
		return false;
	}

	private static boolean tryNationAddonCommand(Player player, String[] split) {
		if (TownyCommandAddonAPI.hasCommand(CommandType.NATION, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.NATION, split[0]).execute(player, "nation", split);
			return true;
		}
		return false;
}

	private void nationSay(Nation nation, String[] split) throws TownyException {
		if (split.length == 0)
			throw new TownyException("ex: /n say [message here]");
		TownyMessaging.sendPrefixedNationMessage(nation, StringMgmt.join(split));

	}

	private void nationBankHistory(Player player, String[] split) throws TownyException {
		int pages = 10;
		if (split.length > 0)
			try {
				pages = Integer.parseInt(split[0]);
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_must_be_int"));
				return;
			}

		getNationFromPlayerOrThrow(player).generateBankHistoryBook(player, pages);
	}

	private void nationTownList(Player player, Nation nation) {
		TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getName() + " " + Translatable.of("town_plu").forLocale(player)));
		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_towns").forLocale(player), new ArrayList<>(nation.getTowns())));
	}

	private void nationAllyList(Player player, Nation nation) throws TownyException {
		if (nation.getAllies().isEmpty())
			throw new TownyException(Translatable.of("msg_error_nation_has_no_allies")); 

		TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getName() + " " + Translatable.of("status_nation_allies").forLocale(player)));
		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_allies").forLocale(player), new ArrayList<>(nation.getAllies())));
	}

	private void nationEnemyList(Player player, Nation nation) throws TownyException {
		if (nation.getEnemies().isEmpty())
			throw new TownyException(Translatable.of("msg_error_nation_has_no_enemies")); 

		TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getName() + " " + Translatable.of("status_nation_enemies").forLocale(player)));
		TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_enemies").forLocale(player), new ArrayList<>(nation.getEnemies())));
	}

	private void parseNationJoin(Player player, String[] args) {
		
		try {
			Resident resident;
			Town town;
			Nation nation;
			String nationName;

			if (args.length < 1)
				throw new TownyException(Translatable.of("msg_usage", "/nation join [nation]"));

			nationName = args[0];
			
			resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();
			nation = getNationOrThrow(nationName);

			// Check if town is currently in a nation.
			if (town.hasNation())
				throw new TownyException(Translatable.of("msg_err_already_in_a_nation"));

			// Check if town is town is free to join.
			if (!nation.isOpen())
				throw new TownyException(Translatable.of("msg_err_nation_not_open", nation.getFormattedName()));
			
			if ((TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation()))
				throw new TownyException(Translatable.of("msg_err_not_enough_residents_join_nation", town.getName()));

			if (TownySettings.getMaxTownsPerNation() > 0) 
	        	if (nation.getTowns().size() >= TownySettings.getMaxTownsPerNation())
	        		throw new TownyException(Translatable.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
				Coord townCoord = town.getHomeBlock().getCoord();
				if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
					throw new TownyException(Translatable.of("msg_err_nation_homeblock_in_another_world"));
				}
				double distance;
				distance = Math.sqrt(Math.pow(capitalCoord.getX() - (double)townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - (double)townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(Translatable.of("msg_err_town_not_close_enough_to_nation", town.getName()));
				}
			}
			
			// Check if the command is not cancelled
			NationPreAddTownEvent preEvent = new NationPreAddTownEvent(nation, town);
			Bukkit.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			List<Town> towns = new ArrayList<>();
			towns.add(town);
			nationAdd(nation, towns);

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		Resident resident = getResidentOrThrow(player.getUniqueId());
		String sent = Translatable.of("nation_sent_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getTown().getNation().getSentInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown().getNation())));

		if (newSplit.length == 0) { // (/nation invite)
			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_SEE_HOME.getNode())) {
				throw new TownyException(Translatable.of("msg_err_command_disable"));
			}
			HelpMenu.NATION_INVITE.send(player);
			TownyMessaging.sendMessage(player, sent);
			return;
		}
		if (newSplit.length >= 1) { // /nation invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				HelpMenu.NATION_INVITE.send(player);
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_LIST_SENT.getNode())) {
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getNation().getSentInvites();
				InviteCommand.sendInviteList(player, sentinvites, getPage(newSplit, 1), true);
				TownyMessaging.sendMessage(player, sent);
				return;
			} else {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode())) {
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				} else {
					nationAdd(player, newSplit);
				}
				// It's none of those 4 subcommands, so it's a townname, I just expect it to be ok.
				// If it is invalid it is handled in townAdd() so, I'm good
			}
		}
	}

	private void parseNationOnlineCommand(Player player, String[] split) throws TownyException {

		if (split.length > 0) {
			Nation nation = getNationOrThrow(split[0]);
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, nation);
			if (onlineResidents.size() > 0 ) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translatable.of("msg_nation_online").forLocale(player), nation, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White +  "0 " + Translatable.of("res_list").forLocale(player) + " " + (Translatable.of("msg_nation_online").forLocale(player) + ": " + nation));
			}
		} else {
			Nation nation = getNationFromPlayerOrThrow(player);
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(Translatable.of("msg_nation_online").forLocale(player), nation, player));
		}
	}

	public void nationRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			TownyMessaging.sendMessage(player, ChatTools.formatTitle("/nation rank"));
			TownyMessaging.sendMessage(player, ChatTools.formatCommand("", "/nation rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			Town targetTown = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3) {
				TownyMessaging.sendErrorMsg(player, "Eg: /nation rank add/remove [resident] [rank]");
				return;
			}

			try {
				resident = getResidentOrThrow(player.getUniqueId());
				target = getResidentOrThrow(split[1]);
				town = resident.getTown();
				targetTown = target.getTown();

				if (town.getNation() != targetTown.getNation())
					throw new TownyException("This resident is not a member of your Nation!");

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage(player));
				return;
			}

			/*
			 * Match casing to an existing rank, returns null if Nation rank doesn't exist.
			 */
			rank = TownyPerms.matchNationRank(split[2]);
			if (rank == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));
				return;
			}
			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase()))) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_permission_to_give_rank"));
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {

				if (!target.hasNationRank(rank)) {
					NationRankAddEvent nationRankAddEvent = new NationRankAddEvent(town.getNation(), rank, target);
					BukkitTools.getPluginManager().callEvent(nationRankAddEvent);
					if (nationRankAddEvent.isCancelled()) {
						TownyMessaging.sendErrorMsg(player, nationRankAddEvent.getCancelMessage());
						return;
					}
					target.addNationRank(rank);
					if (target.isOnline()) {
						TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_been_given_rank", "Nation", rank));
						plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
					}
					TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", "Nation", rank, target.getName()));
				} else {
					// Must already have this rank
					TownyMessaging.sendMsg(player, Translatable.of("msg_resident_already_has_rank", target.getName(), "Nation"));
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {

				if (target.hasNationRank(rank)) {
					NationRankRemoveEvent nationRankRemoveEvent = new NationRankRemoveEvent(town.getNation(), rank, target);
					BukkitTools.getPluginManager().callEvent(nationRankRemoveEvent);
					if (nationRankRemoveEvent.isCancelled()) {
						TownyMessaging.sendErrorMsg(player, nationRankRemoveEvent.getCancelMessage());
						return;
					}
					target.removeNationRank(rank);
					if (target.isOnline()) {
						TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_had_rank_taken", "Nation", rank));
						plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
					}
					TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", "Nation", rank, target.getName()));
				} else {
					// Doesn't have this rank
					TownyMessaging.sendMsg(player, Translatable.of("msg_resident_doesnt_have_rank", target.getName(), "Nation"));
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

	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation
	 * list
	 *
	 * @param sender - Sender (player or console.)
	 * @param split  - Current command arguments.
	 * @throws TownyException - Thrown when player does not have permission node.
	 */
	public void listNations(CommandSender sender, String[] split) throws TownyException {
		
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		boolean console = true;
		Player player = null;
		
		if ( split.length == 2 && split[1].equals("?")) {
			HelpMenu.NATION_LIST.send(sender);
			return;
		}
		
		if (sender instanceof Player) {
			console = false;
			player = (Player) sender;
		}

		/*
		 * The default comparator on /n list is by residents, test it before we start anything else.
		 */
		if (split.length < 2 && !console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode()))
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		
		List<Nation> nationsToSort = new ArrayList<>(TownyUniverse.getInstance().getNations());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_multiple_comparators_nation"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console && !permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(split[i])))
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					
					if (!nationListTabCompletes.contains(split[i].toLowerCase()))
						throw new TownyException(Translatable.of("msg_error_invalid_comparator_nation", nationListTabCompletes.stream().filter(comp -> sender.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(comp))).collect(Collectors.joining(", "))));

					type = ComparatorType.valueOf(split[i].toUpperCase(Locale.ROOT));
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_missing_comparator"));
					return;
				}
				comparatorSet = true;
			} else { // Is a case of someone using /n list, /n list # or /n list by {comparator} #
				if (pageSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_too_many_pages"));
					return;
				}
				page = MathUtil.getPositiveIntOrThrow(split[i]);
				if (page == 0) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
					return;
				}
				pageSet = true;
			}
		}

	    if (page > total) {
	        TownyMessaging.sendErrorMsg(sender, Translatable.of("LIST_ERR_NOT_ENOUGH_PAGES", total));
	        return;
	    }

	    final ComparatorType finalType = type;
	    final int pageNumber = page;
		try {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				TownyMessaging.sendNationList(sender, ComparatorCaches.getNationListCache(finalType), finalType, pageNumber, total);
			});
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_comparator_failed"));
		}

	}

	private void newNation(Player player, String[] split) throws TownyException {
		Resident resident = getResidentOrThrow(player.getUniqueId());
		if (TownySettings.getNumResidentsCreateNation() > 0 && resident.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents_new_nation"));

		if (split.length == 1)
			throw new TownyException(Translatable.of("msg_specify_nation_name"));

		if (!resident.isMayor() && !resident.getTown().hasResidentWithRank(resident, "assistant"))
			throw new TownyException(Translatable.of("msg_peasant_right"));
		
		boolean noCharge = TownySettings.getNewNationPrice() == 0.0 || !TownyEconomyHandler.isActive();
		
		String nationName = String.join("_", StringMgmt.remFirstArg(split));
		newNation(player, nationName, resident.getTown(), noCharge);
	}
	
	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 *
	 * @param player - Player creating the new nation.
	 * @param name - Nation name.
	 * @param capitalTown - Capital city town.
	 * @param noCharge - charging for creation - /ta nation new NAME CAPITAL has no charge.
	 */
	public static void newNation(Player player, String name, Town capitalTown, boolean noCharge) {

		try {
			if (capitalTown.hasNation())
				throw new TownyException(Translatable.of("msg_err_already_nation"));
			
			if (TownySettings.getTownAutomaticCapitalisationEnabled())
				name = StringMgmt.capitalizeStrings(name);

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if (filteredName == null || TownyUniverse.getInstance().hasNation(filteredName) || (!TownySettings.areNumbersAllowedInNationNames() && NameValidation.containsNumbers(filteredName)))
				throw new TownyException(Translatable.of("msg_err_invalid_name", filteredName));

			PreNewNationEvent preEvent = new PreNewNationEvent(capitalTown, filteredName);
			Bukkit.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}

			// If it isn't free to make a nation, send a confirmation.
			if (!noCharge && TownyEconomyHandler.isActive()) {
				// Test if they can pay.
				if (!capitalTown.getAccount().canPayFromHoldings(TownySettings.getNewNationPrice()))			
					throw new TownyException(Translatable.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));

				final String finalName = filteredName;
				Confirmation.runOnAccept(() -> {				
					// Town pays for nation here.
					if (!capitalTown.getAccount().withdraw(TownySettings.getNewNationPrice(), "New Nation Cost")) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_funds_new_nation2", TownySettings.getNewNationPrice()));
						return;
					}
					try {
						// Actually make nation.
						newNation(finalName, capitalTown);
					} catch (AlreadyRegisteredException | NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage(player));
					}
					TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(finalName)));

				})
					.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice())))
					.sendTo(player);
				
			// Or, it is free, so just make the nation.
			} else {
				newNation(filteredName, capitalTown);
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_nation", player.getName(), StringMgmt.remUnderscore(filteredName)));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage(player));
		}
	}

	public static Nation newNation(String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		UUID nationUUID = UUID.randomUUID();
		townyUniverse.getDataSource().newNation(name, nationUUID);
		Nation nation = townyUniverse.getNation(nationUUID);
		
		// Should never happen
		if (nation == null) {
			TownyMessaging.sendErrorMsg(String.format("Error fetching new nation with name %s; it was not properly registered!", name));
			throw new NotRegisteredException(Translatable.of("msg_err_not_registered_1", name));
		}
		nation.setRegistered(System.currentTimeMillis());
		nation.setMapColorHexCode(MapUtil.generateRandomNationColourAsHexCode());
		town.setNation(nation);
		nation.setCapital(town);
		nation.setSpawn(town.getSpawnOrNull());

		if (TownyEconomyHandler.isActive())
			nation.getAccount().setBalance(0, "New Nation Account");

		if (TownySettings.isNationTagSetAutomatically())
			nation.setTag(name.substring(0, Math.min(name.length(), TownySettings.getMaxTagLength())).replace("_","").replace("-", ""));
			
		town.save();
		nation.save();

		BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));

		return nation;
	}

	public void mergeNation(Player player, String[] split) throws TownyException {
		mergeNation(player, StringMgmt.remFirstArg(split), getNationFromPlayerOrThrow(player), false);

	}

	public static void mergeNation(CommandSender sender, String[] split, @NotNull Nation remainingNation, boolean admin) throws TownyException {

		if (split.length <= 0) // /n merge
			throw new TownyException(Translatable.of("msg_specify_nation_name"));

		String name = split[0];
		if (!admin && sender instanceof Player player && !getResidentOrThrow(player.getUniqueId()).isKing())
			throw new TownyException(Translatable.of("msg_err_merging_for_kings_only"));

		Nation nation = TownyUniverse.getInstance().getNation(name);
		if (nation == null || remainingNation.getName().equalsIgnoreCase(name))
			throw new TownyException(Translatable.of("msg_err_invalid_name", name));

		Resident king = nation.getKing();
		if (!king.isOnline()) {
			throw new TownyException(Translatable.of("msg_err_king_of_that_nation_is_not_online", name, king.getName()));
		}

		TownyMessaging.sendMsg(king, Translatable.of("msg_would_you_merge_your_nation_into_other_nation", nation, remainingNation, remainingNation));
		if (TownySettings.getNationRequiresProximity() > 0) {
			List<Town> towns = new ArrayList<>(nation.getTowns());
			towns.addAll(remainingNation.getTowns());
			List<Town> removedTowns = remainingNation.gatherOutOfRangeTowns(towns, remainingNation.getCapital());
			if (!removedTowns.isEmpty()) {
				TownyMessaging.sendMsg(nation.getKing(), Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")));
				TownyMessaging.sendMsg(remainingNation.getKing(), Translatable.of("msg_warn_the_following_towns_will_be_removed_from_the_merged_nation", StringMgmt.join(removedTowns, ", ")));
			}
		}
		Confirmation.runOnAccept(() -> {
			NationPreMergeEvent preEvent = new NationPreMergeEvent(nation, remainingNation);
			Bukkit.getPluginManager().callEvent(preEvent);

			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(nation, preEvent.getCancelMessage());
				return;
			}

			BukkitTools.getPluginManager().callEvent(new NationMergeEvent(nation, remainingNation));
			TownyUniverse.getInstance().getDataSource().mergeNation(nation, remainingNation);
			TownyMessaging.sendGlobalMessage(Translatable.of("nation1_has_merged_with_nation2", nation, remainingNation));
			if (TownySettings.getNationRequiresProximity() > 0)
				remainingNation.removeOutOfRangeTowns();
		}).runOnCancel(() -> {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_request_denied"));     // These messages don't use the word Town or Nation.
			TownyMessaging.sendMsg(nation.getKing(), Translatable.of("msg_town_merge_cancelled"));
		}).sendTo(BukkitTools.getPlayerExact(king.getName()));
	}
	
	public void nationLeave(Player player) {
		Town town = null;
		try {
			Resident resident = getResidentOrThrow(player.getUniqueId());
			town = resident.getTown();

			
			NationPreTownLeaveEvent event = new NationPreTownLeaveEvent(town.getNation(), town);
			Bukkit.getPluginManager().callEvent(event);
			
			if (event.isCancelled())
				throw new TownyException(event.getCancelMessage());

			boolean tooManyResidents = false;
			if (town.isCapital()) {
				// Check that the capital wont have too many residents after deletion. 
				tooManyResidents = TownySettings.getMaxResidentsPerTown() > 0 && TownySettings.getMaxResidentsPerTownCapitalOverride() > 0 && town.getNumResidents() > TownySettings.getMaxResidentsPerTown(); 
				// Show a message preceding the confirmation message if they will lose residents. 
				if (tooManyResidents)
					TownyMessaging.sendMsg(player, Translatable.of("msg_deleting_nation_will_result_in_losing_residents", TownySettings.getMaxResidentsPerTown(), town.getNumResidents() - TownySettings.getMaxResidentsPerTown()));
			}
			final Town finalTown = town;
			final Nation nation = town.getNation();
			final boolean finalTooManyResidents = tooManyResidents;
			Confirmation.runOnAccept(() -> {
				Bukkit.getPluginManager().callEvent(new NationTownLeaveEvent(nation, finalTown));
				finalTown.removeNation();

				if (finalTooManyResidents)
					ResidentUtil.reduceResidentCountToFitTownMaxPop(finalTown);
				
				plugin.resetCache();

				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(finalTown.getName())));
				TownyMessaging.sendPrefixedTownMessage(finalTown, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));

				nation.removeOutOfRangeTowns();
			}).sendTo(player);
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage(player));
			return;
		}
	}

	public void nationDelete(Player player, String[] split) {

		// Player is using "/n delete"
		if (split.length == 0) {
			try {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				Town town = resident.getTown();
				Nation nation = resident.getTown().getNation();
				// Check that the capital wont have too many residents after deletion. 
				boolean tooManyResidents = TownySettings.getMaxResidentsPerTown() > 0 && TownySettings.getMaxResidentsPerTownCapitalOverride() > 0 && town.getNumResidents() > TownySettings.getMaxResidentsPerTown(); 
				// Show a message preceding the confirmation message if they will lose residents. 
				if (tooManyResidents)
					TownyMessaging.sendMsg(player, Translatable.of("msg_deleting_nation_will_result_in_losing_residents", TownySettings.getMaxResidentsPerTown(), town.getNumResidents() - TownySettings.getMaxResidentsPerTown()));

				Confirmation.runOnAccept(() -> {
					TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation.getName()));
					TownyUniverse.getInstance().getDataSource().removeNation(nation);
					if (tooManyResidents)
						ResidentUtil.reduceResidentCountToFitTownMaxPop(town);
				})
				.sendTo(player);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage(player));
			}
		// Admin is using "/n delete NATIONNAME"
		} else
			try {
				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode()))
					throw new TownyException(Translatable.of("msg_err_admin_only_delete_nation"));

				Nation nation = getNationOrThrow(split[0]);
				Confirmation.runOnAccept(() -> {
					TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation.getName()));
					TownyUniverse.getInstance().getDataSource().removeNation(nation);					
				})
				.sendTo(player);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage(player));
			}
	}

	public void nationKing(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			HelpMenu.KING_HELP.send(player);
	}

	/**
	 * First stage of adding towns to a nation.
	 * 
	 * Tests here are performed to make sure Nation is allowed to add the towns:
	 * - make sure the nation hasn't already hit the max towns (if that is required in teh config.)
	 * 
	 * @param player - Player using the command.
	 * @param names - Names that will be matched to towns.
	 * @throws TownyException generic
	 */
	public void nationAdd(Player player, String[] names) throws TownyException {

		if (names.length < 1)
			throw new TownyException("Eg: /nation add [names]");

		Nation nation = getNationFromPlayerOrThrow(player);
		
		if (TownySettings.getMaxTownsPerNation() > 0 && nation.getTowns().size() >= TownySettings.getMaxTownsPerNation())
			throw new TownyException(Translatable.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));

		// The list of valid invites.
		List<String> newtownlist = new ArrayList<>();
		// List of invites to be removed.
		List<String> removeinvites = new ArrayList<>();
		for (String townname : new ArrayList<>(Arrays.asList(names))) {
			if (townname.startsWith("-")) {
				// Add them to removing, remove the "-"
				removeinvites.add(townname.substring(1));
				continue;
			}

			if (nation.hasTown(townname)) {
				// Town is already part of the nation.
				removeinvites.add(townname);
				continue;
			}
			// add them to adding.
			newtownlist.add(townname); 
		}
		names = newtownlist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length >= 1) {
			nationRevokeInviteTown(player, nation, TownyAPI.getInstance().getTowns(namestoremove));
		}

		if (names.length >= 1) {
			nationAdd(player, nation, TownyAPI.getInstance().getTowns(names));
		}
	}

	/**
	 * Second stage of adding towns to a nation.
	 * 
	 * Tests here are performed to make sure the Towns are allowed to join the Nation:
	 * - make sure the town has no nation.
	 * - make sure the town has enough residents to join a nation (if it is required in the config.)
	 * - make sure the town is close enough to the nation capital (if it is required in the config.)
	 * 
	 * Lastly, invites are sent and if successful, the third stage is called by the invite handler.
	 * 
	 * @param player player sending the request
	 * @param nation Nation sending the request
	 * @param invited the Town(s) being invited to the Nation
	 * @throws TownyException executed when the arraylist (invited) returns empty (no valid town was entered)
	 */
	public static void nationAdd(Player player, Nation nation, List<Town> invited) throws TownyException {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : invited) {
			if (town.hasNation()) {
				remove.add(town);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_already_nation"));
				continue;
			}	

			if (TownySettings.getNumResidentsJoinNation() > 0 && town.getNumResidents() < TownySettings.getNumResidentsJoinNation()) {
				remove.add(town);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_enough_residents_join_nation", town.getName()));
				continue;
			}

			if (TownySettings.getNationRequiresProximity() > 0) {
				if (!nation.getCapital().hasHomeBlock() || !town.hasHomeBlock()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_homeblock_has_not_been_set"));
				}
				WorldCoord capitalCoord = nation.getCapital().getHomeBlockOrNull().getWorldCoord();
				WorldCoord townCoord = town.getHomeBlockOrNull().getWorldCoord();
				if (!capitalCoord.getWorldName().equalsIgnoreCase(townCoord.getWorldName())) {
					remove.add(town);
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_homeblock_in_another_world"));
					continue;
				}

				if (MathUtil.distance(capitalCoord, townCoord) > TownySettings.getNationRequiresProximity()) {
					remove.add(town);
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_town_not_close_enough_to_nation", town.getName()));
					continue;
				}
			}

			// Check if the command is not cancelled
			NationPreAddTownEvent preEvent = new NationPreAddTownEvent(nation, town);
			Bukkit.getPluginManager().callEvent(preEvent);
			
			if (preEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preEvent.getCancelMessage());
				return;
			}
			
			nationInviteTown(player, nation, town);
		}

		for (Town town : remove) {
			invited.remove(town);
		}

		if (invited.size() > 0) {
			StringBuilder sb = new StringBuilder();

			for (Town town : invited) {
				sb.append(town.getName()).append(", ");
			}
			
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_invited_join_nation", player.getName(), sb.substring(0, sb.length() - 2)));
		} else {
			// This is executed when the arraylist returns empty (no valid town was entered).
			throw new TownyException(Translatable.of("msg_invalid_name"));
		}
	}

	/**
	 * Final stage of adding towns to a nation.
	 * @param nation - Nation being added to.
	 * @param towns - List of Town(s) being added to Nation.
	 * @throws AlreadyRegisteredException - Shouldn't happen but could.
	 */
	public static void nationAdd(Nation nation, List<Town> towns) throws AlreadyRegisteredException {
		for (Town town : towns) {
			if (!town.hasNation()) {
				town.setNation(nation);
				town.save();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_join_nation", town.getName()));
			}

		}
		plugin.resetCache();
		nation.save();

	}
	
	private static void nationRevokeInviteTown(CommandSender sender, Nation nation, List<Town> towns) {

		for (Town town : towns) {
			if (InviteHandler.inviteIsActive(nation, town)) {
				for (Invite invite : town.getReceivedInvites()) {
					if (invite.getSender().equals(nation)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMsg(sender, Translatable.of("nation_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}
	
	private static void nationInviteTown(Player player, Nation nation, Town town) throws TownyException {

		TownJoinNationInvite invite = new TownJoinNationInvite(player, town, nation);
		try {
			if (!InviteHandler.inviteIsActive(invite)) { 
				town.newReceivedInvite(invite);
				nation.newSentInvite(invite);
				InviteHandler.addInvite(invite); 
				Player mayor = TownyAPI.getInstance().getPlayer(town.getMayor());
				if (mayor != null)
					TownyMessaging.sendRequestMessage(mayor,invite);
				Bukkit.getPluginManager().callEvent(new NationInviteTownEvent(invite));
			} else {
				throw new TownyException(Translatable.of("msg_err_town_already_invited", town.getName()));
			}
		} catch (TooManyInvitesException e) {
			town.deleteReceivedInvite(invite);
			nation.deleteSentInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	public void nationKick(Player player, String[] names) throws TownyException {

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation kick [names]");
			return;
		}

		Nation nation = getNationFromPlayerOrThrow(player);

		nationKick(player, nation, TownyAPI.getInstance().getTowns(names));
	}

	public static void nationKick(CommandSender sender, Nation nation, List<Town> kicking) {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : kicking)
			if (town.isCapital() || !nation.hasTown(town))
				remove.add(town);
			else {
				// Fire cancellable event.
				NationPreTownKickEvent event = new NationPreTownKickEvent(nation, town);
				Bukkit.getPluginManager().callEvent(event);
				if (event.isCancelled()) {
					TownyMessaging.sendErrorMsg(sender, event.getCancelMessage());
					remove.add(town);
					continue;
				}
				
				// Actually remove the nation off the Town.
				town.removeNation();
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_nation_kicked_by", sender.getName()));
			}

		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_kicked", sender.getName(), StringMgmt.join(kicking, ", ")));
			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
	}

	private void nationAlly(Player player, String[] split) throws TownyException {
		if (split.length <= 0) {
			HelpMenu.ALLIES_STRING.send(player);
			return;
		}

		if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY.getNode(split[0].toLowerCase())))
			throw new TownyException(Translatable.of("msg_err_command_disable"));

		Resident resident = getResidentOrThrow(player.getUniqueId());
		Nation nation = getNationFromResidentOrThrow(resident);

		switch (split[0].toLowerCase()) {
		case "add":
			nationAllyAdd(player, resident, nation, StringMgmt.remFirstArg(split));
			break;
		case "remove":
			nationAllyRemove(player, resident, nation, StringMgmt.remFirstArg(split));
			break;
		case "sent":
			nationAllySent(player, nation, StringMgmt.remFirstArg(split));
			break;
		case "received":
			nationAllyReceived(player, nation, StringMgmt.remFirstArg(split));
			break;
		case "accept":
			nationAllyAccept(player, nation, split);
			break;
		case "deny":
			nationAllyDeny(player, nation, split);
			break;
		default:
			HelpMenu.ALLIES_STRING.send(player);
		}
	}

	private void nationAllyAdd(Player player, Resident resident, Nation nation, String[] names) throws TownyException {
		if (names.length == 0)
			throw new TownyException(Translatable.of("msg_usage", "/n ally add [names]"));
		
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		ArrayList<Nation> list = new ArrayList<>();
		ArrayList<Nation> remlist = new ArrayList<>();
		Nation ally;
		for (String name : names) {
			ally = townyUniverse.getNation(name);
			if (ally != null) {
				if (nation.equals(ally)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_own_nation_disallow"));
				} else if (nation.isAlliedWith(ally)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_already_ally", ally));
				} else {
					list.add(ally);
				}
			}
			// So "-Name" isn't a nation, remove the - check if that is a town.
			else {
				if (name.startsWith("-")) {
					ally = townyUniverse.getNation(name.substring(1));

					if (ally != null) {
						if (nation.equals(ally)) {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_own_nation_disallow"));
						} else {
							remlist.add(ally);
						}
					} else {
						// Do nothing here as it doesn't match a Nation
						// Well we don't want to send the commands again so just say invalid name
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_name", name));
					}
				} else {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_name", name));
				}
			}
		}
		if (!list.isEmpty()) {
			nationAddOrRemoveAlly(resident,nation,list,true);
		}
		if (!remlist.isEmpty()) {
			nationRemoveAllyRequest(player,nation, remlist);
		}
		
	}

	private void nationAllyRemove(Player player, Resident resident, Nation nation, String[] names) throws TownyException {
		if (names.length == 0)
			throw new TownyException(Translatable.of("msg_usage", "/n ally add [names]"));
		
		ArrayList<Nation> list = new ArrayList<>();
		Nation ally;
		for (String name : names) {
			ally = TownyUniverse.getInstance().getNation(name);
			if (ally != null) {
				if (nation.equals(ally))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_own_nation_disallow"));
				else if (!nation.isAlliedWith(ally))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_not_allied_with", ally.getName()));
				else
					list.add(ally);
			}
		}
		if (!list.isEmpty()) {
			nationAddOrRemoveAlly(resident,nation,list,false);
		}
	}

	private void nationAllySent(Player player, Nation nation, String[] split) {
		String sent = Translatable.of("nation_sent_ally_requests").forLocale(player)
				.replace("%a", Integer.toString(nation.getSentAllyInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getSentAllyRequestsMaxAmount(nation)));
		InviteCommand.sendInviteList(player, nation.getSentAllyInvites(), getPage(split, 0), true);
		TownyMessaging.sendMessage(player, sent);
	}

	private void nationAllyReceived(Player player, Nation nation, String[] split) {
		String received = Translatable.of("nation_received_requests").forLocale(player)
				.replace("%a", Integer.toString(nation.getReceivedInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(nation)));
		InviteCommand.sendInviteList(player, nation.getReceivedInvites(), getPage(split, 0), false);
		TownyMessaging.sendMessage(player, received);
	}

	private void nationAllyAccept(Player player, Nation nation, String[] split) throws TownyException {
		Nation sendernation;
		List<Invite> invites = nation.getReceivedInvites();

		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_no_requests"));
			return;
		}
		if (split.length >= 2) { // /nation ally accept args[1]
			sendernation = TownyUniverse.getInstance().getNation(split[1]);

			if (sendernation == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
				return;
			}
		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_specify_invite"));
			InviteCommand.sendInviteList(player, invites, 1, false);
			return;
		}
		
		Invite toAccept = null;

		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(sendernation) && invite.getReceiver().equals(nation)) {
				toAccept = invite;
				break;
			}
		}
		if (toAccept != null) {
			
			// Nation has reached the max amount of allies
			if (TownySettings.getMaxNationAllies() >= 0 && nation.getAllies().size() >= TownySettings.getMaxNationAllies()) {
				toAccept.getReceiver().deleteReceivedInvite(toAccept);
				toAccept.getSender().deleteSentInvite(toAccept);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_ally_limit_reached"));
				return;
			}
			
			try {
				NationAcceptAllyRequestEvent acceptAllyRequestEvent = new NationAcceptAllyRequestEvent((Nation)toAccept.getSender(), (Nation) toAccept.getReceiver());
				Bukkit.getPluginManager().callEvent(acceptAllyRequestEvent);
				if (acceptAllyRequestEvent.isCancelled()) {
					toAccept.getReceiver().deleteReceivedInvite(toAccept);
					toAccept.getSender().deleteSentInvite(toAccept);
					TownyMessaging.sendErrorMsg(player, acceptAllyRequestEvent.getCancelMessage());
					return;
				}
				InviteHandler.acceptInvite(toAccept);
				return;
			} catch (InvalidObjectException e) {
				e.printStackTrace(); // Shouldn't happen, however like i said a fallback
			}
		}

	}

	private void nationAllyDeny(Player player, Nation nation, String[] split) {
		Nation sendernation;
		List<Invite> invites = nation.getReceivedInvites();

		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_no_requests"));
			return;
		}
		if (split.length >= 2) { // /invite deny args[1]
			sendernation = TownyUniverse.getInstance().getNation(split[1]);

			if (sendernation == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
				return;
			}
		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_specify_invite"));
			InviteCommand.sendInviteList(player, invites, 1, false);
			return;
		}

		Invite toDecline = null;
		
		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(sendernation) && invite.getReceiver().equals(nation)) {
				toDecline = invite;
				break;
			}
		}
		if (toDecline != null) {
			try {
				NationDenyAllyRequestEvent denyAllyRequestEvent = new NationDenyAllyRequestEvent(nation, sendernation);
				Bukkit.getPluginManager().callEvent(denyAllyRequestEvent);
				if (denyAllyRequestEvent.isCancelled()) {
					sendernation.deleteSentAllyInvite(toDecline);
					nation.deleteReceivedInvite(toDecline);
					TownyMessaging.sendErrorMsg(player, denyAllyRequestEvent.getCancelMessage());
					return;
				}
				InviteHandler.declineInvite(toDecline, false);
				TownyMessaging.sendMsg(player, Translatable.of("successful_deny_request"));
			} catch (InvalidObjectException e) {
				e.printStackTrace(); // Shouldn't happen, however like i said a fallback
			}
		}
	}

	private void nationRemoveAllyRequest(CommandSender sender, Nation invitingNation, ArrayList<Nation> remlist) {
		for (Nation invitedNation : remlist) {
			if (InviteHandler.inviteIsActive(invitingNation, invitedNation)) {
				for (Invite receivedInvite : invitedNation.getReceivedInvites()) {
					if (receivedInvite.getSender().equals(invitingNation)) {
						try {
							InviteHandler.declineInvite(receivedInvite, true);
							TownyMessaging.sendMsg(sender, Translatable.of("nation_revoke_ally_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public void nationAddOrRemoveAlly(Resident resident, final Nation nation, List<Nation> targetNations, boolean add) throws TownyException {
		// This is where we add/remove those invites for nations to ally other nations.
		List<Nation> remove = new ArrayList<>();
		for (Nation targetNation : targetNations) {
			if (add) { // If we are adding as an ally (sending an invite to ally.)
				try {
					nationAddAlly(resident, nation, targetNation);
				} catch (TownyException e) {
					// One of the Allies was not added because the nationCreateAllyRequest() method
					// threw an exception or a non-admin player tried to ally an NPC-led nation, continue;
					TownyMessaging.sendErrorMsg(resident, e.getMessage());
					remove.add(targetNation);
					continue;
				}
	
			} else { // So we are removing an ally
				try {
					nationRemoveAlly(resident, nation, targetNation);
				} catch (TownyException e) {
					// One of the Allies was not removed because the NationRemoveAllyEvent was cancelled, continue;
					TownyMessaging.sendErrorMsg(resident, e.getMessage());
					remove.add(targetNation);
					continue;
				}
			}
		}
		for (Nation removedNation : remove)
			targetNations.remove(removedNation);
	
		if (targetNations.size() > 0) {
			TownyUniverse.getInstance().getDataSource().saveNations();
			plugin.resetCache();
		} else {
			throw new TownyException(Translatable.of("msg_invalid_name"));
		}
	}

	private void nationAddAlly(Resident resident, Nation nation, Nation targetNation) throws TownyException {
		Player player = resident.getPlayer();
		if (player == null)
			throw new TownyException("Could not add " + targetNation + " as Ally because your Player is null! This shouldn't be possible!");
		if (!targetNation.hasEnemy(nation)) {
			NationPreAddAllyEvent preAddAllyEvent = new NationPreAddAllyEvent(nation, targetNation);
			Bukkit.getPluginManager().callEvent(preAddAllyEvent);
			if (preAddAllyEvent.isCancelled()) {
				TownyMessaging.sendErrorMsg(player, preAddAllyEvent.getCancelMessage());
				return;
			}
			if (!targetNation.getCapital().getMayor().isNPC()) {
				nationCreateAllyRequest(player, nation, targetNation);
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_ally_req_sent", targetNation));
			} else {
				nationAddNPCNationAsAlly(player, resident, nation, targetNation);
			}
		} else {
			throw new TownyException(Translatable.of("msg_unable_ally_enemy", targetNation));
		}
	}

	private void nationCreateAllyRequest(CommandSender sender, Nation sendingNation, Nation receivingNation) throws TownyException {
		NationAllyNationInvite invite = new NationAllyNationInvite(sender, receivingNation, sendingNation);
		try {
			if (!InviteHandler.inviteIsActive(invite)) {
				receivingNation.newReceivedInvite(invite);
				sendingNation.newSentAllyInvite(invite);
				InviteHandler.addInvite(invite);
				Player king = receivingNation.getKing().getPlayer();
				if (king != null)
					TownyMessaging.sendRequestMessage(king, invite);
				
				// Player is not the king and has permissions to accept invites, show them the invite as well
				for (Player player : TownyAPI.getInstance().getOnlinePlayers(receivingNation))
					if (!player.getUniqueId().equals(receivingNation.getKing().getUUID()) && player.hasPermission(PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode()))
						TownyMessaging.sendRequestMessage(player, invite);
				
				Bukkit.getPluginManager().callEvent(new NationRequestAllyNationEvent(invite));
			} else {
				throw new TownyException(Translatable.of("msg_err_ally_already_requested", receivingNation));
			}
		} catch (TooManyInvitesException e) {
			receivingNation.deleteReceivedInvite(invite);
			sendingNation.deleteSentAllyInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	private void nationAddNPCNationAsAlly(Player player, Resident resident, Nation nation, Nation targetNation) throws TownyException {
		if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)) {
			try {
				targetNation.addAlly(nation);
				nation.addAlly(targetNation);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_allied_nations", resident, targetNation));
			TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_added_ally", nation));
		} else {
			throw new TownyException(Translatable.of("msg_unable_ally_npc", nation.getName()));
		}
	}

	private void nationRemoveAlly(Resident resident, Nation nation, Nation targetNation) throws TownyException {
		if (nation.hasAlly(targetNation)) {
			NationRemoveAllyEvent removeAllyEvent = new NationRemoveAllyEvent(nation, targetNation);
			Bukkit.getPluginManager().callEvent(removeAllyEvent);
			if (removeAllyEvent.isCancelled())
				throw new TownyException(removeAllyEvent.getCancelMessage());
	
			nation.removeAlly(targetNation);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", targetNation));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_ally_removed_successfully"));
	
			// Remove the reciprocal ally relationship
			if (targetNation.hasAlly(nation)) {
				NationRemoveAllyEvent reciprocalRemoveAllyEvent = new NationRemoveAllyEvent(targetNation, nation);
				Bukkit.getPluginManager().callEvent(reciprocalRemoveAllyEvent );
				if (reciprocalRemoveAllyEvent.isCancelled())
					throw new TownyException(reciprocalRemoveAllyEvent.getCancelMessage());
	
				targetNation.removeAlly(nation);
				TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_removed_ally", nation.getName()));
			}
		}
	}

	public void nationEnemy(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation enemy [add/remove] [name]");
			return;
		}

		Resident resident = getResidentOrThrow(player.getUniqueId());
		Nation nation = getNationFromResidentOrThrow(resident);
		
		ArrayList<Nation> list = new ArrayList<>();
		Nation enemy;
		// test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);
		boolean add = test.equalsIgnoreCase("add");

		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				enemy = townyUniverse.getNation(name);

				if (enemy == null) {
					throw new TownyException(Translatable.of("msg_err_no_nation_with_that_name", name));
				}

				if (nation.equals(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_own_nation_disallow"));
				else if (add && nation.hasEnemy(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_already_enemies_with", enemy.getName()));
				else if (!add && !nation.hasEnemy(enemy))
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_nation_not_enemies_with", enemy.getName()));
				else
					list.add(enemy);
			}
			if (!list.isEmpty())
				nationEnemy(player, nation, list, add);

		} else {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_property", "[add/remove]"));
		}
	}

	public void nationEnemy(Player player, Nation nation, List<Nation> enemies, boolean add) {

		ArrayList<Nation> remove = new ArrayList<>();
		for (Nation targetNation : enemies)
			try {
				if (add && !nation.getEnemies().contains(targetNation)) {
					NationPreAddEnemyEvent npaee = new NationPreAddEnemyEvent(nation, targetNation);
					Bukkit.getPluginManager().callEvent(npaee);
					
					if (!npaee.isCancelled()) {
						nation.addEnemy(targetNation);
						
						NationAddEnemyEvent naee = new NationAddEnemyEvent(nation, targetNation);
						Bukkit.getPluginManager().callEvent(naee);

						// Remove the targetNation from the nation ally list if present.
						if (nation.hasAlly(targetNation)) {
							nation.removeAlly(targetNation);
							Bukkit.getPluginManager().callEvent(new NationRemoveAllyEvent(nation, targetNation));
							TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", targetNation));
							TownyMessaging.sendMsg(player, Translatable.of("msg_ally_removed_successfully"));
						}
						
						// Remove the nation from the targetNation ally list if present.
						if (targetNation.hasAlly(nation)) {
							targetNation.removeAlly(nation);
							Bukkit.getPluginManager().callEvent(new NationRemoveAllyEvent(targetNation, nation));
							TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_removed_ally", nation));
							TownyMessaging.sendMsg(player, Translatable.of("msg_ally_removed_successfully"));
						}

						TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_added_enemy", nation));
					} else {
						TownyMessaging.sendErrorMsg(player, npaee.getCancelMessage());
						remove.add(targetNation);
					}

				} else if (nation.getEnemies().contains(targetNation)) {
					NationPreRemoveEnemyEvent npree = new NationPreRemoveEnemyEvent(nation, targetNation);
					Bukkit.getPluginManager().callEvent(npree);
					if (!npree.isCancelled()) {
						nation.removeEnemy(targetNation);

						NationRemoveEnemyEvent nree = new NationRemoveEnemyEvent(nation, targetNation);
						Bukkit.getPluginManager().callEvent(nree);
						
						TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_removed_enemy", nation));
					} else {
						TownyMessaging.sendErrorMsg(player, npree.getCancelMessage());
						remove.add(targetNation);
					}
				}

			} catch (AlreadyRegisteredException | NotRegisteredException e) {
				remove.add(targetNation);
			}
		
		for (Nation newEnemy : remove)
			enemies.remove(newEnemy);

		if (enemies.size() > 0) {
			String msg = "";

			for (Nation newEnemy : enemies)
				msg += newEnemy.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);
			if (add)
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_nations", player.getName(), msg));
			else
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_enemy_to_neutral", player.getName(), msg));

			TownyUniverse.getInstance().getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
	}

	public static void nationSet(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {
		if (split.length == 0) {
			HelpMenu.NATION_SET.send(sender);
			return;
		}

		/*
		 * Take care of permission nodes tests here.
		 */
		if (!admin && !TownyUniverse.getInstance().getPermissionSource().testPermission(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET.getNode(split[0].toLowerCase()))) {
			// Test if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_SET, split[0]).execute(sender, "nation", split);
				return;
			}
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		}
		Resident resident;
		try {
			if (!admin && sender instanceof Player player) {
				resident = getResidentOrThrow(player.getUniqueId());
				nation = getNationFromResidentOrThrow(resident);
			} else // treat resident as king for testing purposes.
				resident = nation.getKing();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage(sender));
			return;
		}
		
		switch (split[0].toLowerCase()) {
		case "leader":
		case "king":
			nationSetKing(sender, nation, split, admin);
			break;
		case "capital":
			nationSetCapital(sender, nation, split, admin);
			break;
		case "spawn":
			if (sender instanceof Player player)
				parseNationSetSpawnCommand(player, nation, admin);
			else 
				throw new TownyException("Not meant for console!");
			break;
		case "taxes":
			nationSetTaxes(sender, nation, split, admin);
			break;
		case "spawncost":
			nationSetSpawnCost(sender, nation, split, admin);
			break;
		case "name":
			nationSetName(sender, nation, split, admin);
			break;
		case "tag":
			nationSetTag(sender, nation, split, admin);
			break;
		case "title":
			nationSetTitle(sender, nation, resident, split, admin);
			break;
		case "surname":
			nationSetSurname(sender, nation, resident, split, admin);
			break;
		case "board":
			nationSetBoard(sender, nation, split);
			break;
		case "mapcolor":
			nationSetMapColor(sender, nation, split, admin);
			break;
		default:
			// Test if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_SET, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_SET, split[0]).execute(sender, "nation", split);
				return;
			}

			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}
		nation.save();
	}

	private static void nationSetMapColor(CommandSender sender, Nation nation, String[] split, boolean admin) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set mapcolor brown.");
			return;
		} else {
			String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

			if (!TownySettings.getNationColorsMap().containsKey(line.toLowerCase())) {
				String allowedColorsListAsString = TownySettings.getNationColorsMap().keySet().toString();
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_nation_map_color", allowedColorsListAsString));
				return;
			}

			nation.setMapColorHexCode(TownySettings.getNationColorsMap().get(line.toLowerCase()));
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_map_color_changed", line.toLowerCase()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_map_color_changed", line.toLowerCase()));
		}
	}

	private static void nationSetBoard(CommandSender sender, Nation nation, String[] split) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set board " + Translatable.of("town_help_9").forLocale(sender));
			return;
		} else {
			String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

			if (!line.equals("none")) {
				if (!NameValidation.isValidString(line)) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_string_nationboard_not_set"));
					return;
				}
				// TownyFormatter shouldn't be given any string longer than 159, or it has trouble splitting lines.
				if (line.length() > 159)
					line = line.substring(0, 159);
			} else 
				line = "";
			
			nation.setBoard(line);
			TownyMessaging.sendNationBoard(sender, nation);
		}
	}

	private static void nationSetSurname(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws NotRegisteredException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set surname bilbo the dwarf ");
		else
			resident = getResidentOrThrow(split[1]);

		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String surname = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
		if (surname.length() > TownySettings.getMaxTitleLength()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_input_too_long"));
			return;
		}
		
		if (NameValidation.isConfigBlacklistedName(surname)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
			return;
		}
		
		resident.setSurname(surname);
		resident.save();

		if (resident.hasSurname()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_surname", resident.getName(), Colors.translateColorCodes(resident.getSurname())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Surname", resident.getName()));
		}


	}

	private static void nationSetTitle(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws NotRegisteredException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set title bilbo Jester ");
		else
			resident = getResidentOrThrow(split[1]);
		
		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String title = StringMgmt.join(NameValidation.checkAndFilterArray(StringMgmt.remArgs(split, 2)));
		if (title.length() > TownySettings.getMaxTitleLength()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_input_too_long"));
			return;
		}
		
		if (NameValidation.isConfigBlacklistedName(title)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
			return;
		}
		
		resident.setTitle(title);
		resident.save();

		if (resident.hasTitle()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_title", resident.getName(), Colors.translateColorCodes(resident.getTitle())));
		} else {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_clear_title_surname", "Title", resident.getName()));
		}
	}

	private static void nationSetTag(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		String name = (sender instanceof Player) ? ((Player)sender).getName() : "Console"; 
		
		if (split.length < 2)
			throw new TownyException("Eg: /nation set tag PLT");
		else if (split[1].equalsIgnoreCase("clear")) {
			nation.setTag(" ");
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_reset_nation_tag", name));
		} else {
			if (split[1].length() > TownySettings.getMaxTagLength())
				throw new TownyException(Translatable.of("msg_err_tag_too_long"));
			
			nation.setTag(NameValidation.checkAndFilterName(split[1]));
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_set_nation_tag", name, nation.getTag()));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_set_nation_tag", name, nation.getTag()));
		}
	}

	private static void nationSetName(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (admin || !(sender instanceof Player))
			throw new TownyException("Use /ta nation [nation] rename");

		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set name Plutoria");				
		else {
			
			String name = String.join("_", StringMgmt.remFirstArg(split));
			
			if (NameValidation.isBlacklistName(name) || TownyUniverse.getInstance().hasNation(name) || (!TownySettings.areNumbersAllowedInNationNames() && NameValidation.containsNumbers(name)))
				throw new TownyException(Translatable.of("msg_invalid_name"));
			
			if (TownySettings.getTownAutomaticCapitalisationEnabled())
				name = StringMgmt.capitalizeStrings(name);
			
			if(TownyEconomyHandler.isActive() && TownySettings.getNationRenameCost() > 0) {
				if (!nation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost()))
					throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));

				final Nation finalNation = nation;
				final String finalName = name;
				Confirmation.runOnAccept(() -> {
					//Check if nation can still pay rename costs.
					if (!finalNation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost())) {
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));
						return;
					}
					
					finalNation.getAccount().withdraw(TownySettings.getNationRenameCost(), String.format("Nation renamed to: %s", finalName));
						
					nationRename((Player) sender, finalNation, finalName);
				})
				.setTitle(Translatable.of("msg_confirm_purchase", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())))
				.sendTo(sender);

			} else {
				nationRename((Player) sender, nation, name);
			}
		}
	}

	private static void nationSetSpawnCost(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set spawncost 70");
		else {
			double amount = MoneyUtil.getMoneyAboveZeroOrThrow(split[1]);
			if (TownySettings.getSpawnTravelCost() < amount)
				throw new TownyException(Translatable.of("msg_err_cannot_set_spawn_cost_more_than", TownySettings.getSpawnTravelCost()));

			nation.setSpawnCost(amount);
			String name = (sender instanceof Player) ? ((Player)sender).getName() : "Console"; 
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
		}
	}

	private static void nationSetTaxes(CommandSender sender, Nation nation, String[] split, boolean admin) {
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set taxes 70");
		else {
			int amount;
			try {
				amount = MathUtil.getPositiveIntOrThrow(split[1].trim());
			} catch (TownyException ignored) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_negative_money"));
				return;
			}

			String name = (sender instanceof Player) ? ((Player)sender).getName() : "Console"; 
			try {
				nation.setTaxes(amount);
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_set_nation_tax", name, nation.getTaxes()));
				if (admin)
					TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_nation_tax", name, nation.getTaxes()));
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_must_be_int"));
			}
		}
	}

	private static void nationSetCapital(CommandSender sender, Nation nation, String[] split, boolean admin) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set capital {town name}");
			return;
		}
		
		Town newCapital = TownyUniverse.getInstance().getTown(split[1]);
		
		if (newCapital == null) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[1]));
			return;
		}

		boolean capitalNotEnoughResidents = TownySettings.getNumResidentsCreateNation() > 0 && newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation();
		if (capitalNotEnoughResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_enough_residents_capital", newCapital.getName()));
			return;
		}
		
		boolean capitalTooManyResidents = TownySettings.getMaxResidentsPerTown() > 0 && nation.getCapital().getNumResidents() > TownySettings.getMaxResidentsPerTown();
		if (capitalTooManyResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_capital_too_many_residents", newCapital.getName()));
			return;
		}
		
		Runnable processCommand = () -> {
			Resident oldKing = nation.getKing();
			Resident newKing = newCapital.getMayor();

			NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldKing, newKing);
			Bukkit.getPluginManager().callEvent(nationKingChangeEvent);
			if (nationKingChangeEvent.isCancelled() && !admin) {
				TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
				return;
			}

			// Do proximity tests.
			if (TownySettings.getNationRequiresProximity() > 0 ) {
				List<Town> removedTowns = nation.gatherOutOfRangeTowns(nation.getTowns(), newCapital);

				// There are going to be some towns removed from the nation, so we'll do a Confirmation.
				if (!removedTowns.isEmpty()) {
					final Nation finalNation = nation;
					Confirmation.runOnAccept(() -> {
							finalNation.setCapital(newCapital);
							finalNation.removeOutOfRangeTowns();
							plugin.resetCache();
							TownyMessaging.sendPrefixedNationMessage(finalNation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), finalNation.getName()));
							if (admin)
								TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), finalNation.getName()));
						})
						.setTitle(Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
						.sendTo(sender);

					// No towns will be removed, skip the Confirmation.
				} else {
					nation.setCapital(newCapital);
					plugin.resetCache();
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
					if (admin)
						TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
					nation.save();
				}
				// Proximity doesn't factor in.
			} else {
				nation.setCapital(newCapital);
				plugin.resetCache();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
				if (admin)
					TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
				nation.save();
			}
		};

		if (capitalNotEnoughResidents || capitalTooManyResidents)
			Confirmation.runOnAccept(processCommand)
				.setTitle(Translatable.of("msg_warn_overriding_server_config"))
				.sendTo(sender);
		else processCommand.run();
	}

	private static void nationSetKing(CommandSender sender, Nation nation, String[] split, boolean admin) {

		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set leader Dumbo");
		else
			try {
				Resident newKing = getResidentOrThrow(split[1]);
				Resident oldKing = nation.getKing();
				Town newCapital = newKing.getTown();

				if (TownySettings.getNumResidentsCreateNation() > 0 && newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation() && !admin) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_enough_residents_capital", newCapital.getName()));
					return;
				}
				
				if (TownySettings.getMaxResidentsPerTown() > 0 && nation.getCapital().getNumResidents() > TownySettings.getMaxResidentsPerTown() && !admin) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_capital_too_many_residents", newCapital.getName()));
					return;
				}
				
				NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldKing, newKing);
				Bukkit.getPluginManager().callEvent(nationKingChangeEvent);
				if (nationKingChangeEvent.isCancelled() && !admin) {
					TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
					return;
				}

				nation.setKing(newKing);
				plugin.deleteCache(oldKing);
				plugin.deleteCache(newKing);
				TownyPerms.assignPermissions(oldKing, null); // remove permissions from old King.
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newKing.getName(), nation.getName()));
				if (admin)
					TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newKing.getName(), nation.getName()));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
	}

	private static void parseNationSetSpawnCommand(Player player, Nation nation, boolean admin) throws TownyException {
		if (TownyAPI.getInstance().isWilderness(player.getLocation()))
			throw new TownyException(Translatable.of("msg_cache_block_error_wild", "set spawn"));

		NationSetSpawnEvent event = new NationSetSpawnEvent(nation, player, player.getLocation());
		Bukkit.getPluginManager().callEvent(event);
		if (event.isCancelled() && !admin) {
			if (!event.getCancelMessage().isEmpty())
				TownyMessaging.sendErrorMsg(player, event.getCancelMessage());

			return;
		}

		Location newSpawn = admin ? player.getLocation() : event.getNewSpawn();

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(newSpawn);
		Town town = townBlock.getTownOrNull();

		// Nation spawns either have to be inside of the capital.
		if (nation.getCapital() != null 
			&& TownySettings.isNationSpawnOnlyAllowedInCapital()
			&& !town.getUUID().equals(nation.getCapital().getUUID()))
				throw new TownyException(Translatable.of("msg_err_spawn_not_within_capital"));
		// Or they can be in any town in the nation.
		else 
			if(!nation.getTowns().contains(town))
				throw new TownyException(Translatable.of("msg_err_spawn_not_within_nationtowns"));
		
		// Remove the SpawnPoint particles.
		if (nation.hasSpawn())
			TownyUniverse.getInstance().removeSpawnPoint(nation.getSpawn());
		
		// Set the spawn point and send feedback message.
		nation.setSpawn(newSpawn);
		TownyMessaging.sendMsg(player, Translatable.of("msg_set_nation_spawn"));
	}

	private static void parseNationBaltop(Player player, Nation nation) {
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
			StringBuilder sb = new StringBuilder();
			List<Resident> residents = new ArrayList<>(nation.getResidents());
			residents.sort(Comparator.<Resident>comparingDouble(res -> res.getAccount().getCachedBalance()).reversed());

			int i = 0;
			for (Resident res : residents)
				sb.append(Translatable.of("msg_baltop_book_format", ++i, res.getName(), TownyEconomyHandler.getFormattedBalance(res.getAccount().getCachedBalance())).forLocale(player) + "\n");

			player.openBook(BookFactory.makeBook("Nation Baltop", nation.getName(), sb.toString()));
		});
	}

	public static void nationToggle(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_TOGGLE_HELP.send(sender);
			return;
		}
		
		Resident resident;

		if (!admin) {
			resident = getResidentOrThrow(((Player) sender).getUniqueId());
			nation = getNationFromResidentOrThrow(resident);
		} else // Treat any resident tests as though the king were doing it.
			resident = nation.getKing();

		if (!admin && !TownyUniverse.getInstance().getPermissionSource().testPermission((Player) sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE.getNode(split[0].toLowerCase()))) {
			// Check if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_TOGGLE, split[0]).execute(sender, "nation", split);
				return;
			} 
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		}
		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		switch (split[0].toLowerCase()) {
		case "peaceful":
		case "neutral":
			nationTogglePeaceful(sender, nation, choice, admin);
			break;
		case "public":
			nationTogglePublic(sender, nation, choice, admin);
			break;
		case "open":
			nationToggleOpen(sender, nation, choice, admin);
			break;
		default:
			// Check if this is an add-on command.
			if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_TOGGLE, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_TOGGLE, split[0]).execute(sender, "nation", split);
				return;
			}

			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_property", split[0]));
			return;
		}
		nation.save();
	}

	private static void nationTogglePeaceful(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		boolean peacefulState = choice.orElse(!nation.isNeutral());
		double cost = TownySettings.getNationNeutralityCost();

		if (nation.isNeutral() && peacefulState)
			throw new TownyException(Translatable.of("msg_nation_already_peaceful"));
		else if (!nation.isNeutral() && !peacefulState)
			throw new TownyException(Translatable.of("msg_nation_already_not_peaceful"));

		if (peacefulState && TownyEconomyHandler.isActive() && !nation.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_nation_cant_peaceful"));
		
		String uuid = nation.getUUID().toString();
		
		if (TownySettings.getPeacefulCoolDownTime() > 0 && 
			!admin && 
			CooldownTimerTask.hasCooldown(uuid, CooldownType.NEUTRALITY) && 
			!TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(sender))
			throw new TownyException(Translatable.of("msg_err_cannot_toggle_neutral_x_seconds_remaining",
					CooldownTimerTask.getCooldownRemaining(uuid, CooldownType.NEUTRALITY)));

		// Fire cancellable event directly before setting the toggle.
		NationToggleNeutralEvent preEvent = new NationToggleNeutralEvent(sender, nation, admin, peacefulState);
		Bukkit.getPluginManager().callEvent(preEvent);
		if (preEvent.isCancelled())
			throw new TownyException(preEvent.getCancelMessage());

		// If they setting neutral status on send a message confirming they paid
		// something, if they did.
		if (peacefulState && TownyEconomyHandler.isActive() && cost > 0) {
			nation.getAccount().withdraw(cost, "Peaceful Nation Cost");
			TownyMessaging.sendMsg(sender, Translatable.of("msg_you_paid", TownyEconomyHandler.getFormattedBalance(cost)));
		}

		nation.setNeutral(peacefulState);

		// Send message feedback to the whole nation.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_peaceful").append(nation.isNeutral() ? Colors.Green : Colors.Red + " not").append(" peaceful."));
		
		// Add a cooldown to Public toggling.
		if (TownySettings.getPeacefulCoolDownTime() > 0 && !admin && !TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(sender))
			CooldownTimerTask.addCooldownTimer(uuid, CooldownType.NEUTRALITY);

	}

	private static void nationTogglePublic(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationTogglePublicEvent preEvent = new NationTogglePublicEvent(sender, nation, admin, choice.orElse(!nation.isPublic()));
		Bukkit.getPluginManager().callEvent(preEvent);
		if (preEvent.isCancelled())
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setPublic(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_public", nation.isPublic() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void nationToggleOpen(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationToggleOpenEvent preEvent = new NationToggleOpenEvent(sender, nation, admin, choice.orElse(!nation.isOpen()));
		Bukkit.getPluginManager().callEvent(preEvent);
		if (preEvent.isCancelled())
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setOpen(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_open", nation.isOpen() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	public static void nationRename(Player player, Nation nation, String newName) {

		NationPreRenameEvent event = new NationPreRenameEvent(nation, newName);
		Bukkit.getServer().getPluginManager().callEvent(event);
		if (event.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_rename_cancelled"));
			return;
		}
		
		try {
			TownyUniverse.getInstance().getDataSource().renameNation(nation, newName);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_name", player.getName(), nation.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	/**
	 * Wrapper for the nationSpawn() method. All calls should be through here unless
	 * bypassing for admins.
	 *
	 * @param player        - Player.
	 * @param split         - Current command arguments.
	 * @param ignoreWarning - Whether to ignore the cost
	 * @throws TownyException - Exception.
	 */
	public static void nationSpawn(Player player, String[] split, boolean ignoreWarning) throws TownyException {

		Nation nation = getPlayerNationOrNationFromArg(player, split);
		String notAffordMSG = split.length == 0 ? 
			Translatable.of("msg_err_cant_afford_tp").forLocale(player) : 
			Translatable.of("msg_err_cant_afford_tp_nation", nation.getName()).forLocale(player);
		SpawnUtil.sendToTownySpawn(player, split, nation, notAffordMSG, false, ignoreWarning, SpawnType.NATION);
	}

	private static void nationTransaction(Player player, String[] args, boolean withdraw) {
		if (TownySettings.isEconomyAsync() && Bukkit.isPrimaryThread()) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> nationTransaction(player, args, withdraw));
			return;
		}
		
		try {
			Resident resident = getResidentOrThrow(player.getUniqueId());
			Nation nation = getNationFromResidentOrThrow(resident);

			if (args.length < 2 || args.length > 3)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation" + (withdraw ? " withdraw" : " deposit")));

			int amount;
			if ("all".equalsIgnoreCase(args[1].trim()))
				amount = (int) Math.floor(withdraw ? nation.getAccount().getHoldingBalance() : resident.getAccount().getHoldingBalance());
			else 
				amount = MathUtil.getIntOrThrow(args[1].trim());
			
			if (args.length == 2) {
				if (withdraw)
					MoneyUtil.nationWithdraw(player, resident, nation, amount);
				else 
					MoneyUtil.nationDeposit(player, resident, nation, amount);
				return;
			}
			
			if (withdraw)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation withdraw"));

			if (args.length == 3) {
				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT_OTHER.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				
				Town town = TownyUniverse.getInstance().getTown(args[2]);
				if (town != null) {
					if (!nation.hasTown(town))
						throw new TownyException(Translatable.of("msg_err_not_same_nation", town.getName()));

					MoneyUtil.townDeposit(player, resident, town, nation, amount);

				} else {
					throw new NotRegisteredException();
				}
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
    }
    
	private void nationStatusScreen(CommandSender sender, Nation nation) {
		/*
		 * This is run async because it will ping the economy plugin for the nation bank value.
		 */
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(nation, sender)));
	}
	
	/**
	 * Parse a page number from a {@link String[]} 
	 * @param split {@link String[]} which should contain a page number at some position.
	 * @param i the array element which will be looked at for a page value.
	 * @return 1 or the page number from the {@link String[]}.
	 */
	private int getPage(String[] split, int i) {
		int page = 1;
		if (split.length > i) {
			try {
				page = Integer.parseInt(split[i]);
			} catch (NumberFormatException ignored) {}
		}
		return page;
	}
}
