package com.palmergames.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.IConomyException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyIConomyObject;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Compass;
import com.palmergames.util.KeyValue;
import com.palmergames.util.KeyValueTable;
import com.palmergames.util.StringMgmt;

public class TownyCommand implements CommandExecutor {
	
	//protected static TownyUniverse universe;
	private static Towny plugin;
	
	private static final List<String> towny_general_help = new ArrayList<String>();
	private static final List<String> towny_help = new ArrayList<String>();
	private static final List<String> towny_top = new ArrayList<String>();
	private static final List<String> towny_war = new ArrayList<String>();
	private static String towny_version;
	
	static {
		towny_general_help.add(ChatTools.formatTitle(TownySettings.getLangString("help_0")));
		towny_general_help.add(TownySettings.getLangString("help_1"));
		towny_general_help.add(ChatTools.formatCommand("", "/resident", "?", "")
				+ ", " + ChatTools.formatCommand("", "/town", "?", "") 
				+ ", " + ChatTools.formatCommand("", "/nation", "?", "")
				+ ", " + ChatTools.formatCommand("", "/plot", "?", "")
				+ ", " + ChatTools.formatCommand("", "/towny", "?", ""));
		towny_general_help.add(ChatTools.formatCommand("", "/townchat", " [msg]", TownySettings.getLangString("help_2"))
				+ ", " + ChatTools.formatCommand("", "/nationchat", " [msg]", TownySettings.getLangString("help_3")));
		towny_general_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/townyadmin", "?", ""));
		
		towny_help.add(ChatTools.formatTitle("/towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "", "General help for Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "map", "Displays a map of the nearby townblocks"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "prices", "Display the prices used with iConomy"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "top", "Display highscores"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "universe", "Displays stats"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "v", "Displays the version of Towny"));
		towny_help.add(ChatTools.formatCommand("", "/towny", "war", "'/towny war' for more info"));

	}
	
