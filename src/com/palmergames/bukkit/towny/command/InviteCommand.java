package com.palmergames.bukkit.towny.command;

import com.google.common.collect.ListMultimap;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.List;

public class InviteCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> invite_help = new ArrayList<String>();

	static {

		invite_help.add(ChatTools.formatTitle("/invite"));
		invite_help.add(ChatTools.formatCommand("", "/invite", TownySettings.getAcceptCommand() + " [town]", TownySettings.getLangString("invite_help_1")));
		invite_help.add(ChatTools.formatCommand("", "/invite", TownySettings.getDenyCommand() + " [town]", TownySettings.getLangString("invite_help_2")));
		invite_help.add(ChatTools.formatCommand("", "/invite", "list", TownySettings.getLangString("invite_help_3")));

	}

	public InviteCommand(Towny instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (command.getName().equalsIgnoreCase("invite")) {
				if (label.equalsIgnoreCase("invites") && args.length == 0) {
					parseInviteList(player);
				} else {
					parseInviteCommand(player, command, args);
				}
			}
		} else
			// Console
			for (String line : invite_help)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	private void parseInviteCommand(final Player player, Command cmd, String[] split) {
		if (split.length == 0) { // So they just type /invite , We should probably send them to the help menu. Done.
			for (String line : invite_help) {
				player.sendMessage(line);
			}
		} else if (split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("?")) {
			for (String line : invite_help) {
				player.sendMessage(line);
			}
		} else if (split[0].equalsIgnoreCase("list")) {
			parseInviteList(player);
		} else if (split[0].equalsIgnoreCase(TownySettings.getAcceptCommand())) {
			parseAccept(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase(TownySettings.getDenyCommand())) {
			parseDeny(player, StringMgmt.remFirstArg(split));
		}

	}

	private static void parseInviteList(Player player) {
		// Now we check the size of the player invites, if there is more than 10 invites (not possible), We only displayed the first 10.
		Resident resident;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		try {
			if (resident.getReceivedInvites().size() > 0) {
				sendInviteList(player, resident.getReceivedInvites());
			} else {
				throw new TownyException(TownySettings.getLangString("msg_err_player_no_invites"));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	public static void parseDeny(Player player, String[] args) {
		Resident resident;
		Town town = null;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<Invite> invites = resident.getReceivedInvites();

		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_player_no_invites"));
			return;
		}
		if (args.length >= 1) {
			// We cut the first argument out of it so /invite *accept* args[1]
			// SO now args[0] is always the Town, we should check if the argument length is >= 1
			try {
				town = TownyUniverse.getDataSource().getTown(args[0]);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) { // Only 1 Invite.
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_player_has_multiple_invites"));
				parseInviteList(player);
				return;
			}
		}
		ListMultimap<Town, Resident> town2residents = InviteHandler.getTowntoresidentinvites();
		if (town2residents.containsKey(town)) {
			if (town2residents.get(town).contains(resident)) {
				for (Invite invite : resident.getReceivedInvites()) {
					if (invite.getSender().equals(town)) {
						try {
							InviteHandler.declineInvite(invite);
							return;
						} catch (TownyException e) {
							e.printStackTrace();
						} catch (InvalidObjectException e) {
							e.printStackTrace(); // Shouldn't happen, however like i said a fallback
						}
					}
				}
			}
		}
		TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));


	}

	public static void parseAccept(Player player, String[] args) {
		Resident resident;
		Town town = null;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<Invite> invites = resident.getReceivedInvites();
		if (invites.size() == 0) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_player_no_invites"));
			return;
		}
		if (args.length >= 1) {
			try {
				town = TownyUniverse.getDataSource().getTown(args[0]);
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) {
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_player_has_multiple_invites"));
				parseInviteList(player);
				return;
			}
		}
		// At this point I consider having a valid Town & a valid Player so a final check is ran:
		ListMultimap<Town, Resident> town2residents = InviteHandler.getTowntoresidentinvites();
		if (town2residents.containsEntry(town, resident)) {
			for (Invite invite : resident.getReceivedInvites()) {
				if (invite.getSender().equals(town)) {
					try {
						InviteHandler.acceptInvite(invite);
						return;
					} catch (TownyException e) {
						e.printStackTrace();
					} catch (InvalidObjectException e) {
						e.printStackTrace(); // Shouldn't happen, however like i said a fallback
					}
				}
			}
		}
		TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_name"));


	}

	private static void sendInviteList(Player player, List<Invite> list) {
		int total = (int) Math.ceil(((double) list.size()) / ((double) 10));
		List<String> invitesformatted = new ArrayList();
		for (int i = 0; i < list.size(); i++) {
			Invite invite = list.get(i);
			String name = invite.getDirectSender();
			if (name == null) {
				name = "Console";
			} else {
				try {
					name = TownyUniverse.getDataSource().getResident(name).getName();
				} catch (NotRegisteredException e) {
					name = "Unknown";
				}
			}
			String output = Colors.Blue + ((Town) invite.getSender()).getName() + Colors.Gray + " - " + Colors.Green + name;
			invitesformatted.add(output);
			player.sendMessage(ChatTools.formatList(TownySettings.getLangString("invite_plu"),
					Colors.Blue + "Town" + Colors.Gray + " - " + Colors.LightBlue + "Inviter",
					invitesformatted, null
					)
			);
		}
	}
}
