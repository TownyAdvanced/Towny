package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

public class AllianceCommand extends BaseCommand implements CommandExecutor {
	private static Towny plugin;
	private CommandSender sender;
	private Player player;
	private Translator translator;


	public AllianceCommand(Towny instance) {
		plugin = instance;
	}
	
	private static final List<String> allianceTabCompletes = Arrays.asList(
		"new",
		"add",
		"invite",
		"kick",
		"remove",
		"enemy",
		"enemylist",
		"memberlist",
		"list",
		"online",
		"delete"
	);
	
	private static final List<String> allianceEnemyTabCompletes = Arrays.asList(
		"add",
		"remove"
	);
	
	private static final List<String> allianceConsoleTabCompletes = Arrays.asList(
		"?",
		"help",
		"list"
	);
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		if (sender instanceof Player player) {
			switch (args[0].toLowerCase()) {
			case "new":
			case "list":
			case "delete":
				return Collections.emptyList();
			case "add":
			case "invite":
				return getTownyStartingWith(args[args.length - 1], "n");
			case "kick":
			case "remove":
				Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
				if (res != null && res.hasAlliance())
					return NameUtil.filterByStart(NameUtil.getNames(res.getAllianceOrNull().getMembers()), args[args.length - 1]);
				else 
					return Collections.emptyList();
			case "enemy":
				if (args.length == 2) {
					return NameUtil.filterByStart(allianceEnemyTabCompletes, args[1]);
				} else if (args.length >= 3){
					switch (args[1].toLowerCase()) {
						case "add":
							return getTownyStartingWith(args[2], "a");
						case "remove":
							// Return enemies of alliance
							try {
								return NameUtil.filterByStart(NameUtil.getNames(getAllianceFromPlayerOrThrow(player).getEnemies()), args[2]);
							} catch (TownyException ignored) {}
						default:
							return Collections.emptyList();
					}
				}
				break;
			case "enemylist":
			case "memberlist":
			case "online":
				return getTownyStartingWith(args[args.length - 1], "a");
			default:
				if (args.length == 1)
					return filterByStartOrGetTownyStartingWith(TownyCommandAddonAPI.getTabCompletes(CommandType.ALLIANCE, allianceTabCompletes), args[0], "a");
				else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.ALLIANCE, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.ALLIANCE, args[0]).getTabCompletion(sender, args), args[args.length-1]);
			}
		} else if (args.length == 1) {
			return filterByStartOrGetTownyStartingWith(allianceConsoleTabCompletes, args[0], "a");
		}
		return Collections.emptyList();
	}

		
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		this.sender = sender;
		this.translator = Translator.locale(Translation.getLocale(sender));
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(player, "Locked in Safe mode!");
				return false;
			}
			this.player = player;
			try {
				parseAllianceCommand(player, args);
			} catch (TownyException te) {
				TownyMessaging.sendErrorMsg(player, te.getMessage(player));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
			}

		} else
			parseAllianceCommandForConsole(sender, args);

		return true;
	}
	private void parseAllianceCommandForConsole(CommandSender sender, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

//			HelpMenu.ALLIANCE_HELP_CONSOLE.send(sender);
		
		} else if (split[0].equalsIgnoreCase("list")) {

//			try {
////				listAlliances(sender, split);
//			} catch (TownyException e) {
//				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
//			}
			
		} else if (split[0].equalsIgnoreCase("memberlist")) {

			try {
				allianceMemberList(sender, split);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
			}
		} else if (TownyCommandAddonAPI.hasCommand(CommandType.ALLIANCE, split[0])) {
			TownyCommandAddonAPI.getAddonCommand(CommandType.ALLIANCE, split[0]).execute(sender, "alliance", split);
		} else {
			Alliance alliance = TownyUniverse.getInstance().getAlliance(split[0]);
			
			if (alliance != null)
				allianceStatusScreen(sender, alliance);
			else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", split[0]));
		}
		
	}

	private void parseAllianceCommand(Player player, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0) {

			Alliance alliance = getAllianceFromPlayerOrThrow(player);
			allianceStatusScreen(player, alliance);
		} else if (split[0].equalsIgnoreCase("delete")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_DELETE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));

			allianceDelete(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("online")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_ONLINE.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
	
			allianceOnline(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("memberlist")) {

			if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_ALLIANCE_MEMBERLIST.getNode()))
				throw new TownyException(Translatable.of("msg_err_command_disable"));
	
			allianceMemberList(player, split);
		} else if (split[0].equalsIgnoreCase("enemylist")) {
			
			allianceEnemyList(player, split);
		}
		
	}

	private void allianceDelete(Player player, String[] split) throws TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (split.length == 0) {
			Alliance alliance = getAllianceFromPlayerOrThrow(player);
			Confirmation.runOnAccept(() -> TownyUniverse.getInstance().getDataSource().removeAlliance(alliance)).sendTo(player);
		} else {
			if (!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_ALLIANCE_DELETE.getNode()))
				throw new TownyException(Translatable.of("msg_err_admin_only_delete_alliance"));

			Alliance alliance = getAllianceOrThrow(split[0]);
			Confirmation.runOnAccept(() -> {
				TownyMessaging.sendMsg(player, translator.of("alliance_deleted_by_admin", alliance.getName()));
				TownyUniverse.getInstance().getDataSource().removeAlliance(alliance);
			}).sendTo(player);

		}
	}


	private void allianceOnline(Player player, String[] split) throws TownyException {
		if (split.length > 0) {
			Alliance alliance = getAllianceOrThrow(split[0]);
			
			List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, alliance);
			if (onlineResidents.size() > 0) {
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_alliance_online"), alliance, player));
			} else {
				TownyMessaging.sendMessage(player, Colors.White + "0 " + translator.of("res_list") + " " + (translator.of("msg_alliance_online") + ": " + alliance));
			}
		} else {
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(translator.of("msg_alliance_online"), getAllianceFromPlayerOrThrow(player), player));
		}
	}


	private void allianceEnemyList(Player player, String[] split) throws TownyException {
		Alliance alliance = null;
		try {
			if (split.length == 1) {
				alliance = getAllianceFromPlayerOrThrow(player);
			} else {
				alliance = getAllianceOrThrow(split[1]);
			}
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, translator.of("msg_specify_name"));
			return;
		}
		if (alliance.getEnemies().isEmpty())
			TownyMessaging.sendErrorMsg(player, translator.of("msg_error_alliance_has_no_enemies"));
		else {
			TownyMessaging.sendMessage(player, ChatTools.formatTitle(alliance.getName() + " " + translator.of("status_nation_enemies")));
			TownyMessaging.sendMessage(player, TownyFormatter.getFormattedAlliances(translator.of("status_nation_enemies"), new ArrayList<>(alliance.getEnemies())));
		}
	}

	private void allianceMemberList(CommandSender sender, String[] args) throws TownyException {

		Player player = null;
		if (sender instanceof Player)
			player = (Player) sender;

		Alliance alliance = null;
		if (args.length == 1 && player != null) {
			alliance = getAllianceFromPlayerOrThrow(player);
		} else if (args.length == 2){
			alliance = TownyUniverse.getInstance().getAlliance(args[1]);
		}
		
		if (alliance != null) {
			TownyMessaging.sendMessage(sender, ChatTools.formatTitle(alliance.getName() + " " + translator.of("nation_plu")));
			TownyMessaging.sendMessage(sender, TownyFormatter.getFormattedTownyObjects(translator.of("nation_plu"), new ArrayList<>(alliance.getMembers())));
		} else 
			TownyMessaging.sendErrorMsg(sender, translator.of("msg_specify_name"));
	}

	private void allianceStatusScreen(CommandSender sender, Alliance alliance) {
		/*
		 * This is run async because if banks are added to alliance, this will ping the economy plugin.
		 */
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(alliance, sender)));
	}

}