	public TownyCommand(Towny instance) {
		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		towny_version = Colors.Green + "Towny version: " + Colors.LightGreen + plugin.getTownyUniverse().getPlugin().getVersion();
		
		towny_war.add(ChatTools.formatTitle("/towny war"));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "stats", ""));
		towny_war.add(ChatTools.formatCommand("", "/towny war", "scores", ""));
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			System.out.println("[PLAYER_COMMAND] " + player.getName() + ": /" + commandLabel + " " + StringMgmt.join(args));
			parseTownyCommand(player, args);
		} else {
			// Console output
			if (args.length == 0)
				for (String line : towny_general_help)
					sender.sendMessage(Colors.strip(line));
			else if (args[0].equalsIgnoreCase("tree"))
				plugin.getTownyUniverse().sendUniverseTree(sender);
			else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("v"))
				sender.sendMessage(Colors.strip(towny_version));
			else if (args[0].equalsIgnoreCase("war")){
				boolean war = TownyWar(StringMgmt.remFirstArg(args));
				for (String line : towny_war)
					sender.sendMessage(Colors.strip(line));
				if (!war)
					sender.sendMessage("The world isn't currently at war.");
				
				towny_war.clear();
			}else if (args[0].equalsIgnoreCase("universe")) {
				for (String line : getUniverseStats())
					sender.sendMessage(Colors.strip(line));
			}
				
			
		}
		return true;
	}
	
	private void parseTownyCommand(Player player, String[] split) {
		
		if (split.length == 0)
			for (String line : towny_general_help)
				player.sendMessage(line);
		else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help"))
			for (String line : towny_help)
				player.sendMessage(Colors.strip(line));
		else if (split[0].equalsIgnoreCase("map"))
			showMap(player);
		else if (split[0].equalsIgnoreCase("prices")){
			Town town = null;
			if (split.length > 1){
				try {
					town = plugin.getTownyUniverse().getTown(split[1]);
				} catch (NotRegisteredException x) {
					sendErrorMsg(player, x.getError());
					return;
				}
			} else if (split.length == 0)
				try {
					Resident resident = plugin.getTownyUniverse().getResident(player.getName());
					town = resident.getTown();
				} catch (NotRegisteredException x) {
				}
				
			for (String line : getTownyPrices(town))
				player.sendMessage(line);
		
		} else if (split[0].equalsIgnoreCase("top")) {
			TopCommand(player,StringMgmt.remFirstArg(split));
		} else if (split[0].equalsIgnoreCase("tree")) {
			consoleUseOnly(player);
		} else if (split[0].equalsIgnoreCase("universe")) {
			for (String line : getUniverseStats())
				player.sendMessage(line);		
		} else if (split[0].equalsIgnoreCase("version") || split[0].equalsIgnoreCase("v")) {
			player.sendMessage(towny_version);
		} else if (split[0].equalsIgnoreCase("war")) {
			boolean war = TownyWar(StringMgmt.remFirstArg(split));
			for (String line : towny_war)
				player.sendMessage(Colors.strip(line));
			if (!war)
				sendErrorMsg(player, "The world isn't currently at war.");
			
			towny_war.clear();
		} else
			sendErrorMsg(player, "Invalid sub command.");
			
	}
	
	private boolean TownyWar(String[] args){
		
		if (plugin.getTownyUniverse().isWarTime() && args.length > 0) {
			towny_war.clear();
			if (args[0].equalsIgnoreCase("stats"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getStats());
			else if (args[0].equalsIgnoreCase("scores"))
				towny_war.addAll(plugin.getTownyUniverse().getWarEvent().getScores(-1));
		}
		
		return plugin.getTownyUniverse().isWarTime();	
	}
	
	private void TopCommand(Player player, String[] args) {
		
		if (!plugin.isTownyAdmin(player) && (plugin.isPermissions() && !plugin.hasPermission(player, "towny.top"))) {
			sendErrorMsg(player, TownySettings.getLangString("msg_err_command_disable"));
			return;
		}
			
		
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			towny_top.add(ChatTools.formatTitle("/towny top"));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "money [all/resident/town/nation]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
		} else if (args[0].equalsIgnoreCase("money"))
			try {
				if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
					List<TownyIConomyObject> list = new ArrayList<TownyIConomyObject>(plugin.getTownyUniverse().getResidents());
					list.addAll(plugin.getTownyUniverse().getTowns());
					list.addAll(plugin.getTownyUniverse().getNations());
					towny_top.add(ChatTools.formatTitle("Top Bank Accounts"));
					towny_top.addAll(getTopBankBalance(list, 10));
				} else if (args[1].equalsIgnoreCase("resident")) {
					towny_top.add(ChatTools.formatTitle("Top Resident Bank Accounts"));
					towny_top.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(plugin.getTownyUniverse().getResidents()), 10));
				} else if (args[1].equalsIgnoreCase("town")) {
					towny_top.add(ChatTools.formatTitle("Top Town Bank Accounts"));
					towny_top.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(plugin.getTownyUniverse().getTowns()), 10));
				} else if (args[1].equalsIgnoreCase("nation")) {
					towny_top.add(ChatTools.formatTitle("Top Nation Bank Accounts"));
					towny_top.addAll(getTopBankBalance(new ArrayList<TownyIConomyObject>(plugin.getTownyUniverse().getNations()), 10));
				} else 
					sendErrorMsg(player, "Invalid sub command.");
			} catch (IConomyException e) {
				sendErrorMsg(player, "IConomy error.");
				sendErrorMsg(player, e.getError());
			}
		else if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<ResidentList>(plugin.getTownyUniverse().getTowns());
				list.addAll(plugin.getTownyUniverse().getNations());
				towny_top.add(ChatTools.formatTitle("Most Residents"));
				towny_top.addAll(getMostResidents(list, 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Town"));
				towny_top.addAll(getMostResidents(new ArrayList<ResidentList>(plugin.getTownyUniverse().getTowns()), 10));
			} else if (args[1].equalsIgnoreCase("nation")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Nation"));
				towny_top.addAll(getMostResidents(new ArrayList<ResidentList>(plugin.getTownyUniverse().getNations()), 10));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<TownBlockOwner>(plugin.getTownyUniverse().getResidents());
				list.addAll(plugin.getTownyUniverse().getTowns());
				towny_top.add(ChatTools.formatTitle("Most Land Owned"));
				towny_top.addAll(getMostLand(list, 10));
			} else if (args[1].equalsIgnoreCase("resident")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				towny_top.addAll(getMostLand(new ArrayList<TownBlockOwner>(plugin.getTownyUniverse().getResidents()), 10));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Town"));
				towny_top.addAll(getMostLand(new ArrayList<TownBlockOwner>(plugin.getTownyUniverse().getTowns()), 10));
			} else
				sendErrorMsg(player, "Invalid sub command.");
		else
			sendErrorMsg(player, "Invalid sub command.");

		for (String line : towny_top)
			player.sendMessage(line);
		
		towny_top.clear();

	}
	
	public List<String> getUniverseStats() {
		List<String> output = new ArrayList<String>();
		output.add("§0-§4###§0---§4###§0-");
		output.add("§4#§c###§4#§0-§4#§c###§4#§0   §6[§eTowny " + plugin.getTownyUniverse().getPlugin().getVersion() + "§6]");
		output.add("§4#§c####§4#§c####§4#   §3By: §bChris H (Shade)/croxis/ElgarL");
		output.add("§0-§4#§c#######§4#§0-");
		output.add("§0--§4##§c###§4##§0-- " 
				+ "§3Residents: §b" + Integer.toString(plugin.getTownyUniverse().getResidents().size())
				+ Colors.Gray + " | "
				+ "§3Towns: §b" + Integer.toString(plugin.getTownyUniverse().getTowns().size())
				+ Colors.Gray + " | "
				+ "§3Nations: §b" + Integer.toString(plugin.getTownyUniverse().getNations().size()));
		output.add("§0----§4#§c#§4#§0---- "
				+ "§3Worlds: §b" + Integer.toString(plugin.getTownyUniverse().getWorlds().size())
				+ Colors.Gray + " | "
				+ "§3TownBlocks: §b" + Integer.toString(plugin.getTownyUniverse().getAllTownBlocks().size()));
		output.add("§0-----§4#§0----- ");
		return output;
	}
	
	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player
	 */

	public static void showMap(Player player) {

		boolean hasTown = false;
		Resident resident;
		int lineCount = 0;

		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			if (resident.hasTown())
				hasTown = true;
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		TownyWorld world;
		try {
			world = plugin.getTownyUniverse().getWorld(player.getWorld().getName());
		} catch (NotRegisteredException e1) {
			plugin.sendErrorMsg(player, "You are not in a registered world.");
			return;
		}
		if (!world.isUsingTowny()) {
			plugin.sendErrorMsg(player, "This world is not using towny.");
			return;
		}
		Coord pos = Coord.parseCoord(plugin.getCache(player).getLastLocation());

		player.sendMessage(ChatTools.formatTitle("Towny Map " + Colors.White + "(" + pos.toString() + ")"));

		String[][] townyMap = new String[27][7];
		int x, y = 0;
		for (int tby = pos.getZ() - 13; tby <= pos.getZ() + 13; tby++) {
			x = 0;
			for (int tbx = pos.getX() - 3; tbx <= pos.getX() + 3; tbx++) {
				try {
					TownBlock townblock = world.getTownBlock(tbx, tby);
					//TODO: possibly claim outside of towns
					if (!townblock.hasTown())
						throw new TownyException();
					if (x == 3 && y == 13)
						// location
						townyMap[y][x] = Colors.Gold;
					else if (hasTown) {
						if (resident.getTown() == townblock.getTown()) {
							// own town
							townyMap[y][x] = Colors.LightGreen;
							try {
								if (resident == townblock.getResident())
									//own plot
									townyMap[y][x] = Colors.Yellow;
							} catch(NotRegisteredException e) {
							}
						} else if (resident.hasNation()) {
							if (resident.getTown().getNation().hasTown(townblock.getTown()))
								// towns
								townyMap[y][x] = Colors.Green;
							else if (townblock.getTown().hasNation()) {
								Nation nation = resident.getTown().getNation();
								if (nation.hasAlly(townblock.getTown().getNation()))
									townyMap[y][x] = Colors.Green;
								else if (nation.hasEnemy(townblock.getTown().getNation()))
									// towns
									townyMap[y][x] = Colors.Red;
								else
									townyMap[y][x] = Colors.White;
							} else
								townyMap[y][x] = Colors.White;
						} else
							townyMap[y][x] = Colors.White;
					} else
						townyMap[y][x] = Colors.White;

					// Registered town block
					if (townblock.isForSale() != -1)
						townyMap[y][x] += "$";
					else if (townblock.isHomeBlock())
						townyMap[y][x] += "H";
					else
						townyMap[y][x] += "+";
				} catch (TownyException e) {
					if (x == 3 && y == 13)
						townyMap[y][x] = Colors.Gold;
					else
						townyMap[y][x] = Colors.Gray;

					// Unregistered town block
					townyMap[y][x] += "-";
				}
				x++;
			}
			y++;
		}

		Compass.Point dir = Compass.getCompassPointForDirection(player.getLocation().getYaw());
		
		String[] compass = {
				Colors.Black + "  -----  ",
				Colors.Black + "  -" + (dir == Compass.Point.NW ? Colors.Gold + "\\" : "-")
				+ (dir == Compass.Point.N ? Colors.Gold : Colors.White) + "N"
				+ (dir == Compass.Point.NE ? Colors.Gold + "/" + Colors.Black : Colors.Black + "-") + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.W ? Colors.Gold + "W" : Colors.White + "W") + Colors.LightGray + "+"
				+ (dir == Compass.Point.E ? Colors.Gold : Colors.White) + "E" + Colors.Black  + "-  ",
				Colors.Black + "  -" + (dir == Compass.Point.SW ? Colors.Gold + "/" : "-")
				+ (dir == Compass.Point.S ? Colors.Gold : Colors.White) + "S"
				+ (dir == Compass.Point.SE ? Colors.Gold + "\\" + Colors.Black : Colors.Black + "-") + "-  "};

		String[] help = {
				"  " + Colors.Gray + "-" + Colors.LightGray + " = Unclaimed",
				"  " + Colors.White + "+" + Colors.LightGray + " = Claimed",
				"  " + Colors.White + "$" + Colors.LightGray + " = For sale",
				"  " + Colors.LightGreen + "+" + Colors.LightGray + " = Your town",
				"  " + Colors.Yellow + "+" + Colors.LightGray + " = Your plot",
				"  " + Colors.Green + "+" + Colors.LightGray + " = Ally",
				"  " + Colors.Red + "+" + Colors.LightGray + " = Enemy"
		};
		
		String line;
		// Variables have been rotated to fit N/S/E/W properly
		for (int my = 0; my < 7; my++) {
			line = compass[0];
			if (lineCount < compass.length)
				line = compass[lineCount];

			for (int mx = 26; mx >= 0; mx--)
				line += townyMap[mx][my];
			
			if (lineCount < help.length)
				line += help[lineCount];
			
			player.sendMessage(line);
			lineCount++;
			
		}
		//Print the help at the bottom (moved back to the right side)
		/*
		lineCount=0;
		for(int i=0;i<2;i++)
		{
			line = "";
			line += help[lineCount];
			line += help[lineCount+1];
			line += help[lineCount+2];
			player.sendMessage(line);
			lineCount+=3;
		}
		line=help[6];
		player.sendMessage(line);
		*/
		
		// Current town block data
		try {
			TownBlock townblock = world.getTownBlock(pos);
			plugin.sendMsg(player, ("Town: " + (townblock.hasTown() ? townblock.getTown().getName() : "None") + " : "
					+ "Owner: " + (townblock.hasResident() ? townblock.getResident().getName() : "None")));
		} catch (TownyException e) {
			//plugin.sendErrorMsg(player, e.getError());
			// Send a blank line instead of an error, to keep the map position tidy.
			player.sendMessage ("");
		}
	}
	
	/**
	 * Send the list of costs for iConomy to player Command: /towny prices
	 * 
	 * @param town
	 */

	/*
	 * [New] Town: 100 | Nation: 500
	 * [Upkeep] Town: 10 | Nation: 100
	 * Town [Elden]:
	 *     [Price] Plot: 100 | Outpost: 250
	 *     [Upkeep] Resident: 20 | Plot: 50
	 * Nation [Albion]:
	 *     [Upkeep] Town: 100 | Neutrality: 100 
	 */
	
	//TODO: Proceduralize and make parse function for /towny prices [town]
	public List<String> getTownyPrices(Town town) {
		List<String> output = new ArrayList<String>();
		Nation nation = null;
		
		if (town != null)
			if (town.hasNation())
				try {
					nation = town.getNation();
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

		
		output.add(ChatTools.formatTitle("Prices"));
		output.add(Colors.Yellow + "[New] "
				+ Colors.Green + "Town: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNewTownPrice())
				+ Colors.Gray + " | "
				+ Colors.Green + "Nation: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNewNationPrice()));
		output.add(Colors.Yellow + "[Upkeep] "
				+ Colors.Green + "Town: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getTownUpkeepCost(town))
				+ Colors.Gray + " | "
				+ Colors.Green + "Nation: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNationUpkeepCost(nation)));
		if (town != null) {
			output.add(Colors.Yellow + "Town [" + plugin.getTownyUniverse().getFormatter().getFormattedName(town)+"]");
			output.add(Colors.Rose + "    [Price] "
					+ Colors.Green + "Plot: " + Colors.LightGreen + Integer.toString(town.getPlotPrice())
					+ Colors.Gray + " | "
					+ Colors.Green + "Outpost: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getOutpostCost()));
			output.add(Colors.Rose + "    [Upkeep] "
					+ Colors.Green + "Resident: " + Colors.LightGreen + Integer.toString(town.getTaxes())
					+ Colors.Gray + " | "
					+ Colors.Green + "Plot: " + Colors.LightGreen + Integer.toString(town.getPlotTax()));
			
			
			if (nation != null) {
				output.add(Colors.Yellow + "Nation [" + plugin.getTownyUniverse().getFormatter().getFormattedName(nation)+"]");
				output.add(Colors.Rose + "    [Upkeep] "
					+ Colors.Green + "Town: " + Colors.LightGreen + Integer.toString(nation.getTaxes())
					+ Colors.Gray + " | "
					+ Colors.Green + "Neutrality: " + Colors.LightGreen + TownyFormatter.formatMoney(TownySettings.getNationNeutralityCost()));
			}
		}
		return output;
	}
	
	public List<String> getTopBankBalance(List<TownyIConomyObject> list, int maxListing) throws IConomyException {
		List<String> output = new ArrayList<String>();
		KeyValueTable<TownyIConomyObject,Double> kvTable = new KeyValueTable<TownyIConomyObject,Double>();
		for (TownyIConomyObject obj : list)
		{
			kvTable.put(obj, obj.getHoldingBalance());
		}
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownyIConomyObject,Double> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownyIConomyObject town = (TownyIConomyObject)kv.key;
			output.add(String.format(
					Colors.LightGray + "%-20s "+Colors.Gold+"|"+Colors.Blue+" %s",
					plugin.getTownyUniverse().getFormatter().getFormattedName(town),
					TownyFormatter.formatMoney((Double)kv.value)));
		}
		return output;
	}
	
	public List<String> getMostResidents(List<ResidentList> list, int maxListing) {
		List<String> output = new ArrayList<String>();
		KeyValueTable<ResidentList,Integer> kvTable = new KeyValueTable<ResidentList,Integer>();
		for (ResidentList obj : list)
			kvTable.put(obj, obj.getResidents().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<ResidentList,Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			ResidentList residentList = (ResidentList)kv.key;
			output.add(String.format(
					Colors.Blue + "%30s "+Colors.Gold+"|"+Colors.LightGray+" %10d",
					plugin.getTownyUniverse().getFormatter().getFormattedName((TownyObject)residentList),
					(Integer)kv.value));
		}
		return output;
	}
	
	public List<String> getMostLand(List<TownBlockOwner> list, int maxListing) {
		List<String> output = new ArrayList<String>();
		KeyValueTable<TownBlockOwner,Integer> kvTable = new KeyValueTable<TownBlockOwner,Integer>();
		for (TownBlockOwner obj : list)
			kvTable.put(obj, obj.getTownBlocks().size());
		kvTable.sortByValue();
		kvTable.revese();
		int n = 0;
		for (KeyValue<TownBlockOwner,Integer> kv : kvTable.getKeyValues()) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;
			TownBlockOwner town = (TownBlockOwner)kv.key;
			output.add(String.format(
					Colors.Blue + "%30s "+Colors.Gold+"|"+Colors.LightGray+" %10d",
					plugin.getTownyUniverse().getFormatter().getFormattedName(town),
					(Integer)kv.value));
		}
		return output;
	}
	
	public void consoleUseOnly(Player player) {
		plugin.getTownyUniverse().getPlugin().sendErrorMsg(player, "This command was designed for use in the console only.");
	}
	
	public void inGameUseOnly(CommandSender sender) {
		sender.sendMessage("[Towny] InputError: This command was designed for use in game only.");
	}
	
	public boolean sendErrorMsg(CommandSender sender, String msg) {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			plugin.getTownyUniverse().getPlugin().sendErrorMsg(player, msg);
		} else
			// Console
			sender.sendMessage("[Towny] ConsoleError: " + msg);
		
		return false;
	}
}
