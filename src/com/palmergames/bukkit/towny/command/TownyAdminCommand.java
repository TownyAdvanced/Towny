package com.palmergames.bukkit.towny.command;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownClaim;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.TimeTools;
import com.palmergames.util.MemMgmt;
import com.palmergames.util.StringMgmt;

/**
 * Send a list of all general townyadmin help commands to player Command:
 * /townyadmin
 */

public class TownyAdminCommand implements CommandExecutor {

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
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "toggle neutral/war/debug/devmode", ""));

		//TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin", "npc rename [old name] [new name]", ""));
		//TODO: ta_help.add(ChatTools.formatCommand("", "/townyadmin", "npc list", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reload", TownySettings.getLangString("admin_panel_2")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "reset", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "backup", ""));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "newday", TownySettings.getLangString("admin_panel_3")));
		ta_help.add(ChatTools.formatCommand("", "/townyadmin", "purge [number of days]", ""));

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
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));

			if (!plugin.isTownyAdmin(player)) {
				TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_admin_only"));
				return true;
			}
		} else {
			isConsole = true;
			this.player = null;
		}

		try {
			parseTownyAdminCommand(args);
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

	public void parseTownyAdminCommand(String[] split) throws TownyException {
		if (split.length == 0) {
			buildTAPanel();
			for (String line : ta_panel) {
				sender.sendMessage(line);
			}

		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help"))
			for (String line : ta_help) {
				sender.sendMessage(line);
			}
		else if (split[0].equalsIgnoreCase("set"))
			adminSet(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("town"))
			parseAdminTownCommand(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("nation"))
			parseAdminNationCommand(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("toggle"))
			parseToggleCommand(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("givebonus"))
			giveBonus(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("reload"))
			reloadTowny(false);
		else if (split[0].equalsIgnoreCase("reset"))
			reloadTowny(true);
		else if (split[0].equalsIgnoreCase("backup"))
			try {
				TownyUniverse.getDataSource().backup();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("mag_backup_success"));

			} catch (IOException e) {
				TownyMessaging.sendErrorMsg(getSender(), "Error: " + e.getMessage());

			}
		else if (split[0].equalsIgnoreCase("newday"))
			plugin.getTownyUniverse().newDay();
		else if (split[0].equalsIgnoreCase("purge"))
			purge(StringMgmt.remFirstArg(split));
		else if (split[0].equalsIgnoreCase("unclaim"))
			parseAdminUnclaimCommand(StringMgmt.remFirstArg(split));
		/*
		else if (split[0].equalsIgnoreCase("seed") && TownySettings.getDebug())
		        seedTowny();
		else if (split[0].equalsIgnoreCase("warseed") && TownySettings.getDebug())
		        warSeed(player);
		        */
		else
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_sub"));
	}

	private void giveBonus(String[] split) throws TownyException {

		Town town;

		try {
			if (split.length != 2)
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_input"), "Eg: givebonus [town/player] [n]"));
			try {
				town = plugin.getTownyUniverse().getTown(split[0]);
			} catch (NotRegisteredException e) {
				town = plugin.getTownyUniverse().getResident(split[0]).getTown();
			}
			try {
				town.setBonusBlocks(town.getBonusBlocks() + Integer.parseInt(split[1].trim()));
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_give_total"), town.getName(), split[1], town.getBonusBlocks()));
			} catch (NumberFormatException nfe) {
				throw new TownyException(TownySettings.getLangString("msg_error_must_be_int"));
			}
			TownyUniverse.getDataSource().saveTown(town);
		} catch (TownyException e) {
			throw new TownyException(e.getError());
		}

	}

	private void buildTAPanel() {

		ta_panel.clear();
		Runtime run = Runtime.getRuntime();
		ta_panel.add(ChatTools.formatTitle(TownySettings.getLangString("ta_panel_1")));
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] " + Colors.Green + TownySettings.getLangString("ta_panel_2") + Colors.LightGreen + plugin.getTownyUniverse().isWarTime() + Colors.Gray + " | " + Colors.Green + TownySettings.getLangString("ta_panel_3") + (plugin.getTownyUniverse().isHealthRegenRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off") + Colors.Gray + " | " + (Colors.Green + TownySettings.getLangString("ta_panel_5") + (plugin.getTownyUniverse().isDailyTimerRunning() ? Colors.LightGreen + "On" : Colors.Rose + "Off")));
		/*
		ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Towny" + Colors.Blue + "] "
		                + Colors.Green + TownySettings.getLangString("ta_panel_4")
		                + (TownySettings.isRemovingWorldMobs() ? Colors.LightGreen + "On" : Colors.Rose + "Off")
		                + Colors.Gray + " | "
		                + Colors.Green + TownySettings.getLangString("ta_panel_4_1")
		                + (TownySettings.isRemovingTownMobs() ? Colors.LightGreen + "On" : Colors.Rose + "Off"));
		
		try {
		        TownyEconomyObject.checkEconomy();
		        ta_panel.add(Colors.Blue + "[" + Colors.LightBlue + "Economy" + Colors.Blue + "] "
		                        + Colors.Green + TownySettings.getLangString("ta_panel_6") + Colors.LightGreen + TownyFormatter.formatMoney(getTotalEconomy()) + Colors.Gray + " | "
		                        + Colors.Green + TownySettings.getLangString("ta_panel_7") + Colors.LightGreen + getNumBankAccounts());
		} catch (Exception e) {
		}
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
			TownyWorld world;
			try {
				if (plugin.getTownyUniverse().isWarTime())
					throw new TownyException(TownySettings.getLangString("msg_war_cannot_do"));

				world = TownyUniverse.getWorld(player.getWorld().getName());

				List<WorldCoord> selection;
				selection = TownyUtil.selectWorldCoordArea(null, new WorldCoord(world, Coord.parseCoord(player)), split);

				new TownClaim(plugin, player, null, selection, false, true).start();

			} catch (TownyException x) {
				TownyMessaging.sendErrorMsg(player, x.getError());
				return;
			}
		}
	}

	public void parseAdminTownCommand(String[] split) {
		//TODO Make this use the actual town command procedually.

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin town"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin town", "[town] add/delete [] .. []", ""));
		} else {
			try {
				Town town = plugin.getTownyUniverse().getTown(split[0]);
				if (split.length == 1)
					TownyMessaging.sendMessage(getSender(), plugin.getTownyUniverse().getStatus(town));
				else if (split[1].equalsIgnoreCase("add")) {
					if (isConsole) {
						sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
						return;
					}
					TownCommand.townAdd(player, town, StringMgmt.remArgs(split, 2));
				}

			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(getSender(), e.getError());
			}
		}
	}

	public void parseAdminNationCommand(String[] split) {

		if (split.length == 0 || split[0].equalsIgnoreCase("?")) {

			sender.sendMessage(ChatTools.formatTitle("/townyadmin nation"));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation]", ""));
			sender.sendMessage(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin nation", "[nation] add [] .. []", ""));
		} else {
			try {
				Nation nation = plugin.getTownyUniverse().getNation(split[0]);
				if (split.length == 1)
					TownyMessaging.sendMessage(getSender(), plugin.getTownyUniverse().getStatus(nation));
				else if (split[1].equalsIgnoreCase("add")) {
					if (isConsole) {
						sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
						return;
					}
					NationCommand.nationAdd(nation, plugin.getTownyUniverse().getTowns(StringMgmt.remArgs(split, 2)));
				}

			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(getSender(), e.getError());
			} catch (AlreadyRegisteredException e) {
				TownyMessaging.sendErrorMsg(getSender(), e.getError());
			}
		}
	}

	public void adminSet(String[] split) {

		if (split.length == 0) {
			sender.sendMessage(ChatTools.formatTitle("/townyadmin set"));
			//TODO: player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "king [nation] [king]", ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] " + TownySettings.getLangString("town_help_2"), ""));
			sender.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "mayor [town] npc", ""));
			//player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "debugmode [on/off]", ""));
			//player.sendMessage(ChatTools.formatCommand("", "/townyadmin set", "devmode [on/off]", ""));
		} else if (split[0].equalsIgnoreCase("mayor")) {
			if (split.length < 3) {
				sender.sendMessage(ChatTools.formatTitle("/townyadmin set mayor"));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] " + TownySettings.getLangString("town_help_2"), ""));
				sender.sendMessage(ChatTools.formatCommand("Eg", "/townyadmin set mayor", "[town] npc", ""));
			} else
				try {
					Resident newMayor = null;
					Town town = plugin.getTownyUniverse().getTown(split[1]);

					if (split[2].equalsIgnoreCase("npc")) {
						String name = nextNpcName();
						plugin.getTownyUniverse().newResident(name);

						newMayor = plugin.getTownyUniverse().getResident(name);

						newMayor.setRegistered(System.currentTimeMillis());
						newMayor.setLastOnline(0);
						newMayor.setNPC(true);

						TownyUniverse.getDataSource().saveResident(newMayor);
						TownyUniverse.getDataSource().saveResidentList();

						// set for no upkeep as an NPC mayor is assigned
						town.setHasUpkeep(false);

					} else {
						newMayor = plugin.getTownyUniverse().getResident(split[2]);

						//set upkeep again
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
							plugin.getTownyUniverse().removeResident(oldMayor);

							plugin.getTownyUniverse().removeResidentList(oldMayor);

						} catch (EmptyTownException e) {
							// Should never reach here as we are setting a new mayor before removing the old one.
							e.printStackTrace();
						}
					}
					TownyUniverse.getDataSource().saveTown(town);
					String[] msg = TownySettings.getNewMayorMsg(newMayor.getName());
					TownyMessaging.sendTownMessage(town, msg);
					//TownyMessaging.sendMessage(player, msg);
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(getSender(), e.getError());
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
			if (!plugin.getTownyUniverse().hasResident(name))
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
		plugin.load();

		TownyMessaging.sendMsg(sender, TownySettings.getLangString("msg_reloaded"));
		//TownyMessaging.sendMsg(TownySettings.getLangString("msg_reloaded"));
	}
	
	/**
	 * Remove residents who havn't logged in for X amount of days.
	 * 
	 * @param split
	 */
	public void purge(String[] split) {
		if (split.length == 0) {
			//command was '/townyadmin purge'
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

		for (Resident resident : new ArrayList<Resident>(plugin.getTownyUniverse().getResidents())) {
			if (!resident.isNPC() && (System.currentTimeMillis() - resident.getLastOnline() > (TimeTools.getMillis(days + "d"))) && !plugin.isOnline(resident.getName())) {
				TownyMessaging.sendMessage(this.sender, "Deleting resident: " + resident.getName());
				plugin.getTownyUniverse().removeResident(resident);
				plugin.getTownyUniverse().removeResidentList(resident);

			}
		}

	}

	public void parseToggleCommand(String[] split) throws TownyException {
		boolean choice;

		if (split.length == 0) {
			//command was '/townyadmin toggle'
			player.sendMessage(ChatTools.formatTitle("/townyadmin toggle"));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "war", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "neutral", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "devmode", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "debug", ""));
			player.sendMessage(ChatTools.formatCommand("", "/townyadmin toggle", "townwithdraw/nationwithdraw", ""));
			return;

		} else if (split[0].equalsIgnoreCase("war")) {
			choice = plugin.getTownyUniverse().isWarTime();

			if (!choice) {
				plugin.getTownyUniverse().startWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_started"));
			} else {
				plugin.getTownyUniverse().endWarEvent();
				TownyMessaging.sendMsg(getSender(), TownySettings.getLangString("msg_war_ended"));
			}
		} else if (split[0].equalsIgnoreCase("neutral")) {

			try {
				choice = !TownySettings.isDeclaringNeutral();
				TownySettings.setDeclaringNeutral(choice);
				TownyMessaging.sendMsg(getSender(), String.format(TownySettings.getLangString("msg_nation_allow_neutral"), choice ? "Enabled" : "Disabled"));

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
		} else {
			// parameter error message
			// neutral/war/townmobs/worldmobs
			TownyMessaging.sendErrorMsg(getSender(), TownySettings.getLangString("msg_err_invalid_choice"));
		}
	}
	/*
	private void warSeed(Player player) {
	        Resident r1 = plugin.getTownyUniverse().newResident("r1");
	        Resident r2 = plugin.getTownyUniverse().newResident("r2");
	        Resident r3 = plugin.getTownyUniverse().newResident("r3");
	        Coord key = Coord.parseCoord(player);
	        Town t1 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t1", r1, key, player.getLocation());
	        Town t2 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t2", r2, new Coord(key.getX() + 1, key.getZ()), player.getLocation());
	        Town t3 = newTown(plugin.getTownyUniverse(), player.getWorld(), "t3", r3, new Coord(key.getX(), key.getZ() + 1), player.getLocation());
	        Nation n1 = 
	        
	}

	public void seedTowny() {
	        TownyUniverse townyUniverse = plugin.getTownyUniverse();
	        Random r = new Random();
	        for (int i = 0; i < 1000; i++) {

	                try {
	                        townyUniverse.newNation(Integer.toString(r.nextInt()));
	                } catch (TownyException e) {
	                }
	                try {
	                        townyUniverse.newTown(Integer.toString(r.nextInt()));
	                } catch (TownyException e) {
	                }
	                try {
	                        townyUniverse.newResident(Integer.toString(r.nextInt()));
	                } catch (TownyException e) {
	                }
	        }
	}
	
	private static double getTotalEconomy() {
	        double total = 0;
	        try {
	                return total;
	        } catch (Exception e) {
	        }
	        return total;
	}
	
	private static int getNumBankAccounts() {
	        try {
	                return 0;
	        } catch (Exception e) {
	                return 0; 
	        }
	}
	 */
}
