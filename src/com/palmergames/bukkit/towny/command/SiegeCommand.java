package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;
import com.palmergames.bukkit.towny.war.siegewar.Siege;
import com.palmergames.bukkit.towny.war.siegewar.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.*;


public class SiegeCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> siegeHelp = new ArrayList<>();

	static {
		siegeHelp.add(ChatTools.formatTitle("/siege"));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "list", TownySettings.getLangString("siege_help_1")));
		siegeHelp.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/siege", "attack", TownySettings.getLangString("siege_help_3")));
		siegeHelp.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/siege", "abandon " + TownySettings.getLangString("siege_help_2"), TownySettings.getLangString("siege_help_4")));
		siegeHelp.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/siege", "plunder " + TownySettings.getLangString("siege_help_2"), TownySettings.getLangString("siege_help_5")));
		siegeHelp.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/siege", "invade " + TownySettings.getLangString("siege_help_2"), TownySettings.getLangString("siege_help_6")));
		siegeHelp.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/siege", "surrender", TownySettings.getLangString("siege_help_7")));
	}

	public SiegeCommand(Towny instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (args == null) {
				for (String line : siegeHelp)
					player.sendMessage(line);
				parseSiegeCommand(player, args);
			} else {
				parseSiegeCommand(player, args);
			}

		} else
			try {
				parseSiegeCommandForConsole(sender, args);
			} catch (TownyException e) {
			}

		return true;
	}

	private void parseSiegeCommandForConsole(final CommandSender sender, String[] split) throws TownyException {

		if (split.length == 0 || split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {

			for (String line : siegeHelp)
				sender.sendMessage(line);

		} else if (split[0].equalsIgnoreCase("list")) {

			listSieges(sender, split);

		} else {
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
		}
	}

	private void parseSiegeCommand(final Player player, String[] split) {

		try {

			if (split[0].equalsIgnoreCase("?"))
				for (String line : siegeHelp)
					player.sendMessage(line);

			else if (split[0].equalsIgnoreCase("list")) {
				listSieges(player, split);

			//} else if (split[0].equalsIgnoreCase("attack")) {
			//	evaluateSiegeAttackRequest(player);

			} else if (split[0].equalsIgnoreCase("surrender")) {
				processSurrenderRequest(player);

			} else if (split.length==2 && split[0].equalsIgnoreCase("abandon")) {
				processAbandonRequest(player, split[1]);

			} else {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}



	private void processSurrenderRequest(Player player) {

		try {
			if (!TownySettings.getWarSiegeEnabled())
				throw new TownyException("Siege war feature disabled");  //todo - replace w lang string

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWN_SIEGE_SURRENDER.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			Town town = resident.getTown();

			if(!town.hasSiege())
				throw new TownyException("Your town is not under siege");

			Siege siege = town.getSiege();

			if(siege.getStatus() != SiegeStatus.IN_PROGRESS)
				throw new TownyException("The siege is over");

			if(siege.getActiveAttackers().size() > 1)
				throw new TownyException("You cannot surrender if there is more than one attacker");

			//Surrender
			SiegeWarUtil.defenderSurrender(siege);

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}




	private void processAbandonRequest(Player player, String townName) throws TownyException {
		if (!TownySettings.getWarSiegeEnabled())
			throw new TownyException("Siege war feature disabled");

		if(!TownyUniverse.getDataSource().hasTown(townName))
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), townName));

		if(!TownyUniverse.getDataSource().hasSiege(townName))
			throw new TownyException(String.format(TownySettings.getLangString("msg_err_siege_war_no_siege_on_target_town"), townName));

		if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ABANDON.getNode()))
			throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

		final Siege siege = TownyUniverse.getDataSource().getSiege(townName);

		if(siege.getStatus() != SiegeStatus.IN_PROGRESS)
			throw new TownyException("The siege is already over");

		Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
		Nation nation = resident.getTown().getNation();

		if(!siege.getActiveAttackers().contains(nation))
			throw new TownyException("Your nation is not attacking this town rightnow");

		SiegeWarUtil.attackerAbandon(nation, siege);
	}


	/**
	 * Send a list of all sieges in the universe to player Command: /siege list
	 *
	 * @param sender - Player to send the list to.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void listSieges(CommandSender sender, String[] split) {
		List<Siege> siegesToSort = new ArrayList<>(TownyUniverse.getDataSource().getSieges());

		int page = 1;
	    int total = (int) Math.ceil(((double) siegesToSort.size()) / ((double) 10));
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

	    //Siege with lowest siege timer value goes on top
		Collections.sort(siegesToSort, new Comparator() {
			@Override
			public int compare(Object s1, Object s2) {
				double s1TimeUntilCompletion= ((Siege) s1).getTimeUntilCompletionMillis();
				double s2timeUntilCompletion = ((Siege)s2).getTimeUntilCompletionMillis();

				if(s1TimeUntilCompletion == s2timeUntilCompletion) {
					return 0;
				} else if (s1TimeUntilCompletion > s2timeUntilCompletion) {
					return 1;
				} else {
					return -1;
				}
			}
		});

		int iMax = page * 10;
		if ((page * 10) > siegesToSort.size()) {
			iMax = siegesToSort.size();
		}

		String headerLine = Colors.Red + TownySettings.getLangString("siege_list_header_town")
					+ Colors.Gray + " - "
					+ Colors.LightBlue + "("+TownySettings.getLangString("siege_list_header_victory_timer") + ")"
					+ Colors.Gray + " - "
					+ Colors.LightBlue + "(" + TownySettings.getLangString("siege_list_header_winner") + ")";

		Siege siege;
		Town town;
		String output;
		List<String> siegesOrdered = new ArrayList();
		for (int i = (page - 1) * 10; i < iMax; i++) {
			siege = siegesToSort.get(i);
			town = siege.getDefendingTown();
			output = Colors.Red + town.getName()
					+ Colors.Gray + " - "
					+ Colors.LightBlue + "(" + siege.getFormattedHoursUntilCompletion() + ")"
					+ Colors.Gray + " - "
					+ Colors.LightBlue + "(" + siege.getWinnerName() + ")";
			siegesOrdered.add(output);
		}

		sender.sendMessage(
			ChatTools.formatList(
					TownySettings.getLangString("siege_plu"),
					headerLine,
					siegesOrdered,
					TownySettings.getListPageMsg(page, total)
			));
	}
}
