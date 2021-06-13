package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumOnlinePlayersCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumResidentsCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownBlocksCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownsCalculationEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;

public class ComparatorCaches {
	
	private static LoadingCache<ComparatorType, List<TextComponent>> townCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<ComparatorType, List<TextComponent>>() {
				public List<TextComponent> load(ComparatorType compType) throws Exception {
					return gatherTownLines(compType);
				}
			});
	
	private static LoadingCache<ComparatorType, List<TextComponent>> nationCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<ComparatorType, List<TextComponent>>() {
				public List<TextComponent> load(ComparatorType compType) throws Exception {
					return gatherNationLines(compType);
				}
			}); 
	
	public static List<TextComponent> getTownListCache(ComparatorType compType) {
		try {
			return townCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	public static List<TextComponent> getNationListCache(ComparatorType compType) {
		try {
			return nationCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static List<TextComponent> gatherTownLines(ComparatorType compType) {
		List<TextComponent> output = new ArrayList<>();
		List<Town> towns = TownyUniverse.getInstance().getDataSource().getTowns();
		towns.sort((Comparator<? super Town>) compType.getComparator());
		
		for (Town town : towns) {
			TextComponent townName = Component.text(Colors.LightBlue + StringMgmt.remUnderscore(town.getName()))
					.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
				
			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = Colors.LightBlue + "(" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")";
				break;
			case TOWNBLOCKS:
				slug = Colors.LightBlue + "(" + town.getTownBlocks().size() + ")";
				break;
			case RUINED:
				slug = Colors.LightBlue + "(" + town.getResidents().size() + ") " + (town.isRuined() ? Translation.of("msg_ruined"):"");
				break;
			case BANKRUPT:
				slug = Colors.LightBlue + "(" + town.getResidents().size() + ") " + (town.isBankrupt() ? Translation.of("msg_bankrupt"):"");
				break;
			case ONLINE:
				slug = Colors.LightBlue + "(" + TownyAPI.getInstance().getOnlinePlayersInTown(town).size() + ")";
				break;
			default:
				slug = Colors.LightBlue + "(" + town.getResidents().size() + ")";
				break;
			}
			townName = townName.append(Component.text(Colors.Gray + " - " + slug));
			
			if (town.isOpen())
				townName = townName.append(Component.text(" " + Colors.LightBlue + Translation.of("status_title_open")));
			
			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			townName = townName.hoverEvent(HoverEvent.showText(Component.text(Translation.of("msg_click_spawn", town) + "\n" + spawnCost).color(NamedTextColor.GOLD)));
			output.add(townName);
		}
		return output;
	}
	
	@SuppressWarnings("unchecked")
	private static List<TextComponent> gatherNationLines(ComparatorType compType) {
		List<TextComponent> output = new ArrayList<>();
		List<Nation> nations = TownyUniverse.getInstance().getDataSource().getNations();
		nations.sort((Comparator<? super Nation>) compType.getComparator());
		
		for (Nation nation : nations) {
			TextComponent nationName = Component.text(Colors.LightBlue + StringMgmt.remUnderscore(nation.getName()))
					.clickEvent(ClickEvent.runCommand("/towny:nation spawn " + nation + " -ignore"));

			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = TownyEconomyHandler.getFormattedBalance(nation.getAccount().getCachedBalance());
				break;
			case TOWNBLOCKS:
				int rawNumTownsBlocks = nation.getTownBlocks().size();
				NationListDisplayedNumTownBlocksCalculationEvent tbEvent = new NationListDisplayedNumTownBlocksCalculationEvent(nation, rawNumTownsBlocks);
				Bukkit.getPluginManager().callEvent(tbEvent);
				slug = tbEvent.getDisplayedValue() + "";
				break;
			case TOWNS:
				int rawNumTowns = nation.getTowns().size();
				NationListDisplayedNumTownsCalculationEvent tEvent = new NationListDisplayedNumTownsCalculationEvent(nation, rawNumTowns);
				Bukkit.getPluginManager().callEvent(tEvent);
				slug = tEvent.getDisplayedValue() + "";
				break;
			case ONLINE:
				int rawNumOnlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation).size();
				NationListDisplayedNumOnlinePlayersCalculationEvent opEvent = new NationListDisplayedNumOnlinePlayersCalculationEvent(nation, rawNumOnlinePlayers);
				Bukkit.getPluginManager().callEvent(opEvent);
				slug = opEvent.getDisplayedValue() + "";
				break;
			default:
				int rawNumResidents = nation.getResidents().size();
				NationListDisplayedNumResidentsCalculationEvent rEvent = new NationListDisplayedNumResidentsCalculationEvent(nation, rawNumResidents);
				Bukkit.getPluginManager().callEvent(rEvent);
				slug = rEvent.getDisplayedValue() + "";
				break;
			}
			
			nationName = nationName.append(Component.text(Colors.Gray + " - " + Colors.LightBlue + "(" + slug + ")"));

			if (nation.isOpen())
				nationName = nationName.append(Component.text(" " + Colors.LightBlue + Translation.of("status_title_open")));

			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));
			
			nationName = nationName.hoverEvent(HoverEvent.showText(Component.text(Colors.Gold + Translation.of("msg_click_spawn", nation) + "\n" + spawnCost)));
			output.add(nationName);
		}
		return output;
	}
}
