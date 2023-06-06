package com.palmergames.bukkit.towny.command;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.paper.PaperCommandManager;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyAsciiMap;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.TownyUpdateChecker;
import com.palmergames.bukkit.towny.command.cloud.argument.TownArgument;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
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
import com.palmergames.bukkit.towny.object.gui.SelectionGUI;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.TimeMgmt;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class TownyCommand extends BaseCommand {

	public static void register(PaperCommandManager<CommandSender> manager, Towny plugin) {
		final Command.Builder<CommandSender> root = manager.commandBuilder("towny", ArgumentDescription.of("Base Towny command, see /towny ?"));

		final Command.Builder<CommandSender> map = root.literal("map")
			.permission(PermissionNodes.TOWNY_COMMAND_TOWNY_MAP.getNode())
			.senderType(Player.class);

		manager.command(map.handler(context -> showMap((Player) context.getSender())))
			.command(map.literal("big")
				.handler(context -> TownyAsciiMap.generateAndSend(plugin, (Player) context.getSender(), 18)))
			.command(map.literal("hud")
				.handler(context -> HUDManager.toggleMapHud((Player) context.getSender())));

		manager.command(root.literal("prices")
			.argument(TownArgument.optional("town", ArgumentDescription.of("The town to get prices for")))
			.handler(context -> {
				if (!TownyEconomyHandler.isActive())
					sneaky(new TownyException(Translatable.of("msg_err_no_economy")));

				Town town = context.getOrSupplyDefault("town", () -> {
					if (context.getSender() instanceof Player player)
						return TownyAPI.getInstance().getTown(player);
					else
						return null;
				});

				for (String line : getTownyPrices(town, Translator.locale(context.getSender())))
					TownyMessaging.sendMessage(context.getSender(), line);
			}));

		manager.command(root.literal("switches")
			.senderType(Player.class)
			.handler(context -> {
				final TownyWorld world = TownyAPI.getInstance().getTownyWorld(((Player) context.getSender()).getWorld());
				if (world == null || !world.isUsingTowny())
					sneaky(new TownyException(Translatable.of("msg_err_usingtowny_disabled")));

				Resident resident = getResidentOrThrow((Player) context.getSender());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.SWITCHES);
			}));

		manager.command(root.literal("itemuse")
			.senderType(Player.class)
			.handler(context -> {
				final TownyWorld world = TownyAPI.getInstance().getTownyWorld(((Player) context.getSender()).getWorld());
				if (world == null || !world.isUsingTowny())
					sneaky(new TownyException(Translatable.of("msg_err_usingtowny_disabled")));

				Resident resident = getResidentOrThrow((Player) context.getSender());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ITEMUSE);
			}));

		manager.command(root.literal("allowedblocks")
			.senderType(Player.class)
			.handler(context -> {
				final TownyWorld world = TownyAPI.getInstance().getTownyWorld(((Player) context.getSender()).getWorld());
				if (world == null || !world.isUsingTowny())
					sneaky(new TownyException(Translatable.of("msg_err_usingtowny_disabled")));

				Resident resident = getResidentOrThrow((Player) context.getSender());
				ResidentUtil.openSelectionGUI(resident, SelectionGUI.SelectionType.ALLOWEDBLOCKS);
			}));

		manager.command(root.literal("wildsblocks")
			.senderType(Player.class)
			.handler(context -> {
				final TownyWorld world = TownyAPI.getInstance().getTownyWorld(((Player) context.getSender()).getWorld());
				if (world == null || !world.isUsingTowny())
					sneaky(new TownyException(Translatable.of("msg_err_usingtowny_disabled")));

				Resident resident = getResidentOrThrow((Player) context.getSender());
				ResidentUtil.openGUIInventory(resident, world.getUnclaimedZoneIgnoreMaterials(), Translatable.of("gui_title_towny_wildsblocks").forLocale(context.getSender()));
			}));

		manager.command(root.literal("plotclearblocks")
			.senderType(Player.class)
			.handler(context -> {
				final TownyWorld world = TownyAPI.getInstance().getTownyWorld(((Player) context.getSender()).getWorld());
				if (world == null || !world.isUsingTowny())
					sneaky(new TownyException(Translatable.of("msg_err_usingtowny_disabled")));

				Resident resident = getResidentOrThrow((Player) context.getSender());
				ResidentUtil.openGUIInventory(resident, world.getPlotManagementMayorDelete(), Translatable.of("gui_title_towny_plotclear").forLocale(context.getSender()));
			}));
		
		manager.command(root.literal("tree")
			.senderType(ConsoleCommandSender.class)
			.permission(PermissionNodes.TOWNY_COMMAND_TOWNY_TREE.getNode())
			.handler(context -> {
				for (String line : TownyUniverse.getInstance().getTreeString(0))
					TownyMessaging.sendMessage(context.getSender(), line);
			}));

		manager.command(root.literal("time")
			.permission(PermissionNodes.TOWNY_COMMAND_TOWNY_TIME.getNode())
			.handler(context -> TownyMessaging.sendMsg(context.getSender(), Translatable.of("msg_time_until_a_new_day").append(TimeMgmt.formatCountdownTime(TimeMgmt.townyTime(true))))));

		manager.command(root.literal("version", "v")
			.permission(PermissionNodes.TOWNY_COMMAND_TOWNY_VERSION.getNode())
			.handler(context -> {
				if (TownyUpdateChecker.shouldShowNotification()) {
					TownyMessaging.sendMsg(context.getSender(), Translatable.of("msg_latest_version", plugin.getVersion(), TownyUpdateChecker.getNewVersion()));
				} else {
					TownyMessaging.sendMsg(context.getSender(), Translatable.of("msg_towny_version", plugin.getVersion()));

					if (TownyUpdateChecker.hasCheckedSuccessfully())
						TownyMessaging.sendMsg(context.getSender(), Translatable.of("msg_up_to_date"));
				}
			}));

		manager.command(root.literal("spy")
			.senderType(Player.class)
			.permission(PermissionNodes.TOWNY_CHAT_SPY.getNode())
			.handler(context -> {
				getResidentOrThrow((Player) context.getSender()).toggleMode(new String[]{"spy"}, true);
			}));

		manager.command(root.literal("universe")
			.permission(PermissionNodes.TOWNY_COMMAND_TOWNY_UNIVERSE.getNode())
			.handler(context -> {
				for (String line : getUniverseStats(Translator.locale(context.getSender())))
					TownyMessaging.sendMessage(context.getSender(), line);
			}));
		
		final Command.Builder<CommandSender> top = root.literal("top");
		
		manager.command(top.handler(context -> {
			TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("/towny top"));
			TownyMessaging.sendMessage(context.getSender(), ChatTools.formatCommand("", "/towny top", "residents [all/town/nation]", ""));
			TownyMessaging.sendMessage(context.getSender(), ChatTools.formatCommand("", "/towny top", "land [all/resident/town]", ""));
			TownyMessaging.sendMessage(context.getSender(), ChatTools.formatCommand("", "/towny top", "balance [all/town/nation]", ""));
		})).command(top.literal("residents").permission(PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_RESIDENTS.getNode()))
			.command(top.literal("residents").literal("all")
				.handler(context -> {
					List<ResidentList> list = new ArrayList<>(TownyUniverse.getInstance().getTowns());
					list.addAll(TownyUniverse.getInstance().getNations());
					
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Residents"));
					for (String message : getMostResidents(list))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("residents").literal("town")
				.handler(context -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Residents in a Town"));
					for (String message : getMostResidents(new ArrayList<>(TownyUniverse.getInstance().getTowns())))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("residents").literal("nations")
				.handler(context -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Residents in a Nation"));
					for (String message : getMostResidents(new ArrayList<>(TownyUniverse.getInstance().getNations())))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("land").permission(PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_LAND.getNode()))
			.command(top.literal("land").literal("all")
				.handler(context -> {
					List<TownBlockOwner> list = new ArrayList<>(TownyUniverse.getInstance().getResidents());
					list.addAll(TownyUniverse.getInstance().getTowns());
					
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Land Owned"));
					for (String message : getMostLand(list))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("land").literal("resident")
				.handler(context -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Land Owned By Resident"));
					for (String message : getMostLand(new ArrayList<>(TownyUniverse.getInstance().getResidents())))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("land").literal("town")
				.handler(context -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Most Land Owned By Town"));
					for (String message : getMostLand(new ArrayList<>(TownyUniverse.getInstance().getTowns())))
						TownyMessaging.sendMessage(context.getSender(), message);
				}))
			.command(top.literal("balance").permission(PermissionNodes.TOWNY_COMMAND_TOWNY_TOP_BALANCE.getNode()))
			.command(top.literal("balance").literal("all")
				.handler(context -> plugin.getScheduler().runAsync(() -> {
					List<Government> list = new ArrayList<>(TownyUniverse.getInstance().getTowns());
					list.addAll(TownyUniverse.getInstance().getNations());

					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Top Bank Balances"));
					for (String message : getTopBankBalance(list))
						TownyMessaging.sendMessage(context.getSender(), message);
				})))
			.command(top.literal("balance").literal("town")
				.handler(context -> plugin.getScheduler().runAsync(() -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Top Bank Balances by Town"));
					for (String message : getTopBankBalance(new ArrayList<>(TownyUniverse.getInstance().getTowns())))
						TownyMessaging.sendMessage(context.getSender(), message);
				})))
			.command(top.literal("balance").literal("nation")
				.handler(context -> plugin.getScheduler().runAsync(() -> {
					TownyMessaging.sendMessage(context.getSender(), ChatTools.formatTitle("Top Bank Balances by Nation"));
					for (String message : getTopBankBalance(new ArrayList<>(TownyUniverse.getInstance().getNations())))
						TownyMessaging.sendMessage(context.getSender(), message);
				})));
	}

	public static List<String> getUniverseStats(Translator translator) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<String> output = new ArrayList<>();
		
		output.add(""); // Intentionally left blank
		output.add("\u00A70-\u00A74###\u00A70---\u00A74###\u00A70-   " + Colors.Gold + "[" + Colors.Yellow + "Towny " + Colors.Green + Towny.getPlugin().getVersion() + Colors.Gold + "]");
		output.add("\u00A74#\u00A7c###\u00A74#\u00A70-\u00A74#\u00A7c###\u00A74#\u00A70   " + Colors.Blue + translator.of("msg_universe_attribution") + Colors.LightBlue + "Chris H (Shade), ElgarL, LlmDl");
		output.add("\u00A74#\u00A7c####\u00A74#\u00A7c####\u00A74#   " + Colors.LightBlue + translator.of("msg_universe_contributors") + Colors.Rose + translator.of("msg_universe_heart"));
		output.add("\u00A70-\u00A74#\u00A7c#######\u00A74#\u00A70-");
		output.add("\u00A70--\u00A74##\u00A7c###\u00A74##\u00A70--   " + Colors.Blue + translator.of("res_list")+ ": " + Colors.LightBlue + townyUniverse.getNumResidents() + Colors.Gray + " | " + Colors.Blue + translator.of("town_plu") + ": " + Colors.LightBlue + townyUniverse.getTowns().size() + Colors.Gray + " | " + Colors.Blue + translator.of("nation_plu") + ": " + Colors.LightBlue + townyUniverse.getNumNations());
		output.add("\u00A70----\u00A74#\u00A7c#\u00A74#\u00A70----   " + Colors.Blue + translator.of("world_plu") + ": " + Colors.LightBlue + townyUniverse.getTownyWorlds().size() + Colors.Gray + " | " + Colors.Blue + translator.of("townblock_plu") + ": " + Colors.LightBlue + townyUniverse.getTownBlocks().size());
		output.add("\u00A70-----\u00A74#\u00A70-----   " + Colors.LightGreen + "https://TownyAdvanced.github.io/");
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

		TownyAsciiMap.generateAndSend(Towny.getPlugin(), player, 7);
	}

	/**
	 * Returns prices for town's taxes/upkeep.
	 * @param town - The town being checked.
	 * @param translator - The Translator to use.
	 * @return - Prices screen for a town.
	 */
	public static List<String> getTownyPrices(Town town, Translator translator) {

		List<String> output = new ArrayList<>();
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
			output.add(translator.of("towny_prices_nation_upkeep_based_on", upkeepformula));
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
			output.add(translator.of("towny_prices_town_neutral_tax", getMoney(TownySettings.getTownNeutralityCost(town))));
			
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
				output.add(translator.of("towny_prices_nation_tax", (nation.isTaxPercentage() ? nation.getTaxes() + "%" : getMoney(nation.getTaxes())), getMoney(TownySettings.getNationNeutralityCost(nation))));
			}
		}
		return output;
	}
	
	private static String getMoney(double cost) {
		return TownyEconomyHandler.getFormattedBalance(cost);
	}
	
	private static List<String> getTopBankBalance(final List<Government> governments) {
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

	private static List<String> getMostResidents(List<ResidentList> list) {
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

	private static List<String> getMostLand(List<TownBlockOwner> list) {
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
