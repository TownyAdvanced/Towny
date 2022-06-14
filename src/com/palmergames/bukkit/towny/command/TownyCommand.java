package com.palmergames.bukkit.towny.command;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
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
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	// protected static TownyUniverse universe;
	private static Towny plugin;
	private final Component[] heart = new Component[] {
		Component.text("-", NamedTextColor.BLACK).append(Component.text("###", NamedTextColor.DARK_RED)).append(Component.text("---", NamedTextColor.BLACK)).append(Component.text("###", NamedTextColor.DARK_RED)).append(Component.text("-   ", NamedTextColor.BLACK)),
		Component.text("#", NamedTextColor.DARK_RED).append(Component.text("###", NamedTextColor.RED)).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("-", NamedTextColor.BLACK)).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("###", NamedTextColor.RED)).append(Component.text("#   ", NamedTextColor.DARK_RED)),
		Component.text("#", NamedTextColor.DARK_RED).append(Component.text("####", NamedTextColor.RED)).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("####", NamedTextColor.RED)).append(Component.text("#   ", NamedTextColor.DARK_RED)),
		Component.text("-", NamedTextColor.BLACK).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("#######", NamedTextColor.RED)).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("-   ", NamedTextColor.BLACK)),
		Component.text("--", NamedTextColor.BLACK).append(Component.text("##", NamedTextColor.DARK_RED)).append(Component.text("###", NamedTextColor.RED)).append(Component.text("##", NamedTextColor.DARK_RED)).append(Component.text("--   ", NamedTextColor.BLACK)),
		Component.text("----", NamedTextColor.BLACK).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("#", NamedTextColor.RED)).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("----   ", NamedTextColor.BLACK)),
		Component.text("-----", NamedTextColor.BLACK).append(Component.text("#", NamedTextColor.DARK_RED)).append(Component.text("-----   ", NamedTextColor.BLACK))
	};

	private static final List<String> townyTabCompletes = Arrays.asList(
		"map",
		"prices",
		"time",
		"top",
		"spy",
		"universe",
		"version",
		"v",
		"tree",
		"switches",
		"itemuse",
		"allowedblocks",
		"wildsblocks",
		"plotclearblocks"
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
		if (plugin.isError()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_safe_mode"));
			return true;
		}

		parseTownyCommand(sender, args);
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
				if (args.length == 1)
					return NameUtil.filterByStart(TownyCommandAddonAPI.getTabCompletes(CommandType.TOWNY, townyTabCompletes), args[0]);
				else if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNY, args[0]))
					return NameUtil.filterByStart(TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNY, args[0]).getTabCompletion(sender, args), args[args.length-1]);
		}

		return Collections.emptyList();
	}

	private void parseTownyCommand(CommandSender sender, String[] split) {
		TownyPermissionSource permSource = TownyUniverse.getInstance().getPermissionSource();
		if (split.length == 0) {
			HelpMenu.GENERAL_HELP.send(sender);
			return;
		} else if (split[0].equalsIgnoreCase("?") || split[0].equalsIgnoreCase("help")) {
			HelpMenu.HELP.send(sender);
			return;
		}
		
		Player player = null;
		TownyWorld world = null;
		if (sender instanceof Player) {
			player = (Player) sender;
			world = TownyAPI.getInstance().getTownyWorld(player.getWorld());
		}

		try {
			switch(split[0].toLowerCase(Locale.ROOT)) {
				case "map": {
					catchConsole(sender);
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));
					if (split.length > 1 && split[1].equalsIgnoreCase("big"))
						TownyAsciiMap.generateAndSend(plugin, player, 18);
					else if (split.length > 1 && split[1].equalsIgnoreCase("hud"))
						HUDManager.toggleMapHud(player);
					else
						showMap(player);
					break;
				}
				case "prices": {
					Town town = null;
					if (!TownyEconomyHandler.isActive())
						throw new TownyException(Translatable.of("msg_err_no_economy"));

					if (split.length > 1) {
						town = TownyUniverse.getInstance().getTown(split[1]);

						if (town == null) {
							TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_not_registered_1", split[1]));
							return;
						}
					} else if (player != null) {
						Resident resident = TownyAPI.getInstance().getResident(player);
						
						if (resident != null)
							town = resident.getTownOrNull();
					}

					TownyMessaging.sendMessage(sender, getTownyPrices(town, Translator.locale(Translation.getLocale(sender))));
					break;
				}
				case "switches": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.SWITCHES);
					break;
				}
				case "itemuse": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ITEMUSE);
					break;
				}
				case "allowedblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ALLOWEDBLOCKS);
					break;
				}
				case "wildsblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					ResidentUtil.openGUIInventory(resident, world.getUnclaimedZoneIgnoreMaterials(), Translatable.of("gui_title_towny_wildsblocks").forLocale(player));
					break;
				}
				case "plotclearblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					ResidentUtil.openGUIInventory(resident, world.getPlotManagementMayorDelete(), Translatable.of("gui_title_towny_plotclear").forLocale(player));
					break;
				}
				case "top": {
					parseTopCommand(sender, StringMgmt.remFirstArg(split));
					break;
				}
				case "tree": {
					catchPlayer(sender);
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TREE))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					for (String line : TownyUniverse.getInstance().getTreeString(0))
						TownyMessaging.sendMessage(sender, line);
					break;
				}
				case "time": {
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TIME))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					TownyMessaging.sendMsg(sender, Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true))));
					break;
				}
				case "universe": {
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					TownyMessaging.sendMessage(sender, getUniverseStats(Translator.locale(Translation.getLocale(sender))));
					break;
				}
				case "version":
				case "v": {
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					if (TownyUpdateChecker.shouldShowNotification()) {
						TownyMessaging.sendMsg(sender, Translatable.of("msg_latest_version", plugin.getVersion(), TownyUpdateChecker.getNewVersion()));
					} else {
						TownyMessaging.sendMsg(sender, Translatable.of("msg_towny_version", plugin.getVersion()));

						if (TownyUpdateChecker.hasCheckedSuccessfully())
							TownyMessaging.sendMsg(sender, Translatable.of("msg_up_to_date"));
					}
					break;
				}
				case "spy": {
					catchConsole(sender);
					
					if (!permSource.testPermission(sender, PermissionNodes.TOWNY_CHAT_SPY.getNode()))
						throw new TownyException(Translatable.of("msg_err_command_disable"));

					Resident resident = getResidentOrThrow(player.getUniqueId());
					resident.toggleMode(split, true);
					break;
				}
				default: {
					if (TownyCommandAddonAPI.hasCommand(CommandType.TOWNY, split[0]))
						TownyCommandAddonAPI.getAddonCommand(CommandType.TOWNY, split[0]).execute(sender, "towny", split);
					else
						TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
				}
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.message(sender));
		}

	}

	private void parseTopCommand(CommandSender sender, String[] args) throws TownyException {
		List<Component> townyTop = new ArrayList<>();
		TownyUniverse universe = TownyUniverse.getInstance();

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			townyTop.add(ChatTools.formatTitle("/towny top"));
			townyTop.add(ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			townyTop.add(ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
			townyTop.add(ChatTools.formatCommand("", "/towny top", "balance [all/town/nation]", ""));
			TownyMessaging.sendMessage(sender, townyTop);
			return;
		} 
		
		if (!TownyUniverse.getInstance().getPermissionSource().testPermission(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP.getNode(args[0].toLowerCase())))
			throw new TownyException(Translatable.of("msg_err_command_disable"));
		
		if (args[0].equalsIgnoreCase("residents"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = new ArrayList<>(universe.getTowns());
				list.addAll(universe.getNations());
				townyTop.add(ChatTools.formatTitle("Most Residents"));
				townyTop.addAll(getMostResidents(list));
			} else if (args[1].equalsIgnoreCase("town")) {
				townyTop.add(ChatTools.formatTitle("Most Residents in a Town"));
				townyTop.addAll(getMostResidents(new ArrayList<>(universe.getTowns())));
			} else if (args[1].equalsIgnoreCase("nation")) {
				townyTop.add(ChatTools.formatTitle("Most Residents in a Nation"));
				townyTop.addAll(getMostResidents(new ArrayList<>(universe.getNations())));
			} else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
		else if (args[0].equalsIgnoreCase("land"))
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<>(universe.getResidents());
				list.addAll(universe.getTowns());
				townyTop.add(ChatTools.formatTitle("Most Land Owned"));
				townyTop.addAll(getMostLand(list));
			} else if (args[1].equalsIgnoreCase("resident")) {
				townyTop.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				townyTop.addAll(getMostLand(new ArrayList<>(universe.getResidents())));
			} else if (args[1].equalsIgnoreCase("town")) {
				townyTop.add(ChatTools.formatTitle("Most Land Owned by Town"));
				townyTop.addAll(getMostLand(new ArrayList<>(universe.getTowns())));
			} else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
		else if (args[0].equalsIgnoreCase("balance")) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
				if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
					List<Government> list = new ArrayList<>();
					list.addAll(universe.getTowns());
					list.addAll(universe.getNations());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances"));
					townyTop.addAll(getTopBankBalance(list));
				} else if (args[1].equalsIgnoreCase("town")) {
					List<Government> list = new ArrayList<>(universe.getTowns());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances by Town"));
					townyTop.addAll(getTopBankBalance(list));
				} else if (args[1].equalsIgnoreCase("nation")) {
					List<Government> list = new ArrayList<>(universe.getNations());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances by Nation"));
					townyTop.addAll(getTopBankBalance(list));
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
				}
				TownyMessaging.sendMessage(sender, townyTop);
			});
			return;
		}
		else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));

		TownyMessaging.sendMessage(sender, townyTop);
	}

	public List<Component> getUniverseStats(Translator translator) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Component> output = new ArrayList<>();
		
		output.add(Component.empty()); // Intentionally left blank
		output.add(heart[0].append(Component.text("[", NamedTextColor.GOLD)).append(Component.text("Towny ", NamedTextColor.YELLOW)).append(Component.text(plugin.getVersion(), NamedTextColor.DARK_GREEN)).append(Component.text("]", NamedTextColor.GOLD)));
		output.add(heart[1].append(translator.comp("msg_universe_attribution").color(NamedTextColor.DARK_AQUA)).append(Component.text("Chris H (Shade), ElgarL, LlmDl", NamedTextColor.AQUA)));
		output.add(heart[2].append(translator.comp("msg_universe_contributors").color(NamedTextColor.AQUA)).append(translator.comp("msg_universe_heart").color(NamedTextColor.RED)));
		output.add(heart[3]);
		output.add(heart[4].append(Component.text(translator.of("res_list") + ": ", NamedTextColor.DARK_AQUA)).append(Component.text(townyUniverse.getNumResidents(), NamedTextColor.AQUA)).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(Component.text(translator.of("town_plu") + ": ", NamedTextColor.DARK_AQUA)).append(Component.text(townyUniverse.getTowns().size(), NamedTextColor.AQUA)).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(Component.text(translator.of("nation_plu") + ": ", NamedTextColor.DARK_AQUA)).append(Component.text(townyUniverse.getNumNations(), NamedTextColor.AQUA)));
		output.add(heart[5].append(Component.text(translator.of("world_plu") + ": ", NamedTextColor.DARK_AQUA)).append(Component.text(townyUniverse.getTownyWorlds().size(), NamedTextColor.AQUA)).append(Component.text(" | ", NamedTextColor.DARK_GRAY)).append(Component.text(translator.of("townblock_plu") + ": ", NamedTextColor.DARK_AQUA)).append(Component.text(townyUniverse.getTownBlocks().size(), NamedTextColor.AQUA)));
		output.add(heart[6].append(Component.text("https://TownyAdvanced.github.io/", NamedTextColor.GREEN)));
		output.add(Component.empty()); // Intentionally left blank
		

		// Other TownyAdvanced plugins to report versions
		int plugins = 0;
		Component townyPlugins = Component.text("[", NamedTextColor.GOLD);
		
		// LlmDl Sponsor exclusive
		Plugin tCamps = Bukkit.getServer().getPluginManager().getPlugin("TownyCamps");
		if (tCamps != null) {
			townyPlugins = townyPlugins.append(Component.text("TownyCamps ", NamedTextColor.YELLOW)).append(Component.text(tCamps.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}
		
		Plugin townyChat = Bukkit.getServer().getPluginManager().getPlugin("TownyChat");
		if (townyChat != null){
			townyPlugins = townyPlugins.append(Component.text("TownyChat ", NamedTextColor.YELLOW)).append(Component.text(townyChat.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}
		
		Plugin tCult = Bukkit.getServer().getPluginManager().getPlugin("TownyCultures");
		if (tCult != null) {
			townyPlugins = townyPlugins.append(Component.text("TownyCultures ", NamedTextColor.YELLOW)).append(Component.text(tCult.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}
		
		Plugin tFlight = Bukkit.getServer().getPluginManager().getPlugin("TownyFlight");
		if (tFlight != null) {
			townyPlugins = townyPlugins.append(Component.text("TownyFlight ", NamedTextColor.YELLOW)).append(Component.text(tFlight.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}

		// LlmDl Sponsor exclusive
		Plugin tHist = Bukkit.getServer().getPluginManager().getPlugin("TownyHistories");
		if (tHist != null) {
			townyPlugins = townyPlugins.append(Component.text("TownyHistories ", NamedTextColor.YELLOW)).append(Component.text(tHist.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}
		
		Plugin flagWar = Bukkit.getServer().getPluginManager().getPlugin("FlagWar");
		if (flagWar != null) {
			townyPlugins = townyPlugins.append(Component.text("FlagWar ", NamedTextColor.YELLOW)).append(Component.text(flagWar.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}
		
		Plugin siegeWar = Bukkit.getServer().getPluginManager().getPlugin("SiegeWar");
		if (siegeWar != null) {
			townyPlugins = townyPlugins.append(Component.text("SiegeWar ", NamedTextColor.YELLOW)).append(Component.text(siegeWar.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}

		Plugin eventWar = Bukkit.getServer().getPluginManager().getPlugin("EventWar");
		if (eventWar != null) {
			townyPlugins = townyPlugins.append(Component.text("EventWar ", NamedTextColor.YELLOW)).append(Component.text(eventWar.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}

		Plugin townyRTP = Bukkit.getServer().getPluginManager().getPlugin("TownyRTP");
		if (townyRTP != null) {
			townyPlugins = townyPlugins.append(Component.text("TownyRTP ", NamedTextColor.YELLOW)).append(Component.text(townyRTP.getDescription().getVersion() + " ", NamedTextColor.DARK_GREEN));
			plugins++;
		}

		if (plugins > 0)
			output.add(townyPlugins.append(Component.text("]", NamedTextColor.GOLD)));

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
	 * @param translator - The Translator to use.
	 * @return - Prices screen for a town.
	 */
	public List<Component> getTownyPrices(Town town, Translator translator) {

		List<Component> output = new ArrayList<>();
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				nation = town.getNationOrNull();

		output.add(ChatTools.formatTitle(translator.of("towny_prices_title")));
		output.add(translator.comp("towny_prices_town_nation", getMoney(TownySettings.getNewTownPrice()), getMoney(TownySettings.getNewNationPrice())));
		output.add(translator.comp("towny_prices_reclaim", getMoney(TownySettings.getEcoPriceReclaimTown())));
		if (town != null) {
			output.add(translator.comp("towny_prices_upkeep", getMoney(TownySettings.getTownUpkeepCost(town)), getMoney(TownySettings.getNationUpkeepCost(nation))));
			output.add(translator.comp("towny_prices_upkeep_based_on", (TownySettings.isUpkeepByPlot() ? translator.of("towny_prices_upkeep_num_plots") : translator.of("towny_prices_upkeep_town_level"))));
			String upkeepformula;
			if (TownySettings.isNationUpkeepPerPlot())
				upkeepformula = translator.of("towny_prices_upkeep_num_plots");
			else if (TownySettings.isNationUpkeepPerTown())
				upkeepformula = translator.of("towny_prices_upkeep_num_towns");
			else 
				upkeepformula = translator.of("towny_prices_upkeep_nation_level");
			output.add(translator.comp("towny_prices_nation_upkeep_based_on", upkeepformula));
			if (town.isOverClaimed() && TownySettings.getUpkeepPenalty() > 0)
				output.add(translator.comp("towny_prices_overclaimed_upkeep", getMoney(TownySettings.getTownPenaltyUpkeepCost(town))));
			if (TownySettings.getUpkeepPenalty() > 0 )
				output.add(translator.comp("towny_prices_overclaimed_based_on", (TownySettings.isUpkeepPenaltyByPlot() ? translator.of("towny_prices_overclaimed_num_plots") : translator.of("towny_prices_overclaimed_flat_cost")), TownySettings.getUpkeepPenalty()));

			output.add(translator.comp("towny_prices_town_merge", getMoney(TownySettings.getBaseCostForTownMerge()), getMoney(town.getTownBlockCost()/2)));
			output.add(translator.comp("towny_prices_claiming_townblock", getMoney(town.getTownBlockCost()) +  
					(Double.valueOf(TownySettings.getClaimPriceIncreaseValue()).equals(1.0) ? "" : translator.of("towny_prices_claiming_townblock_increase", new DecimalFormat("##.##%").format(TownySettings.getClaimPriceIncreaseValue()-1)))));
			output.add(translator.comp("towny_prices_claiming_outposts", getMoney(TownySettings.getOutpostCost())));
		}
		if (town == null)
			output.add(translator.comp("towny_prices_upkeep", getMoney(TownySettings.getTownUpkeep()), getMoney(TownySettings.getNationUpkeep())));

		if (town != null) {
			output.add(translator.comp("towny_prices_townname", town.getFormattedName()));
			output.add(translator.comp("towny_prices_price_plot", getMoney(town.getPlotPrice()),getMoney(TownySettings.getOutpostCost())));
			output.add(translator.comp("towny_prices_price_shop", getMoney(town.getCommercialPlotPrice()), getMoney(town.getEmbassyPlotPrice())));

			output.add(translator.comp("towny_prices_taxes_plot", (town.isTaxPercentage()? town.getTaxes() + "%" : getMoney(town.getTaxes())), getMoney(town.getPlotTax())));
			output.add(translator.comp("towny_prices_taxes_shop", getMoney(town.getCommercialPlotTax()), getMoney(town.getEmbassyPlotTax())));
			output.add(translator.comp("towny_prices_town_neutral_tax", getMoney(TownySettings.getTownNeutralityCost())));
			
			output.add(translator.comp("towny_prices_plots"));
			List<TownBlockType> townBlockTypes = new ArrayList<>(TownBlockTypeHandler.getTypes().values());
			for (int i = 0; i < townBlockTypes.size(); i++) {
				if (i == townBlockTypes.size() - 1)
					output.add(translator.comp("towny_prices_type_single", townBlockTypes.get(i).getFormattedName(), getMoney(townBlockTypes.get(i).getCost())));
				else {
					output.add(translator.comp("towny_prices_type_double",
						townBlockTypes.get(i).getFormattedName(), getMoney(townBlockTypes.get(i).getCost()),
						townBlockTypes.get(i+1).getFormattedName(), getMoney(townBlockTypes.get(i+1).getCost())
					));

					i++;
				}
			}

			if (nation != null) {
				output.add(translator.comp("towny_prices_nationname", nation.getFormattedName()));
				output.add(translator.comp("towny_prices_nation_tax", nation.getTaxes(), getMoney(TownySettings.getNationNeutralityCost())));
			}
		}
		return output;
	}
	
	private String getMoney(double cost) {
		return TownyEconomyHandler.getFormattedBalance(cost);
	}
	
	public List<Component> getTopBankBalance(final List<Government> governments) {
		final int maxListing = TownySettings.getTownyTopSize();
		final List<Component> output = new ArrayList<>();

		// Sort by their bank balance first
		governments.sort(GovernmentComparators.BY_BANK_BALANCE);

		int index = 0;
		// Loop through each one (already sorted) and add to the map
		for (final Government gov : governments) {
			index++;
			if (maxListing != -1 && index > maxListing) {
				break;
			}
			output.add(TownyComponents.miniMessage(String.format("<gray>%-20s <gold>| <dark_aqua>%s", gov.getFormattedName(), getMoney(gov.getAccount().getCachedBalance()))));
		}
		return output;
	}

	public List<Component> getMostResidents(List<ResidentList> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<Component> output = new ArrayList<>();
		list.sort(Comparator.comparingInt(residentList -> residentList.getResidents().size()));
		Collections.reverse(list);

		int n = 0;
		for (ResidentList residentList : list) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;

			output.add(TownyComponents.miniMessage(String.format("<dark_aqua>%30s <gold>| <gray>%10d", ((TownyObject) residentList).getFormattedName(), residentList.getResidents().size())));
		}

		return output;
	}

	public List<Component> getMostLand(List<TownBlockOwner> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<Component> output = new ArrayList<>();
		list.sort(Comparator.comparingInt(owner -> owner.getTownBlocks().size()));
		Collections.reverse(list);

		int n = 0;
		for (TownBlockOwner owner : list) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;

			output.add(TownyComponents.miniMessage(String.format("<dark_aqua>%30s <gold>| <gray>%10d", owner.getFormattedName(), owner.getTownBlocks().size())));
		}

		return output;
	}
}
