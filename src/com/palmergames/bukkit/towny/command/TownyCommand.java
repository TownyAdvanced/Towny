package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.TownyUpdateChecker;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.GovernmentComparators;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.gui.SelectionGUI;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
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

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	// protected static TownyUniverse universe;
	private static Towny plugin;

	private static List<String> towny_top = new ArrayList<>();
	private static final List<String> townyTabCompletes = Arrays.asList(
		"map",
		"prices",
		"time",
		"top",
		"spy",
		"universe",
		"v",
		"switches",
		"itemuse",
		"allowedblocks",
		"wildsblocks",
		"plotclearblocks"
	);
	
	private static final List<String> townyConsoleTabCompletes = Arrays.asList(
		"prices",
		"time",
		"top",
		"spy",
		"universe",
		"tree",
		"v"
	);
	
	private static final List<String> townyTopTabCompletes = Arrays.asList(
		"residents",
		"land",
		"balance"
	);
	
	private static final List<String> townyTopTownNationCompletes = Arrays.asList(
		"all",
		"town",
		"nation"
	);
	
	private static final List<String> townyTopLandTabCompletes = Arrays.asList(
		"all",
		"resident",
		"town"
	);

	public TownyCommand(Towny instance) {

		plugin = instance;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			if (plugin.isError()) {
				TownyMessaging.sendErrorMsg(sender, "Locked in Safe mode!");
				return false;
			}
			Player player = (Player) sender;
			parseTownyCommand(player, args);
		} else {
			// Console output
			if (args.length == 0) {
				HelpMenu.GENERAL_HELP.send(sender);
			} else if (args[0].equalsIgnoreCase("tree")) {
				for (String line : TownyUniverse.getInstance().getTreeString(0)) {
					TownyMessaging.sendMessage(sender, line);
				}
			} else if (args[0].equalsIgnoreCase("time")) {
				TownyMessaging.sendMsg(Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime())));
			} else if (args[0].equalsIgnoreCase("version") || args[0].equalsIgnoreCase("v")) {
				if (TownyUpdateChecker.shouldShowNotification()) {
					TownyMessaging.sendMessage(sender, Colors.strip(Translation.of("msg_latest_version", plugin.getVersion(), TownyUpdateChecker.getNewVersion())));
				} else {
					TownyMessaging.sendMsg(sender, Translatable.of("msg_towny_version", plugin.getVersion()));
					
					if (TownyUpdateChecker.hasCheckedSuccessfully())
						TownyMessaging.sendMsg(sender, Translatable.of("msg_up_to_date"));
				}
			} else if (args[0].equalsIgnoreCase("universe")) {
				for (String line : getUniverseStats(Translation.getDefaultLocale()))
					TownyMessaging.sendMessage(sender, Colors.strip(line));
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNY, args[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNY, args[0]).execute(sender, "towny", args);
			}

		}
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		
		switch (args[0].toLowerCase()) {
			case "top":
				switch (args.length) {
					case 2:
						return NameUtil.filterByStart(townyTopTabCompletes, args[1]);
					case 3:
						switch (args[1].toLowerCase()) {
							case "residents":
							case "balance":
								return NameUtil.filterByStart(townyTopTownNationCompletes, args[2]);
							case "land":
								return NameUtil.filterByStart(townyTopLandTabCompletes, args[2]);
							default:
								return Collections.emptyList();
						}
					default:
						return Collections.emptyList();
				}
			case "map":
				if (args.length == 2)
					return NameUtil.filterByStart(Arrays.asList("big", "hud"), args[1]);
				break;
			default:
				if (args.length == 1) {
					if (sender instanceof Player) {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNY, townyTabCompletes), args[0]);
					} else {
						return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNY, townyConsoleTabCompletes), args[0]);
					}
				} else if (args.length > 1 && TownyCommandAddonAPI.hasCommand(CommandType.TOWNY, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNY, args[0]).getTabCompletion(sender, args), args[args.length-1]);
		}
		
		return Collections.emptyList();
	}

	private void parseTownyCommand(Player player, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		if (split.length == 0) {
			HelpMenu.GENERAL_HELP.send(player);
			return;
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.HELP.send(player);
			return;
		}

		try {

			TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld().getName());
			if (world == null && (split[0].equalsIgnoreCase("wildsblocks") || split[0].equalsIgnoreCase("plotclearblocks")))
				throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));
				
			if (split[0].equalsIgnoreCase("map")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				
				if (split.length > 1 && split[1].equalsIgnoreCase("big"))
					TownyAsciiMap.generateAndSend(plugin, player, 18);
				else if (split.length > 1 && split[1].equalsIgnoreCase("hud"))
					HUDManager.toggleMapHud(player);
				else
					showMap(player);
			} else if (split[0].equalsIgnoreCase("prices")) {
				Town town = null;
				if (!TownyEconomyHandler.isActive())
					throw new TownyException(Translatable.of("msg_err_no_economy"));
				
				if (split.length > 1) {
					town = TownyUniverse.getInstance().getTown(split[1]);
					
					if (town == null) {
						TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[1]));
						return;
					}
				} else {
					Optional<Resident> resOpt = TownyUniverse.getInstance().getResidentOpt(player.getUniqueId());
					
					if (resOpt.isPresent() && resOpt.get().hasTown()) {
						town = resOpt.get().getTownOrNull();
					}
				}

				for (String line : getTownyPrices(town, Translation.getLocale(player)))
					TownyMessaging.sendMessage(player, line);
			} else if (split[0].equalsIgnoreCase("switches")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.SWITCHES);
			} else if (split[0].equalsIgnoreCase("itemuse")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ITEMUSE);
			} else if (split[0].equalsIgnoreCase("allowedblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ALLOWEDBLOCKS);
			} else if (split[0].equalsIgnoreCase("wildsblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, world.getUnclaimedZoneIgnoreMaterials(), Translatable.of("gui_title_towny_wildsblocks").forLocale(player));
			} else if (split[0].equalsIgnoreCase("plotclearblocks")) {
				Resident resident = getResidentOrThrow(player.getUniqueId());
				ResidentUtil.openGUIInventory(resident, world.getPlotManagementMayorDelete(), Translatable.of("gui_title_towny_plotclear").forLocale(player));
			} else if (split[0].equalsIgnoreCase("top")) {
				topCommand(player, StringMgmt.remFirstArg(split));
			} else if (split[0].equalsIgnoreCase("tree")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TREE.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				consoleUseOnly(player);
			} else if (split[0].equalsIgnoreCase("time")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TIME.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				TownyMessaging.sendMsg(player, Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TownyTimerHandler.townyTime())));
			} else if (split[0].equalsIgnoreCase("universe")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));
				for (String line : getUniverseStats(Translation.getLocale(player)))
					TownyMessaging.sendMessage(player, line);
			} else if (split[0].equalsIgnoreCase("version") || split[0].equalsIgnoreCase("v")) {
				if (!permSource.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION.getNode()))
					throw new TownyException(Translatable.of("msg_err_command_disable"));

				if (TownyUpdateChecker.shouldShowNotification()) {
					TownyMessaging.sendMsg(player, Translatable.of("msg_latest_version", plugin.getVersion(), TownyUpdateChecker.getNewVersion()).stripColors(true));
				} else {
					TownyMessaging.sendMsg(player, Translatable.of("msg_towny_version", plugin.getVersion()));
					
					if (TownyUpdateChecker.hasCheckedSuccessfully())
						TownyMessaging.sendMsg(player, Translatable.of("msg_up_to_date"));
				}
			} else if (split[0].equalsIgnoreCase("spy")) {
				if (permSource.testPermission(player, PermissionNodes.TOWNY_CHAT_SPY.getNode())) {
					Resident resident = getResidentOrThrow(player.getUniqueId());
					resident.toggleMode(split, true);
				} else
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
			} else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNY, split[0])) {
				TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNY, split[0]).execute(player, "towny", split);
			} else
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_sub"));

		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
		}

	}

	private void topCommand(Player player, String[] args) throws TownyException {
		TownyUniverse universe = TownyUniverse.getInstance();
		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			towny_top.add(ChatTools.formatTitle("/towny top"));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
			towny_top.add(ChatTools.formatCommand("", "/towny top", "balance [all/town/nation]", ""));
			for (String line : towny_top)
				TownyMessaging.sendMessage(player, line);
			return;
		} 
		
		if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP.getNode(args[0].toLowerCase())))
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		
		if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<>(universe.getTowns());
				list.addAll(universe.getNations());
				towny_top.add(ChatTools.formatTitle("Most Residents"));
				towny_top.addAll(getMostResidents(list));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Town"));
				towny_top.addAll(getMostResidents(new ArrayList<>(universe.getTowns())));
			} else if (args[1].equalsIgnoreCase("nation")) {
				towny_top.add(ChatTools.formatTitle("Most Residents in a Nation"));
				towny_top.addAll(getMostResidents(new ArrayList<>(universe.getNations())));
			} else
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_sub"));
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<>(universe.getResidents());
				list.addAll(universe.getTowns());
				towny_top.add(ChatTools.formatTitle("Most Land Owned"));
				towny_top.addAll(getMostLand(list));
			} else if (args[1].equalsIgnoreCase("resident")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				towny_top.addAll(getMostLand(new ArrayList<>(universe.getResidents())));
			} else if (args[1].equalsIgnoreCase("town")) {
				towny_top.add(ChatTools.formatTitle("Most Land Owned by Town"));
				towny_top.addAll(getMostLand(new ArrayList<>(universe.getTowns())));
			} else
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_sub"));
		else if (args[0].equalsIgnoreCase("balance")) {
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<Government> list = new ArrayList<>();
				list.addAll(universe.getTowns());
				list.addAll(universe.getNations());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances"));
				Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> towny_top.addAll(getTopBankBalance(list)));
			} else if (args[1].equalsIgnoreCase("town")) {
				List<Government> list = new ArrayList<>(universe.getTowns());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances by Town"));
				Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> towny_top.addAll(getTopBankBalance(list)));
			} else if (args[1].equalsIgnoreCase("nation")) {
				List<Government> list = new ArrayList<>(universe.getNations());
				towny_top.add(ChatTools.formatTitle("Top Bank Balances by Nation"));
				Bukkit.getScheduler().runTaskAsynchronously(plugin, ()-> towny_top.addAll(getTopBankBalance(list)));
			} else {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_sub"));
			}
		}
		else
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_invalid_sub"));

		for (String line : towny_top)
			TownyMessaging.sendMessage(player, line);

		towny_top.clear();

	}

	public List<String> getUniverseStats(Locale locale) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<String> output = new ArrayList<>();
		final Translator translator = Translator.locale(locale);
		
		output.add(""); // Intentionally left blank
		output.add("\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-   " + Colors.Gold + "[" + Colors.Yellow + "Towny " + Colors.Green + plugin.getVersion() + Colors.Gold + "]");
		output.add("\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   " + Colors.Blue + translator.of("msg_universe_attribution") + Colors.LightBlue + "Chris H (Shade), ElgarL, LlmDl");
		output.add("\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   " + Colors.LightBlue + translator.of("msg_universe_contributors") + Colors.Rose + translator.of("msg_universe_heart"));
		output.add("\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-");
		output.add("\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70--   " + Colors.Blue + translator.of("res_list")+ ": " + Colors.LightBlue + townyUniverse.getNumResidents() + Colors.Gray + " | " + Colors.Blue + translator.of("town_plu") + ": " + Colors.LightBlue + townyUniverse.getTowns().size() + Colors.Gray + " | " + Colors.Blue + translator.of("nation_plu") + ": " + Colors.LightBlue + townyUniverse.getNumNations());
		output.add("\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70----   " + Colors.Blue + translator.of("world_plu") + ": " + Colors.LightBlue + townyUniverse.getTownyWorlds().size() + Colors.Gray + " | " + Colors.Blue + translator.of("townblock_plu") + ": " + Colors.LightBlue + townyUniverse.getTownBlocks().size());
		output.add("\u00A70-----\u00A74#\u00A70-----   " + Colors.LightGreen + "https://TownyAdvanced.github.io/");
		output.add(""); // Intentionally left blank
		

		// Other TownyAdvanced plugins to report versions
		int plugins = 0;
		String townyPlugins = Colors.Gold + "[";
		
		// LlmDl Sponsor exclusive
		Plugin tCamps = Bukkit.getServer().getPluginManager().getPlugin("TownyCamps");
		if (tCamps != null) {
			townyPlugins += Colors.Yellow + "TownyCamps " + Colors.Green + tCamps.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin townyChat = Bukkit.getServer().getPluginManager().getPlugin("TownyChat");
		if (townyChat != null){
			townyPlugins += Colors.Yellow + "TownyChat " + Colors.Green + townyChat.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin tCult = Bukkit.getServer().getPluginManager().getPlugin("TownyCultures");
		if (tCult != null) {
			townyPlugins += Colors.Yellow + "TownyCultures " + Colors.Green + tCult.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin tFlight = Bukkit.getServer().getPluginManager().getPlugin("TownyFlight");
		if (tFlight != null) {
			townyPlugins += Colors.Yellow + "TownyFlight " + Colors.Green + tFlight.getDescription().getVersion() + " ";
			plugins++;
		}

		// LlmDl Sponsor exclusive
		Plugin tHist = Bukkit.getServer().getPluginManager().getPlugin("TownyHistories");
		if (tHist != null) {
			townyPlugins += Colors.Yellow + "TownyHistories " + Colors.Green + tHist.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin flagWar = Bukkit.getServer().getPluginManager().getPlugin("FlagWar");
		if (flagWar != null) {
			townyPlugins += Colors.Yellow + "FlagWar " + Colors.Green + flagWar.getDescription().getVersion() + " ";
			plugins++;
		}
		
		Plugin siegeWar = Bukkit.getServer().getPluginManager().getPlugin("SiegeWar");
		if (siegeWar != null) {
			townyPlugins += Colors.Yellow + "SiegeWar " + Colors.Green + siegeWar.getDescription().getVersion() + " ";
			plugins++;
		}

		if (plugins > 0)
			output.add(townyPlugins + Colors.Gold + "]");
		return output;
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player - Player.
	 */

	public static void showMap(Player player) {

		TownyAsciiMap.generateAndSend(plugin, player, 7);
	}

	/**
	 * Returns prices for town's taxes/upkeep.
	 * @param town - The town being checked.
	 * @return - Prices screen for a town.
	 */
	public List<String> getTownyPrices(Town town, Locale locale) {

		List<String> output = new ArrayList<>();
		final Translator translator = Translator.locale(locale);
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				nation = town.getNationOrNull();

		output.add(ChatTools.formatTitle(translator.of("towny_prices_title")));
		output.add(translator.of("towny_prices_town_nation", getMoney(TownySettings.getNewTownPrice()), getMoney(TownySettings.getNewNationPrice())));
		output.add(translator.of("towny_prices_reclaim", getMoney(TownySettings.getEcoPriceReclaimTown())));
		if (town != null) {
			output.add(translator.of("towny_prices_upkeep", getMoney(TownySettings.getTownUpkeepCost(town)), getMoney(TownySettings.getNationUpkeepCost(nation))));
			output.add(translator.of("towny_prices_upkeep_based_on", (TownySettings.isUpkeepByPlot() ? translator.of("towny_prices_upkeep_num_plots") : translator.of("towny_prices_upkeep_town_level"))));
			String upkeepformula;
			if (TownySettings.isNationUpkeepPerPlot())
				upkeepformula = translator.of("towny_prices_upkeep_num_plots");
			else if (TownySettings.isNationUpkeepPerTown())
				upkeepformula = translator.of("towny_prices_upkeep_num_towns");
			else 
				upkeepformula = translator.of("towny_prices_upkeep_nation_level");
			output.add(Translation.of("towny_prices_nation_upkeep_based_on", upkeepformula));
			if (town.isOverClaimed() && TownySettings.getUpkeepPenalty() > 0)
				output.add(translator.of("towny_prices_overclaimed_upkeep", getMoney(TownySettings.getTownPenaltyUpkeepCost(town))));
			if (TownySettings.getUpkeepPenalty() > 0 )
				output.add(translator.of("towny_prices_overclaimed_based_on", (TownySettings.isUpkeepPenaltyByPlot() ? translator.of("towny_prices_overclaimed_num_plots") : translator.of("towny_prices_overclaimed_flat_cost")), TownySettings.getUpkeepPenalty()));

			output.add(translator.of("towny_prices_town_merge", getMoney(TownySettings.getBaseCostForTownMerge()), getMoney(town.getTownBlockCost()/2)));
			output.add(translator.of("towny_prices_claiming_townblock", getMoney(town.getTownBlockCost()) +  
					(Double.valueOf(TownySettings.getClaimPriceIncreaseValue()).equals(1.0) ? "" : translator.of("towny_prices_claiming_townblock_increase", new DecimalFormat("##.##%").format(TownySettings.getClaimPriceIncreaseValue()-1)))));
			output.add(translator.of("towny_prices_claiming_outposts", getMoney(TownySettings.getOutpostCost())));
		}
		if (town == null)
			output.add(translator.of("towny_prices_upkeep", getMoney(TownySettings.getTownUpkeep()), getMoney(TownySettings.getNationUpkeep())));

		if (town != null) {
			output.add(translator.of("towny_prices_townname", town.getFormattedName()));
			output.add(translator.of("towny_prices_price_plot", getMoney(town.getPlotPrice()),getMoney(TownySettings.getOutpostCost())));
			output.add(translator.of("towny_prices_price_shop", getMoney(town.getCommercialPlotPrice()), getMoney(town.getEmbassyPlotPrice())));

			output.add(translator.of("towny_prices_taxes_plot", (town.isTaxPercentage()? town.getTaxes() + "%" : getMoney(town.getTaxes())), getMoney(town.getPlotTax())));
			output.add(translator.of("towny_prices_taxes_shop", getMoney(town.getCommercialPlotTax()), getMoney(town.getEmbassyPlotTax())));
			output.add(translator.of("towny_prices_town_neutral_tax", getMoney(TownySettings.getTownNeutralityCost())));
			
			output.add(translator.of("towny_prices_plots"));
			List<TownBlockType> townBlockTypes = new ArrayList<>(TownBlockTypeHandler.getTypes().values());
			for (int i = 0; i < townBlockTypes.size(); i++) {
				if (i == townBlockTypes.size() - 1)
					output.add(translator.of("towny_prices_type_single", townBlockTypes.get(i).getFormattedName(), getMoney(townBlockTypes.get(i).getCost())));
				else {
					output.add(translator.of("towny_prices_type_double",
						townBlockTypes.get(i).getFormattedName(), getMoney(townBlockTypes.get(i).getCost()),
						townBlockTypes.get(i+1).getFormattedName(), getMoney(townBlockTypes.get(i+1).getCost())
					));

					i++;
				}
			}

			if (nation != null) {
				output.add(translator.of("towny_prices_nationname", nation.getFormattedName()));
				output.add(translator.of("towny_prices_nation_tax", nation.getTaxes(), getMoney(TownySettings.getNationNeutralityCost())));
			}
		}
		return output;
	}
	
	private String getMoney(double cost) {
		return TownyEconomyHandler.getFormattedBalance(cost);
	}
	
	public List<String> getTopBankBalance(final List<Government> governments) {
		final int maxListing = TownySettings.getTownyTopSize();
		final List<String> output = new ArrayList<>();

		// Sort by their bank balance first
		governments.sort(GovernmentComparators.BY_BANK_BALANCE);

		int index = 0;
		// Loop through each one (already sorted) and add to the map
		for (final Government gov : governments) {
			index++;
			if (maxListing != -1 && index > maxListing) {
				break;
			}
			output.add(String.format(Colors.LightGray + "%-20s " + Colors.Gold + "|" + Colors.Blue + " %s", gov.getFormattedName(), getMoney(gov.getAccount().getCachedBalance())));
		}
		return output;
	}

	public List<String> getMostResidents(List<ResidentList> list) {
		final int maxListing = TownySettings.getTownyTopSize();

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
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", ((TownyObject) residentList).getFormattedName(), kv.value));
		}
		return output;
	}

	public List<String> getMostLand(List<TownBlockOwner> list) {
		final int maxListing = TownySettings.getTownyTopSize();

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
			String name = null;
			if (kv.key instanceof Town)
				name = ((Town) kv.key).getFormattedName();
			else 
				name = ((Resident) kv.key).getFormattedName();
			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", name, kv.value));
		}
		return output;
	}

	public void consoleUseOnly(Player player) {

		TownyMessaging.sendErrorMsg(player, "This command was designed for use in the console only.");
	}
}
