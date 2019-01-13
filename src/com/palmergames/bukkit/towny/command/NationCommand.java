package com.palmergames.bukkit.towny.command;

import com.earth2me.essentials.Teleport;
import com.earth2me.essentials.User;
import com.google.common.collect.ListMultimap;
import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;
import com.palmergames.bukkit.towny.confirmations.ConfirmationType;
import com.palmergames.bukkit.towny.event.NationInviteTownEvent;
import com.palmergames.bukkit.towny.event.NationRequestAllyNationEvent;
import com.palmergames.bukkit.towny.event.NewNationEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.object.inviteobjects.NationAllyNationInvite;
import com.palmergames.bukkit.towny.object.inviteobjects.TownJoinNationInvite;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

import javax.naming.InvalidNameException;

import java.io.InvalidObjectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;




public class NationCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> nation_help = new ArrayList<>();
	private static final List<String> king_help = new ArrayList<>();

	static {

		nation_help.add(ChatTools.formatTitle("/nation"));
		nation_help.add(ChatTools.formatCommand("", "/nation", "", TownySettings.getLangString("nation_help_1")));
		nation_help.add(ChatTools.formatCommand("", "/nation", TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("nation_help_3")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "list", TownySettings.getLangString("nation_help_4")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "online", TownySettings.getLangString("nation_help_9")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "spawn", TownySettings.getLangString("nation_help_10")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/nation", "deposit [$]", ""));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/nation", "leave", TownySettings.getLangString("nation_help_5")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "king ?", TownySettings.getLangString("nation_help_7")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "new " + TownySettings.getLangString("nation_help_2") + " [capital]", TownySettings.getLangString("nation_help_8")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "delete " + TownySettings.getLangString("nation_help_2"), ""));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "say", "[message]"));

		king_help.add(ChatTools.formatTitle(TownySettings.getLangString("king_help_1")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "withdraw [$]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "[add/kick] [town] .. [town]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "rank [add/remove] " + TownySettings.getLangString("res_2"), "[Rank]"));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "set [] .. []", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "toggle [] .. []", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "ally [] .. [] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_2")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "enemy [add/remove] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_3")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "delete", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "say", "[message]"));

	}

	public NationCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args == null) {
				for (String line : nation_help)
					player.sendMessage(line);
				parseNationCommand(player, args);
			} else {
				parseNationCommand(player, args);
			}

		} else
			try {
				parseNationCommandForConsole(sender, args);
			} catch (TownyException e) {
			}

		return true;
	}

	private void parseNationCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			for (String line : nation_help)
				sender.sendMessage(line);

		} else if (split[0].equalsIgnoreCase("list")) {

			listNations(sender, split);

		} else {
			try {
				final Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(nation)));

			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		}


	}

	public void parseNationCommand(final Player player, String[] split) {

		String nationCom = "/nation";

		try {

			if (split.length == 0)
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						Town town = resident.getTown();
						Nation nation = town.getNation();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
					}
				});

			else if (split[0].equalsIgnoreCase("?"))
				for (String line : nation_help)
					player.sendMessage(line);
			else if (split[0].equalsIgnoreCase("list")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listNations(player, split);

			} else if (split[0].equalsIgnoreCase("new")) {

				Resident resident = TownyUniverse.getDataSource().getResident(player.getName());

		        if ((TownySettings.getNumResidentsCreateNation() > 0) && (resident.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
		          TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_enough_residents_new_nation")));
		          return;
		        }

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_NEW.getNode()))
					throw new TownyException(TownySettings.getNotPermToNewNationLine());

				// TODO: Make an overloaded function
				// newNation(Player,String,Town)
				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_nation_name"));
				else if (split.length == 2) {

					if (!resident.isMayor() && !resident.getTown().hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_peasant_right"));
					newNation(player, split[1], resident.getTown().getName());

				} else {
					// TODO: Check if player is an admin
					newNation(player, split[1], split[2]);
				}
			} else if (split[0].equalsIgnoreCase("withdraw")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyUniverse.isWilderness(player.getLocation().getBlock()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					TownBlock tb = TownyUniverse.getTownBlock(player.getLocation());
					Nation tbNation = tb.getTown().getNation();
					Nation pNation= TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation();
					if ((tbNation != pNation) || (!tb.getTown().isCapital()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
				}

				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyUniverse.isWilderness(player.getLocation().getBlock()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
					Coord coord = Coord.parseCoord(plugin.getCache(player).getLastLocation());
					Town town = TownyUniverse.getDataSource().getWorld(player.getLocation().getWorld().getName()).getTownBlock(coord).getTown();
					if (!town.isCapital())
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
					Nation nation = town.getNation();
					if (!TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().equals(nation))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
				}

				if (split.length == 2)
					try {
						nationWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom));
			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LEAVE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				nationLeave(player);

			} else if(split[0].equalsIgnoreCase("spawn")){
			    /*
			        Parse standard nation spawn command.
			     */
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SPAWN.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				String[] newSplit = StringMgmt.remFirstArg(split);
				nationSpawn(player, newSplit);
            }
			else if (split[0].equalsIgnoreCase("deposit")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (TownySettings.isBankActionLimitedToBankPlots()) {
					if (TownyUniverse.isWilderness(player.getLocation().getBlock()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					TownBlock tb = TownyUniverse.getTownBlock(player.getLocation());
					Nation tbNation = tb.getTown().getNation();
					Nation pNation= TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation();
					if ((tbNation != pNation) || (!tb.getTown().isCapital()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
					boolean goodPlot = false;
					if (tb.getType().equals(TownBlockType.BANK) || tb.isHomeBlock())
						goodPlot = true;
					if (!goodPlot)
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_bank_plot"));
				}

				if (TownySettings.isBankActionDisallowedOutsideTown()) {
					if (TownyUniverse.isWilderness(player.getLocation().getBlock()))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
					Coord coord = Coord.parseCoord(plugin.getCache(player).getLastLocation());
					Town town = TownyUniverse.getDataSource().getWorld(player.getLocation().getWorld().getName()).getTownBlock(coord).getTown();
					if (!town.isCapital())
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
					Nation nation = town.getNation();
					if (!TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().equals(nation))
						throw new TownyException(TownySettings.getLangString("msg_err_unable_to_use_bank_outside_nation_capital"));
				}

				if (split.length == 2)
					try {
						nationDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom + " deposit"));

			}  else {
				String[] newSplit = StringMgmt.remFirstArg(split);

				if (split[0].equalsIgnoreCase("rank")) {

					/*
					 * Rank perm tests are performed in the nationrank method.
					 */
					nationRank(player, newSplit);

				} else if (split[0].equalsIgnoreCase("king")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KING.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationKing(player, newSplit);

				} else if (split[0].equalsIgnoreCase("add")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationAdd(player, newSplit);

				} else if (split[0].equalsIgnoreCase("invite") || split[0].equalsIgnoreCase("invites")) {
						parseInviteCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_KICK.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationKick(player, newSplit);

				} else if (split[0].equalsIgnoreCase("set")) {

					/*
					 * perm test performed in method.
					 */
					nationSet(player, newSplit);

				} else if (split[0].equalsIgnoreCase("toggle")) {

					/*
					 * perm test performed in method.
					 */
					nationToggle(player, newSplit);

				} else if (split[0].equalsIgnoreCase("ally")) {

					nationAlly(player, newSplit);

				} else if (split[0].equalsIgnoreCase("enemy")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ENEMY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationEnemy(player, newSplit);

				} else if (split[0].equalsIgnoreCase("delete")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DELETE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationDelete(player, newSplit);

				} else if (split[0].equalsIgnoreCase("online")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ONLINE.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					parseNationOnlineCommand(player, newSplit);

				} else if (split[0].equalsIgnoreCase("say")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SAY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					try {
						Nation nation = TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation();
						StringBuilder builder = new StringBuilder();
						for (String s : newSplit) {
							builder.append(s + " ");
						}
						String message = builder.toString();
						TownyMessaging.sendPrefixedNationMessage(nation, message);
					} catch (Exception e) {
					}

				} else {

					try {
						final Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_OTHERNATION.getNode()) && ( (resident.hasTown() && resident.getTown().hasNation() && (resident.getTown().getNation() != nation) )  || !resident.hasTown() )) {
							throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
						}
						Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
							@Override
						    public void run() {
								TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
							}
						});

					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
					}
				}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}


	}
	private static final List<String> invite = new ArrayList<String>();
	static {
		invite.add(ChatTools.formatTitle("/town invite"));
		invite.add(ChatTools.formatCommand("", "/nation", "invite [town]", TownySettings.getLangString("nation_invite_help_1")));
		invite.add(ChatTools.formatCommand("", "/nation", "invite -[town]", TownySettings.getLangString("nation_invite_help_2")));
		invite.add(ChatTools.formatCommand("", "/nation", "invite sent", TownySettings.getLangString("nation_invite_help_3")));
	}

	private void parseInviteCommand(Player player, String[] newSplit) throws TownyException {
		Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
		String sent = TownySettings.getLangString("nation_sent_invites")
				.replace("%a", Integer.toString(InviteHandler.getSentInvitesAmount(resident.getTown().getNation()))
				)
				.replace("%m", Integer.toString(InviteHandler.getSentInvitesMaxAmount(resident.getTown().getNation())));

		if (newSplit.length == 0) { // (/nation invite)
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_SEE_HOME.getNode())) {
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			}
			String[] msgs;
			List<String> messages = new ArrayList<String>();


			for (String msg : invite) {
				messages.add(Colors.strip(msg));
			}
			messages.add(sent);
			msgs = messages.toArray(new String[0]);
			player.sendMessage(msgs);
			return;
		}
		if (newSplit.length >= 1) { // /town invite [something]
			if (newSplit[0].equalsIgnoreCase("help") || newSplit[0].equalsIgnoreCase("?")) {
				for (String msg : invite) {
					player.sendMessage(Colors.strip(msg));
				}
				return;
			}
			if (newSplit[0].equalsIgnoreCase("sent")) { //  /invite(remfirstarg) sent args[1]
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_LIST_SENT.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getNation().getSentInvites();
				int page = 1;
				if (newSplit.length >= 2) {
					try {
						page = Integer.parseInt(newSplit[1]);
					} catch (NumberFormatException e) {
						page = 1;
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			} else {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_INVITE_ADD.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
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
			try {
				Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
				List<Resident> onlineResidents = TownyUniverse.getOnlineResidentsViewable(player, nation);
				if (onlineResidents.size() > 0 ) {
					TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_nation_online"), nation, player));
				} else {
					TownyMessaging.sendMessage(player, ChatTools.color(TownySettings.getLangString("default_towny_prefix") + Colors.White +  "0 " + TownySettings.getLangString("res_list") + " " + (TownySettings.getLangString("msg_nation_online") + ": " + nation)));
				}

			} catch (NotRegisteredException e) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		} else {
			try {
				Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_nation_online"), nation, player));
			} catch (NotRegisteredException x) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
			}
		}
	}

	public void nationRank(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			// Help output.
			player.sendMessage(ChatTools.formatTitle("/nation rank"));
			player.sendMessage(ChatTools.formatCommand("", "/nation rank", "add/remove [resident] rank", ""));

		} else {

			Resident resident, target;
			Town town = null;
			Town targetTown = null;
			String rank;

			/*
			 * Does the command have enough arguments?
			 */
			if (split.length < 3) {
				TownyMessaging.sendErrorMsg(player, "Eg: /town rank add/remove [resident] [rank]");
				return;
			}

			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				target = TownyUniverse.getDataSource().getResident(split[1]);
				town = resident.getTown();
				targetTown = target.getTown();

				if (town.getNation() != targetTown.getNation())
					throw new TownyException("This resident is not a member of your Town!");

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			rank = split[2];
			/*
			 * Is this a known rank?
			 */
			if (!TownyPerms.getNationRanks().contains(rank)) {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_unknown_rank_available_ranks"), rank, StringMgmt.join(TownyPerms.getNationRanks(), ",") ));
				return;
			}
			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank.toLowerCase()))) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_no_permission_to_give_rank"));
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {
				try {
					if (target.addNationRank(rank)) {
						TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_been_given_rank"), "Nation", rank));
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_given_rank"), "Nation", rank, target.getName()));
						plugin.deleteCache(TownyUniverse.getPlayer(target));
					} else {
						// Not in a nation or Rank doesn't exist
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_resident_not_part_of_any_town"));
						return;
					}
				} catch (AlreadyRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_resident_already_has_rank"), target.getName(), "Nation"));
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				try {
					if (target.removeNationRank(rank)) {
						TownyMessaging.sendMsg(target, String.format(TownySettings.getLangString("msg_you_have_had_rank_taken"), "Nation", rank));
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_have_taken_rank_from"), "Nation", rank, target.getName()));
						plugin.deleteCache(TownyUniverse.getPlayer(target));
					}
				} catch (NotRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, String.format("msg_resident_doesnt_have_rank", target.getName(), "Nation"));
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
			TownyUniverse.getDataSource().saveResident(target);

		}

	}

	private void nationWithdraw(Player player, int amount) {

		Resident resident;
		Nation nation;
		try {
			if (!TownySettings.geNationBankAllowWithdrawls())
				throw new TownyException(TownySettings.getLangString("msg_err_withdraw_disabled"));

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

			nation.withdrawFromBank(resident, amount);
			TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), resident.getName(), amount, "nation"));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	private void nationDeposit(Player player, int amount) {

		Resident resident;
		Nation nation;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

			double bankcap = TownySettings.getNationBankCap();
			if (bankcap > 0) {
				if (amount + nation.getHoldingBalance() > bankcap)
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
			}

			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));

			if (!resident.payTo(amount, nation, "Nation Deposit"))
				throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));

			TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_xx_deposited_xx"), resident.getName(), amount, "nation"));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation
	 * list
	 *
	 * @param sender - Player to send the list to.
	 */

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void listNations(CommandSender sender, String[] split) {
		List<Nation> nationsToSort = TownyUniverse.getDataSource().getNations();

		int page = 1;
	    int total = (int) Math.ceil(((double) nationsToSort.size()) / ((double) 10));
	    if (split.length > 1) {
	        try {
	            page = Integer.parseInt(split[1]);
	            if (page < 0) {
	                TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_err_negative"));
	                return;
	            } else if (page == 0) {
	                TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_must_be_int"));
	                return;
	            }
	        } catch (NumberFormatException e) {
	            TownyMessaging.sendErrorMsg(sender, TownySettings.getLangString("msg_error_must_be_int"));
	            return;
	        }
	    }
	    if (page > total) {
	        TownyMessaging.sendErrorMsg(sender, TownySettings.getListNotEnoughPagesMsg(total));
	        return;
	    }

		Collections.sort(nationsToSort, new Comparator() {
			@Override
			public int compare(Object n1, Object n2) {
				if (((Nation) n2).getNumResidents() == ((Nation) n1).getNumResidents()) return 0;
				return (((Nation) n2).getNumResidents() > ((Nation) n1).getNumResidents()) ? 1 : -1;
			}
		});
		int iMax = page * 10;
		if ((page * 10) > nationsToSort.size()) {
			iMax = nationsToSort.size();
		}
		List<String> nationsordered = new ArrayList();
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Nation nation = nationsToSort.get(i);
			String output = Colors.Gold + nation.getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + nation.getNumResidents() + ")" + Colors.Gray + " - " + Colors.LightBlue + "(" + nation.getNumTowns() + ")";
			nationsordered.add(output);
		}
		sender.sendMessage(
				ChatTools.formatList(
						TownySettings.getLangString("nation_plu"),
						Colors.Gold + "Nation Name" + Colors.Gray + " - " + Colors.LightBlue + "(Number of Residents)" + Colors.Gray + " - " + Colors.LightBlue + "(Number of Towns)",
						nationsordered,
						TownySettings.getListPageMsg(page, total)
				));

	}


	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 *
	 * @param player - Player creating the new nation.
	 */

	public void newNation(Player player, String name, String capitalName) {

		TownyUniverse universe = plugin.getTownyUniverse();
		try {

			Town town = TownyUniverse.getDataSource().getTown(capitalName);
			if (town.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_already_nation"));

			// Check the name is valid and doesn't already exist.
			String filteredName;
			try {
				filteredName = NameValidation.checkAndFilterName(name);
			} catch (InvalidNameException e) {
				filteredName = null;
			}

			if ((filteredName == null) || TownyUniverse.getDataSource().hasNation(filteredName))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));

			if (TownySettings.isUsingEconomy() && !town.pay(TownySettings.getNewNationPrice(), "New Nation Cost"))
				throw new TownyException(String.format(TownySettings.getLangString("msg_no_funds_new_nation2"), TownySettings.getNewNationPrice()));

			newNation(universe, name, town);
			/*
			 * universe.newNation(name); Nation nation =
			 * universe.getNation(name); nation.addTown(town);
			 * nation.setCapital(town);
			 *
			 * universe.getDataSource().saveTown(town);
			 * universe.getDataSource().saveNation(nation);
			 * universe.getDataSource().saveNationList();
			 */

			TownyMessaging.sendGlobalMessage(TownySettings.getNewNationMsg(player.getName(), name));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			// TODO: delete town data that might have been done
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}

	public Nation newNation(TownyUniverse universe, String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {

		TownyUniverse.getDataSource().newNation(name);
		Nation nation = TownyUniverse.getDataSource().getNation(name);
		nation.addTown(town);
		nation.setCapital(town);
		nation.setUuid(UUID.randomUUID());
		nation.setRegistered(System.currentTimeMillis());
		if (TownySettings.isUsingEconomy()) {
			try {
				nation.setBalance(0, "Deleting Nation");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getDataSource().saveTown(town);
		TownyUniverse.getDataSource().saveNation(nation);
		TownyUniverse.getDataSource().saveNationList();

		BukkitTools.getPluginManager().callEvent(new NewNationEvent(nation));

		return nation;
	}

	public void nationLeave(Player player) {

		Town town = null;
		Nation nation = null;

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			town = resident.getTown();
			nation = town.getNation();

			nation.removeTown(town);

			/*
			 * Remove all resident titles/nationRanks before saving the town itself.
			 */
			List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

			for (Resident res : titleRemove) {
				if (res.hasTitle() || res.hasSurname()) {
					res.setTitle("");
					res.setSurname("");
				}
				res.updatePermsForNationRemoval(); // Clears the nationRanks.
				TownyUniverse.getDataSource().saveResident(res);
			}
			TownyUniverse.getDataSource().saveNation(nation);
			TownyUniverse.getDataSource().saveNationList();

			plugin.resetCache();

			TownyMessaging.sendNationMessage(nation, ChatTools.color(String.format(TownySettings.getLangString("msg_nation_town_left"), town.getName())));
			TownyMessaging.sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_town_left_nation"), nation.getName())));
		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		} catch (EmptyNationException en) {
			TownyUniverse.getDataSource().removeNation(en.getNation());
			TownyUniverse.getDataSource().saveNationList();
			TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(TownySettings.getLangString("msg_del_nation"), en.getNation().getName())));
		} finally {
			TownyUniverse.getDataSource().saveTown(town);
		}
	}

	public void nationDelete(Player player, String[] split) {

		if (split.length == 0)
			try {
				Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
				ConfirmationHandler.addConfirmation(resident, ConfirmationType.NATIONDELETE, null); // It takes the resident's town & nation, done finished
				TownyMessaging.sendConfirmationMessage(player, null, null, null, null);
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
		else
			try {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_DELETE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_nation"));

				Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
				TownyUniverse.getDataSource().removeNation(nation);
				TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}
	}

	public void nationKing(Player player, String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			for (String line : king_help)
				player.sendMessage(line);
	}

	public void nationAdd(Player player, String[] names) throws TownyException {

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation add [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

	        if ((TownySettings.getNumResidentsJoinNation() > 0) && (resident.getTown().getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
	        	TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_enough_residents_join_nation"), resident.getTown().getName()));
	        	return;
	        }

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}
		List<String> reslist = new ArrayList<>(Arrays.asList(names));
		// Our Arraylist is above
		List<String> newreslist = new ArrayList<>();
		// The list of valid invites is above, there are currently none
		List<String> removeinvites = new ArrayList<>();
		// List of invites to be removed;
		for (String townname : reslist) {
			if (townname.startsWith("-")) {
				// Add to removing them, remove the "-"
				removeinvites.add(townname.substring(1));
			} else {
				// add to adding them,
				newreslist.add(townname);
			}
		}
		names = newreslist.toArray(new String[0]);
		String[] namestoremove = removeinvites.toArray(new String[0]);
		if (namestoremove.length >= 1) {
			nationRevokeInviteTown(player,nation,TownyUniverse.getDataSource().getTowns(namestoremove));
		}

		if (names.length >= 1) {
			nationAdd(player, nation, TownyUniverse.getDataSource().getTowns(names));
		}
	}

	private static void nationRevokeInviteTown(Object sender,Nation nation, List<Town> towns) {

		for (Town town : towns) {
			if (InviteHandler.getNationtotowninvites().containsEntry(nation, town)) {
				InviteHandler.getNationtotowninvites().remove(nation, town);
				for (Invite invite : town.getReceivedInvites()) {
					if (invite.getSender().equals(nation)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMessage(sender, TownySettings.getLangString("nation_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	public static void nationAdd(Player player, Nation nation, List<Town> invited) throws TownyException {

		ArrayList<Town> remove = new ArrayList<>();
		for (Town town : invited) {
			try {
				if ((TownySettings.getNumResidentsJoinNation() > 0) && (town.getNumResidents() < TownySettings.getNumResidentsJoinNation())) {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_enough_residents_join_nation"), town.getName()));
					remove.add(town);
					continue;
				}

				if (TownySettings.getNationRequiresProximity() > 0) {
					Coord capitalCoord = nation.getCapital().getHomeBlock().getCoord();
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!nation.getCapital().getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
						remove.add(town);
						// TODO: String to tell the player that the town and nation are in 2 different worlds.
						continue;
					}
					double distance;

					distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), town.getName()));
						remove.add(town);
						continue;
					}
				}

				nationInviteTown(player, nation, town);
			} catch (AlreadyRegisteredException e) {
				remove.add(town);
			}
		}

		for (Town town : remove) {
			invited.remove(town);
		}

		if (invited.size() > 0) {
			String msg = "";

			for (Town town : invited) {
				msg += town.getName() + ", ";
			}

			msg = msg.substring(0, msg.length() - 2);
			msg = String.format(TownySettings.getLangString("msg_invited_join_nation"), player.getName(), msg);
			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
		} else {
			// This is executed when the arraylist returns empty (no valid town was entered).
			throw new TownyException(TownySettings.getLangString("msg_invalid_name"));
		}
	}

	private static void nationInviteTown(Player player, Nation nation, Town town) throws TownyException {

		TownJoinNationInvite invite = new TownJoinNationInvite(player.getName(), nation, town);
		try {
			if (!InviteHandler.getNationtotowninvites().containsEntry(nation, town)) {
				town.newReceivedInvite(invite);
				nation.newSentInvite(invite);
				InviteHandler.addInviteToList(invite);
				TownyMessaging.sendRequestMessage(town.getMayor(),invite);
				Bukkit.getPluginManager().callEvent(new NationInviteTownEvent(invite));
			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_already_invited"), town.getName()));
			}
		} catch (TooManyInvitesException e) {
			town.deleteReceivedInvite(invite);
			nation.deleteSentInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	public static void nationAdd(Nation nation, List<Town> towns) throws AlreadyRegisteredException {

		for (Town town : towns) {
			if (!town.hasNation()) {
				nation.addTown(town);
				TownyUniverse.getDataSource().saveTown(town);
				TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_join_nation"), town.getName()));
			}

		}
		plugin.resetCache();
		TownyUniverse.getDataSource().saveNation(nation);

	}

	public void nationKick(Player player, String[] names) {

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation kick [names]");
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		nationKick(player, resident, nation, TownyUniverse.getDataSource().getTowns(names));
	}

	public void nationKick(Player player, Resident resident, Nation nation, List<Town> kicking) {

		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : kicking)
			if (town.isCapital())
				remove.add(town);
			else
				try {
					nation.removeTown(town);
					/*
					 * Remove all resident titles/nationRanks before saving the town itself.
					 */
					List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

					for (Resident res : titleRemove) {
						if (res.hasTitle() || res.hasSurname()) {
							res.setTitle("");
							res.setSurname("");
						}
						res.updatePermsForNationRemoval(); // Clears the nationRanks.
						TownyUniverse.getDataSource().saveResident(res);
					}

					TownyUniverse.getDataSource().saveTown(town);
				} catch (NotRegisteredException e) {
					remove.add(town);
				} catch (EmptyNationException e) {
					// You can't kick yourself and only the mayor can kick
					// assistants
					// so there will always be at least one resident.
				}

		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			String msg = "";

			for (Town town : kicking) {
				msg += town.getName() + ", ";

				TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_nation_kicked_by"), player.getName()));
			}

			msg = msg.substring(0, msg.length() - 2);
			msg = String.format(TownySettings.getLangString("msg_nation_kicked"), player.getName(), msg);
			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveNation(nation);

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

	private static final List<String> alliesstring = new ArrayList<String>();

	static {
		alliesstring.add(ChatTools.formatTitle("/nation invite"));
		alliesstring.add(ChatTools.formatCommand("", "/nation", "ally add [nation]", TownySettings.getLangString("nation_ally_help_1")));
		if (TownySettings.isDisallowOneWayAlliance()) {
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally add -[nation]", TownySettings.getLangString("nation_ally_help_7")));
		}
		alliesstring.add(ChatTools.formatCommand("", "/nation", "ally remove [nation]", TownySettings.getLangString("nation_ally_help_2")));
		if (TownySettings.isDisallowOneWayAlliance()) {
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally sent", TownySettings.getLangString("nation_ally_help_3")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally received", TownySettings.getLangString("nation_ally_help_4")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally accept [nation]", TownySettings.getLangString("nation_ally_help_5")));
			alliesstring.add(ChatTools.formatCommand("", "/nation", "ally deny [nation]", TownySettings.getLangString("nation_ally_help_6")));
		}
	}


	private void nationAlly(Player player, String[] split) throws TownyException {
		if (split.length <= 0) {
			TownyMessaging.sendMessage(player, alliesstring);
			return;
		}

		Resident resident;
		Nation nation;
		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		ArrayList<Nation> list = new ArrayList<Nation>();
		ArrayList<Nation> remlist = new ArrayList<Nation>();
		Nation ally;

		String[] names = StringMgmt.remFirstArg(split);
		if (split[0].equalsIgnoreCase("add")) {

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ADD.getNode())) {
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			}
			for (String name : names) {
				try {
					ally = TownyUniverse.getDataSource().getNation(name);
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_own_nation_disallow"));
						return;
					} else {
						list.add(ally);
					}
				} catch (NotRegisteredException e) { // So "-Name" isn't a town, remove the - check if that is a town.
					if (name.startsWith("-") && TownySettings.isDisallowOneWayAlliance()) {
						try {
							ally = TownyUniverse.getDataSource().getNation(name.substring(1));
							if (nation.equals(ally)) {
								TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_own_nation_disallow"));
								return;
							} else {
								remlist.add(ally);
							}
						} catch (NotRegisteredException x){
							// Do nothing here as it doesn't match a Nation
							// Well we don't want to send the commands again so just say invalid name
							TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
							return;
						}
					} else {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
						return;
					}
				}
			}
			if (!list.isEmpty()) {
				if (TownySettings.isDisallowOneWayAlliance()) {
					nationAlly(resident,nation,list,true);
				} else {
					nationlegacyAlly(resident, nation, list, true);
				}
			}
			if (!remlist.isEmpty()) {
				nationRemoveAllyRequest(player,nation, remlist);
			}
			return;
		}
		if (split[0].equalsIgnoreCase("remove")) {
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_REMOVE.getNode())) {
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
			}
			for (String name : names) {
				try {
					ally = TownyUniverse.getDataSource().getNation(name);
					if (nation.equals(ally)) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_own_nation_disallow"));
						return;
					} else {
						list.add(ally);
					}
				} catch (NotRegisteredException e) {
				}
			}
			if (!list.isEmpty()) {
				if (TownySettings.isDisallowOneWayAlliance()) {
					nationAlly(resident,nation,list,false);
				} else {
					nationlegacyAlly(resident, nation, list, false);
				}
			}
			return;
		} else {
			if (!TownySettings.isDisallowOneWayAlliance()){
				TownyMessaging.sendMessage(player, alliesstring);
				return;
			}
		}
		if (TownySettings.isDisallowOneWayAlliance()) {
			String received = TownySettings.getLangString("nation_received_requests")
					.replace("%a", Integer.toString(InviteHandler.getReceivedInvitesAmount(resident.getTown().getNation()))
					)
					.replace("%m", Integer.toString(InviteHandler.getReceivedInvitesMaxAmount(resident.getTown().getNation())));
			String sent = TownySettings.getLangString("nation_sent_ally_requests")
					.replace("%a", Integer.toString(InviteHandler.getSentAllyRequestsAmount(resident.getTown().getNation()))
					)
					.replace("%m", Integer.toString(InviteHandler.getSentAllyRequestsMaxAmount(resident.getTown().getNation())));
			if (split[0].equalsIgnoreCase("sent")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_SENT.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				List<Invite> sentinvites = resident.getTown().getNation().getSentAllyInvites();
				int page = 1;
				if (split.length >= 2) {
					try {
						page = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						page = 1;
					}
				}
				InviteCommand.sendInviteList(player, sentinvites, page, true);
				player.sendMessage(sent);
				return;
			}
			if (split[0].equalsIgnoreCase("received")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_LIST_RECEIVED.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				List<Invite> receivedinvites = resident.getTown().getNation().getReceivedInvites();
				int page = 1;
				if (split.length >= 2) {
					try {
						page = Integer.parseInt(split[2]);
					} catch (NumberFormatException e) {
						page = 1;
					}
				}
				InviteCommand.sendInviteList(player, receivedinvites, page, true);
				player.sendMessage(received);
				return;

			}
			if (split[0].equalsIgnoreCase("accept")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_ACCEPT.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					try {
						sendernation = TownyUniverse.getDataSource().getNation(split[1]);
					} catch (NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_nation_specify_invite"));
					InviteCommand.sendInviteList(player, invites,1,false);
					return;
				}
				ListMultimap<Nation, Nation> nation2nations = InviteHandler.getNationtonationinvites();
				if (nation2nations.containsKey(sendernation)) {
					if (nation2nations.get(sendernation).contains(nation)) {
						for (Invite invite : nation.getReceivedInvites()) {
							if (invite.getSender().equals(sendernation)) {
								try {
									InviteHandler.acceptInvite(invite);
									return;
								} catch (InvalidObjectException e) {
									e.printStackTrace(); // Shouldn't happen, however like i said a fallback
								}
							}
						}
					}
				}

			}
			if (split[0].equalsIgnoreCase("deny")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY_DENY.getNode())) {
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				}
				Nation sendernation;
				List<Invite> invites = nation.getReceivedInvites();

				if (invites.size() == 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_nation_no_requests"));
					return;
				}
				if (split.length >= 2) { // /invite deny args[1]
					try {
						sendernation = TownyUniverse.getDataSource().getNation(split[1]);
					} catch (NotRegisteredException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_nation_specify_invite"));
					InviteCommand.sendInviteList(player, invites, 1, false);
					return;
				}
				ListMultimap<Nation, Nation> nation2nations = InviteHandler.getNationtonationinvites();
				if (nation2nations.containsKey(sendernation)) {
					if (nation2nations.get(sendernation).contains(nation)) {
						for (Invite invite : nation.getReceivedInvites()) {
							if (invite.getSender().equals(sendernation)) {
								try {
									InviteHandler.declineInvite(invite, false);
									TownyMessaging.sendMessage(player, TownySettings.getLangString("successful_deny_request"));
									return;
								} catch (InvalidObjectException e) {
									e.printStackTrace(); // Shouldn't happen, however like i said a fallback
								}
							}
						}
					}
				}
			} else {
				TownyMessaging.sendMessage(player, alliesstring);
				return;
			}
		}

	}

	private void nationRemoveAllyRequest(Object sender,Nation nation, ArrayList<Nation> remlist) {
		for (Nation invited : remlist) {
			if (InviteHandler.getNationtonationinvites().containsEntry(nation, invited)) {
				InviteHandler.getNationtonationinvites().remove(nation, invited);
				for (Invite invite : invited.getReceivedInvites()) {
					if (invite.getSender().equals(nation)) {
						try {
							InviteHandler.declineInvite(invite, true);
							TownyMessaging.sendMessage(sender, TownySettings.getLangString("town_revoke_invite_successful"));
							break;
						} catch (InvalidObjectException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
	}

	private void nationCreateAllyRequest(String sender, Nation nation, Nation receiver) throws TownyException {
		NationAllyNationInvite invite = new NationAllyNationInvite(sender, nation, receiver);
		try {
			if (!InviteHandler.getNationtonationinvites().containsEntry(nation, receiver)) {
				receiver.newReceivedInvite(invite);
				nation.newSentAllyInvite(invite);
				InviteHandler.addInviteToList(invite);
				TownyMessaging.sendRequestMessage(receiver.getCapital().getMayor(),invite);
				Bukkit.getPluginManager().callEvent(new NationRequestAllyNationEvent(invite));
			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_player_already_invited"), receiver.getName()));
			}
		} catch (TooManyInvitesException e) {
			receiver.deleteReceivedInvite(invite);
			nation.deleteSentAllyInvite(invite);
			throw new TownyException(e.getMessage());
		}
	}

	public void nationlegacyAlly(Resident resident, final Nation nation, List<Nation> allies, boolean add) {

		Player player = BukkitTools.getPlayer(resident.getName());

		ArrayList<Nation> remove = new ArrayList<Nation>();

		for (Nation targetNation : allies)
			try {
				if (add && !nation.getAllies().contains(targetNation)) {
					if (!targetNation.hasEnemy(nation)) {
							try {
								nation.addAlly(targetNation);
							} catch (AlreadyRegisteredException e) {
								e.printStackTrace();
							}

							TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_allied_nations"), resident.getName(), targetNation.getName()));
							TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_ally"), nation.getName()));
//						}
					} else {
						// We are set as an enemy so can't allY
						remove.add(targetNation);
						TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_unable_ally_enemy"), targetNation.getName()));
					}
				} else if (nation.getAllies().contains(targetNation)) {
					nation.removeAlly(targetNation);

					TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_ally"), nation.getName()));
					TownyMessaging.sendMessage(player, TownySettings.getLangString("msg_ally_removed_successfully"));
					// Remove any mirrored allies settings from the target nation
					if (targetNation.hasAlly(nation))
						nationlegacyAlly(resident, targetNation, Arrays.asList(nation), false);
				}

			} catch (NotRegisteredException e) {
				remove.add(targetNation);
			}

		for (Nation newAlly : remove)
			allies.remove(newAlly);

		if (allies.size() > 0) {

			TownyUniverse.getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

	}

	public void nationAlly(Resident resident, final Nation nation, List<Nation> allies, boolean add) throws TownyException {
		// This is where we add /remove those invites for nations to ally other nations.

		Player player = BukkitTools.getPlayer(resident.getName());

		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : allies) {
			if (add) { // If we are adding as an ally.
				if (!targetNation.hasEnemy(nation)) {
					if (!targetNation.getCapital().getMayor().isNPC()) {
						for (Nation newAlly : allies) {
							nationCreateAllyRequest(player.getName(), nation, targetNation);
							TownyMessaging.sendNationMessage(nation, ChatTools.color(String.format(TownySettings.getLangString("msg_ally_req_sent"), newAlly.getName())));
						}
					} else {
						if (TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode())) {
							try {
								targetNation.addAlly(nation);
								nation.addAlly(targetNation);
							} catch (AlreadyRegisteredException e) {
								e.printStackTrace();
							}
							TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_allied_nations"), resident.getName(), targetNation.getName()));
							TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_ally"), nation.getName()));
						} else
							TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_unable_ally_npc"), nation.getName()));
					}
				}
			} else { // So we are removing an ally
				if (nation.getAllies().contains(targetNation)) {
					try {
						nation.removeAlly(targetNation);
						TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_ally"), nation.getName()));
						TownyMessaging.sendMessage(player, TownySettings.getLangString("msg_ally_removed_successfully"));
					} catch (NotRegisteredException e) {
						remove.add(targetNation);
					}
					// Remove any mirrored allies settings from the target nation
					// We know the linked allies are enabled so:
					if (targetNation.hasAlly(nation)) {
						try {
							targetNation.removeAlly(nation);
						} catch (NotRegisteredException e) {
							// This should genuinely not be possible since we "hasAlly it beforehand"
						}
					}
				}

			}
		}
		for (Nation newAlly : remove) {
			allies.remove(newAlly);
		}

		if (allies.size() > 0) {

			TownyUniverse.getDataSource().saveNations();

			plugin.resetCache();
		} else {
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
		}

	}

	public void nationEnemy(Player player, String[] split) {

		Resident resident;
		Nation nation;

		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation enemy [add/remove] [name]");
			return;
		}

		try {
			resident = TownyUniverse.getDataSource().getResident(player.getName());
			nation = resident.getTown().getNation();

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
			return;
		}

		ArrayList<Nation> list = new ArrayList<Nation>();
		Nation enemy;
		// test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);

		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				try {
					enemy = TownyUniverse.getDataSource().getNation(name);
					if (nation.equals(enemy))
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_own_nation_disallow"));
					else
						list.add(enemy);
				} catch (NotRegisteredException e) {
					// Do nothing here as the name doesn't match a Nation
				}
			}
			if (!list.isEmpty())
				nationEnemy(resident, nation, list, test.equalsIgnoreCase("add"));

		} else {
			TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "[add/remove]"));
		}
	}

	public void nationEnemy(Resident resident, Nation nation, List<Nation> enemies, boolean add) {

		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : enemies)
			try {
				if (add && !nation.getEnemies().contains(targetNation)) {
					nation.addEnemy(targetNation);
					TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_enemy"), nation.getName()));
					// Remove any ally settings from the target nation
					if (targetNation.hasAlly(nation))
						nationlegacyAlly(resident, targetNation, Arrays.asList(nation), false);

				} else if (nation.getEnemies().contains(targetNation)) {
					nation.removeEnemy(targetNation);
					TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_enemy"), nation.getName()));
				}

			} catch (AlreadyRegisteredException e) {
				remove.add(targetNation);
			} catch (NotRegisteredException e) {
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
				msg = String.format(TownySettings.getLangString("msg_enemy_nations"), resident.getName(), msg);
			else
				msg = String.format(TownySettings.getLangString("msg_enemy_to_neutral"), resident.getName(), msg);

			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(resident, TownySettings.getLangString("msg_invalid_name"));
	}

	public void nationSet(Player player, String[] split) throws TownyException, InvalidNameException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation set"));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "king " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "capital [town]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "title/surname [resident] [text]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "tag [upto 4 letters] or clear", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "board [message ... ]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "spawn", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "spawncost [$]", ""));
		} else {
			Resident resident;
			Nation nation;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				nation = resident.getTown().getNation();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("king")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_KING.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set king Dumbo");
				else
					try {
						Resident newKing = TownyUniverse.getDataSource().getResident(split[1]);
						String oldKingsName = nation.getCapital().getMayor().getName();

			            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newKing.getTown().getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			              TownyMessaging.sendResidentMessage(resident, String.format(TownySettings.getLangString("msg_not_enough_residents_capital"), newKing.getTown().getName()));
			              return;
			            }

						nation.setKing(newKing);
						plugin.deleteCache(oldKingsName);
						plugin.deleteCache(newKing.getName());
						TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newKing.getName(), nation.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
			} else if (split[0].equalsIgnoreCase("capital")) {
				try {
					Town newCapital = TownyUniverse.getDataSource().getTown(split[1]);

		            if ((TownySettings.getNumResidentsCreateNation() > 0) && (newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
		              TownyMessaging.sendResidentMessage(resident, String.format(TownySettings.getLangString("msg_not_enough_residents_capital"), newCapital.getName()));
		              return;
		            }

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITOL.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					if (split.length < 2)
						TownyMessaging.sendErrorMsg(player, "Eg: /nation set capital {town name}");
					else
							nation.setCapital(newCapital);
							nation.recheckTownDistance();
							plugin.resetCache();
							TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName(), nation.getName()));
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

			} else if (split[0].equalsIgnoreCase("spawn")){

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWN.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try{
					nation.setNationSpawn(player.getLocation());
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_set_nation_spawn"));
				} catch (TownyException e){
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			}
			else if (split[0].equalsIgnoreCase("taxes")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAXES.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set taxes 70");
				else {
					Integer amount = Integer.parseInt(split[1].trim());
					if (amount < 0) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
						return;
					}

					try {
						nation.setTaxes(amount);
						TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_town_set_nation_tax"), player.getName(), split[1]));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
				}

			} else if (split[0].equalsIgnoreCase("spawncost")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SPAWNCOST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set spawncost 70");
				else {
					try {
						Double amount = Double.parseDouble(split[1]);
						if (amount < 0) {
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative_money"));
							return;
						}
						if (TownySettings.getSpawnTravelCost() < amount) {
							TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_cannot_set_spawn_cost_more_than"), TownySettings.getSpawnTravelCost()));
							return;
						}
						nation.setSpawnCost(amount);
						TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_spawn_cost_set_to"), player.getName(), TownySettings.getLangString("nation_sing"), split[1]));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_num"));
						return;
					}
				}

			} else if (split[0].equalsIgnoreCase("name")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set name Plutoria");
				else {

				    if(TownySettings.getNationRenameCost() > 0) {
                        try {
                            if (TownySettings.isUsingEconomy() && !nation.pay(TownySettings.getNationRenameCost(), String.format("Nation renamed to: %s", split[1])))
                                throw new TownyException(String.format(TownySettings.getLangString("msg_err_no_money"), TownyEconomyHandler.getFormattedBalance(TownySettings.getNationRenameCost())));
                        } catch (EconomyException e) {
                            throw new TownyException("Economy Error");
                        }
                    }

					if (!NameValidation.isBlacklistName(split[1]))
						nationRename(player, nation, split[1]);
					else
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
				}

			} else if (split[0].equalsIgnoreCase("tag")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TAG.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set tag PLT");
				else if (split[1].equalsIgnoreCase("clear")) {
					try {
						nation.setTag(" ");
						TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_reset_nation_tag"), player.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
				} else

					nation.setTag(NameValidation.checkAndFilterName(split[1]));
				TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_set_nation_tag"), player.getName(), nation.getTag()));

			} else if (split[0].equalsIgnoreCase("title")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_TITLE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set title bilbo Jester ");
				else

					resident = TownyUniverse.getDataSource().getResident(split[1]);
				if (resident.hasNation()) {
					if (resident.getTown().getNation() != TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation()) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_same_nation"), resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_same_nation"), resident.getName()));
					return;
				}
				split = StringMgmt.remArgs(split, 2);
				if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
					return;
				}

				String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
				resident.setTitle(title + " ");
				TownyUniverse.getDataSource().saveResident(resident);

				if (resident.hasTitle())
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
				else
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Title", resident.getName()));

			} else if (split[0].equalsIgnoreCase("surname")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_SURNAME.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				// Give the resident a title
				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set surname bilbo the dwarf ");
				else

					resident = TownyUniverse.getDataSource().getResident(split[1]);
				if (resident.hasNation()) {
					if (resident.getTown().getNation() != TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation()) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_same_nation"), resident.getName()));
						return;
					}
				} else {
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_same_nation"), resident.getName()));
					return;
				}
				split = StringMgmt.remArgs(split, 2);
				if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
					return;
				}

				String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
				resident.setSurname(" " + surname);
				TownyUniverse.getDataSource().saveResident(resident);

				if (resident.hasSurname())
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
				else
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Surname", resident.getName()));


			} else if (split[0].equalsIgnoreCase("board")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_BOARD.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2) {
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set board " + TownySettings.getLangString("town_help_9"));
					return;
				} else {
					String line = StringMgmt.join(StringMgmt.remFirstArg(split), " ");

					if (!NameValidation.isValidString(line)) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_invalid_string_nationboard_not_set"));
						return;
					}

					nation.setNationBoard(line);
					TownyMessaging.sendNationBoard(player, nation);
				}
			} else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), split[0]));
				return;
			}

			TownyUniverse.getDataSource().saveNation(nation);
			TownyUniverse.getDataSource().saveNationList();
		}
	}

	public void nationToggle(Player player, String[] split) throws TownyException {

		if (split.length == 0) {
			player.sendMessage(ChatTools.formatTitle("/nation toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/nation toggle", "peaceful public", ""));
		} else {
			Resident resident;
			Nation nation;
			try {
				resident = TownyUniverse.getDataSource().getResident(player.getName());
				nation = resident.getTown().getNation();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getMessage());
				return;
			}

			if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try {
					boolean choice = !nation.isNeutral();
					Double cost = TownySettings.getNationNeutralityCost();

					if (choice && TownySettings.isUsingEconomy() && !nation.pay(cost, "Peaceful Nation Cost"))
						throw new TownyException(TownySettings.getLangString("msg_nation_cant_peaceful"));

					nation.setNeutral(choice);

					// send message depending on if using IConomy and charging
					// for peaceful
					if (TownySettings.isUsingEconomy() && cost > 0)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_paid"), TownyEconomyHandler.getFormattedBalance(cost)));
					else
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_nation_set_peaceful"));

					TownyMessaging.sendNationMessage(nation, TownySettings.getLangString("msg_nation_peaceful") + (nation.isNeutral
							() ? Colors.Green : Colors.Red + " not") + " peaceful.");
				} catch (EconomyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				} catch (TownyException e) {
					try {
						nation.setNeutral(false);
					} catch (TownyException e1) {
						e1.printStackTrace();
					}
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
			} else if(split[0].equalsIgnoreCase("public")){
                if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_PUBLIC.getNode()))
                    throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

                nation.setPublic(!nation.isPublic());
                TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_changed_public"), nation.isPublic() ? TownySettings.getLangString("enabled") : TownySettings.getLangString("disabled")));

            } else {
				TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "nation"));
				return;
			}

			plugin.getTownyUniverse();
			TownyUniverse.getDataSource().saveNation(nation);
		}
	}

	public void nationRename(Player player, Nation nation, String newName) {

		try {
			TownyUniverse.getDataSource().renameNation(nation, newName);
			TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_set_name"), player.getName(), nation.getName()));
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}
	}


    /**
     * Wrapper for the nationSpawn() method. All calls should be through here
     * unless bypassing for admins.
     *
     * @param player
     * @param split
     * @throws TownyException
     */
    public static void nationSpawn(Player player, String[] split) throws TownyException {

        try {

            Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
            Nation nation;
            String notAffordMSG;

            // Set target nation and affiliated messages.
            if (split.length == 0) {

                if (!resident.hasTown()) {
                    TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
                    return;
                }

                if (!resident.getTown().hasNation()) {
                    TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
                    return;
                }

                nation = resident.getTown().getNation();
                notAffordMSG = TownySettings.getLangString("msg_err_cant_afford_tp");

                nationSpawn(player, split, nation, notAffordMSG);

            } else {
                // split.length > 1
                nation = TownyUniverse.getDataSource().getNation(split[0]);
                notAffordMSG = String.format(TownySettings.getLangString("msg_err_cant_afford_tp_nation"), nation.getName());

                nationSpawn(player, split, nation, notAffordMSG);

            }
        } catch (NotRegisteredException e) {

            throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));

        }

    }

    /**
     * Core nation spawn function to allow admin use.
     *
     * @param player
     * @param split
     * @param nation
     * @param notAffordMSG
     */
    public static void nationSpawn(Player player, String[] split, Nation nation, String notAffordMSG) {

        try {
            boolean isTownyAdmin = TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SPAWN_OTHER.getNode());
            Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
            Location spawnLoc;
            NationSpawnLevel nationSpawnPermission;

            spawnLoc = nation.getNationSpawn();

            // Determine conditions
            if (isTownyAdmin) {
                nationSpawnPermission = NationSpawnLevel.ADMIN;
            } else if ((split.length == 0)) {
				nationSpawnPermission = NationSpawnLevel.PART_OF_NATION;
            } else {
                // split.length > 1
                if (!resident.hasTown()) {
                    nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
                }
                else if (resident.hasNation()) {
                    Nation playerNation = resident.getTown().getNation();
                    Nation targetNation = nation;

                    if (playerNation == targetNation) {
                        nationSpawnPermission = NationSpawnLevel.PART_OF_NATION;
                    } else if (targetNation.hasEnemy(playerNation)) {
                        // Prevent enemies from using spawn travel.
                        throw new TownyException(TownySettings.getLangString("msg_err_public_spawn_enemy"));
                    } else if (targetNation.hasAlly(playerNation)) {
                        nationSpawnPermission = NationSpawnLevel.NATION_ALLY;
                    } else {
                        nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
                    }
                } else {
                    nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
                }
            }

            // Check the permissions
        	if (!(isTownyAdmin || ((nationSpawnPermission == NationSpawnLevel.UNAFFILIATED) ? nation.isPublic() : nationSpawnPermission.hasPermissionNode(plugin, player, nation)))) {

         		throw new TownyException(TownySettings.getLangString("msg_err_nation_not_public"));
   			}


//			// Check the permissions (Inspired by the town command but rewritten. (So we can actually read it :3 ))
//            if(!isTownyAdmin) {
//                if (nationSpawnPermission == TownSpawnLevel.UNAFFILIATED) {
//					boolean war = TownyUniverse.isWarTime();
//					if(war){
//						throw new TownyException(TownySettings.getLangString("msg_err_nation_spawn_war"));
//					}
//
//					if (!nation.isPublic()) {
//                        throw new TownyException(TownySettings.getLangString("msg_err_nation_not_public"));
//                    }
//                }
//            }

            if (!isTownyAdmin) {
                // Prevent spawn travel while in disallowed zones (if
                // configured)
                List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();

                if (!disallowedZones.isEmpty()) {
                    String inTown = null;
                    try {
                        Location loc = plugin.getCache(player).getLastLocation();
                        inTown = TownyUniverse.getTownName(loc);
                    } catch (NullPointerException e) {
                        inTown = TownyUniverse.getTownName(player.getLocation());
                    }

                    if (inTown == null && disallowedZones.contains("unclaimed"))
                        throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_spawn_disallowed_from"), "the Wilderness"));
                    if (inTown != null && resident.hasNation() && TownyUniverse.getDataSource().getTown(inTown).hasNation()) {
                        Nation inNation = TownyUniverse.getDataSource().getTown(inTown).getNation();
                        Nation playerNation = resident.getTown().getNation();
                        if (inNation.hasEnemy(playerNation) && disallowedZones.contains("enemy"))
                            throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_spawn_disallowed_from"), "Enemy areas"));
                        if (!inNation.hasAlly(playerNation) && !inNation.hasEnemy(playerNation) && disallowedZones.contains("neutral"))
                            throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_spawn_disallowed_from"), "Neutral towns"));
                    }
                }
            }

            double travelCost = 0;
            if (nationSpawnPermission == NationSpawnLevel.UNAFFILIATED)
            	travelCost = nationSpawnPermission.getCost(nation);
            else
            	travelCost = nationSpawnPermission.getCost();

            // Check if need/can pay
            if ( (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode())) &&
                    (travelCost > 0 && TownySettings.isUsingEconomy() && (resident.getHoldingBalance() < travelCost)) )
                throw new TownyException(notAffordMSG);

            // Used later to make sure the chunk we teleport to is loaded.
            Chunk chunk = spawnLoc.getChunk();

            // isJailed test
            if (resident.isJailed()) {
                TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_cannot_spawn_while_jailed"));
                return;
            }

            // Essentials tests
            boolean UsingESS = plugin.isEssentials();

            if (UsingESS && !isTownyAdmin) {
                try {
                    User user = plugin.getEssentials().getUser(player);

                    if (!user.isJailed() && !resident.isJailed()) {

                        Teleport teleport = user.getTeleport();
                        if (!chunk.isLoaded())
                            chunk.load();
                        // Cause an essentials exception if in cooldown.
                        teleport.cooldown(true);
                        teleport.teleport(spawnLoc, null);
                    }
                } catch (Exception e) {
                    TownyMessaging.sendErrorMsg(player, "Error: " + e.getMessage());
                    // cooldown?
                    return;
                }
            }


            // Show message if we are using Vault and are charging for spawn travel.
            if ( !TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode()) ) {
                TownyEconomyObject payee = nation;
                if (!TownySettings.isTownSpawnPaidToTown())
                    payee = TownyEconomyObject.SERVER_ACCOUNT;
                if (travelCost > 0 && TownySettings.isUsingEconomy() && resident.payTo(travelCost, payee, String.format("Nation Spawn (%s)", nationSpawnPermission))) {
                    TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"), TownyEconomyHandler.getFormattedBalance(travelCost)));
                }
            }

            // If an Admin or Essentials teleport isn't being used, use our own.
            if (isTownyAdmin) {
                if (player.getVehicle() != null)
                    player.getVehicle().eject();
                if (!chunk.isLoaded())
                    chunk.load();
                player.teleport(spawnLoc, PlayerTeleportEvent.TeleportCause.COMMAND);
                return;
            }

            if (!UsingESS) {
                if (TownyTimerHandler.isTeleportWarmupRunning()) {
                    // Use teleport warmup
                    player.sendMessage(String.format(TownySettings.getLangString("msg_nation_spawn_warmup"), TownySettings.getTeleportWarmupTime()));
                    plugin.getTownyUniverse().requestTeleport(player, spawnLoc, travelCost);
                } else {
                    // Don't use teleport warmup
                    if (player.getVehicle() != null)
                        player.getVehicle().eject();
                    if (!chunk.isLoaded())
                        chunk.load();
                    player.teleport(spawnLoc, PlayerTeleportEvent.TeleportCause.COMMAND);
                }
            }
        } catch (TownyException e) {
            TownyMessaging.sendErrorMsg(player, e.getMessage());
        } catch (EconomyException e) {
            TownyMessaging.sendErrorMsg(player, e.getMessage());
        }
    }

}
