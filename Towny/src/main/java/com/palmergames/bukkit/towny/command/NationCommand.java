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
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NationAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationInviteTownEvent;
import com.palmergames.bukkit.towny.event.NationPreAddEnemyEvent;
import com.palmergames.bukkit.towny.event.NationPreRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationRankRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationSanctionTownAddEvent;
import com.palmergames.bukkit.towny.event.nation.NationSanctionTownRemoveEvent;
import com.palmergames.bukkit.towny.event.nation.NationSetSpawnEvent;
import com.palmergames.bukkit.towny.event.nation.NationTownLeaveEvent;
import com.palmergames.bukkit.towny.event.NationRemoveEnemyEvent;
import com.palmergames.bukkit.towny.event.NationRequestAllyNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.event.nation.NationMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreAddAllyEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreInviteTownEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreMergeEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownKickEvent;
import com.palmergames.bukkit.towny.event.nation.NationPreTownLeaveEvent;
import com.palmergames.bukkit.towny.event.nation.PreNewNationEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleOpenEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationTogglePublicEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleTaxPercentEvent;
import com.palmergames.bukkit.towny.event.NationPreAddTownEvent;
import com.palmergames.bukkit.towny.event.NationPreRenameEvent;
import com.palmergames.bukkit.towny.event.NationRemoveAllyEvent;
import com.palmergames.bukkit.towny.event.NationDenyAllyRequestEvent;
import com.palmergames.bukkit.towny.event.NationAcceptAllyRequestEvent;
import com.palmergames.bukkit.towny.event.nation.NationKingChangeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteReceiver;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.ComparatorCaches;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ProximityUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.SpawnUtil;
import com.palmergames.bukkit.util.BookFactory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MathUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.InvalidObjectException;
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


