package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.SiegeWarUtil;
import com.palmergames.bukkit.towny.war.siegewar.Siege;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;


public class SiegeCommand extends BaseCommand implements CommandExecutor {

	private static Towny plugin;
	private static final List<String> siegeHelp = new ArrayList<>();

	static {
		siegeHelp.add(ChatTools.formatTitle("/siege"));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "list", TownySettings.getLangString("siege_help_1")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "", TownySettings.getLangString("siege_help_2")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "here", TownySettings.getLangString("siege_help_3")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", TownySettings.getLangString("siege_help_4"), TownySettings.getLangString("siege_help_5")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "attack", TownySettings.getLangString("siege_help_6")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "abandon " + TownySettings.getLangString("siege_help_4"), TownySettings.getLangString("siege_help_7")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "plunder " + TownySettings.getLangString("siege_help_4"), TownySettings.getLangString("siege_help_8")));
		siegeHelp.add(ChatTools.formatCommand("", "/siege", "surrender", TownySettings.getLangString("siege_help_9")));
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
			try {
				final Siege siege = TownyUniverse.getDataSource().getSiege(split[0]);
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> TownyMessaging.sendMessage(sender, TownyFormatter.getStatus(siege)));

			} catch (NotRegisteredException x) {
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
			}
		}
	}

	private void parseSiegeCommand(final Player player, String[] split) {

		try {

			if (split.length == 0)
				Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						Town town = resident.getTown();

						if(resident.hasTown())
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_town"));

						if(!town.hasSiege())
							TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_no_siege_on_your_town"));

						Siege siege = town.getSiege();
						TownyMessaging.sendMessage(player, TownyFormatter.getStatus(siege));
					} catch (NotRegisteredException x) {
						//Exception (no town) dealt with in block
					}
				});

			else if (split[0].equalsIgnoreCase("?"))
				for (String line : siegeHelp)
					player.sendMessage(line);

			else if (split[0].equalsIgnoreCase("list")) {
				listSieges(player, split);

			} else if (split[0].equalsIgnoreCase("attack")) {
				processAttackRequest(player);
			}

		} catch (Exception x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}


	//////////////////// PROCESS ATTACK REQUESTS //////////////////////////////////////


	private void processAttackRequest(Player player) {

		try {
			if (!TownySettings.getWarSiegeEnabled())
				throw new TownyException("Siege war feature disabled");  //todo - replace w lang string

			if (!TownySettings.getWarSiegeAllowSieges())
				throw new TownyException("Sieges not allowed");

			if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_NATION_SIEGE_ASSAULT_START.getNode()))
				throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));

			TownBlock townBlockWherePlayerIsLocated = TownyUniverse.getTownBlockWherePlayerIsLocated(player);
			if (townBlockWherePlayerIsLocated == null)
				throw new TownyException("You must be standing in a town to attack the town.");

			if(!SiegeWarUtil.isTownBlockOnTheTownBorder(townBlockWherePlayerIsLocated))
				throw new TownyException("You must be in a town border block to attack the town.");

			Town defendingTown = townBlockWherePlayerIsLocated.getTown();
			Nation nationOfAttackingPlayer = TownyUniverse.getNationOfPlayer(player);

			if (defendingTown.hasNation()) {
				Nation nationOfDefendingTown = defendingTown.getNation();

				if(nationOfAttackingPlayer == nationOfDefendingTown)
					throw new TownyException("You cannot attack a town in your own nation.");

				if (!nationOfAttackingPlayer.hasEnemy(nationOfDefendingTown))
					throw new TownyException("You cannot attack a town unless the nation of that town is an enemy of your nation.");
			}

			if (nationOfAttackingPlayer.isNationAttackingTown(defendingTown))
				throw new TownyException("Your nation is already attacking this town.");

			if (defendingTown.isSiegeCooldownActive()) {
				throw new TownyException(
						"This town is in a siege cooldown period. It cannot be attacked for " +
								defendingTown.getHoursUntilSiegeCooldownEnds() + " hours");
			}

			if (TownySettings.isUsingEconomy()) {
				double initialSiegeCost =
						TownySettings.getWarSiegeAttackerCostUpFrontPerPlot()
								* defendingTown.getTownBlocks().size();

				if (nationOfAttackingPlayer.canPayFromHoldings(initialSiegeCost))
					//Deduct upfront cost
					nationOfAttackingPlayer.pay(initialSiegeCost, "Cost of Initiating an assault siege.");
				else {
					throw new TownyException(TownySettings.getLangString("msg_err_no_money."));
				}
			}

			if (player.isFlying())
				throw new TownyException("You cannot be flying to start a siege.");

			if (SiegeWarUtil.doesPlayerHaveANonAirBlockAboveThem(player))
				throw new TownyException("The god(s) favour wars on the land surface. You must have only sky above you to start a siege.");

			if (TownySettings.getNationRequiresProximity() > 0) {
				Coord capitalCoord = nationOfAttackingPlayer.getCapital().getHomeBlock().getCoord();
				Coord townCoord = defendingTown.getHomeBlock().getCoord();
				if (!nationOfAttackingPlayer.getCapital().getHomeBlock().getWorld().getName().equals(defendingTown.getHomeBlock().getWorld().getName())) {
					throw new TownyException("This town cannot join your nation because the capital of your your nation is in a different world.");
				}
				double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
				if (distance > TownySettings.getNationRequiresProximity()) {
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_town_not_close_enough_to_nation"), defendingTown.getName()));
				}
			}

			if (TownySettings.getMaxTownsPerNation() > 0) {
				if (nationOfAttackingPlayer.getTowns().size() >= TownySettings.getMaxTownsPerNation()){
					throw new TownyException(String.format(TownySettings.getLangString("msg_err_nation_over_town_limit"), TownySettings.getMaxTownsPerNation()));
				}
			}

			//Setup attack
			SiegeWarUtil.attackTown(nationOfAttackingPlayer, defendingTown);

		} catch (TownyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		} catch (EconomyException x) {
			TownyMessaging.sendErrorMsg(player, x.getMessage());
		}
	}



	/**
	 * Send a list of all sieges in the universe to player Command: /siege list
	 *
	 * @param sender - Player to send the list to.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void listSieges(CommandSender sender, String[] split) {
		List<Siege> siegesToSort = TownyUniverse.getDataSource().getSieges();

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

	    //Put the highest value ones first
		Collections.sort(siegesToSort, new Comparator() {
			@Override
			public int compare(Object s1, Object s2) {
				double s1PlunderValue= ((Siege) s1).getDefendingTown().getPlunderValue();
				double s2PlunderValue = ((Siege)s2).getDefendingTown().getPlunderValue();

				if(s1PlunderValue == s2PlunderValue) {
					return 0;
				} else if (s1PlunderValue > s2PlunderValue) {
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

		Siege siege;
		Town town;
		String output;
		List<String> siegesOrdered = new ArrayList();

		for (int i = (page - 1) * 10; i < iMax; i++) {
			siege = siegesToSort.get(i);
			town = siege.getDefendingTown();

			if(TownySettings.isUsingEconomy()) {
				output = Colors.Red + town.getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + siege.getFormattedHoursUntilCompletion() + ")";
			} else {
				output = Colors.Red + town.getName() + Colors.Gray + " - " + Colors.LightBlue + "(" + town.getFormattedPlunderValue() + ")" + Colors.Gray + " - " + Colors.LightBlue + "(" + siege.getFormattedHoursUntilCompletion() + ")";
			}

			siegesOrdered.add(output);
		}

		String headerLine;
		if(TownySettings.isUsingEconomy()) {
			headerLine = Colors.Red + TownySettings.getLangString("town_sing") + Colors.Gray + " - " + Colors.LightBlue + "("+TownySettings.getLangString("plunder_value") + ")" + Colors.Gray + " - " + Colors.LightBlue + "(" + TownySettings.getLangString("victory_timer") + ")";
		} else {
			headerLine = Colors.Red + TownySettings.getLangString("town_sing") + Colors.Gray + " - " + Colors.LightBlue + "(" + TownySettings.getLangString("victory_timer") + ")";
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
