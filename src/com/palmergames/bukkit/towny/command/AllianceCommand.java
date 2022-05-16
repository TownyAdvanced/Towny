package com.palmergames.bukkit.towny.command;

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
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.utils.NameUtil;

public class AllianceCommand extends BaseCommand implements CommandExecutor {
	private static Towny plugin;
	private CommandSender sender;
	private Player player;

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
	private void parseAllianceCommandForConsole(CommandSender sender, String[] args) {
		// TODO Auto-generated method stub
		
	}
	private void parseAllianceCommand(Player player, String[] split) throws TownyException {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		
		if (split.length == 0) {

			Alliance alliance = getAllianceFromPlayerOrThrow(player);
			allianceStatusScreen(player, alliance);
		}
		
	}


	private void allianceStatusScreen(Player player2, Alliance alliance) {
		/*
		 * This is run async because if banks are added to alliance, this will ping the economy plugin.
		 */
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> TownyMessaging.sendStatusScreen(sender, TownyFormatter.getStatus(alliance, sender)));
	}

}
