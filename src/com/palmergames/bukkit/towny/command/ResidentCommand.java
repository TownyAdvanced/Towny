package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all towny resident help commands to player Command: /resident
 */

public class ResidentCommand implements TabExecutor {

	private static Towny plugin;
	private static final List<String> output = new ArrayList<String>();
 	private static final List<String> subCommands = new ArrayList<String>();
 	private static final List<String> setVars = new ArrayList<String>();
 	public static final List<String> toggles = new ArrayList<String>();
 	public static final List<String> modes = new ArrayList<String>();
 	public static final List<String> addRemoves = new ArrayList<String>();

	static {
		output.add(ChatTools.formatTitle("/resident"));
		output.add(ChatTools.formatCommand("", "/resident", "", TownySettings.getLangString("res_1")));
		output.add(ChatTools.formatCommand("", "/resident", TownySettings.getLangString("res_2"), TownySettings.getLangString("res_3")));
		output.add(ChatTools.formatCommand("", "/resident", "list", TownySettings.getLangString("res_4")));
		output.add(ChatTools.formatCommand("", "/resident", "tax", ""));
		output.add(ChatTools.formatCommand("", "/resident", "toggle", "[mode]...[mode]"));
		output.add(ChatTools.formatCommand("", "/resident", "set [] .. []", "'/resident set' " + TownySettings.getLangString("res_5")));
		output.add(ChatTools.formatCommand("", "/resident", "friend [add/remove] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_6")));
		output.add(ChatTools.formatCommand("", "/resident", "friend [add+/remove+] " + TownySettings.getLangString("res_2") + " ", TownySettings.getLangString("res_7")));
		//output.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/resident", "delete " + TownySettings.getLangString("res_2"), ""));
		subCommands.add("list");
		subCommands.add("tax");
		subCommands.add("toggle");
		subCommands.add("set");
		subCommands.add("friend");

		toggles.add("mobs");
		toggles.add("explosion");
		toggles.add("fire");
		toggles.add("pvp");
		
		setVars.add("perm");
		setVars.add("mode");
		
		modes.add("map");
		modes.add("townclaim");
		modes.add("townunclaim");
		modes.add("clear");
		modes.add("reset");
		modes.add("town");
		modes.add("nation");
		modes.add("general");
		
