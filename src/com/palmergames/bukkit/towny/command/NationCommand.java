package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.naming.InvalidNameException;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.questioner.JoinNationTask;
import com.palmergames.bukkit.towny.questioner.ResidentNationQuestionTask;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all nation commands to player Command: /nation ?
 * 
 * @param player
 * 
 *            handles all nation based commands
 */

public class NationCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> nation_help = new ArrayList<String>();
	private static final List<String> king_help = new ArrayList<String>();

	static {

		nation_help.add(ChatTools.formatTitle("/nation"));
		nation_help.add(ChatTools.formatCommand("", "/nation", "", TownySettings.getLangString("nation_help_1")));
		nation_help.add(ChatTools.formatCommand("", "/nation", TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("nation_help_3")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "list", TownySettings.getLangString("nation_help_4")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "online", TownySettings.getLangString("nation_help_9")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/nation", "deposit [$]", ""));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/nation", "leave", TownySettings.getLangString("nation_help_5")));
		if (!TownySettings.isNationCreationAdminOnly())
			nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/nation", "new " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("nation_help_6")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "king ?", TownySettings.getLangString("nation_help_7")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "new " + TownySettings.getLangString("nation_help_2") + " [capital]", TownySettings.getLangString("nation_help_8")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "delete " + TownySettings.getLangString("nation_help_2"), ""));

		king_help.add(ChatTools.formatTitle(TownySettings.getLangString("king_help_1")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "withdraw [$]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "[add/kick] [town] .. [town]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "assistant [add/remove] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_6")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "assistant [add+/remove+] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_7")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "set [] .. []", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "toggle [] .. []", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "ally [add/remove] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_2")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "enemy [add/remove] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_3")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "delete", ""));

	}

	public NationCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			if (args == null) {
				for (String line : nation_help)
					player.sendMessage(line);
				parseNationCommand(player, args);
			} else {
				parseNationCommand(player, args);
			}

		} else
			// Console
			for (String line : nation_help)
				sender.sendMessage(Colors.strip(line));
		return true;
	}

	public void parseNationCommand(Player player, String[] split) {

		String nationCom = "/nation";

		try {

			if (split.length == 0)
				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					Town town = resident.getTown();
					Nation nation = town.getNation();
					TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
				} catch (NotRegisteredException x) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
				}
			else if (split[0].equalsIgnoreCase("?"))
				for (String line : nation_help)
					player.sendMessage(line);
			else if (split[0].equalsIgnoreCase("list")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LIST.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				listNations(player);

			} else if (split[0].equalsIgnoreCase("new")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_NEW.getNode()))
					throw new TownyException(TownySettings.getNotPermToNewNationLine());

				// TODO: Make an overloaded function
				// newNation(Player,String,Town)
				if (split.length == 1)
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_specify_nation_name"));
				else if (split.length == 2) {

					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					if (!resident.isMayor() && !resident.getTown().hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_peasant_right"));
					newNation(player, split[1], resident.getTown().getName());

				} else {
					// TODO: Check if player is an admin
					newNation(player, split[1], split[2]);
				}
			} else if (split[0].equalsIgnoreCase("leave")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_LEAVE.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				nationLeave(player);

			} else if (split[0].equalsIgnoreCase("withdraw")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_WITHDRAW.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 2)
					try {
						nationWithdraw(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom));

			} else if (split[0].equalsIgnoreCase("deposit")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length == 2)
					try {
						nationDeposit(player, Integer.parseInt(split[1].trim()));
					} catch (NumberFormatException e) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
				else
					TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom + " deposit"));

			} else {
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

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ADD.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

					nationAdd(player, newSplit);

				} else if (split[0].equalsIgnoreCase("kick")) {

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_DEPOSIT.getNode()))
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

					if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_ALLY.getNode()))
						throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

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

					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						Town town = resident.getTown();
						Nation nation = town.getNation();
						TownyMessaging.sendMessage(player, TownyFormatter.getFormattedOnlineResidents(TownySettings.getLangString("msg_nation_online"), nation, player));
					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
					}
				} else
					try {
						Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(nation));
					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
					}
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}

	}

	public void nationRank(Player player, String[] split) {

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

			rank = split[2].toLowerCase();
			/*
			 * Is this a known rank?
			 */
			if (!TownyPerms.getNationRanks().contains(rank)) {
				TownyMessaging.sendErrorMsg(player, "Unknown rank '" + rank + "'. Permissible ranks are :- " + StringMgmt.join(TownyPerms.getNationRanks(), ",") + ".");
				return;
			}
			/*
			 * Only allow the player to assign ranks if they have the grant perm
			 * for it.
			 */
			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank))) {
				TownyMessaging.sendErrorMsg(player, "You do not have permission to modify this rank.");
				return;
			}

			if (split[0].equalsIgnoreCase("add")) {
				try {
					if (target.addNationRank(rank)) {
						TownyMessaging.sendMsg(target, "You have been granted the Nation rank of '" + rank + "'.");
						TownyMessaging.sendMsg(player, "You have granted the Nation rank of '" + rank + "' to " + target.getName() + ".");
					} else {
						// Not in a nation or Rank doesn't exist
						TownyMessaging.sendErrorMsg(player, "That resident isn't a member of a town!");
						return;
					}
				} catch (AlreadyRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, target.getName() + " already holds this Nation rank.");
					return;
				}

			} else if (split[0].equalsIgnoreCase("remove")) {
				try {
					if (target.removeNationRank(rank)) {
						TownyMessaging.sendMsg(target, "You have been demoted from the Nation rank of '" + rank + "'.");
						TownyMessaging.sendMsg(player, "You have removed the Nation rank of '" + rank + "' from " + target.getName() + ".");
					}
				} catch (NotRegisteredException e) {
					// Must already have this rank
					TownyMessaging.sendMsg(player, target.getName() + " doesn't hold this Nation rank.");
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
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money")); // TODO

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
	 * @param player
	 */

	public void listNations(Player player) {

		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("nation_plu")));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : TownyUniverse.getDataSource().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}

	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 * 
	 * @param player
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
				throw new TownyException(TownySettings.getLangString("msg_no_funds_new_nation"));

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
		if (TownySettings.isUsingEconomy()) {
			//TODO
			try {
				nation.setBalance(0, "Deleting Nation");
			} catch (EconomyException e) {
				e.printStackTrace();
			}
		}
		TownyUniverse.getDataSource().saveTown(town);
		TownyUniverse.getDataSource().saveNation(nation);
		TownyUniverse.getDataSource().saveNationList();

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
			 * Remove all resident titles before saving the town itself.
			 */
			List<Resident> titleRemove = new ArrayList<Resident>(town.getResidents());

			for (Resident res : titleRemove) {
				if (res.hasTitle() || res.hasSurname()) {
					res.setTitle("");
					res.setSurname("");
					TownyUniverse.getDataSource().saveResident(res);
				}
			}

			TownyUniverse.getDataSource().saveNation(nation);
			TownyUniverse.getDataSource().saveNationList();

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
				Town town = resident.getTown();
				Nation nation = town.getNation();

				TownyUniverse.getDataSource().removeNation(nation);
				TownyMessaging.sendGlobalMessage(TownySettings.getDelNationMsg(nation));
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

	public void nationAdd(Player player, String[] names) {

		if (names.length < 1) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation add [names]");
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

		nationAdd(player, nation, TownyUniverse.getDataSource().getTowns(names));

	}

	public static void nationAdd(Player player, Nation nation, List<Town> invited) {

		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : invited)
			try {
				// nation.addTown(town);

				nationInviteTown(player, nation, town);
			} catch (AlreadyRegisteredException e) {
				remove.add(town);
			}
		for (Town town : remove)
			invited.remove(town);

		if (invited.size() > 0) {
			String msg = "";

			for (Town town : invited)
				msg += town.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);
			msg = String.format(TownySettings.getLangString("msg_invited_join_nation"), player.getName(), msg);
			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
			// plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

	private static void nationInviteTown(Player player, Nation nation, Town town) throws AlreadyRegisteredException {

		Plugin test = plugin.getServer().getPluginManager().getPlugin("Questioner");

		Resident townMayor = town.getMayor();

		if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner) test;
			questioner.loadClasses();

			List<Option> options = new ArrayList<Option>();
			options.add(new Option(TownySettings.questionerAccept(), new JoinNationTask(townMayor, nation)));
			options.add(new Option(TownySettings.questionerDeny(), new ResidentNationQuestionTask(townMayor, nation) {

				@Override
				public void run() {

					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_deny_invite"), getResident().getName()));
				}
			}));
			Question question = new Question(townMayor.getName(), String.format(TownySettings.getLangString("msg_invited"), nation.getName()), options);
			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {

			nation.addTown(town);
			plugin.resetCache();
			TownyUniverse.getDataSource().saveTown(town);
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

				msg = msg.substring(0, msg.length() - 2);
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

	public void nationAlly(Player player, String[] split) {

		if (split.length < 2) {
			TownyMessaging.sendErrorMsg(player, "Eg: /nation ally [add/remove] [name]");
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
		Nation ally;
		// test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);

		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				try {
					ally = TownyUniverse.getDataSource().getNation(name);
					if (nation.equals(ally))
						TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_own_nation_disallow"));
					else
						list.add(ally);
				} catch (NotRegisteredException e) {
					// Do nothing here as the name doesn't match a Nation
				}
			}
			if (!list.isEmpty())
				nationAlly(player, nation, list, test.equalsIgnoreCase("add"));

		} else {
			TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "[add/remove]"));
		}

	}

	public void nationAlly(Player player, Nation nation, List<Nation> allies, boolean add) {

		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : allies)
			try {
				if (add && !nation.getAllies().contains(targetNation)) {
					if (!targetNation.hasEnemy(nation)) {
						// We are not set as an enemy so we can set as ally.
						nation.addAlly(targetNation);
						TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_ally"), nation.getName()));
					} else {
						// We are set as an enemy so can't ally.
						remove.add(targetNation);
						TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_unable_ally_enemy"), targetNation.getName()));
					}
				} else if (nation.getAllies().contains(targetNation)) {
					nation.removeAlly(targetNation);
					TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_ally"), nation.getName()));
					// Remove any mirrored ally settings from the target nation
					if (targetNation.hasAlly(nation))
						nationAlly(player, targetNation, Arrays.asList(nation), false);
				}

			} catch (AlreadyRegisteredException e) {
				remove.add(targetNation);
			} catch (NotRegisteredException e) {
				remove.add(targetNation);
			}

		for (Nation newAlly : remove)
			allies.remove(newAlly);

		if (allies.size() > 0) {
			String msg = "";

			for (Nation newAlly : allies)
				msg += newAlly.getName() + ", ";

			msg = msg.substring(0, msg.length() - 2);
			if (add)
				msg = String.format(TownySettings.getLangString("msg_allied_nations"), player.getName(), msg);
			else
				msg = String.format(TownySettings.getLangString("msg_broke_alliance"), player.getName(), msg);

			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

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
				nationEnemy(player, nation, list, test.equalsIgnoreCase("add"));

		} else {
			TownyMessaging.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "[add/remove]"));
		}

	}

	public void nationEnemy(Player player, Nation nation, List<Nation> enemies, boolean add) {

		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : enemies)
			try {
				if (add && !nation.getEnemies().contains(targetNation)) {
					nation.addEnemy(targetNation);
					TownyMessaging.sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_enemy"), nation.getName()));
					// Remove any ally settings from the target nation
					if (targetNation.hasAlly(nation))
						nationAlly(player, targetNation, Arrays.asList(nation), false);

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
				msg = String.format(TownySettings.getLangString("msg_enemy_nations"), player.getName(), msg);
			else
				msg = String.format(TownySettings.getLangString("msg_enemy_to_neutral"), player.getName(), msg);

			TownyMessaging.sendNationMessage(nation, ChatTools.color(msg));
			TownyUniverse.getDataSource().saveNations();

			plugin.resetCache();
		} else
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

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
						nation.setKing(newKing);
						plugin.deleteCache(oldKingsName);
						plugin.deleteCache(newKing.getName());
						TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newKing.getName(), nation.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}
			} else if (split[0].equalsIgnoreCase("capital")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_CAPITOL.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set capital {town name}");
				else
					try {
						Town newCapital = TownyUniverse.getDataSource().getTown(split[1]);
						nation.setCapital(newCapital);
						plugin.resetCache();
						TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName(), nation.getName()));
					} catch (TownyException e) {
						TownyMessaging.sendErrorMsg(player, e.getMessage());
					}

			} else if (split[0].equalsIgnoreCase("taxes")) {

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

			} else if (split[0].equalsIgnoreCase("name")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SET_NAME.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				if (split.length < 2)
					TownyMessaging.sendErrorMsg(player, "Eg: /nation set name Plutoria");
				else {

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
			player.sendMessage(ChatTools.formatCommand("", "/nation toggle", "neutral", ""));
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

			if (split[0].equalsIgnoreCase("neutral")) {

				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_TOGGLE_NEUTRAL.getNode()))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

				try {
					if (!TownySettings.isDeclaringNeutral())
						throw new TownyException(TownySettings.getLangString("msg_neutral_disabled"));

					boolean choice = !nation.isNeutral();
					Double cost = TownySettings.getNationNeutralityCost();

					if (choice && TownySettings.isUsingEconomy() && !nation.pay(cost, "Nation Neutrality Cost"))
						throw new TownyException(TownySettings.getLangString("msg_nation_cant_neutral"));

					nation.setNeutral(choice);

					// send message depending on if using IConomy and charging
					// for neutral
					if (TownySettings.isUsingEconomy() && cost > 0)
						TownyMessaging.sendMsg(player, String.format(TownySettings.getLangString("msg_you_paid"), TownyEconomyHandler.getFormattedBalance(cost)));
					else
						TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_nation_set_neutral"));

					TownyMessaging.sendNationMessage(nation, TownySettings.getLangString("msg_nation_neutral") + (nation.isNeutral() ? Colors.Green : Colors.Red + " not") + " neutral.");
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

}