public class NationCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	
	@VisibleForTesting
	public static final List<String> nationTabCompletes = Arrays.asList(
		"list",
		"online",
		"leave",
		"withdraw",
		"deposit",
		"new",
		"create",
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
		"sanctiontown",
		"king",
		"leader",
		"bankhistory",
		"baltop"
	);

	@VisibleForTesting
	public static final List<String> nationSetTabCompletes = Arrays.asList(
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
		"mapcolor",
		"conqueredtax",
		"taxpercentcap"
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
		"open",
		"taxpercent"
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
			Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (res == null)
				return Collections.emptyList();
			Nation nation = res.getNationOrNull();

			switch (args[0].toLowerCase(Locale.ROOT)) {
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
						return Collections.singletonList("-ignore");
					}
					break;
				case "sanctiontown":
					if (nation == null)
						break;
					if (args.length == 2) 
						return NameUtil.filterByStart(Arrays.asList("add", "remove", "list"), args[1]); 
					if (args.length == 3 && (args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("remove")))
						return NameUtil.filterByStart(TownyUniverse.getInstance().getTowns()
							.stream()
							.filter(t -> !nation.hasTown(t))
							.map(Town::getName)
							.collect(Collectors.toList()), args[2]);
					if (args.length == 3 && args[1].equalsIgnoreCase("list"))
						return getTownyStartingWith(args[2], "n");
					break;
				case "add":
					return getTownyStartingWith(args[args.length - 1], "t");
				case "kick":
					if (res.hasNation())
						return NameUtil.filterByStart(NameUtil.getNames(res.getNationOrNull().getTowns()), args[args.length - 1]);
					break;
				case "ally":
					if (!res.hasNation())
						break;
					if (args.length == 2) {
						return NameUtil.filterByStart(nationAllyTabCompletes, args[1]);
					} else if (args.length > 2){
						switch (args[1].toLowerCase(Locale.ROOT)) {
							case "add":
								if (args[args.length - 1].startsWith("-")) {
									return NameUtil.filterByStart(nation.getSentAllyInvites()
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
								} else {
									// Otherwise return possible nations to send invites to
									return getTownyStartingWith(args[args.length - 1], "n");
								}
							case "remove":
								return NameUtil.filterByStart(NameUtil.getNames(nation.getAllies()), args[args.length - 1]);
							case "accept":
							case "deny":
								return NameUtil.filterByStart(nation.getReceivedInvites()
									.stream()
									.map(Invite::getSender)
									.map(InviteSender::getName)
									.collect(Collectors.toList()), args[args.length - 1]);
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "rank":
					if (!res.hasNation())
						break;
					switch (args.length) {
					case 2:
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					case 3:
						return getNationResidentNamesOfPlayerStartingWith(player, args[2]);
					case 4:
						switch (args[1].toLowerCase(Locale.ROOT)) {
							case "add":
								if (nation == null)
									return Collections.emptyList();
								return NameUtil.filterByStart(TownyPerms.getNationRanks(nation), args[3]);
							case "remove": {
								Resident rankHaver = TownyUniverse.getInstance().getResident(args[2]);
								if (rankHaver != null)
									return rankHaver.getNationRanks().isEmpty() ? Collections.emptyList() : NameUtil.filterByStart(rankHaver.getNationRanks(), args[3]);
								break;
							}
							default:
								return Collections.emptyList();
						}
					default:
						return Collections.emptyList();
					}
				case "enemy":
					if (!res.hasNation())
						break;
					if (args.length == 2) {
						return NameUtil.filterByStart(nationEnemyTabCompletes, args[1]);
					} else if (args.length >= 3){
						switch (args[1].toLowerCase(Locale.ROOT)) {
							case "add":
								return getTownyStartingWith(args[2], "n");
							case "remove":
								return NameUtil.filterByStart(NameUtil.getNames(nation.getEnemies()), args[2]);
							default:
								return Collections.emptyList();
						}
					}
					break;
				case "set":
					if (!res.hasNation())
						return Collections.emptyList();
					else 
						return nationSetTabComplete(sender, nation, args);
				case "list":
					switch (args.length) {
						case 2:
							return Collections.singletonList("by");
						case 3:
							return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.NATION_LIST_BY, nationListTabCompletes), args[2]);
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
			
			switch (args[1].toLowerCase(Locale.ROOT)) {
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
					break;
				case "mapcolor":
					if (args.length == 3)
						return NameUtil.filterByStart(TownySettings.getNationColorsMap().keySet().stream().collect(Collectors.toList()), args[2]);
					break;
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

		if (split.length == 0) {
			nationStatusScreen(player, getNationFromPlayerOrThrow(player));
			return;
		} 

		if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_HELP.send(player);
			return;
		}

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "list":
			listNations(player, split);
			break;
		case "townlist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_TOWNLIST.getNode());
			nationTownList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "allylist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLYLIST.getNode());
			nationAllyList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "enemylist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMYLIST.getNode());
			nationEnemyList(player, getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)));
			break;
		case "new":
		case "create":
			newNation(player, StringMgmt.remFirstArg(split));
			break;
		case "join":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_JOIN.getNode());
			parseNationJoin(player, StringMgmt.remFirstArg(split));
			break;
		case "merge":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_MERGE.getNode());
			mergeNation(player, split);
			break;
		case "withdraw":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode());
			TownyEconomyHandler.economyExecutor().execute(() -> nationTransaction(player, StringMgmt.remFirstArg(split), true));
			break;
		case "leave":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LEAVE.getNode());
			nationLeave(player);
			break;
		case "spawn":
			/* Permission test is internal*/
			boolean ignoreWarning = (split.length > 1 && split[1].equals("-ignore")) || (split.length > 2 && split[2].equals("-ignore"));
			nationSpawn(player, StringMgmt.remFirstArg(split), ignoreWarning);
			break;
		case "deposit":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode());
			TownyEconomyHandler.economyExecutor().execute(() -> nationTransaction(player, StringMgmt.remFirstArg(split), false));
			break;
		case "rank":
			/* Permission test is internal*/
			nationRank(player, StringMgmt.remFirstArg(split));
			break;
		case "ranklist":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_RANKLIST.getNode());
			TownyMessaging.sendMessage(player, TownyFormatter.getRanksForNation(getPlayerNationOrNationFromArg(player, StringMgmt.remFirstArg(split)), Translator.locale(player)));
			break;
		case "king":
		case "leader":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LEADER.getNode());
			nationKing(player, StringMgmt.remFirstArg(split));
			break;
		case "add":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode());
			nationAdd(player, StringMgmt.remFirstArg(split));
			break;
		case "invite":
		case "invites":
			/* Permission test is internal*/
			parseInviteCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "kick":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_KICK.getNode());
			nationKick(player, StringMgmt.remFirstArg(split));
			break;
		case "sanctiontown":
			nationSanctionTown(player, null, StringMgmt.remFirstArg(split));
			break;
		case "set":
			/* Permission test is internal*/
			nationSet(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "toggle":
			/* Permission test is internal*/
			nationToggle(player, StringMgmt.remFirstArg(split), false, null);
			break;
		case "ally":
			/* Permission test is internal*/
			nationAlly(player, StringMgmt.remFirstArg(split));
			break;
		case "enemy":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMY.getNode());
			nationEnemy(player, StringMgmt.remFirstArg(split));
			break;
		case "delete":
			nationDelete(player, StringMgmt.remFirstArg(split));
			break;
		case "online":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode());
			parseNationOnlineCommand(player, StringMgmt.remFirstArg(split));
			break;
		case "say":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_SAY.getNode());
			nationSay(player, StringMgmt.remFirstArg(split));
			break;
		case "bankhistory":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_BANKHISTORY.getNode());
			nationBankHistory(player, StringMgmt.remFirstArg(split));
			break;
		case "baltop":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_BALTOP.getNode());
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
			if (!nation.hasResident(player.getName()))
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode());

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

	private void nationSay(Player player, String[] split) throws TownyException {
		if (split.length == 0)
			throw new TownyException("ex: /n say [message here]");
		getNationFromPlayerOrThrow(player).playerBroadCastMessageToNation(player, StringMgmt.join(split));
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
			TownyMessaging.sendMsg(player, Translatable.of("msg_error_nation_has_no_enemies"));
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(nation.getName() + " " + Translatable.of("status_nation_enemies").forLocale(player)));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_enemies").forLocale(player), new ArrayList<>(nation.getEnemies())));
		}
		List<Nation> enemiedByList = TownyAPI.getInstance().getNations().stream().filter(n-> n.hasEnemy(nation)).collect(Collectors.toList());
		if (!enemiedByList.isEmpty()) {
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedTownyObjects(Translatable.of("status_nation_enemied_by").forLocale(player), new ArrayList<>(enemiedByList)));
		}
	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		Nation nation = getNationFromPlayerOrThrow(player);
		String sent = Translatable.of("nation_sent_invites").forLocale(player)
				.replace("%a", Integer.toString(nation.getSentInvites().size()))
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(nation)));

		if (newSplit.length == 0) { // (/nation invite)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_SEE_HOME.getNode());
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
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_LIST_SENT.getNode());
				List<Invite> sentinvites = nation.getSentInvites();
				InviteCommand.sendInviteList(player, sentinvites, getPage(newSplit, 1), true);
				TownyMessaging.sendMessage(player, sent);
				return;
			} else {
				checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode());
				nationAdd(player, newSplit);
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

		if (split.length < 3
			|| !(split[0].equalsIgnoreCase("add") || split[0].equalsIgnoreCase("remove"))) {
			// Help output.
			HelpMenu.NATION_RANK.send(player);
			return;
		}

		Resident resident = getResidentOrThrow(player);
		Resident target = getResidentOrThrow(split[1]);
		Nation nation = getNationFromResidentOrThrow(resident);

		if (!nation.hasResident(target))
			throw new TownyException(Translatable.of("msg_err_not_same_nation", target.getName()));

		/*
		 * Match casing to an existing rank, returns null if Nation rank doesn't exist.
		 */
		String rank = TownyPerms.matchNationRank(split[2]);
		if (rank == null)
			throw new TownyException(Translatable.of("msg_unknown_rank_available_ranks", split[2], StringMgmt.join(TownyPerms.getNationRanks(), ", ")));

		/*
		 * Only allow the player to assign ranks if they have the grant perm for it.
		 */
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase(Locale.ROOT)), Translatable.of("msg_no_permission_to_give_rank"));

		Translatable nationWord = Translatable.of("nation_sing");
		if (split[0].equalsIgnoreCase("add")) {
			if (target.hasNationRank(rank)) // Must already have this rank
				throw new TownyException(Translatable.of("msg_resident_already_has_rank", target.getName(), nationWord));

			if (TownyPerms.ranksWithNationLevelRequirementPresent()) {
				int rankLevelReq = TownyPerms.getRankNationLevelReq(rank);
				int levelNumber = target.getNationOrNull().getLevelNumber();
				if (rankLevelReq > levelNumber)
					throw new TownyException(Translatable.of("msg_town_or_nation_level_not_high_enough_for_this_rank", nationWord, rank, nationWord, levelNumber, rankLevelReq));
			}

			BukkitTools.ifCancelledThenThrow(new NationRankAddEvent(nation, rank, target));

			target.addNationRank(rank);
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_given_rank", nationWord, rank, target.getName()));
			if (target.isOnline()) {
				TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_been_given_rank", nationWord, rank));
				plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
			}
		}

		if (split[0].equalsIgnoreCase("remove")) {
			if (!target.hasNationRank(rank)) // Doesn't have this rank
				throw new TownyException(Translatable.of("msg_resident_doesnt_have_rank", target.getName(), nationWord));

			BukkitTools.ifCancelledThenThrow(new NationRankRemoveEvent(nation, rank, target));

			target.removeNationRank(rank);
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_taken_rank_from", nationWord, rank, target.getName()));
			if (target.isOnline()) {
				TownyMessaging.sendMsg(target.getPlayer(), Translatable.of("msg_you_have_had_rank_taken", nationWord, rank));
				plugin.deleteCache(TownyAPI.getInstance().getPlayer(target));
			}
		}

		/*
		 * If we got here we have made a change Save the altered resident
		 * data.
		 */
		target.save();

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
		if (split.length < 2 && !console)
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST_RESIDENTS.getNode());
		
		List<Nation> nationsToSort = new ArrayList<>(TownyUniverse.getInstance().getNations());
		int page = 1;
		boolean pageSet = false;
		boolean comparatorSet = false;
		ComparatorType type = ComparatorType.RESIDENTS;
		int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
		for (int i = 1; i < split.length; i++) {
			if (split[i].equalsIgnoreCase("by")) { // Is a case of someone using /n list by {comparator}
				if (TownyCommandAddonAPI.hasCommand(CommandType.NATION_LIST_BY, split[i+1])) {
					TownyCommandAddonAPI.getAddonCommand(CommandType.NATION_LIST_BY, split[i+1]).execute(sender, "nation", split);
					return;
				}

				if (comparatorSet) {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_multiple_comparators_nation"));
					return;
				}
				i++;
				if (i < split.length) {
					comparatorSet = true;
					if (split[i].equalsIgnoreCase("resident")) 
						split[i] = "residents";
					
					if (!console)
						checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode(split[i]));
					
					if (!nationListTabCompletes.contains(split[i].toLowerCase(Locale.ROOT)))
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
			plugin.getScheduler().runAsync(() -> TownyMessaging.sendNationList(sender, ComparatorCaches.getNationListCache(finalType), finalType, pageNumber, total));
		} catch (RuntimeException e) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_error_comparator_failed"));
		}

	}

	private void newNation(Player player, String[] split) throws TownyException {
		checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_NEW.getNode());
		if (split.length == 0)
			throw new TownyException(Translatable.of("msg_specify_nation_name"));

		String nationName = String.join("_", split);
		if (TownySettings.getTownAutomaticCapitalisationEnabled())
			nationName = StringMgmt.capitalizeStrings(nationName);

		Town town = getTownForNationCapital(player);
		boolean noCharge = TownySettings.getNewNationPrice() == 0.0 || !TownyEconomyHandler.isActive();
		newNation((CommandSender) player, nationName, town, noCharge);
	}

	private Town getTownForNationCapital(Player player) throws TownyException {
		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident);
		if (!town.hasEnoughResidentsToBeANationCapital())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents_new_nation"));

		if (!resident.isMayor() && !town.hasResidentWithRank(resident, "assistant"))
			throw new TownyException(Translatable.of("msg_peasant_right"));
		return town;
	}

	/**
	 * Ties together the player-run /new nation and the admin-run /ta nation new
	 * NAME CAPITAL code. Vets the name supplied, throws the cancellable event and
	 * then charges (if required) before creating a new nation.
	 *
	 * @param sender      Sender who initiated the creation of the nation.
	 * @param name        Nation name to vet.
	 * @param capitalTown Town which will become the capital city.
	 * @param noCharge    when true and the Economy is enabled we charge the new
	 *                    nation cost
	 */
	public static void newNation(CommandSender sender, String name, Town capitalTown, boolean noCharge) throws TownyException {

		if (capitalTown.hasNation())
			throw new TownyException(Translatable.of("msg_err_already_nation"));

		String filteredName = NameValidation.checkAndFilterNationNameOrThrow(name);
		if (TownyUniverse.getInstance().hasNation(filteredName))
			throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", filteredName));

		BukkitTools.ifCancelledThenThrow(new PreNewNationEvent(capitalTown, filteredName));

		if (noCharge || !TownyEconomyHandler.isActive()) {
			// It's free so make the nation.
			Nation nation = newNation(filteredName, capitalTown);
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_nation", sender.getName(), nation.getFormattedName()));
			return;
		}

		// It isn't free to make a nation, send a confirmation.
		double cost = TownySettings.getNewNationPrice();
		// Test if they can pay.
		if (!capitalTown.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_no_funds_new_nation2", prettyMoney(cost)));

		Confirmation.runOnAccept(() -> {
			try {
				Nation nation = newNation(filteredName, capitalTown);
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_nation", sender.getName(), nation.getFormattedName()));
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
		})
		.setCost(new ConfirmationTransaction(TownySettings::getNewNationPrice, capitalTown, "New Nation Cost",
				Translatable.of("msg_no_funds_new_nation2", prettyMoney(cost))))
		.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
		.sendTo(sender);
	}

	public static Nation newNation(String name, Town town) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		UUID nationUUID = UUID.randomUUID();
		townyUniverse.getDataSource().newNation(name, nationUUID);
		Nation nation = townyUniverse.getNation(nationUUID);
		
		// Should never happen.
		if (nation == null) {
			TownyMessaging.sendErrorMsg(String.format("Error fetching new nation with name %s; it was not properly registered!", name));
			throw new TownyException(Translatable.of("msg_err_not_registered_1", name));
		}

		nation.setRegistered(System.currentTimeMillis());
		nation.setMapColorHexCode(TownySettings.getDefaultNationMapColor());
		town.setNation(nation);
		nation.setCapital(town);
		nation.setSpawn(town.getSpawnOrNull());

		if (TownyEconomyHandler.isActive())
			nation.getAccount().setBalance(0, "New Nation Account");

		if (TownySettings.isNationTagSetAutomatically())
			nation.setTag(NameUtil.getTagFromName(name));

		town.save();
		nation.save();

		BukkitTools.fireEvent(new NewNationEvent(nation));

		return nation;
	}

	public void mergeNation(Player player, String[] split) throws TownyException {
		mergeNation(player, StringMgmt.remFirstArg(split), getNationFromPlayerOrThrow(player), false);

	}

	public static void mergeNation(CommandSender sender, String[] split, @NotNull Nation remainingNation, boolean admin) throws TownyException {

		if (split.length == 0) // /n merge
			throw new TownyException(Translatable.of("msg_specify_nation_name"));

		String name = split[0];
		if (!admin && sender instanceof Player player && !getResidentOrThrow(player).isKing())
			throw new TownyException(Translatable.of("msg_err_merging_for_kings_only"));

		Nation nation = TownyUniverse.getInstance().getNation(name);
		if (nation == null || remainingNation.getName().equalsIgnoreCase(name))
			throw new TownyException(Translatable.of("msg_err_invalid_name", name));

		Resident king = nation.getKing();
		if (!king.isOnline()) {
			throw new TownyException(Translatable.of("msg_err_king_of_that_nation_is_not_online", name, king.getName()));
		}

		TownyMessaging.sendMsg(king, Translatable.of("msg_would_you_merge_your_nation_into_other_nation", nation, remainingNation, remainingNation));
		if (TownySettings.getNationProximityToCapital() > 0) {
			List<Town> towns = new ArrayList<>(nation.getTowns());
			towns.addAll(remainingNation.getTowns());
			List<Town> removedTowns = ProximityUtil.gatherOutOfRangeTowns(remainingNation);
			if (!removedTowns.isEmpty()) {
				TownyMessaging.sendMsg(nation.getKing(), Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")));
				TownyMessaging.sendMsg(remainingNation.getKing(), Translatable.of("msg_warn_the_following_towns_will_be_removed_from_the_merged_nation", StringMgmt.join(removedTowns, ", ")));
			}
		}
		Confirmation.runOnAccept(() -> {
			BukkitTools.fireEvent(new NationMergeEvent(nation, remainingNation));
			TownyUniverse.getInstance().getDataSource().mergeNation(nation, remainingNation);
			TownyMessaging.sendGlobalMessage(Translatable.of("nation1_has_merged_with_nation2", nation, remainingNation));
			if (TownySettings.getNationProximityToCapital() > 0)
				ProximityUtil.removeOutOfRangeTowns(remainingNation);
		}).runOnCancel(() -> {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_merge_request_denied"));     // These messages don't use the word Town or Nation.
			TownyMessaging.sendMsg(nation.getKing(), Translatable.of("msg_town_merge_cancelled"));
		})
		.setCancellableEvent(new NationPreMergeEvent(nation, remainingNation))
		.sendTo(BukkitTools.getPlayerExact(king.getName()));
	}
	
	public void nationLeave(Player player) throws TownyException {
		Resident resident = getResidentOrThrow(player);
		Town town = getTownFromResidentOrThrow(resident); 
		Nation nation = getNationFromResidentOrThrow(resident);
	
		BukkitTools.ifCancelledThenThrow(new NationPreTownLeaveEvent(nation, town));

		// Check that the capital wont have too many residents after deletion.
		final boolean tooManyResidents = town.isCapital() && town.isAllowedThisAmountOfResidents(town.getNumResidents(), false);
		if (tooManyResidents) {
			// Show a message preceding the confirmation message if they will lose residents.
			int maxResidentsPerTown = town.getMaxAllowedNumberOfResidentsWithoutNation();
			TownyMessaging.sendMsg(player, Translatable.of("msg_deleting_nation_will_result_in_losing_residents", maxResidentsPerTown, town.getNumResidents() - maxResidentsPerTown));
		}

		Confirmation.runOnAccept(() -> {
			BukkitTools.fireEvent(new NationTownLeaveEvent(nation, town));
			town.removeNation();

			if (tooManyResidents)
				ResidentUtil.reduceResidentCountToFitTownMaxPop(town);

			plugin.resetCache();

			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", StringMgmt.remUnderscore(town.getName())));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", StringMgmt.remUnderscore(nation.getName())));

			ProximityUtil.removeOutOfRangeTowns(nation);
		}).sendTo(player);
	}

	public void nationDelete(Player player, String[] split) throws TownyException {
		// Player is using "/n delete"
		if (split.length == 0) {
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_DELETE.getNode());

			Town town = getTownFromPlayerOrThrow(player);
			Nation nation = getNationFromTownOrThrow(town);
			// Check that the capital wont have too many residents after deletion. 
			boolean tooManyResidents = !town.isAllowedThisAmountOfResidents(town.getNumResidents(), false); 
			// Show a message preceding the confirmation message if they will lose residents. 
			if (tooManyResidents) {
				int maxResidentsPerTown = town.getMaxAllowedNumberOfResidentsWithoutNation();
				TownyMessaging.sendMsg(player, Translatable.of("msg_deleting_nation_will_result_in_losing_residents", maxResidentsPerTown, town.getNumResidents() - maxResidentsPerTown));
			}

			Confirmation.runOnAccept(() -> {
				if (TownyUniverse.getInstance().getDataSource().removeNation(nation, DeleteNationEvent.Cause.COMMAND, player)) {
					TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation.getName()));
					if (tooManyResidents)
						ResidentUtil.reduceResidentCountToFitTownMaxPop(town);
				}
			})
			.sendTo(player);
			return;
		}

		// Admin is using "/n delete NATIONNAME"
		checkPermOrThrowWithMessage(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode(), Translatable.of("msg_err_admin_only_delete_nation"));

		Nation nation = getNationOrThrow(split[0]);
		Confirmation.runOnAccept(() -> {
			TownyUniverse.getInstance().getDataSource().removeNation(nation, DeleteNationEvent.Cause.ADMIN_COMMAND, player);
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_del_nation", nation.getName()));
		}).sendTo(player);
	}

	public void nationKing(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			HelpMenu.KING_HELP.send(player);
	}


	private void parseNationJoin(Player player, String[] args) throws TownyException {
		if (args.length < 1)
			throw new TownyException(Translatable.of("msg_usage", "/nation join [nation]"));

		Town town = getTownFromPlayerOrThrow(player);
		Nation nation = getNationOrThrow(args[0]);

		// Check if the nation open able to be joined without an invite.
		if (!nation.isOpen())
			throw new TownyException(Translatable.of("msg_err_nation_not_open", nation.getFormattedName()));

		// Vet whether the town can join the nation.
		testNationAddTownOrThrow(town, nation);

		// Actually go through with adding the town.
		nationAdd(nation, town);
	}

	private void testNationAddTownOrThrow(Town town, Nation nation) throws TownyException {
		// Check if town is currently in a nation.
		if (nation.hasTown(town) || town.hasNation())
			throw new TownyException(Translatable.of("msg_err_already_in_town", town.getName(), town.getNationOrNull().getName()));

		if (!town.hasEnoughResidentsToJoinANation())
			throw new TownyException(Translatable.of("msg_err_not_enough_residents_join_nation", town.getName()));

		// Check if the town is sanctioned and not allowed to join.
		if (nation.hasSanctionedTown(town))
			throw new TownyException(Translatable.of("msg_err_cannot_join_nation_sanctioned_town", nation.getName()));

		if (nation.hasReachedMaxTowns())
			throw new TownyException(Translatable.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));

		if (!nation.canAddResidents(town.getNumResidents()))
			throw new TownyException(Translatable.of("msg_err_cannot_join_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation()));

		if (TownySettings.getNationProximityToCapital() > 0)
			ProximityUtil.testTownProximityToNation(town, nation); // Throws TownyException with error message when it fails.

		// Check if the command is not cancelled
		BukkitTools.ifCancelledThenThrow(new NationPreAddTownEvent(nation, town));
	}

	/**
	 * First stage of adding towns to a nation. We go through the player-submitted
	 * list of Names, vet them for invite or invite-revocation, then either send
	 * them an invite or revoke their invite when a town name is preceded by "-".
	 * 
	 * @param player Player using the command.
	 * @param names  Names that will be matched to towns.
	 * @throws TownyException when no towns were able to be invited.
	 */
	public void nationAdd(Player player, String[] names) throws TownyException {

		if (names.length < 1)
			throw new TownyException("Eg: /nation add [names]");

		Nation nation = getNationFromPlayerOrThrow(player);

		// Our list of names to scan through.
		List<String> nameList = new ArrayList<>(Arrays.asList(names));

		// Revoke invites from towns who have already had invites sent.
		nameList.stream()
				.filter(name -> name.startsWith("-") || nation.hasTown(name))
				.map(name -> name.startsWith("-") ? name.substring(1) : name)
				.filter(name -> TownyAPI.getInstance().getTown(name) != null)
				.map(name -> TownyAPI.getInstance().getTown(name))
				.filter(town -> nation.getSentInvites().stream().anyMatch(invite -> town.equals(invite.getReceiver())))
				.forEach(town -> nationRevokeInviteTown(player, nation, town));

		// Gather a list of Towns able to receive an invite to the nation, sending back
		// error messages for the towns that cannot, finally dumping them to a list for
		// the feedback message.
		List<String> invitedNames = nameList.stream()
				.filter(name -> !name.startsWith("-"))
				.filter(name -> TownyAPI.getInstance().getTown(name) != null)
				.map(name -> TownyAPI.getInstance().getTown(name))
				.filter(town -> {
					try {
						// test that the town can join the nation. 
						testNationAddTownOrThrow(town, nation);
						// Send the actual invite to the town being added to the nation.
						nationInviteTown(player, nation, town);
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage(player));
						return false;
					}
					return true;
				})
				.map(town -> town.getName())
				.collect(Collectors.toList());

		// Send the feedback message.
		if (!invitedNames.isEmpty())
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_invited_join_nation", player.getName(), String.join(", ", invitedNames)));
	}

	private static void nationRevokeInviteTown(CommandSender sender, Nation nation, Town town) {
		InviteHandler.getActiveInvitesFor(nation, town).forEach(invite -> {
			try {
				InviteHandler.declineInvite(invite, true);
				TownyMessaging.sendMsg(sender, Translatable.of("nation_revoke_invite_successful"));
			} catch (InvalidObjectException e) {
				plugin.getLogger().log(Level.WARNING, "unknown exception occurred while revoking invite", e);
			}
		});
	}

	/**
	 * Final stage of adding towns to a nation.
	 * 
	 * @deprecated since 0.100.1.2 use {@link #nationAdd(Nation, Town)} instead.
	 * @param nation Nation being added to.
	 * @param towns  List of Town(s) being added to Nation.
	 */
	@Deprecated
	public static void nationAdd(Nation nation, List<Town> towns) {
		for (Town town : towns)
			nationAdd(nation, town);
	}

	/**
	 * Final stage of adding a town to a nation, via joining or via accepting an
	 * invite. We re-test the rules for joining a nation in case the town or
	 * nation's situation has changed since being sent the invite/join confirmation.
	 * 
	 * @param nation Nation which would take on a new Town.
	 * @param town   Town which would join the nation.
	 */
	public static void nationAdd(Nation nation, Town town) {
		if (town.hasNation()) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_err_town_already_belong_nation", town.getName(), town.getNationOrNull().getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_err_already_belong_nation"));
			return;
		}

		if (!town.hasEnoughResidentsToJoinANation()) {
			// Town has dropped below min.-residents-to-join-nation limit. 
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_err_not_enough_residents_join_nation", town.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_err_not_enough_residents_join_nation", town.getName()));
			return;
		}

		if (nation.hasSanctionedTown(town)) {
			// Nation has sanctioned this town, since inviting them.
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_err_cannot_add_sanctioned_town", town.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_err_cannot_join_nation_sanctioned_town", nation.getName()));
			return;
		}

		if (nation.hasReachedMaxTowns()) {
			// Nation has hit the max-towns limit.
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_err_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_err_cannot_join_nation_over_town_limit", TownySettings.getMaxTownsPerNation()));
			return;
		}

		if (!nation.canAddResidents(town.getNumResidents())) {
			// Nation has hit the max-residents limit.
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_err_cannot_add_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation(), town.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_err_cannot_join_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation()));
			return;
		}

		if (TownySettings.getNationProximityToCapital() > 0) {
			try {
				ProximityUtil.testTownProximityToNation(town, nation);
			} catch (TownyException e) {
				TownyMessaging.sendPrefixedNationMessage(nation, e.getMessage());
				TownyMessaging.sendPrefixedTownMessage(town, e.getMessage());
				return;
			}
		}

		try {
			town.setNation(nation);
		} catch (AlreadyRegisteredException ignored) {}
		town.save();
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_join_nation", StringMgmt.remUnderscore(town.getName())));

		// Reset the town's player cache to account for potential new plot permissions.
		TownyAPI.getInstance().getOnlinePlayers(town).forEach(p-> plugin.resetCache(p));
	}

	private static void nationInviteTown(Player player, Nation nation, Town town) throws TownyException {

		TownJoinNationInvite invite = new TownJoinNationInvite(player, town, nation);
		
		BukkitTools.ifCancelledThenThrow(new NationPreInviteTownEvent(invite));

		try {
			if (!InviteHandler.inviteIsActive(invite)) { 
				town.newReceivedInvite(invite);
				nation.newSentInvite(invite);
				InviteHandler.addInvite(invite); 
				Player mayor = TownyAPI.getInstance().getPlayer(town.getMayor());
				if (mayor != null)
					TownyMessaging.sendRequestMessage(mayor,invite);
				BukkitTools.fireEvent(new NationInviteTownEvent(invite));
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
				if (BukkitTools.isEventCancelled(event)) {
					TownyMessaging.sendErrorMsg(sender, event.getCancelMessage());
					remove.add(town);
					continue;
				}
				
				// Actually remove the nation off the Town.
				town.removeNation();
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_nation_kicked_by", sender.getName()));
				TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_you_kicked", town.getName()));
			}

		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_kicked", sender.getName(), StringMgmt.join(kicking, ", ")));
			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_invalid_name"));
	}

	public static void nationSanctionTown(CommandSender sender, Nation nation, String[] args) throws TownyException {
		if (args.length == 0 || args[0].equals("?")) {
			HelpMenu.NATION_SANCTIONTOWN.send(sender);
			return;
		}

		if (nation == null && sender instanceof Player player)
			nation = getNationFromPlayerOrThrow(player);

		if (nation == null)
			throw new TownyException(Translatable.of("msg_err_no_nation_cannot_do"));

		if (args[0].toLowerCase(Locale.ROOT).equals("list")) {
			if (args.length == 2)
				nation = getNationOrThrow(args[1]);
			nationSanctionTownList(sender, nation);
			return;
		}

		if (args.length != 2) {
			HelpMenu.NATION_SANCTIONTOWN.send(sender);
			return;
		}
		checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SANCTIONTOWN.getNode());
		Town town = getTownOrThrow(args[1]);
		switch(args[0].toLowerCase(Locale.ROOT)) {
		case "add" -> nationSanctionTownAdd(sender, nation, town);
		case "remove" -> nationSactionTownRemove(sender, nation, town);
		default -> HelpMenu.NATION_SANCTIONTOWN.send(sender);
		}
	}

	private static void nationSanctionTownList(CommandSender sender, Nation nation) {
		if (nation.getSanctionedTowns().isEmpty()) {
			TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_has_no_sanctioned_towns"));
			return;
		}
		Translator translator = Translator.locale(sender);
		TownyMessaging.sendMessage(sender, ChatTools.formatTitle(nation.getName() + " " + translator.of("title_nation_sanctioned_towns")));
		TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(translator.of("title_nation_sanctioned_towns"), new ArrayList<>(nation.getSanctionedTowns())));
	}

	private static void nationSanctionTownAdd(CommandSender sender, Nation nation, Town town) throws TownyException {
		if (nation.hasTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_cannot_sanction_own_town"));

		if (nation.hasSanctionedTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_town_already_sanctioned"));

		BukkitTools.ifCancelledThenThrow(new NationSanctionTownAddEvent(nation, town));

		nation.addSanctionedTown(town);
		nation.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_town_sanctioned", town.getName()));
	}

	private static void nationSactionTownRemove(CommandSender sender, Nation nation, Town town) throws TownyException {
		if (!nation.hasSanctionedTown(town))
			throw new TownyException(Translatable.of("msg_err_nation_town_isnt_sanctioned"));

		BukkitTools.ifCancelledThenThrow(new NationSanctionTownRemoveEvent(nation, town));

		nation.removeSanctionedTown(town);
		nation.save();
		TownyMessaging.sendMsg(sender, Translatable.of("msg_err_nation_town_unsanctioned", town.getName()));
	}

	private void nationAlly(Player player, String[] split) throws TownyException {
		if (split.length == 0) {
			HelpMenu.ALLIES_STRING.send(player);
			return;
		}
		Resident resident = getResidentOrThrow(player);
		Nation nation = getNationFromResidentOrThrow(resident);

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "add":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ADD.getNode());
			nationAllyAdd(player, resident, nation, StringMgmt.remFirstArg(split));
			break;
		case "remove":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_REMOVE.getNode());
			nationAllyRemove(player, resident, nation, StringMgmt.remFirstArg(split));
			break;
		case "sent":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_SENT.getNode());
			nationAllySent(player, nation, StringMgmt.remFirstArg(split));
			break;
		case "received":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_RECEIVED.getNode());
			nationAllyReceived(player, nation, StringMgmt.remFirstArg(split));
			break;
		case "accept":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode());
			nationAllyAccept(player, nation, split);
			break;
		case "deny":
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_DENY.getNode());
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
			
			// Check if either nation has reached the max amount of allies
			if (nation.hasReachedMaximumAllies()) {
				toAccept.getReceiver().deleteReceivedInvite(toAccept);
				toAccept.getSender().deleteSentInvite(toAccept);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_ally_limit_reached"));
				return;
			}
			if (sendernation.hasReachedMaximumAllies()) {
				toAccept.getReceiver().deleteReceivedInvite(toAccept);
				toAccept.getSender().deleteSentInvite(toAccept);
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_ally_limit_reached_cannot_accept", sendernation));
				return;
			}
			
			try {
				NationAcceptAllyRequestEvent acceptAllyRequestEvent = new NationAcceptAllyRequestEvent((Nation)toAccept.getSender(), (Nation) toAccept.getReceiver());
				if (BukkitTools.isEventCancelled(acceptAllyRequestEvent)) {
					toAccept.getReceiver().deleteReceivedInvite(toAccept);
					toAccept.getSender().deleteSentInvite(toAccept);
					TownyMessaging.sendErrorMsg(player, acceptAllyRequestEvent.getCancelMessage());
					return;
				}
				InviteHandler.acceptInvite(toAccept);
			} catch (InvalidObjectException e) {
				plugin.getLogger().log(Level.WARNING, "unknown exception occurred while accepting invite", e); // Shouldn't happen, however like i said a fallback
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
				if (BukkitTools.isEventCancelled(denyAllyRequestEvent)) {
					sendernation.deleteSentAllyInvite(toDecline);
					nation.deleteReceivedInvite(toDecline);
					TownyMessaging.sendErrorMsg(player, denyAllyRequestEvent.getCancelMessage());
					return;
				}
				InviteHandler.declineInvite(toDecline, false);
				TownyMessaging.sendMsg(player, Translatable.of("successful_deny_request"));
			} catch (InvalidObjectException e) {
				plugin.getLogger().log(Level.WARNING, "unknown exception occurred while declining invite", e); // Shouldn't happen, however like i said a fallback
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
							plugin.getLogger().log(Level.WARNING, "An exception occurred while revoking invites for nation " + invitedNation.getName(), e);
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
				}
	
			} else { // So we are removing an ally
				try {
					nationRemoveAlly(resident, nation, targetNation);
				} catch (TownyException e) {
					// One of the Allies was not removed because the NationRemoveAllyEvent was cancelled, continue;
					TownyMessaging.sendErrorMsg(resident, e.getMessage());
					remove.add(targetNation);
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
		if (nation.hasAlly(targetNation))
			throw new TownyException(Translatable.of("msg_already_ally", targetNation));
		if (nation.hasReachedMaximumAllies())
			throw new TownyException(Translatable.of("msg_err_ally_limit_reached_cannot_send", targetNation));
		if (targetNation.hasReachedMaximumAllies())
			throw new TownyException(Translatable.of("msg_err_ally_limit_reached_cannot_send_targetNation", targetNation, targetNation));
		if (!targetNation.hasEnemy(nation)) {
			BukkitTools.ifCancelledThenThrow(new NationPreAddAllyEvent(nation, targetNation));

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
				
				BukkitTools.fireEvent(new NationRequestAllyNationEvent(invite));
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
			targetNation.addAlly(nation);
			nation.addAlly(targetNation);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_allied_nations", resident, targetNation));
			TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_added_ally", nation));
		} else {
			throw new TownyException(Translatable.of("msg_unable_ally_npc", nation.getName()));
		}
	}

	private void nationRemoveAlly(Resident resident, Nation nation, Nation targetNation) throws TownyException {
		if (nation.hasAlly(targetNation)) {
			BukkitTools.ifCancelledThenThrow(new NationRemoveAllyEvent(nation, targetNation));
	
			nation.removeAlly(targetNation);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", targetNation));
			TownyMessaging.sendMsg(resident, Translatable.of("msg_ally_removed_successfully"));
	
			// Remove the reciprocal ally relationship
			if (targetNation.hasAlly(nation)) {
				BukkitTools.ifCancelledThenThrow(new NationRemoveAllyEvent(targetNation, nation));

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

		Resident resident = getResidentOrThrow(player);
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
		for (Nation targetNation : enemies) {
			if (add) {
				if (!nation.getEnemies().contains(targetNation)) {
					
					NationPreAddEnemyEvent npaee = new NationPreAddEnemyEvent(nation, targetNation);
					if (BukkitTools.isEventCancelled(npaee)) {
						TownyMessaging.sendErrorMsg(player, npaee.getCancelMessage());
						remove.add(targetNation);
						continue;
					}
	
					nation.addEnemy(targetNation);
					BukkitTools.fireEvent(new NationAddEnemyEvent(nation, targetNation));
	
					// Remove the targetNation from the nation ally list if present.
					if (nation.hasAlly(targetNation)) {
						nation.removeAlly(targetNation);
						BukkitTools.fireEvent(new NationRemoveAllyEvent(nation, targetNation));
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_removed_ally", targetNation));
						TownyMessaging.sendMsg(player, Translatable.of("msg_ally_removed_successfully"));
					}
					
					// Remove the nation from the targetNation ally list if present.
					if (targetNation.hasAlly(nation)) {
						targetNation.removeAlly(nation);
						BukkitTools.fireEvent(new NationRemoveAllyEvent(targetNation, nation));
						TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_removed_ally", nation));
						TownyMessaging.sendMsg(player, Translatable.of("msg_ally_removed_successfully"));
					}
	
					TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_added_enemy", nation));
				} else {
					// TargetNation is already an enemy.
					remove.add(targetNation);
				}
			}

			if (!add) {
				if (nation.getEnemies().contains(targetNation)) {
	
					NationPreRemoveEnemyEvent npree = new NationPreRemoveEnemyEvent(nation, targetNation);
					if (BukkitTools.isEventCancelled(npree)) {
						TownyMessaging.sendErrorMsg(player, npree.getCancelMessage());
						remove.add(targetNation);
						continue;
					}
	
					nation.removeEnemy(targetNation);
					BukkitTools.fireEvent(new NationRemoveEnemyEvent(nation, targetNation));
					TownyMessaging.sendPrefixedNationMessage(targetNation, Translatable.of("msg_removed_enemy", nation));
				} else {
					// TargetNation is already not an enemy.
					remove.add(targetNation);
				}
			}
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

		Resident resident;
		try {
			if (!admin && sender instanceof Player player) {
				resident = getResidentOrThrow(player);
				nation = getNationFromResidentOrThrow(resident);
			} else // treat resident as king for testing purposes.
				resident = nation.getKing();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(sender, x.getMessage(sender));
			return;
		}
		
		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "leader":
		case "king":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_KING.getNode());
			nationSetKing(sender, nation, split, admin);
			break;
		case "capital":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITAL.getNode());
			nationSetCapital(sender, nation, split, admin);
			break;
		case "spawn":
			final Player player = catchConsole(sender);
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode());
			parseNationSetSpawnCommand(player, nation, admin);
			break;
		case "taxes":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXES.getNode());
			nationSetTaxes(sender, nation, split, admin);
			break;
		case "taxpercentcap":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXPERCENTCAP.getNode());
			nationSetTaxPercentCap(sender, split, nation);
			break;
		case "conqueredtax":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_CONQUEREDTAX.getNode());
			nationSetConqueredTax(sender, split, nation);
			break;
		case "spawncost":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWNCOST.getNode());
			nationSetSpawnCost(sender, nation, split, admin);
			break;
		case "name":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode());
			nationSetName(sender, nation, split, admin);
			break;
		case "tag":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAG.getNode());
			nationSetTag(sender, nation, split, admin);
			break;
		case "title":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode());
			nationSetTitle(sender, nation, resident, split, admin);
			break;
		case "surname":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode());
			nationSetSurname(sender, nation, resident, split, admin);
			break;
		case "board":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode());
			nationSetBoard(sender, nation, split);
			break;
		case "mapcolor":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_MAPCOLOR.getNode());
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

	private static void nationSetMapColor(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /nation set mapcolor brown.");

		String color = StringMgmt.join(StringMgmt.remFirstArg(split), " ").toLowerCase(Locale.ROOT);

		if (!TownySettings.getNationColorsMap().containsKey(color))
			throw new TownyException(Translatable.of("msg_err_invalid_nation_map_color", TownySettings.getNationColorsMap().keySet().toString()));

		double cost = TownySettings.getNationSetMapColourCost();
		if (cost > 0)
			Confirmation
				.runOnAccept(() -> setNationMapColor(nation, color, admin, sender))
				.setTitle(Translatable.of("msg_confirm_purchase", prettyMoney(cost)))
				.setCost(new ConfirmationTransaction(() -> cost, nation, "Cost of setting nation map color."))
				.sendTo(sender);
		else 
			setNationMapColor(nation, color, admin, sender);
	}

	private static void setNationMapColor(Nation nation, String color, boolean admin, CommandSender sender) {
		nation.setMapColorHexCode(TownySettings.getNationColorsMap().get(color));
		nation.save();
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_map_color_changed", color));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_nation_map_color_changed", color));
	}

	private static void nationSetBoard(CommandSender sender, Nation nation, String[] split) {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set board " + Translatable.of("town_help_9").forLocale(sender));
			return;
		} else {
			String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

			if (!line.equals("none")) {
				if (!NameValidation.isValidBoardString(line)) {
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

	private static void nationSetSurname(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws TownyException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set surname bilbo the dwarf ");
		else
			resident = getResidentOrThrow(split[1]);

		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String surname = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(surname))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

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

	private static void nationSetTitle(CommandSender sender, Nation nation, Resident resident, String[] split, boolean admin) throws TownyException {
		// Give the resident a title
		if (split.length < 2)
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set title bilbo Jester ");
		else
			resident = getResidentOrThrow(split[1]);
		
		if (!nation.hasResident(resident)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_same_nation", resident.getName()));
			return;
		}

		String title = NameValidation.checkAndFilterTitlesSurnameOrThrow(StringMgmt.remArgs(split, 2));

		if (TownySettings.doesSenderRequirePermissionNodeToAddColourToTitleOrSurname() && Colors.containsColourCode(title))
			checkPermOrThrowWithMessage(sender, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE_COLOUR.getNode(),
					Translatable.of("msg_err_you_dont_have_permission_to_use_colours"));

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
		String name = sender instanceof Player ? sender.getName() : "Console"; 
		
		if (split.length < 2)
			throw new TownyException("Eg: /nation set tag PLT");
		else if (split[1].equalsIgnoreCase("clear")) {
			nation.setTag(" ");
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_reset_nation_tag", name));
		} else {
			String tag = NameValidation.checkAndFilterTagOrThrow(split[1]);
			nation.setTag(tag);
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

			name = NameValidation.checkAndFilterGovernmentNameOrThrow(name, nation);
			if (TownyUniverse.getInstance().hasNation(name))
				throw new TownyException(Translatable.of("msg_err_name_validation_name_already_in_use", name));


			if (TownySettings.getTownAutomaticCapitalisationEnabled())
				name = StringMgmt.capitalizeStrings(name);
			
			if(TownyEconomyHandler.isActive() && TownySettings.getNationRenameCost() > 0) {
				if (!nation.getAccount().canPayFromHoldings(TownySettings.getNationRenameCost()))
					throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));

				final Nation finalNation = nation;
				final String finalName = name;
				Confirmation.runOnAccept(() -> nationRename((Player) sender, finalNation, finalName))
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
			String name = sender instanceof Player ? sender.getName() : "Console"; 
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
			if (admin)
				TownyMessaging.sendMsg(sender, Translatable.of("msg_spawn_cost_set_to", name, Translatable.of("nation_sing"), split[1]));
		}
	}

	private static void nationSetTaxes(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2)
			throw new TownyException("Eg: /nation set taxes 70");
		Double amount = MathUtil.getDoubleOrThrow(split[1]);
		if (amount < 0 && !TownySettings.isNegativeNationTaxAllowed())
			throw new TownyException(Translatable.of("msg_err_negative_money"));
		if (nation.isTaxPercentage() && (amount > 100 || amount < 0.0))
			throw new TownyException(Translatable.of("msg_err_not_percentage"));
		if (!TownySettings.isNegativeNationTaxAllowed() && TownySettings.getNationDefaultTaxMinimumTax() > amount)
			throw new TownyException(Translatable.of("msg_err_tax_minimum_not_met", TownySettings.getNationDefaultTaxMinimumTax()));
		nation.setTaxes(amount);
		if (admin) 
			TownyMessaging.sendMsg(sender, Translatable.of("msg_town_set_nation_tax", sender.getName(), nation.getTaxes()));
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_set_nation_tax", sender.getName(), nation.getTaxes()));
	}

	public static void nationSetTaxPercentCap(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (!nation.isTaxPercentage())
			throw new TownyException(Translatable.of("msg_max_tax_amount_only_for_percent"));

		if (split.length < 2) 
			throw new TownyException("Eg. /nation set taxpercentcap 10000");

		nation.setMaxPercentTaxAmount(MathUtil.getPositiveIntOrThrow(split[1]));

		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_set_tax_max_percent_amount", sender.getName(), TownyEconomyHandler.getFormattedBalance(nation.getMaxPercentTaxAmount())));
	}

	public static void nationSetConqueredTax(CommandSender sender, String[] split, Nation nation) throws TownyException {
		if (split.length < 2) 
			throw new TownyException("Eg. /nation set conqueredtax 10000");

		double input = MathUtil.getPositiveIntOrThrow(split[1]);
		double max = TownySettings.getMaxNationConqueredTaxAmount();
		if (input > max)
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_cannot_set_nation_conquere_tax_amount_higher_than", TownyEconomyHandler.getFormattedBalance(max)));

		nation.setConqueredTax(Math.min(input, max));

		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_conquered_tax__amount_set", sender.getName(), TownyEconomyHandler.getFormattedBalance(nation.getConqueredTax())));
	}

	private static void nationSetCapital(CommandSender sender, Nation nation, String[] split, boolean admin) throws TownyException {
		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(sender, "Eg: /nation set capital {town name}");
			return;
		}

		final Town newCapital = getTownOrThrow(split[1]);
		Nation newCapitalNation = getNationFromTownOrThrow(newCapital);
		if (!nation.equals(newCapitalNation))
			throw new TownyException(Translatable.of("msg_err_not_same_nation", nation));
		changeNationOwnership(sender, nation, newCapital, admin);
	}
	
	private static void changeNationOwnership(CommandSender sender, final Nation nation, Town newCapital, boolean admin) {
		final Town existingCapital = nation.getCapital();
		if (existingCapital != null && existingCapital.getUUID().equals(newCapital.getUUID())) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_warn_town_already_capital", newCapital.getName()));
			return;
		}

		boolean capitalNotEnoughResidents = !newCapital.hasEnoughResidentsToBeANationCapital();
		if (capitalNotEnoughResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_not_enough_residents_capital", newCapital.getName()));
			return;
		}
		
		boolean capitalTooManyResidents = !existingCapital.isAllowedThisAmountOfResidents(existingCapital.getNumResidents(), false); 
		if (capitalTooManyResidents && !admin) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_nation_capital_too_many_residents", newCapital.getName()));
			return;
		}
		
		Runnable processCommand = () -> {
			Resident oldKing = nation.getKing();
			Resident newKing = newCapital.getMayor();

			NationKingChangeEvent nationKingChangeEvent = new NationKingChangeEvent(oldKing, newKing);

			// Do proximity tests.
			if (TownySettings.getNationProximityToCapital() > 0 ) {
				List<Town> removedTowns = ProximityUtil.gatherOutOfRangeTowns(nation, newCapital);

				// There are going to be some towns removed from the nation, so we'll do a Confirmation.
				if (!removedTowns.isEmpty()) {
					Confirmation.runOnAccept(() -> {
						if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin) {
							TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
							return;
						}
						
						nation.setCapital(newCapital);
						ProximityUtil.removeOutOfRangeTowns(nation);
						plugin.resetCache();
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
						
						if (admin)
							TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));
						
						nation.save();
					})
					.setTitle(Translatable.of("msg_warn_the_following_towns_will_be_removed_from_your_nation", StringMgmt.join(removedTowns, ", ")))
					.sendTo(sender);
					
					return;
				}
			}
			
			// Proximity doesn't factor in or no towns would be considered out of range after changing the capital.
			// Send a confirmation
			Confirmation.runOnAccept(() -> {
				if (BukkitTools.isEventCancelled(nationKingChangeEvent) && !admin) {
					TownyMessaging.sendErrorMsg(sender, nationKingChangeEvent.getCancelMessage());
					return;
				}
				
				nation.setCapital(newCapital);
				plugin.resetCache();
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));

				if (admin)
					TownyMessaging.sendMsg(sender, Translatable.of("msg_new_king", newCapital.getMayor().getName(), nation.getName()));

				nation.save();
			})
			.setTitle(Translatable.of("msg_warn_are_you_sure_you_want_to_transfer_nation_ownership", newCapital.getMayor().getName()))
			.sendTo(sender);
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
				final Resident newKing = getResidentOrThrow(split[1]);
				if (!nation.hasResident(newKing))
					throw new TownyException(Translatable.of("msg_err_king_not_in_nation"));
				
				if (!newKing.isMayor())
					throw new TownyException(Translatable.of("msg_err_new_king_notmayor"));
				
				changeNationOwnership(sender, nation, getResidentOrThrow(split[1]).getTown(), admin);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
	}

	private static void parseNationSetSpawnCommand(Player player, Nation nation, boolean admin) throws TownyException {
		if (TownyAPI.getInstance().isWilderness(player.getLocation()))
			throw new TownyException(Translatable.of("msg_cache_block_error_wild", "set spawn"));

		NationSetSpawnEvent event = new NationSetSpawnEvent(nation, player, player.getLocation());
		if (BukkitTools.isEventCancelled(event) && !admin)
			throw new TownyException(event.getCancelMessage());

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
		plugin.getScheduler().runAsync(() -> {
			StringBuilder sb = new StringBuilder();
			List<Resident> residents = new ArrayList<>(nation.getResidents());
			residents.sort(Comparator.<Resident>comparingDouble(res -> res.getAccount().getCachedBalance()).reversed());

			int i = 0;
			for (Resident res : residents)
				sb.append(Translatable.of("msg_baltop_book_format", ++i, res.getName(), TownyEconomyHandler.getFormattedBalance(res.getAccount().getCachedBalance())).forLocale(player) + "\n");

			plugin.getScheduler().run(player, () -> player.openBook(BookFactory.makeBook("Nation Baltop", nation.getName(), sb.toString())));
		});
	}

	public static void nationToggle(CommandSender sender, String[] split, boolean admin, Nation nation) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.NATION_TOGGLE_HELP.send(sender);
			return;
		}
		Resident resident;

		if (!admin) {
			resident = getResidentOrThrow(((Player) sender));
			nation = getNationFromResidentOrThrow(resident);
		} else // Treat any resident tests as though the king were doing it.
			resident = nation.getKing();

		Optional<Boolean> choice = Optional.empty();
		if (split.length == 2)
			choice = BaseCommand.parseToggleChoice(split[1]);

		switch (split[0].toLowerCase(Locale.ROOT)) {
		case "peaceful":
		case "neutral":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL.getNode());
			nationTogglePeaceful(sender, nation, choice, admin);
			break;
		case "public":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_PUBLIC.getNode());
			nationTogglePublic(sender, nation, choice, admin);
			break;
		case "open":
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_OPEN.getNode());
			nationToggleOpen(sender, nation, choice, admin);
			break;
        case "taxpercent":
            checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_TAXPERCENT.getNode());
            nationToggleTaxPercent(sender, nation, choice, admin);
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
		double cost = TownySettings.getNationNeutralityCost(nation);

		if (nation.isNeutral() && peacefulState)
			throw new TownyException(Translatable.of("msg_nation_already_peaceful"));
		else if (!nation.isNeutral() && !peacefulState)
			throw new TownyException(Translatable.of("msg_nation_already_not_peaceful"));

		if (peacefulState && TownyEconomyHandler.isActive() && !nation.getAccount().canPayFromHoldings(cost))
			throw new TownyException(Translatable.of("msg_nation_cant_peaceful", TownyEconomyHandler.getFormattedBalance(cost)));
		
		String uuid = nation.getUUID().toString();
		
		if (TownySettings.getPeacefulCoolDownTime() > 0 && 
			!admin && 
			CooldownTimerTask.hasCooldown(uuid, CooldownType.NEUTRALITY) && 
			!TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(sender))
			throw new TownyException(Translatable.of("msg_err_cannot_toggle_neutral_x_seconds_remaining",
					CooldownTimerTask.getCooldownRemaining(uuid, CooldownType.NEUTRALITY)));

		// Fire cancellable event directly before setting the toggle.
		NationToggleNeutralEvent preEvent = new NationToggleNeutralEvent(sender, nation, admin, peacefulState);
		if (BukkitTools.isEventCancelled(preEvent))
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

		// Reassign permissions because neutrality can add/remove nodes.
		if (TownyPerms.hasPeacefulNodes())
			TownyPerms.updateNationPerms(nation);
	}

	private static void nationTogglePublic(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationTogglePublicEvent preEvent = new NationTogglePublicEvent(sender, nation, admin, choice.orElse(!nation.isPublic()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setPublic(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_public", nation.isPublic() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

	private static void nationToggleOpen(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
		// Fire cancellable event directly before setting the toggle.
		NationToggleOpenEvent preEvent = new NationToggleOpenEvent(sender, nation, admin, choice.orElse(!nation.isOpen()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());

		// Set the toggle setting.
		nation.setOpen(preEvent.getFutureState());

		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_changed_open", nation.isOpen() ? Translatable.of("enabled") : Translatable.of("disabled")));
	}

    private static void nationToggleTaxPercent(CommandSender sender, Nation nation, Optional<Boolean> choice, boolean admin) throws TownyException {
        	// Fire cancellable event directly before setting the toggle.
		NationToggleTaxPercentEvent preEvent = new NationToggleTaxPercentEvent(sender, nation, admin, choice.orElse(!nation.isTaxPercentage()));
		if (BukkitTools.isEventCancelled(preEvent))
			throw new TownyException(preEvent.getCancelMessage());
		// Set the toggle setting.
		nation.setTaxPercentage(preEvent.getFutureState());
		
		// Send message feedback.
		TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_changed_taxpercent", nation.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
		if (admin)
			TownyMessaging.sendMsg(sender, Translatable.of("msg_changed_taxpercent", nation.isTaxPercentage() ? Translatable.of("enabled") : Translatable.of("disabled")));
    }

	public static void nationRename(Player player, Nation nation, String newName) {
		try {
			BukkitTools.ifCancelledThenThrow(new NationPreRenameEvent(nation, newName));
	
			double renameCost = TownySettings.getNationRenameCost();
			if (TownyEconomyHandler.isActive() && renameCost > 0 && !nation.getAccount().withdraw(renameCost, String.format("Nation renamed to: %s", newName)))
				throw new TownyException(Translatable.of("msg_err_no_money", TownyEconomyHandler.getFormattedBalance(renameCost)));
	
			TownyUniverse.getInstance().getDataSource().renameNation(nation, newName);
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_set_name", player.getName(), nation.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
	}

	/**
	 * Performs final checks before sending to SpawnUtil.
	 *
	 * @param player        Player spawning.
	 * @param split         Current command arguments.
	 * @param ignoreWarning Whether to ignore the cost
	 * @throws TownyException Exception thrown to deliver feedback message denying
	 *                        spawn.
	 */
	public static void nationSpawn(Player player, String[] split, boolean ignoreWarning) throws TownyException {

		Nation nation = getPlayerNationOrNationFromArg(player, split);
		if (TownySettings.isConqueredTownsDeniedNationSpawn()) {
			Town town = TownyAPI.getInstance().getTown(player);
			if (town != null && nation.hasTown(town) && town.isConquered())
				throw new TownyException(Translatable.of("nation_spawn_not_allowed_for_conquered_towns"));
		}

		String notAffordMSG = split.length == 0 ? 
			Translatable.of("msg_err_cant_afford_tp").forLocale(player) : 
			Translatable.of("msg_err_cant_afford_tp_nation", nation.getName()).forLocale(player);
		SpawnUtil.sendToTownySpawn(player, split, nation, notAffordMSG, false, ignoreWarning, SpawnType.NATION);
	}

	private static void nationTransaction(Player player, String[] args, boolean withdraw) {
		try {
			Resident resident = getResidentOrThrow(player);
			Nation nation = getNationFromResidentOrThrow(resident);

			if (args.length < 1 || args.length > 2)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation" + (withdraw ? " withdraw" : " deposit")));

			int amount;
			if ("all".equalsIgnoreCase(args[0].trim()))
				amount = (int) Math.floor(withdraw ? nation.getAccount().getHoldingBalance() : resident.getAccount().getHoldingBalance());
			else 
				amount = MathUtil.getIntOrThrow(args[0].trim());

			// Stop 0 amounts being supplied.
			if (amount == 0)
				throw new TownyException(Translatable.of("msg_err_amount_must_be_greater_than_zero"));

			if (args.length == 1) {
				if (withdraw)
					MoneyUtil.nationWithdraw(player, resident, nation, amount);
				else 
					MoneyUtil.nationDeposit(player, resident, nation, amount);
				return;
			}
			
			if (withdraw)
				throw new TownyException(Translatable.of("msg_must_specify_amnt", "/nation withdraw"));

			// Check depositing into another town
			checkPermOrThrow(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT_OTHER.getNode());

			Town town = getTownOrThrow(args[1]);
			if (!nation.hasTown(town))
				throw new TownyException(Translatable.of("msg_err_not_same_nation", town.getName()));

			MoneyUtil.townDeposit(player, resident, town, nation, amount);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}
    }
    
	private void nationStatusScreen(CommandSender sender, Nation nation) {
		/*
		 * This is run async because it will ping the economy plugin for the nation bank value.
		 */
		TownyEconomyHandler.economyExecutor().execute(() -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(nation, sender)));
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
