package com.palmergames.bukkit.towny.command;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.questioner.PurgeQuestionTask;
import com.palmergames.bukkit.towny.tasks.ResidentPurge;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.towny.utils.AreaSelectionUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.NameValidation;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeTools;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> ta_help = new ArrayList<String>();
	private static final List<String> ta_panel = new ArrayList<String>();
	private static final List<String> ta_unclaim = new ArrayList<String>();

	private boolean isConsole;
	private Player player;
	private CommandSender sender;

	static {
		ta_help.add(ChatTools.formatTitle("/townyadmin"));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "", TownySettings.getLangString("admin_panel_1")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "set [] .. []", "'/townyadmin set' " + TownySettings.getLangString("res_5")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "unclaim [radius]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "town/nation", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "givebonus [town/player] [num]", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "toggle peaceful/war/debug/devmode", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "resident/town/nation", ""));

		// TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin",
		// "npc rename [old name] [new name]", ""));
		// TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin",
		// "npc list", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reload", TownySettings.getLangString("admin_panel_2")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reset", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "backup", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newday", TownySettings.getLangString("admin_panel_3")));
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

	private Object getSender() {

		if (isConsole)
			return sender;
		else
			return player;
	}

	public boolean parseTownyAdminCommand(String[] split) throws TownyException {

		if (split.length == 0) {
			if (getSender()==player && !TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_ADMIN.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
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

			}

			if ((!isConsole) && (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode(split[0].toLowerCase()))))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[0].equalsIgnoreCase("givebonus")) {

				giveBonus(StringMgmt.remFirstArg(split));

			} else if (split[0].equalsIgnoreCase("reload")) {

				reloadTowny(false);

			} else if (split[0].equalsIgnoreCase("reset")) {

				reloadTowny(true);

			} else if (split[0].equalsIgnoreCase("backup")) {

				try {
					TownyUniverse.getDataSource().backup();
					TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("mag_backup_success"));

				} catch (IOException e) {
					TownyMessaging.sendErrorMsg(getSender(), "Error: " + e.getMessage());

				}

			} else if (split[0].equalsIgnoreCase("newday")) {

				TownyTimerHandler.newDay();

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

			} else {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_sub"));
				return false;
			}
		}

		return true;
	}

	private void giveBonus(String[] split) throws TownyException {

		Town town;

		try {
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: givebonus [town/player] [n]"));
			try {
				town = TownyUniverse.getDataSource().getTown(split[0]);
			} catch (NotRegisteredException e) {
				town = TownyUniverse.getDataSource().getResident(split[0]).getTown();
			}
			try {
				town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[1].trim()));
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_give_total"), town.getName(), split[1], town.getBonusBlocks()));
				TownyMessaging.sendTownMessagePrefixed(town, "You have been given " + Integer.parseInt(split[1].trim()) + " bonus townblocks.");
				TownyMessaging.sendTownMessagePrefixed(town, "If you have paid any real-life money for these townblocks please understand: the creators of Towny do not condone this transaction, the server you play on breaks the Minecraft EULA and, worse, is selling a part of Towny which your server admin did not create.");
				TownyMessaging.sendTownMessagePrefixed(town, "You should consider changing servers and requesting a refund of your money.");
			} catch (NumberFormatException nfe) {
				throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
			}
			TownyUniverse.getDataSource().saveTown(town);
		} catch (TownyException e) {
			throw new TownyException(e.getMessage());
		}

	}

	private void buildTAPanel() {

		ta_panel.clear();
		Runtime run = Runtime.getRuntime();
		ta_panel.add(ChatTools.formatTitle(TownySettings.getLangString("ta_panel_1")));
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_2") + Colors.LightGreen + TownyUniverse.isWarTime() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_3") + (TownyTimerHandler.isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off") + Colors.Gray + " | " + (Colors.Green + TownySettings.getLangString("ta_panel_5") + (TownyTimerHandler.isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")));
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
				if (TownyUniverse.isWarTime())
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
		if (split.length == 0 || split[0].equalsIgnoreCase("?")){
			sender.sendMessage(ChatTools.formatTitle("/townyadmin resident"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin resident", "[resident] rename [newname]", ""));
			
			return;
		}

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(split[0]);

			if (split.length == 1){
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(resident, player));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_RESIDENT.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if(split[1].equalsIgnoreCase("rename")){	
				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renamePlayer(resident, split[2]);
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {	
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}
	
	public void parseAdminTownCommand(String[] split) throws TownyException {

		// TODO Make this use the actual town command procedually.

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] add/kick [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] delete", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] spawn", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] outpost #", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] rank", "")); 

			return;
		}

		try {
			
			Town town = TownyUniverse.getDataSource().getTown(split[0]);
			
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(town));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				TownCommand.townAdd(getSender(), town, StringMgmt.remArgs(split, 2));

			} else if (split[1].equalsIgnoreCase("kick")) {

				TownCommand.townKickResidents(getSender(), town.getMayor(), town, TownyUniverse.getValidatedResidents(getSender(), StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {

				TownyUniverse.getDataSource().removeTown(town);
				TownyMessaging.sendMessage(sender, town + " deleted.");

			} else if (split[1].equalsIgnoreCase("rename")) {

				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renameTown(town, split[2]);
					TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_town_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), town.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
				
			} else if (split[1].equalsIgnoreCase("spawn")) {

				TownCommand.townSpawn(player, StringMgmt.remArgs(split, 2), town, "", false);

			} else if (split[1].equalsIgnoreCase("outpost")) {

				TownCommand.townSpawn(player, StringMgmt.remArgs(split, 2), town, "", true);

			} else if (split[1].equalsIgnoreCase("rank")) {
				
				parseAdminTownRankCommand(player, town, StringMgmt.remArgs(split, 2));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}

	}

	private void parseAdminTownRankCommand(Player player, Town town, String[] split) throws TownyException {

		/*
		 * Does the command have enough arguments?
		 */
		if (split.length < 3)
			throw new TownyException("Eg: /townyadmin town [townname] rank add/remove [resident] [rank]");

		Resident target;
		
		try {

			target = TownyUniverse.getDataSource().getResident(split[1]);
			if (!target.hasTown()) {
				throw new TownyException(TownySettings.getLangString("msg_resident_not_your_town"));
			}
			if (target.getTown() != town) {
				throw new TownyException(TownySettings.getLangString("msg_err_townadmintownrank_wrong_town"));
			}
				
		} catch (TownyException x) {
			throw new TownyException(x.getMessage());
		}

		String rank = split[2].toLowerCase();
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
		TownyUniverse.getDataSource().saveResident(target);
		
	}

	public void parseAdminNationCommand(String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			sender.sendMessage(ChatTools.formatTitle("/townyadmin nation"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] add [] .. []", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] rename [newname]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] delete", ""));

			return;
		}
		try {
			Nation nation = TownyUniverse.getDataSource().getNation(split[0]);
			if (split.length == 1) {
				TownyMessaging.sendMessage(getSender(), TownyFormatter.getStatus(nation));
				return;
			}

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION.getNode(split[1].toLowerCase())))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			if (split[1].equalsIgnoreCase("add")) {
				/*
				 * if (isConsole) { sender.sendMessage(
				 * "[Towny] InputError: This command was designed for use in game only."
				 * ); return; }
				 */
				NationCommand.nationAdd(nation, TownyUniverse.getDataSource().getTowns(StringMgmt.remArgs(split, 2)));

			} else if (split[1].equalsIgnoreCase("delete")) {

				TownyUniverse.getDataSource().removeNation(nation);

			} else if (split[1].equalsIgnoreCase("rename")) {

				if (!NameValidation.isBlacklistName(split[2])) {
					TownyUniverse.getDataSource().renameNation(nation, split[2]);
					TownyMessaging.sendNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_set_name"), ((getSender() instanceof Player) ? player.getName() : "CONSOLE"), nation.getName()));
				} else
					TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_invalid_name"));
			}

		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		} catch (AlreadyRegisteredException e) {
			TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
		}
	}

	public void adminSet(String[] split) throws TownyException {

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin set"));
			// TODO: player.sendMessage(ChatTools.formatCommand("",
			// "/townyadmin set", "king [nation] [king]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] " + TownySettings.getLangString("town_help_2"), ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "capital [town]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "title [resident] [title]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "surname [resident] [surname]", ""));

			return;
		}

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_SET.getNode(split[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("mayor")) {
			if (split.length < 3) {
				
				sender.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] " + TownySettings.getLangString("town_help_2"), ""));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));
				
			} else
				try {
					Resident newMayor = null;
					Town town = TownyUniverse.getDataSource().getTown(split[1]);

					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						TownyUniverse.getDataSource().newResident(name);

						newMayor = TownyUniverse.getDataSource().getResident(name);

						newMayor.setRegistered(System.currentTimeMillis());
						newMayor.setLastOnline(0);
						newMayor.setNPC(true);

						TownyUniverse.getDataSource().saveResident(newMayor);
						TownyUniverse.getDataSource().saveResidentList();

						// set for no upkeep as an NPC mayor is assigned
						town.setHasUpkeep(false);

					} else {
						newMayor = TownyUniverse.getDataSource().getResident(split[2]);

						// set upkeep again
						town.setHasUpkeep(true);
					}

					if (!town.hasResident(newMayor))
						TownCommand.townAddResident(town, newMayor);
					// Delete the resident if the old mayor was an NPC.
					Resident oldMayor = town.getMayor();

					town.setMayor(newMayor);

					if (oldMayor.isNPC()) {
						try {
							town.removeResident(oldMayor);
							TownyUniverse.getDataSource().removeResident(oldMayor);

							TownyUniverse.getDataSource().removeResidentList(oldMayor);

						} catch (EmptyTownException e) {
							// Should never reach here as we are setting a new
							// mayor before removing the old one.
							e.printStackTrace();
						}
					}
					TownyUniverse.getDataSource().saveTown(town);
					String[] msg = TownySettings.getNewMayorMsg(newMayor.getName());
					TownyMessaging.sendTownMessage(town, msg);
					// TownyMessaging.sendMessage(player, msg);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(getSender(), e.getMessage());
				}
			
		} else if (split[0].equalsIgnoreCase("capital")) {

			if (split.length < 2) {
				
				sender.sendMessage(ChatTools.formatTitle("/townyadmin set capital"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/ta set capital", "[town name]", ""));
				
			} else {
				
				try {
					Town newCapital = TownyUniverse.getDataSource().getTown(split[1]);
					
			        if ((TownySettings.getNumResidentsCreateNation() > 0) && (newCapital.getNumResidents() < TownySettings.getNumResidentsCreateNation())) {
			            TownyMessaging.sendErrorMsg(this.player, String.format(TownySettings.getLangString("msg_not_enough_residents_capital"), newCapital.getName()));
			            return;
			        }
			        
					Nation nation = newCapital.getNation();
					
					nation.setCapital(newCapital);
					plugin.resetCache();
					
					TownyMessaging.sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName(), nation.getName()));
					
					TownyUniverse.getDataSource().saveNation(nation);
					TownyUniverse.getDataSource().saveNationList();
					
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}
				
			}
		} else if (split[0].equalsIgnoreCase("title")) {
			Resident resident = null;
			// Give the resident a title
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set title bilbo Jester");
			else
				resident = TownyUniverse.getDataSource().getResident(split[1]);
			
			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String title = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setTitle(title + " ");
			TownyUniverse.getDataSource().saveResident(resident);

			if (resident.hasTitle()){
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_title"), resident.getName(), resident.getTitle()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Title", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Title", resident.getName()));
			}
			
		} else if (split[0].equalsIgnoreCase("surname")) {
			Resident resident = null;
			// Give the resident a surname
			if (split.length < 2)
				TownyMessaging.sendErrorMsg(player, "Eg: /townyadmin set surname bilbo Jester");
			else
				resident = TownyUniverse.getDataSource().getResident(split[1]);
			
			split = StringMgmt.remArgs(split, 2);
			if (StringMgmt.join(split).length() > TownySettings.getMaxTitleLength()) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_input_too_long"));
				return;
			}

			String surname = StringMgmt.join(NameValidation.checkAndFilterArray(split));
			resident.setSurname(surname + " ");
			TownyUniverse.getDataSource().saveResident(resident);

			if (resident.hasSurname()){
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_set_surname"), resident.getName(), resident.getSurname()));
			} else {
				TownyMessaging.sendMessage(sender, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Surname", resident.getName()));
				TownyMessaging.sendMessage(resident, String.format(TownySettings.getLangString("msg_clear_title_surname"), "Surname", resident.getName()));
			}

		} else {
			TownyMessaging.sendErrorMsg(getSender(), String.format(TownySettings.getLangString("msg_err_invalid_property"), "administrative"));
			return;
		}
	}

	public String nextNpcName() throws TownyException {

		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!TownyUniverse.getDataSource().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException(TownySettings.getLangString("msg_err_too_many_npc"));
		} while (true);
	}

	public void reloadTowny(Boolean reset) {

		if (reset) {
			TownyUniverse.getDataSource().deleteFile(plugin.getConfigPath());
		}
		TownyLogger.shutDown();
		if (plugin.load()) {
			
			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
			
			// Update permissions for all online players
			TownyPerms.updateOnlinePerms();
						
		}

		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded"));
		// TownyMessaging.sendMsg(TownySettings.getLangString("msg_reloaded"));
	}

	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split
	 */
	public void purge(String[] split) {

		if (split.length == 0) {
			// command was '/townyadmin purge'
			player.sendMessage(ChatTools.formatTitle("/townyadmin purge"));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin purge", "[number of days]", ""));
			player.sendMessage(ChatTools.formatCommand("", "", "Removes offline residents not seen for this duration.", ""));

			return;
		}

		int days = 1;

		try {
			days = Integer.parseInt(split[0]);
		} catch (NumberFormatException e) {
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_error_must_be_int"));
			return;
		}
		
		
		// Use questioner to confirm.
		Plugin test = BukkitTools.getServer().getPluginManager().getPlugin("Questioner");

		if (this.sender instanceof Player && TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner) test;
			questioner.loadClasses();

			List<Option> options = new ArrayList<Option>();
			options.add(new Option(TownySettings.questionerAccept(), new PurgeQuestionTask(plugin, this.sender, TimeTools.getMillis(days + "d"))));
			options.add(new Option(TownySettings.questionerDeny(), new PurgeQuestionTask(plugin, this.sender, TimeTools.getMillis(days + "d")) {

				@Override
				public void run() {

					TownyMessaging.sendMessage(getSender(), "Purge Aborted!");
				}
			}));
			
			Question question = new Question(this.sender.getName(), "Do you really want to perform this purge", options);
			
			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else {

			// Run a purge in it's own thread
			new ResidentPurge(plugin, this.sender, TimeTools.getMillis(days + "d")).start();
		}

	}

	/**
	 * Delete a resident and it's data file (if not online) Available Only to
	 * players with the 'towny.admin' permission node.
	 * 
	 * @param player
	 * @param split
	 */
	public void residentDelete(Player player, String[] split) {

		if (split.length == 0)
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
		else
			try {
				if (!TownyUniverse.getPermissionSource().isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete"));

				for (String name : split) {
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(name);
						if (!resident.isNPC() && !BukkitTools.isOnline(resident.getName())) {
							TownyUniverse.getDataSource().removeResident(resident);
							TownyUniverse.getDataSource().removeResidentList(resident);
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
				return;
			}
	}

	public void parseToggleCommand(String[] split) throws TownyException {

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

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOGGLE.getNode(split[0].toLowerCase())))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		if (split[0].equalsIgnoreCase("war")) {
			choice = TownyUniverse.isWarTime();

			if (!choice) {
				plugin.getTownyUniverse().startWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_started"));
			} else {
				plugin.getTownyUniverse().endWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_ended"));
			}
		} else if (split[0].equalsIgnoreCase("peaceful") || split[0].equalsIgnoreCase("neutral")) {

			try {
				choice = !TownySettings.isDeclaringNeutral();
				TownySettings.setDeclaringNeutral(choice);
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_nation_allow_peaceful"), choice ? "Enabled" : "Disabled"));

			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
				return;
			}

		} else if (split[0].equalsIgnoreCase("devmode")) {
			try {
				choice = !TownySettings.isDevMode();
				TownySettings.setDevMode(choice);
				TownyMessaging.sendMsg(getSender(), "Dev Mode " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("debug")) {
			try {
				choice = !TownySettings.getDebug();
				TownySettings.setDebug(choice);
				TownyMessaging.sendMsg(getSender(), "Debug Mode " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("townwithdraw")) {
			try {
				choice = !TownySettings.getTownBankAllowWithdrawls();
				TownySettings.SetTownBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "Town Withdrawls " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
		} else if (split[0].equalsIgnoreCase("nationwithdraw")) {
			try {
				choice = !TownySettings.geNationBankAllowWithdrawls();
				TownySettings.SetNationBankAllowWithdrawls(choice);
				TownyMessaging.sendMsg(getSender(), "Nation Withdrawls " + (choice ? Colors.Green + "Enabled" : Colors.Red + "Disabled"));
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
			}
			
		} else if (split[0].equalsIgnoreCase("npc")) {
			
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: toggle npc [resident]"));
			
			try {
				Resident resident = TownyUniverse.getDataSource().getResident(split[1]);
				resident.setNPC(!resident.isNPC());
				
				TownyUniverse.getDataSource().saveResident(resident);
				
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
	/*
	 * private void warSeed(Player player) { Resident r1 =
	 * plugin.getTownyUniverse().newResident("r1"); Resident r2 =
	 * plugin.getTownyUniverse().newResident("r2"); Resident r3 =
	 * plugin.getTownyUniverse().newResident("r3"); Coord key =
	 * Coord.parseCoord(player); Town t1 = newTown(plugin.getTownyUniverse(),
	 * player.getWorld(), "t1", r1, key, player.getLocation()); Town t2 =
	 * newTown(plugin.getTownyUniverse(), player.getWorld(), "t2", r2, new
	 * Coord(key.getX() + 1, key.getZ()), player.getLocation()); Town t3 =
	 * newTown(plugin.getTownyUniverse(), player.getWorld(), "t3", r3, new
	 * Coord(key.getX(), key.getZ() + 1), player.getLocation()); Nation n1 =
	 * 
	 * }
	 * 
	 * public void seedTowny() { TownyUniverse townyUniverse =
	 * plugin.getTownyUniverse(); Random r = new Random(); for (int i = 0; i <
	 * 1000; i++) {
	 * 
	 * try { townyUniverse.newNation(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } try {
	 * townyUniverse.newTown(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } try {
	 * townyUniverse.newResident(Integer.toString(r.nextInt())); } catch
	 * (TownyException e) { } } }
	 * 
	 * private static double getTotalEconomy() { double total = 0; try { return
	 * total; } catch (Exception e) { } return total; }
	 * 
	 * private static int getNumBankAccounts() { try { return 0; } catch
	 * (Exception e) { return 0; } }
	 */
}
