package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyEconomyObject;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	// protected static TownyUniverse universe;
	private static Towny plugin;

	private static final List<String> towny_general_help = new ArrayList<>();
	private static final List<String> towny_help = new ArrayList<>();
	private static final List<String> towny_top = new ArrayList<>();
	private static final List<String> towny_war = new ArrayList<>();
	private static String towny_version;

	static {
		towny_general_help.add(ChatTools.formatTitle(TownySettings.getLangString("help_0")));
		towny_general_help.add(TownySettings.getLangString("help_1"));
		towny_general_help.add(ChatTools.formatCommand("", "/resident", "?", "") + ", " + ChatTools.formatCommand("", "/town", "?", "") + ", " + ChatTools.formatCommand("", "/nation", "?", "") + ", " + ChatTools.formatCommand("", "/plot", "?", "") + ", " + ChatTools.formatCommand("", "/towny", "?", ""));
		towny_general_help.add(ChatTools.formatCommand("", "/tc", "[msg]", TownySettings.getLangString("help_2")) + ", " + ChatTools.formatCommand("", "/nc", "[msg]", TownySettings.getLangString("help_3")).trim());
		towny_general_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin", "?", ""));

		towny_help.add(ChatTools.formatTitle("/towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "prices", "Display the prices used with Economy"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "top", "Display highscores"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "time", "Display time until a new day"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "v", "Displays the version of Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "war", "'/towny war' for more info"));

	}

	public TownyCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		towny_version = Colors.Green + "Towny version: " + Colors.LightGreen + plugin.getVersion();

		towny_war.add(ChatTools.formatTitle("/towny war"));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "stats", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "scores", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "participants [page #]", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "hud", ""));

		if (sender instanceof Player) {
			Player player = (Player) sender;
			parseTownyCommand(player, args);
		} else {
			// Console output
			if (args.length == 0)
				for (String line : towny_general_help)
					sender.sendMessage(Colors.strip(line));
			else if (args[0].equalsIgnoreCase("tree"))
				plugin.getTownyUniverse().sendUniverseTree(sender);
			else if (args[0].equalsIgnoreCase("time")) {
				TownyMessaging.sendMsg(TownySettings.getLangString("msg_time_until_a_new_day") + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("v"))
				sender.sendMessage(Colors.strip(towny_version));
			else if (args[0].equalsIgnoreCase("war")) {
				boolean war = TownyWar(StringMgmt.remFirstArg(args), null);
				if (war)
					for (String line : towny_war)
						sender.sendMessage(Colors.strip(line));
				else
					sender.sendMessage("The world isn't currently at war.");

				towny_war.clear();
			} else if (args[0].equalsIgnoreCase("universe")) {
				for (String line : getUniverseStats())
					sender.sendMessage(Colors.strip(line));
			}

		}
		return true;
	}

	private void parseTownyCommand(Player player, String[] split) {

		if (split.length == 0) {
			for (String line : towny_general_help)
				player.sendMessage(line);

			return;
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			for (String line : towny_help)
				player.sendMessage(Colors.strip(line));

			return;
		}

		try {

			if (split[0].equalsIgnoreCase("map")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				
				if (split.length > 1 && split[1].equalsIgnoreCase("big"))
					TownyAsciiMap.generateAndSend(plugin, player, 18);
				else
					showMap(player);
			} else if (split[0].equalsIgnoreCase("prices")) {
				Town town = null;
				if (split.length > 1) {
					try {
						town = TownyUniverse.getDataSource().getTown(split[1]);
					} catch (NotRegisteredException x) {
						sendErrorMsg(player, x.getMessage());
						return;
					}
				} else if (split.length == 1)
					try {
						Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
						town = resident.getTown();
					} catch (NotRegisteredException e) {
					}

				for (String line : getTownyPrices(town))
					player.sendMessage(line);

			} else if (split[0].equalsIgnoreCase("top")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				TopCommand(player, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("tree")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TREE.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				consoleUseOnly(player);
			} else if (split[0].equalsIgnoreCase("time")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TIME.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_time_until_a_new_day") + TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime()));
			} else if (split[0].equalsIgnoreCase("universe")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				for (String line : getUniverseStats())
					player.sendMessage(line);
			} else if (split[0].equalsIgnoreCase("version") || split[0].equalsIgnoreCase("v")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				player.sendMessage(towny_version);
			} else if (split[0].equalsIgnoreCase("war")) {
				if (!TownyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_WAR.getNode(split[0].toLowerCase())))
					throw new TownyException(TownySettings.getLangString("msg_err_command_disable"));
				boolean war = TownyWar(StringMgmt.remFirstArg(split), player);
				if (war)
					for (String line : towny_war)
						player.sendMessage(Colors.strip(line));
				else
					sendErrorMsg(player, "The world isn't currently at war.");

				towny_war.clear();
			} else if (split[0].equalsIgnoreCase("spy")) {
				if (plugin.isPermissions() && TownyUniverse.getPermissionSource().has(player, PermissionNodes.TOWNY_CHAT_SPY.getNode())) {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					resident.toggleMode(split, true);
				} else
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));

			} else
				sendErrorMsg(player, "Invalid sub command.");

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
		}

	}

	private boolean TownyWar(String[] args, Player p) {

		if (TownyUniverse.isWarTime() && args.length > 0) {
			towny_war.clear();
			if (args[0].equalsIgnoreCase("stats"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getStats());
			else if (args[0].equalsIgnoreCase("scores"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getScores(-1));
			else if (args[0].equalsIgnoreCase("participants")) {
				try {
					parseWarParticipants(p, args);
				} catch (NotRegisteredException e) {
				}
				return true;
			}
			else if (args[0].equalsIgnoreCase("hud") && p == null)
				towny_war.add("No hud for console!");
			else if (args[0].equalsIgnoreCase("hud") && p != null) {
				if (plugin.isPermissions() && TownyUniverse.getPermissionSource().has(p, PermissionNodes.TOWNY_COMMAND_TOWNY_WAR_HUD.getNode())) {
					plugin.getHUDManager().toggleWarHUD(p);
				} else {
					TownyMessaging.sendErrorMsg(p, TownySettings.getLangString("msg_err_command_disable"));
				}
			}
		}

		return TownyUniverse.isWarTime();
	}

	@SuppressWarnings("null")
	private void parseWarParticipants(Player player, String[] split) throws NotRegisteredException {

		List<Town> townsToSort = War.warringTowns;
		List<Nation> nationsToSort = War.warringNations;
		int page = 1;
		List<String> output = new ArrayList<>();
		String nationLine = null;
		String townLine = null;
		for (Nation nations : nationsToSort) {
			nationLine = Colors.Gold + "-" + nations.getName().toString();
			if (TownyUniverse.getDataSource().getResident(player.getName()).hasNation())
				if (TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().hasEnemy(nations))
					nationLine += Colors.Red + " (Enemy)";
				else if (TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().hasAlly(nations))
					nationLine += Colors.Green + " (Ally)";
			output.add(nationLine);
			for (Town towns : townsToSort) {
				if (towns.getNation().equals(nations)) {
					townLine = Colors.Blue + "  -" + towns.getName().toString();
					if (towns.isCapital())
						townLine += Colors.LightBlue + " (Capital)";
					output.add(townLine);
				}
			}
		}
		int total = (int) Math.ceil((output.size()) / (double) 10);
		if (split.length > 1) {
			try {
				page = Integer.parseInt(split[1]);
				if (page < 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_negative"));
					return;
				} else if (page == 0) {
					TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					return;
				}
			} catch (NumberFormatException e) {
				TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
				return;
			}
		}
		if (page > total) {
			TownyMessaging.sendErrorMsg(player, TownySettings.getListNotEnoughPagesMsg(total));
			return;
		}

		int iMax = page * 10;
		if ((page * 10) > output.size()) {
			iMax = output.size();
		}
		List<String> warparticipantsformatted = new ArrayList<>();
		for (int i = (page - 1) * 10; i < iMax; i++) {
			String line = output.get(i);
			warparticipantsformatted.add(line);
		}
		player.sendMessage(ChatTools.formatList("War Participants",
				Colors.Gold + "Nation Name" + Colors.Gray + " - " + Colors.Blue + "Town Names",
				warparticipantsformatted, TownySettings.getListPageMsg(page, total)
				)
		);
		output.clear();
	}	
	
	private void TopCommand(Player player, String[] args) {

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			towny_top.add(ChatTools.formatTitle("/towny top"));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
		} else if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<>(TownyUniverse.getDataSource().getTowns());
				list.addAll(TownyUniverse.getDataSource().getNations());
				towny_top.add(ChatTools.formatTitle("Most Residents"));
				towny_top.addAll(getMostResidents(list, 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Town"));
				towny_top.addAll(getMostResidents(new ArrayList<>(TownyUniverse.getDataSource().getTowns()), 10));
			} else if (args[1].equalsIgnoreCase("nation")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Nation"));
				towny_top.addAll(getMostResidents(new ArrayList<>(TownyUniverse.getDataSource().getNations()), 10));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<>(TownyUniverse.getDataSource().getResidents());
				list.addAll(TownyUniverse.getDataSource().getTowns());
				towny_top.add(ChatTools.formatTitle("Most Land Owned"));
				towny_top.addAll(getMostLand(list, 10));
			} else if (args[1].equalsIgnoreCase("resident")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				towny_top.addAll(getMostLand(new ArrayList<>(TownyUniverse.getDataSource().getResidents()), 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Town"));
				towny_top.addAll(getMostLand(new ArrayList<>(TownyUniverse.getDataSource().getTowns()), 10));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else
			sendErrorMsg(player, "Invalid sub command.");

		for (String line : towny_top)
			player.sendMessage(line);

		towny_top.clear();

	}

	public List<String> getUniverseStats() {

		List<String> output = new ArrayList<>();
		output.add("\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-");
		output.add("\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   \u00A76[\u00A7eTowny " + plugin.getVersion() + "\u00A76]");
		output.add("\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   \u00A73By: \u00A7bChris H (Shade)/ElgarL/LlmDl");
		output.add("\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-");
		output.add("\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70--   " + "\u00A73Residents: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getResidents().size()) + Colors.Gray + " | " + "\u00A73Towns: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getTowns().size()) + Colors.Gray + " | " + "\u00A73Nations: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getNations().size()));
		output.add("\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70----   " + "\u00A73Worlds: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getWorlds().size()) + Colors.Gray + " | " + "\u00A73TownBlocks: \u00A7b" + Integer.toString(TownyUniverse.getDataSource().getAllTownBlocks().size()));
		output.add("\u00A70-----\u00A74#\u00A70----- ");
		Plugin test = Bukkit.getServer().getPluginManager().getPlugin("TownyChat");
		if (test != null){
			output.add("\u00A70-----------   \u00A76[\u00A7eTownyChat " + BukkitTools.getPluginManager().getPlugin("TownyChat").getDescription().getVersion() + "\u00A76]");
		}
		return output;
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player
	 */

	public static void showMap(Player player) {

		TownyAsciiMap.generateAndSend(plugin, player, 7);
	}

	/**
	 * Send the list of costs for Economy to player Command: /towny prices
	 * 
	 * @param town
	 */

	/*
	 * [New] Town: 100 | Nation: 500 [Upkeep] Town: 10 | Nation: 100 Town
	 * [Elden]: [Price] Plot: 100 | Outpost: 250 [Upkeep] Resident: 20 | Plot:
	 * 50 Nation [Albion]: [Upkeep] Town: 100 | Peace: 100
	 */

	// TODO: Proceduralize and make parse function for /towny prices [town]
	public List<String> getTownyPrices(Town town) {

		List<String> output = new ArrayList<>();
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				try {
					nation = town.getNation();
				} catch (NotRegisteredException e) {
					e.printStackTrace();
				}

		output.add(ChatTools.formatTitle("Prices"));
		output.add(Colors.Yellow + "[New] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewTownPrice()) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNewNationPrice()));
		if (town != null)
			output.add(Colors.Yellow + "[Upkeep] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(town)) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeepCost(nation)));
		if (town == null)
			output.add(Colors.Yellow + "[Upkeep] " + Colors.Green + "Town: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeep()) + Colors.Gray + " | " + Colors.Green + "Nation: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeep()));
		output.add(Colors.Gray + "Town upkeep is based on " + Colors.LightGreen + " the " + (TownySettings.isUpkeepByPlot() ? " number of plots" : " town level (num residents)."));

		if (town != null) {
			output.add(Colors.Yellow + "Town [" + TownyFormatter.getFormattedName(town) + "]");
			output.add(Colors.Rose + "    [Price] " + Colors.Green + "Plot: " + Colors.LightGreen + Double.toString(town.getPlotPrice()) + Colors.Gray + " | " + Colors.Green + "Outpost: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getOutpostCost()));
			output.add(Colors.Rose + "            " + Colors.Green + "Shop: " + Colors.LightGreen + Double.toString(town.getCommercialPlotPrice()) + Colors.Gray + " | " + Colors.Green + "Embassy: " + Colors.LightGreen + Double.toString(town.getEmbassyPlotPrice()));

			output.add(Colors.Rose + "    [Taxes] " + Colors.Green + "Resident: " + Colors.LightGreen + Double.toString(town.getTaxes()) + (town.isTaxPercentage()? "%" : "") + Colors.Gray + " | " + Colors.Green + "Plot: " + Colors.LightGreen + Double.toString(town.getPlotTax()));
			output.add(Colors.Rose + "            " + Colors.Green + "Shop: " + Colors.LightGreen + Double.toString(town.getCommercialPlotTax()) + Colors.Gray + " | " + Colors.Green + "Embassy: " + Colors.LightGreen + Double.toString(town.getEmbassyPlotTax()));

			if (nation != null) {
				output.add(Colors.Yellow + "Nation [" + TownyFormatter.getFormattedName(nation) + "]");
				output.add(Colors.Rose + "    [Taxes] " + Colors.Green + "Town: " + Colors.LightGreen + Double.toString(nation.getTaxes()) + Colors.Gray + " | " + Colors.Green + "Peace: " + Colors.LightGreen + TownyEconomyHandler.getFormattedBalance(TownySettings.getNationNeutralityCost()));
			}
		}
		return output;
	}

	public List<String> getTopBankBalance(List<TownyEconomyObject> list, int maxListing) throws EconomyException {

		List<String> output = new ArrayList<>();
		KeyValueTable<TownyEconomyObject, Double> kvTable = new KeyValueTable<>();
		for (TownyEconomyObject obj : list) {
			kvTable.put(obj, obj.getHoldingBalance());
		}
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<TownyEconomyObject, Double> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownyEconomyObject town = kv.key;
			output.add(String.format(Colors.LightGray + "%-20s " + Colors.Gold + "|" + Colors.Blue + " %s", TownyFormatter.getFormattedName(town), TownyEconomyHandler.getFormattedBalance(kv.value)));
		}
		return output;
	}

	public List<String> getMostResidents(List<ResidentList> list, int maxListing) {

		List<String> output = new ArrayList<>();
		KeyValueTable<ResidentList, Integer> kvTable = new KeyValueTable<>();
		for (ResidentList obj : list)
			kvTable.put(obj, obj.getResidents().size());
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<ResidentList, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			ResidentList residentList = kv.key;
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", TownyFormatter.getFormattedName((TownyObject) residentList), kv.value));
		}
		return output;
	}

	public List<String> getMostLand(List<TownBlockOwner> list, int maxListing) {

		List<String> output = new ArrayList<>();
		KeyValueTable<TownBlockOwner, Integer> kvTable = new KeyValueTable<>();
		for (TownBlockOwner obj : list)
			kvTable.put(obj, obj.getTownBlocks().size());
		kvTable.sortByValue();
		kvTable.reverse();
		int n = 0;
		for (KeyValue<TownBlockOwner, Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownBlockOwner town = kv.key;
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", TownyFormatter.getFormattedName(town), kv.value));
		}
		return output;
	}

	public void consoleUseOnly(Player player) {

		TownyMessaging.sendErrorMsg(player, "This command was designed for use in the console only.");
	}

	public void inGameUseOnly(CommandSender sender) {

		sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
	}

	public boolean sendErrorMsg(CommandSender sender, String msg) {

		if (sender instanceof Player) {
			Player player = (Player) sender;
			TownyMessaging.sendErrorMsg(player, msg);
		} else
			// Console
			sender.sendMessage("[Towny] ConsoleError: " + msg);

		return false;
	}
}
