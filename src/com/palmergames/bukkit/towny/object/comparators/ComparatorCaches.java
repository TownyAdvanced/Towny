package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import org.bukkit.Bukkit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.nation.DisplayedNationsListSortEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumOnlinePlayersCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumResidentsCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownBlocksCalculationEvent;
import com.palmergames.bukkit.towny.event.nation.NationListDisplayedNumTownsCalculationEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class ComparatorCaches {
	
	private static final LoadingCache<ComparatorType, List<Component>> townCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<>() {
				public @NotNull List<Component> load(@NotNull ComparatorType compType) {
					return gatherTownLines(compType);
				}
			});
	
	private static final LoadingCache<ComparatorType, List<Component>> nationCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<>() {
				public @NotNull List<Component> load(@NotNull ComparatorType compType) {
					return gatherNationLines(compType);
				}
			}); 
	
	public static List<Component> getTownListCache(ComparatorType compType) {
		try {
			return townCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	public static List<Component> getNationListCache(ComparatorType compType) {
		try {
			return nationCompCache.get(compType);
		} catch (ExecutionException e) {
			e.printStackTrace();
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static List<Component> gatherTownLines(ComparatorType compType) {
		List<Component> output = new ArrayList<>();
		List<Town> towns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
		towns.sort((Comparator<? super Town>) compType.getComparator());
		
		for (Town town : towns) {
			Component townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
				
			Component slug = Component.empty();
			switch (compType) {
			case BALANCE:
				slug = Component.text("(" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")", NamedTextColor.AQUA);
				break;
			case TOWNBLOCKS:
				slug = Component.text("(" + town.getTownBlocks().size() + ")", NamedTextColor.AQUA);
				break;
			case RUINED:
				slug = Component.text("(" + town.getResidents().size() + ") ", NamedTextColor.AQUA).append(town.isRuined() ? TownyComponents.miniMessage(Translation.of("msg_ruined")) : Component.empty());
				break;
			case BANKRUPT:
				slug = Component.text("(" + town.getResidents().size() + ") ", NamedTextColor.AQUA).append(town.isBankrupt() ? TownyComponents.miniMessage(Translation.of("msg_bankrupt")) : Component.empty());
				break;
			case ONLINE:
				slug = Component.text("(" + TownyAPI.getInstance().getOnlinePlayersInTown(town).size() + ")", NamedTextColor.AQUA);
				break;
			case FOUNDED:
				if (town.getRegistered() != 0)
					slug = Component.text("(" + TownyFormatter.registeredFormat.format(town.getRegistered()) + ")", NamedTextColor.AQUA);
				break;
			default:
				slug = Component.text("(" + town.getResidents().size() + ")", NamedTextColor.AQUA);
				break;
			}
			townName = townName.append(Component.text(" - ", NamedTextColor.DARK_GRAY).append(slug));
			
			if (town.isOpen())
				townName = townName.append(TownyComponents.miniMessage(" " + Translation.of("status_title_open")));
			
			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			townName = townName.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(Translation.of("msg_click_spawn", town) + "\n" + spawnCost)));
			output.add(townName);
		}
		return output;
	}
	
	@SuppressWarnings("unchecked")
	private static List<Component> gatherNationLines(ComparatorType compType) {
		List<Component> output = new ArrayList<>();
		List<Nation> nations = new ArrayList<>(TownyUniverse.getInstance().getNations());

		//Sort nations
		nations.sort((Comparator<? super Nation>) compType.getComparator());
		DisplayedNationsListSortEvent nationListSortEvent = new DisplayedNationsListSortEvent(nations, compType);
		Bukkit.getPluginManager().callEvent(nationListSortEvent);
		nations = nationListSortEvent.getNations();

		for (Nation nation : nations) {
			Component nationName = Component.text(StringMgmt.remUnderscore(nation.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:nation spawn " + nation + " -ignore"));

			Component slug = Component.empty();
			switch (compType) {
			case BALANCE:
				slug = Component.text("(" + TownyEconomyHandler.getFormattedBalance(nation.getAccount().getCachedBalance()) + ")", NamedTextColor.AQUA);
				break;
			case TOWNBLOCKS:
				int rawNumTownsBlocks = nation.getTownBlocks().size();
				NationListDisplayedNumTownBlocksCalculationEvent tbEvent = new NationListDisplayedNumTownBlocksCalculationEvent(nation, rawNumTownsBlocks);
				Bukkit.getPluginManager().callEvent(tbEvent);
				slug = Component.text("(" + tbEvent.getDisplayedValue() + ")", NamedTextColor.AQUA);
				break;
			case TOWNS:
				int rawNumTowns = nation.getTowns().size();
				NationListDisplayedNumTownsCalculationEvent tEvent = new NationListDisplayedNumTownsCalculationEvent(nation, rawNumTowns);
				Bukkit.getPluginManager().callEvent(tEvent);
				slug = Component.text("(" + tEvent.getDisplayedValue() + ")", NamedTextColor.AQUA);
				break;
			case ONLINE:
				int rawNumOnlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation).size();
				NationListDisplayedNumOnlinePlayersCalculationEvent opEvent = new NationListDisplayedNumOnlinePlayersCalculationEvent(nation, rawNumOnlinePlayers);
				Bukkit.getPluginManager().callEvent(opEvent);
				slug = Component.text("(" + opEvent.getDisplayedValue() + ")", NamedTextColor.AQUA);
				break;
			case FOUNDED:
				if (nation.getRegistered() != 0)
					slug = Component.text("(" + TownyFormatter.registeredFormat.format(nation.getRegistered()) + ")", NamedTextColor.AQUA);
				break;
			default:
				int rawNumResidents = nation.getResidents().size();
				NationListDisplayedNumResidentsCalculationEvent rEvent = new NationListDisplayedNumResidentsCalculationEvent(nation, rawNumResidents);
				Bukkit.getPluginManager().callEvent(rEvent);
				slug = Component.text("(" + rEvent.getDisplayedValue() + ")", NamedTextColor.AQUA);
				break;
			}
			
			nationName = nationName.append(Component.text(" - ", NamedTextColor.DARK_GRAY)).append(slug);

			if (nation.isOpen())
				nationName = nationName.append(TownyComponents.miniMessage(" " + Translation.of("status_title_open")));

			String spawnCost = "Free";
			if (TownyEconomyHandler.isActive())
				spawnCost = Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));
			
			nationName = nationName.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(Translation.of("msg_click_spawn", nation) + "\n" + spawnCost)));
			output.add(nationName);
		}
		return output;
	}
}
