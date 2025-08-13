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
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.huds.HUDManager;
import com.palmergames.bukkit.towny.object.BuildInfo;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.comparators.GovernmentComparators;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.gui.SelectionGUI;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import com.palmergames.util.TimeMgmt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class TownyCommand extends BaseCommand implements CommandExecutor {

	private final Towny plugin;

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
	public boolean onCommand(@NotNull CommandSender sender, @NotNull Command cmd, @NotNull String label, String[] args) {
		if (plugin.isError()) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_safe_mode"));
			return true;
		}

		parseTownyCommand(sender, args);
		return true;
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		switch (args[0].toLowerCase(Locale.ROOT)) {
			case "top":
				switch (args.length) {
					case 2:
						return NameUtil.filterByStart(townyTopTabCompletes, args[1]);
					case 3:
						switch (args[1].toLowerCase(Locale.ROOT)) {
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
		if (split.length == 0) {
			if (TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(sender))
				HelpMenu.GENERAL_HELP_ADMIN.send(sender);
			else
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
					checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode());
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
						town = getTownOrThrow(split[1]);
					} else if (player != null) {
						Resident resident = TownyAPI.getInstance().getResident(player);
						
						if (resident != null)
							town = resident.getTownOrNull();
					}

					for (String line : getTownyPrices(town, Translator.locale(sender)))
						TownyMessaging.sendMessage(sender, line);
					break;
				}
				case "switches": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player);
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.SWITCHES);
					break;
				}
				case "itemuse": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player);
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ITEMUSE);
					break;
				}
				case "allowedblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player);
					ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ALLOWEDBLOCKS);
					break;
				}
				case "wildsblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player);
					ResidentUtil.openGUIInventory(resident, world.getUnclaimedZoneIgnoreMaterials(), Translatable.of("gui_title_towny_wildsblocks").forLocale(player));
					break;
				}
				case "plotclearblocks": {
					catchConsole(sender);
					if (world == null || !world.isUsingTowny())
						throw new TownyException(Translatable.of("msg_err_usingtowny_disabled"));

					Resident resident = getResidentOrThrow(player);
					ResidentUtil.openGUIInventory(resident, world.getPlotManagementMayorDelete(), Translatable.of("gui_title_towny_plotclear").forLocale(player));
					break;
				}
				case "top": {
					parseTopCommand(sender, StringMgmt.remFirstArg(split));
					break;
				}
				case "tree": {
					catchPlayer(sender);
					checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TREE.getNode());

					for (String line : TownyUniverse.getInstance().getTreeString(0))
						TownyMessaging.sendMessage(sender, line);
					break;
				}
				case "time": {
					checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TIME.getNode());

					TownyMessaging.sendMsg(sender, Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true))));
					break;
				}
				case "universe": {
					checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE.getNode());

					for (String line : getUniverseStats(Translator.locale(sender)))
						TownyMessaging.sendMessage(sender, line);
					break;
				}
				case "version":
				case "v": {
					checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION.getNode());

					if (TownyUpdateChecker.shouldShowNotification()) {
						TownyMessaging.sendMsg(sender, Translatable.of("msg_latest_version", plugin.getVersion(), TownyUpdateChecker.getNewVersion()));
					} else {
						TownyMessaging.sendMsg(sender, Translatable.of("msg_towny_version", plugin.getVersion()));
						
						try {
							final BuildInfo buildInfo = BuildInfo.retrieveBuildInfo(plugin);
							
							String repositoryUrl = buildInfo.repositoryUrl();
							if (repositoryUrl.endsWith(".git")) {
								repositoryUrl = repositoryUrl.substring(0, repositoryUrl.length() - ".git".length());
							}

							final String viewCommitUrl = repositoryUrl + "/commit/" + buildInfo.commit();

							final Component buildInfoMessage = Translatable.of("default_towny_prefix").append(
								Translatable.of("msg_version_build_info", buildInfo.commitShort(), buildInfo.branch()).component(Translation.getLocale(sender))
									.clickEvent(viewCommitUrl.startsWith("http") ? ClickEvent.openUrl(viewCommitUrl) : null)
									.hoverEvent(HoverEvent.showText(Component.text(buildInfo.message(), NamedTextColor.GREEN)))
							).component(Translation.getLocale(sender));

							Towny.getAdventure().sender(sender).sendMessage(buildInfoMessage);
						} catch (IOException e) {
							plugin.getLogger().log(Level.WARNING, "Could not retrieve build information", e);
							TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_version_build_info_failed"));
						}

						if (TownyUpdateChecker.hasCheckedSuccessfully())
							TownyMessaging.sendMsg(sender, Translatable.of("msg_up_to_date"));
					}
					break;
				}
				case "spy": {
					catchConsole(sender);
					ResidentModeHandler.toggleMode(getResidentOrThrow(player), "spy", true);
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
			TownyMessaging.sendErrorMsg(sender, e.getMessage(sender));
		}

	}

	private void parseTopCommand(CommandSender sender, String[] args) throws TownyException {
		List<String> townyTop = new ArrayList<>();
		TownyUniverse universe = TownyUniverse.getInstance();

		if (args.length == 0 || args[0].equalsIgnoreCase("?")) {
			HelpMenu.TOWNY_TOP_HELP.send(sender);
			return;
		} 

		if (args[0].equalsIgnoreCase("residents")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_RESIDENTS.getNode());
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<ResidentList> list = universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList());
				list.addAll(universe.getNations());
				townyTop.add(ChatTools.formatTitle("Most Residents"));
				townyTop.addAll(getMostResidents(list));
			} else if (args[1].equalsIgnoreCase("town")) {
				townyTop.add(ChatTools.formatTitle("Most Residents in a Town"));
				townyTop.addAll(getMostResidents(universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList())));
			} else if (args[1].equalsIgnoreCase("nation")) {
				townyTop.add(ChatTools.formatTitle("Most Residents in a Nation"));
				townyTop.addAll(getMostResidents(new ArrayList<>(universe.getNations())));
			} else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
		} else if (args[0].equalsIgnoreCase("land")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_LAND.getNode());
			if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
				List<TownBlockOwner> list = new ArrayList<>(universe.getResidents());
				list.addAll(universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList()));
				townyTop.add(ChatTools.formatTitle("Most Land Owned"));
				townyTop.addAll(getMostLand(list));
			} else if (args[1].equalsIgnoreCase("resident")) {
				townyTop.add(ChatTools.formatTitle("Most Land Owned by Resident"));
				townyTop.addAll(getMostLand(new ArrayList<>(universe.getResidents())));
			} else if (args[1].equalsIgnoreCase("town")) {
				townyTop.add(ChatTools.formatTitle("Most Land Owned by Town"));
				townyTop.addAll(getMostLand(universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList())));
			} else
				TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
		} else if (args[0].equalsIgnoreCase("balance")) {
			checkPermOrThrow(sender, PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_BALANCE.getNode());
			plugin.getScheduler().runAsync(() -> {
				if (args.length == 1 || args[1].equalsIgnoreCase("all")) {
					List<Government> list = new ArrayList<>();
					list.addAll(universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList()));
					list.addAll(universe.getNations());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances"));
					townyTop.addAll(getTopBankBalance(list));
				} else if (args[1].equalsIgnoreCase("town")) {
					List<Government> list = universe.getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances by Town"));
					townyTop.addAll(getTopBankBalance(list));
				} else if (args[1].equalsIgnoreCase("nation")) {
					List<Government> list = new ArrayList<>(universe.getNations());
					townyTop.add(ChatTools.formatTitle("Top Bank Balances by Nation"));
					townyTop.addAll(getTopBankBalance(list));
				} else {
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));
				}
				for (String line : townyTop)
					TownyMessaging.sendMessage(sender, line);
			});
			return;
		}
		else
			TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_invalid_sub"));

		for (String line : townyTop)
			TownyMessaging.sendMessage(sender, line);
	}

	public List<String> getUniverseStats(Translator translator) {
		String heart1 = "\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-   ";
		String heart2 = "\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   ";
		String heart3 = "\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   ";
		String heart4 = "\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-";
		String heart5 = "\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70--   ";
		String heart6 = "\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70----   ";
		String heart7 = "\u00A70-----\u00A74#\u00A70-----   ";
		String splitter = Colors.Gray + " | ";
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<String> output = new ArrayList<>();
		output.add(""); // Intentionally left blank
		output.add(heart1 + Colors.Gold + "[" + Colors.Yellow + "Towny " + Colors.Green + plugin.getVersion() + Colors.Gold + "]");
		output.add(heart2 + Colors.Blue + translator.of("msg_universe_attribution") + Colors.LightBlue + "LlmDl, Warrior, ElgarL, Chris H (Shade)");
		output.add(heart3 + Colors.LightBlue + translator.of("msg_universe_contributors") + Colors.Rose + translator.of("msg_universe_heart"));
		output.add(heart4);
		output.add(heart5 + Colors.Blue + translator.of("res_list")+ ": " + Colors.LightBlue + townyUniverse.getNumResidents() + splitter
				+ Colors.Blue + translator.of("town_plu") + ": " + Colors.LightBlue + townyUniverse.getTowns().size() + splitter
				+ Colors.Blue + translator.of("nation_plu") + ": " + Colors.LightBlue + townyUniverse.getNumNations());
		output.add(heart6 + Colors.Blue + translator.of("world_plu") + ": " + Colors.LightBlue + townyUniverse.getTownyWorlds().size() + splitter
				+ Colors.Blue + translator.of("townblock_plu") + ": " + Colors.LightBlue + townyUniverse.getTownBlocks().size());
		output.add(heart7 + Colors.LightGreen + "https://TownyAdvanced.github.io/");
		output.add(""); // Intentionally left blank

		// Other TownyAdvanced plugins to report versions
		String townyPlugins = "";
		List<String> pluginList = PluginIntegrations.getInstance().getTownyPluginsForUniverseCommand();
		townyPlugins = String.join(" ", pluginList);
		if (pluginList.size() > 0)
			output.add(Colors.Gold + "[" + townyPlugins + Colors.Gold + "]");

		return output;
	}

	/**
	 * Send a map of the nearby townblocks status to player Command: /towny map
	 * 
	 * @param player - Player.
	 */

	public static void showMap(Player player) {

		TownyAsciiMap.generateAndSend(Towny.getPlugin(), player, TownySettings.asciiMapHeight());
	}

	/**
	 * Returns prices for town's taxes/upkeep.
	 * @param town - The town being checked.
	 * @param translator - The Translator to use.
	 * @return - Prices screen for a town.
	 */
	public List<String> getTownyPrices(Town town, Translator translator) {

		List<String> output = new ArrayList<>();
		Nation nation = null;

		if (town != null)
			if (town.hasNation())
				nation = town.getNationOrNull();

		output.add(ChatTools.formatTitle(translator.of("towny_prices_title")));
		output.add(translator.of("towny_prices_town_nation", prettyMoney(TownySettings.getNewTownPrice()), prettyMoney(TownySettings.getNewNationPrice())));
		output.add(translator.of("towny_prices_reclaim", prettyMoney(TownySettings.getEcoPriceReclaimTown())));
		if (town != null) {
			output.add(translator.of("towny_prices_upkeep", prettyMoney(TownySettings.getTownUpkeepCost(town)), prettyMoney(TownySettings.getNationUpkeepCost(nation))));
			output.add(translator.of("towny_prices_upkeep_based_on", (TownySettings.isUpkeepByPlot() ? translator.of("towny_prices_upkeep_num_plots") : translator.of("towny_prices_upkeep_town_level"))));
			String upkeepformula;
			if (TownySettings.isNationUpkeepPerPlot())
				upkeepformula = translator.of("towny_prices_upkeep_num_plots");
			else if (TownySettings.isNationUpkeepPerTown())
				upkeepformula = translator.of("towny_prices_upkeep_num_towns");
			else 
				upkeepformula = translator.of("towny_prices_upkeep_nation_level");
			output.add(translator.of("towny_prices_nation_upkeep_based_on", upkeepformula));
			if (town.isOverClaimed() && TownySettings.getUpkeepPenalty() > 0)
				output.add(translator.of("towny_prices_overclaimed_upkeep", prettyMoney(TownySettings.getTownPenaltyUpkeepCost(town))));
			if (TownySettings.getUpkeepPenalty() > 0 )
				output.add(translator.of("towny_prices_overclaimed_based_on", (TownySettings.isUpkeepPenaltyByPlot() ? translator.of("towny_prices_overclaimed_num_plots") : translator.of("towny_prices_overclaimed_flat_cost")), TownySettings.getUpkeepPenalty()));

			output.add(translator.of("towny_prices_town_merge", prettyMoney(TownySettings.getBaseCostForTownMerge()), prettyMoney(town.getTownBlockCost()/2)));
			output.add(translator.of("towny_prices_claiming_townblock", prettyMoney(town.getTownBlockCost()) +  
					(Double.valueOf(TownySettings.getClaimPriceIncreaseValue()).equals(1.0) ? "" : translator.of("towny_prices_claiming_townblock_increase", new DecimalFormat("##.##%").format(TownySettings.getClaimPriceIncreaseValue()-1)))));
			output.add(translator.of("towny_prices_claiming_outposts", prettyMoney(TownySettings.getOutpostCost())));
			if (TownySettings.getPerOutpostUpkeepCost() > 0)
				output.add(translator.of("towny_prices_outposts_upkeept", prettyMoney(TownySettings.getPerOutpostUpkeepCost())));
		}
		if (town == null)
			output.add(translator.of("towny_prices_upkeep", prettyMoney(TownySettings.getTownUpkeep()), prettyMoney(TownySettings.getNationUpkeep())));

		if (town != null) {
			output.add(translator.of("towny_prices_townname", town.getFormattedName()));
			output.add(translator.of("towny_prices_price_plot", prettyMoney(town.getPlotPrice()),prettyMoney(TownySettings.getOutpostCost())));
			output.add(translator.of("towny_prices_price_shop", prettyMoney(town.getCommercialPlotPrice()), prettyMoney(town.getEmbassyPlotPrice())));

			output.add(translator.of("towny_prices_taxes_plot", (town.isTaxPercentage()? town.getTaxes() + "%" : prettyMoney(town.getTaxes())), prettyMoney(town.getPlotTax())));
			output.add(translator.of("towny_prices_taxes_shop", prettyMoney(town.getCommercialPlotTax()), prettyMoney(town.getEmbassyPlotTax())));
			output.add(translator.of("towny_prices_town_neutral_tax", prettyMoney(TownySettings.getTownNeutralityCost(town))));
			
			output.add(translator.of("towny_prices_plots"));
			List<TownBlockType> townBlockTypes = new ArrayList<>(TownBlockTypeHandler.getTypes().values());
			for (int i = 0; i < townBlockTypes.size(); i++) {
				if (i == townBlockTypes.size() - 1)
					output.add(translator.of("towny_prices_type_single", townBlockTypes.get(i).getFormattedName(), prettyMoney(townBlockTypes.get(i).getCost())));
				else {
					output.add(translator.of("towny_prices_type_double",
						townBlockTypes.get(i).getFormattedName(), prettyMoney(townBlockTypes.get(i).getCost()),
						townBlockTypes.get(i+1).getFormattedName(), prettyMoney(townBlockTypes.get(i+1).getCost())
					));

					i++;
				}
			}

			if (nation != null) {
				output.add(translator.of("towny_prices_nationname", nation.getFormattedName()));
				output.add(translator.of("towny_prices_nation_tax", (nation.isTaxPercentage() ? nation.getTaxes() + "%" : prettyMoney(nation.getTaxes())), prettyMoney(TownySettings.getNationNeutralityCost(nation))));
			}
		}
		return output;
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
			output.add(String.format(Colors.LightGray + "%-20s " + Colors.Gold + "|" + Colors.Blue + " %s", gov.getFormattedName(), prettyMoney(gov.getAccount().getCachedBalance())));
		}
		return output;
	}

	public List<String> getMostResidents(List<ResidentList> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<String> output = new ArrayList<>();
		list.sort(Comparator.comparingInt(residentList -> residentList.getResidents().size()));
		Collections.reverse(list);

		int n = 0;
		for (ResidentList residentList : list) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;

			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", ((TownyObject) residentList).getFormattedName(), residentList.getResidents().size()));
		}

		return output;
	}

	public List<String> getMostLand(List<TownBlockOwner> list) {
		final int maxListing = TownySettings.getTownyTopSize();

		List<String> output = new ArrayList<>();
		list.sort(Comparator.comparingInt(owner -> owner.getTownBlocks().size()));
		Collections.reverse(list);

		int n = 0;
		for (TownBlockOwner owner : list) {
			n++;
			if (maxListing != -1 && n > maxListing)
				break;

			output.add(String.format(Colors.Blue + "%30s " + Colors.Gold + "|" + Colors.LightGray + " %10d", owner.getFormattedName(), owner.getTownBlocks().size()));
		}

		return output;
	}
}
