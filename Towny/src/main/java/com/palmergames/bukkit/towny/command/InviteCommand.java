package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class InviteCommand extends BaseCommand implements CommandExecutor {

	private static final List<String> inviteTabCompletes = Arrays.asList(
		TownySettings.getAcceptCommand(),
		TownySettings.getDenyCommand()
	);
	
	private final Towny plugin;

	public InviteCommand(Towny instance) {
		plugin = instance;
	}
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		return switch (args.length) {
			case 1 -> NameUtil.filterByStart(inviteTabCompletes, args[0]);
			case 2 -> switch (args[0].toLowerCase(Locale.ROOT)) {
				case "accept", "deny" -> {
					if (sender instanceof Player player) {
						Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
						if (res != null) {
							yield NameUtil.filterByStart(
								res.getReceivedInvites()
									.stream()
									.map(Invite::getSender)
									.map(InviteSender::getName)
									.collect(Collectors.toList()),
								args[1]
							);
						}
					}
					yield Collections.emptyList();
				}
				default -> Collections.emptyList();
			};
			default -> Collections.emptyList();
		};
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
		
		if (sender instanceof Player player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}

			if (command.getName().equalsIgnoreCase("invite")) {
				parseInviteCommand(player, args);
			}
		} else
			// Console
			HelpMenu.INVITE_HELP.send(sender);
		
		return true;
	}

	private void parseInviteCommand(final Player player, String[] split) {
		// So they just type /invite , We should probably send them to the help menu. Done.
		if (split.length == 0 || split[0].equalsIgnoreCase("help") || split[0].equalsIgnoreCase("?")) {
			HelpMenu.INVITE_HELP.send(player);
		} else if (split[0].equalsIgnoreCase("list")) {
			parseInviteList(player, split);
		} else if (split[0].equalsIgnoreCase(TownySettings.getAcceptCommand())) {
			parseAccept(player, StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase(TownySettings.getDenyCommand())) {
			parseDeny(player, StringMgmt.remFirstArg(split));
		}

	}

	private static void parseInviteList(Player player, String[] split) {
		// Now we check the size of the player invites, if there is more than 10 invites (not possible), We only displayed the first 10.
		// /invite args[0] args[1}
		Resident resident = TownyAPI.getInstance().getResident(player);
		
		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
			return;
		}
		
		String received = Translatable.of("player_received_invites").forLocale(player)
				.replace("%a", Integer.toString(resident.getReceivedInvites().size())
				)
				.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident)));
		try {
			if (!resident.getReceivedInvites().isEmpty()) {
				int page = 1;
				if (split != null) {
					if (split.length >= 2) {
						try {
							page = Integer.parseInt(split[1]);
						} catch (NumberFormatException ignored) {
						}
					}
				}
				sendInviteList(player, resident.getReceivedInvites(), page, false);
				TownyMessaging.sendMessage(player, received);

			} else {
				throw new TownyException(Translatable.of("msg_err_player_no_invites"));
			}
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage(player));
		}

	}

	public static void parseDeny(Player player, String[] args) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		
		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
			return;
		}
		
		List<Invite> invites = resident.getReceivedInvites();

		if (invites.isEmpty()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_player_no_invites"));
			return;
		}
		
		Town town;
		
		if (args.length >= 1) {
			// We cut the first argument out of it so /invite deny args[1]
			// SO now args[0] is always the Town, we should check if the argument length is >= 1
			if (args[0].equalsIgnoreCase("all")) {
				denyAllInvites(new ArrayList<>(invites));
				TownyMessaging.sendMsg(resident, Translatable.of("msg_player_denied_all_invites"));
				return;
			}

			town = TownyAPI.getInstance().getTown(args[0]);
			
			if (town == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) { // Only 1 Invite.
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_player_has_multiple_invites"));
				parseInviteList(player, null);
				return;
			}
		}
		
		Invite toDecline = null;

		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(town) && invite.getReceiver().equals(resident)) {
				toDecline = invite;
				break;
			}
		}
		if (toDecline != null) {
			try {
				InviteHandler.declineInvite(toDecline, false);
			} catch (InvalidObjectException e) {
				Towny.getPlugin().getLogger().log(Level.WARNING, "unknown exception occurred while denying invite", e);
			}
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_specify_name"));			


	}

	private static void denyAllInvites(List<Invite> invites) {
		for (Invite invite : invites) {
			try {
				InviteHandler.declineInvite(invite, true);
			} catch (InvalidObjectException ignored) {}
		}
	}

	public static void parseAccept(Player player, String[] args) {
		final Resident resident = TownyAPI.getInstance().getResident(player);

		if (resident == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered"));
			return;
		}

		List<Invite> invites = resident.getReceivedInvites();
		if (invites.isEmpty()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_player_no_invites"));
			return;
		}

		Town town;
		if (args.length >= 1) {
			town = TownyAPI.getInstance().getTown(args[0]);
			
			if (town == null) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_invalid_name"));
				return;
			}
		} else {
			if (invites.size() == 1) {
				town = (Town) invites.get(0).getSender();
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_player_has_multiple_invites"));
				parseInviteList(player, null);
				return;
			}
		}
		// At this point I consider having a valid Town & a valid Player so a final check is ran:

		Invite toAccept = null;

		for (Invite invite : InviteHandler.getActiveInvites()) {
			if (invite.getSender().equals(town) && invite.getReceiver().equals(resident)) {
				toAccept = invite;
				break;
			}
		}

		if (toAccept != null) {
			try {
				if (!town.isAllowedThisAmountOfResidents(town.getNumResidents() + 1, town.isCapital())) {
					TownyMessaging.sendMsg(player, Translatable.of("msg_err_max_residents_per_town_reached", TownySettings.getMaxResidentsForTown(town)));
				} else if (town.getNationOrNull() != null && TownySettings.getMaxResidentsPerNation() > 0 && town.getNationOrNull().getResidents().size() >= TownySettings.getMaxResidentsPerNation()) {
					TownyMessaging.sendMsg(player, Translatable.of("msg_err_cannot_join_nation_over_resident_limit", TownySettings.getMaxResidentsPerNation()));
				} else
					InviteHandler.acceptInvite(toAccept);
			} catch (TownyException | InvalidObjectException e) {
				Towny.getPlugin().getLogger().log(Level.WARNING, "unknown exception occurred while accepting invite", e);
			}
		} else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_specify_name"));


	}

	public static void sendInviteList(Player player, List<Invite> list, int page, boolean fromSender) {

		if (page < 0) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_negative"));
			return;
		} else if (page == 0) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_error_must_be_int"));
			return;
		}
		int total = (int) Math.ceil(((double) list.size()) / ((double) 10));
		if (page > total) {
			return;
		}
		List<String> invitesFormatted = new ArrayList<>();
		int iMax = page * 10;
		if ((page * 10) > list.size()) {
			iMax = list.size();
		}
		Translatable object = Translatable.literal("null");
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Invite invite = list.get(i);
			String name = invite.getSenderName();
			
			// If it's from the sender, do it differently
			String output;
			if (fromSender) {
				output = Colors.DARK_AQUA + invite.getReceiver().getName() + Colors.DARK_GRAY + " - " + Colors.DARK_GREEN + name;
				if (invite.getSender() instanceof Town) { // If it's sent by a town to a resident
					object = Translatable.of("player_sing");
				}
				if (invite.getSender() instanceof Nation) {
					if (invite.getReceiver() instanceof Town) {
						object = Translatable.of("town_sing");
					}
					if (invite.getReceiver() instanceof Nation) {
						object = Translatable.of("nation_sing");
					}
				}
			} else { // So it's not from the sender, then it's from the receiver so
				output = Colors.DARK_AQUA + invite.getSender().getName() + Colors.DARK_GRAY + " - " + Colors.DARK_GREEN + name;
				if (invite.getReceiver() instanceof Resident) {
					object = Translatable.of("town_sing");
				}
				if (invite.getReceiver() instanceof Town || invite.getReceiver() instanceof Nation) {
					object = Translatable.of("nation_sing");
				}
			}
			invitesFormatted.add(output);
		}

		TownyMessaging.sendMessage(player, ChatTools.formatList(Translatable.of("invite_plu").forLocale(player),
				Colors.DARK_AQUA + object.forLocale(player) + Colors.DARK_GRAY + " - " + Colors.AQUA + Translatable.of("invite_sent_by").forLocale(player),
				invitesFormatted, Translatable.of("LIST_PAGE", page, total).forLocale(player)
		));
	}
}