		addRemoves.add("add");
		addRemoves.add("remove");
		addRemoves.add("add+");
		addRemoves.add("remove+");
	}

	public ResidentCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			if (args == null) {
				for (String line : output)
					player.sendMessage(line);
				parseResidentCommand(player, args);
			} else {
				parseResidentCommand(player, args);
			}

		} else
			// Console
			for (String line : output)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	public void parseResidentCommand(Player player, String[] split) {

		try {

			if (split.length == 0) {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}

			} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

				for (String line : output)
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("list")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listResidents(player);

			} else if (split[0].equalsIgnoreCase("tax")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TAX.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					TownyMessaging.sendMessage(player, TownyFormatter.getTaxStatus(resident));
				} catch (NotRegisteredException x) {
					throw new TownyException(TownySettings.getLangString("msg_err_not_registered"));
				}

			} else if (split[0].equalsIgnoreCase("set")) {

				/*
				 * perms checked in method.
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentSet(player, newSplit);

			} else if (split[0].equalsIgnoreCase("toggle")) {

				/*
				 * 
				 */
				String[] newSplit = StringMgmt.remFirstArg(split);
				residentToggle(player, newSplit);

			} else if (split[0].equalsIgnoreCase("friend")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_FRIEND.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				String[] newSplit = StringMgmt.remFirstArg(split);
				residentFriend(player, newSplit);

			} else {

				try {
					Resident resident = TownyUniverse.getDataSource().getResident(split[0]);
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(resident, player));
				} catch (NotRegisteredException x) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	/**
	 * Toggle modes for this player.
	 * 
	 * @param player
	 * @param newSplit
	 * @throws TownyException
	 */
	private void residentToggle(Player player, String[] newSplit) throws TownyException {

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_TOGGLE.getNode(newSplit[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());

			TownyPermission perm = resident.getPermissions();

			if (newSplit[0].equalsIgnoreCase("pvp")) {
				perm.pvp = !perm.pvp;
			} else if (newSplit[0].equalsIgnoreCase("fire")) {
				perm.fire = !perm.fire;
			} else if (newSplit[0].equalsIgnoreCase("explosion")) {
				perm.explosion = !perm.explosion;
			} else if (newSplit[0].equalsIgnoreCase("mobs")) {
				perm.mobs = !perm.mobs;
			} else {

				resident.toggleMode(newSplit, true);
				return;
			}

			notifyPerms(player, perm);
			TownyUniverse.getDataSource().saveResident(resident);

		} catch (NotRegisteredException e) {
			// unknown resident
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered"), player.getName()));
		}

	}

	/**
	 * Show the player the new Permission settings after the toggle.
	 * 
	 * @param player
	 * @param perm
	 */
	private void notifyPerms(Player player, TownyPermission perm) {

		TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_perms"));
		TownyMessaging.sendMessage(player, Colors.Green + "PvP: " + ((perm.pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((perm.explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((perm.fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((perm.mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

	}

	public void listResidents(Player player) {

		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("res_list")));
		String colour;
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Resident resident : plugin.getTownyUniverse().getActiveResidents()) {
			if (resident.isKing())
				colour = Colors.Gold;
			else if (resident.isMayor())
				colour = Colors.LightBlue;
			else
				colour = Colors.White;
			formatedList.add(colour + resident.getName() + Colors.White);
		}
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}

	/**
	 * 
	 * Command: /resident set [] ... []
	 * 
	 * @param player
	 * @param split
	 * @throws TownyException
	 */

	/*
	 * perm [resident/outsider] [build/destroy] [on/off]
	 */

	public void residentSet(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "perm ...", "'/resident set perm' " + TownySettings.getLangString("res_5")));
			player.sendMessage(ChatTools.formatCommand("", "/resident set", "mode ...", "'/resident set mode' " + TownySettings.getLangString("res_5")));
		} else {
			Resident resident;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_RESIDENT_SET.getNode(split[0].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("perm")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				TownCommand.setTownBlockPermissions(player, resident, resident.getPermissions(), newSplit, true);
			} else if (split[0].equalsIgnoreCase("mode")) {
				String[] newSplit = StringMgmt.remFirstArg(split);
				setMode(player, newSplit);
			} else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "town"));
				return;
			}

			TownyUniverse.getDataSource().saveResident(resident);
		}
	}

	private void setMode(Player player, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "clear", ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident set mode", "[mode] ...[mode]", ""));
			player.sendMessage(ChatTools.formatCommand("Mode", "map", "", TownySettings.getLangString("mode_1")));
			player.sendMessage(ChatTools.formatCommand("Mode", "townclaim", "", TownySettings.getLangString("mode_2")));
			player.sendMessage(ChatTools.formatCommand("Mode", "townunclaim", "", TownySettings.getLangString("mode_3")));
			player.sendMessage(ChatTools.formatCommand("Mode", "tc", "", TownySettings.getLangString("mode_4")));
			player.sendMessage(ChatTools.formatCommand("Mode", "nc", "", TownySettings.getLangString("mode_5")));
			// String warFlagMaterial = (TownyWarConfig.getFlagBaseMaterial() ==
			// null ? "flag" :
			// TownyWarConfig.getFlagBaseMaterial().name().toLowerCase());
			// player.sendMessage(ChatTools.formatCommand("Mode", "warflag", "",
			// String.format(TownySettings.getLangString("mode_6"),
			// warFlagMaterial)));
			player.sendMessage(ChatTools.formatCommand("Eg", "/resident set mode", "map townclaim town nation general", ""));

			return;
		}

		if (split[0].equalsIgnoreCase("reset") || split[0].equalsIgnoreCase("clear")) {
			plugin.removePlayerMode(player);
			return;
		}

		List<String> list = Arrays.asList(split);
		if ((list.contains("spy")) && (plugin.isPermissions() && !TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_CHAT_SPY.getNode()))) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));
			return;
		}

		plugin.setPlayerMode(player, split, true);

	}

	public void residentFriend(Player player, String[] split) {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "add " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "remove " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/resident friend", "clear", ""));
		} else {
			Resident resident;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("add")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, TownyUniverse.getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("remove")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, TownyUniverse.getOnlineResidents(player, names));
			} else if (split[0].equalsIgnoreCase("add+")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendAdd(player, resident, getResidents(player, names));
			} else if (split[0].equalsIgnoreCase("remove+")) {
				String[] names = StringMgmt.remFirstArg(split);
				residentFriendRemove(player, resident, getResidents(player, names));
			} else if (split[0].equalsIgnoreCase("clearlist") || split[0].equalsIgnoreCase("clear")) {
				residentFriendRemove(player, resident, resident.getFriends());
			}

		}
	}

	private static List<Resident> getResidents(Player player, String[] names) {

		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names)
			try {
				Resident target = TownyUniverse.getDataSource().getResident(name);
				invited.add(target);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
			}
		return invited;
	}

	public void residentFriendAdd(Player player, Resident resident, List<Resident> invited) {

		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newFriend : invited)
			try {
				resident.addFriend(newFriend);
				plugin.deleteCache(newFriend.getName());
			} catch (AlreadyRegisteredException e) {
				remove.add(newFriend);
			}
		for (Resident newFriend : remove)
			invited.remove(newFriend);

		if (invited.size() > 0) {
			String msg = "Added ";
			for (Resident newFriend : invited) {
				msg += newFriend.getName() + ", ";
				Player p = plugin.getServer().getPlayer(newFriend.getName());
				if (p != null)
					TownyMessaging.sendMsg(p, String.format(TownySettings.getLangString("msg_friend_add"), player.getName()));
			}
			msg = msg.substring(0, msg.length() - 2);
			msg += TownySettings.getLangString("msg_to_list");
			TownyMessaging.sendMsg(player, msg);
			TownyUniverse.getDataSource().saveResident(resident);
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

	public void residentFriendRemove(Player player, Resident resident, List<Resident> kicking) {

		List<Resident> remove = new ArrayList<Resident>();
		List<Resident> toKick = new ArrayList<Resident>(kicking);

		for (Resident friend : toKick) {
			try {
				resident.removeFriend(friend);
				plugin.deleteCache(friend.getName());
			} catch (NotRegisteredException e) {
				remove.add(friend);
			}
		}
		// remove invalid names so we don't try to send them messages
		if (remove.size() > 0)
			for (Resident friend : remove)
				toKick.remove(friend);

		if (toKick.size() > 0) {
			String msg = TownySettings.getLangString("msg_removed");
			Player p;
			for (Resident member : toKick) {
				msg += member.getName() + ", ";
				p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					TownyMessaging.sendMsg(p, String.format(TownySettings.getLangString("msg_friend_remove"), player.getName()));
			}
			msg = msg.substring(0, msg.length() - 2);
			msg += TownySettings.getLangString("msg_from_list");
			;
			TownyMessaging.sendMsg(player, msg);
			TownyUniverse.getDataSource().saveResident(resident);
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

	}

	
	public List<String> residentListCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (Player p: Bukkit.matchPlayer(partial)) {
			matches.add(p.getName());
		}
		return matches;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command,
			String alias, String[] args) {
		if (args.length == 1) {
			List<String> matches = subCommandCompletion(args[0]);
			matches.addAll(residentListCompletion(args[0]));
			return matches;
		} else if (args.length == 2) {
			switch (args[0].toLowerCase()) {
			case "set":
				return setCompletion(args[1]);
			case "toggle":
				return toggleCompletion(args[1]);
			case "friend":
				return addRemoveCompletion(args[1]);
			default:
				return null;
			}
		} else if (args.length >= 3) {
			if (args[0].toLowerCase().equals("set")) {
				switch(args[1].toLowerCase()) {
				case "perm":
					return PermCompletion.handleSetPerm(args, "friend");
				case "mode":
					return modeCompletion(args[2]);
				default:
					return null;
				}
			} else if (args[0].toLowerCase().equals("friend")) {
				switch (args[1].toLowerCase()) {
				case "add":
					return ObjectCompletion.playerCompletion(args[2], true);
				case "add+":
					return ObjectCompletion.playerCompletion(args[2], false);
				case "remove":
				case "remove+":
					return ObjectCompletion.friendCompletion((Player)sender, args[2]);
				}
			}
		}
		return null;
	}

	public List<String> addRemoveCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String s: addRemoves) {
			if (s.startsWith(partial.toLowerCase())) {
				matches.add(s);
			}
		}
		return matches;
	}
	public List<String> subCommandCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String c: subCommands) {
			if (c.startsWith(partial.toLowerCase())) {
				matches.add(c);
			}
		}
		return matches;
	}
	

	public List<String> modeCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String m: modes) {
			if (m.startsWith(partial.toLowerCase())) {
				matches.add(m);
			}
		}
		return matches;
	}

	public List<String> toggleCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String t: toggles) {
			if (t.startsWith(partial.toLowerCase())) {
				matches.add(t);
			}
		}
		return matches;
	}
	
	public List<String> setCompletion(String partial) {
		List<String> matches = new ArrayList<String>();
		for (String s: setVars) {
			if (s.startsWith(partial.toLowerCase())) {
				matches.add(s);
			}
		}
		return matches;
	}
	
}
