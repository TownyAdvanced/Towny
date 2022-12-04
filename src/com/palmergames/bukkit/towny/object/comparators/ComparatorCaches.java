package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Translatable;
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
import com.palmergames.bukkit.util.BukkitTools;
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
		List<Town> towns = new ArrayList<>(TownyUniverse.getInstance().getTowns());
		towns.sort((Comparator<? super Town>) compType.getComparator());
		
		for (Town town : towns) {
			TextComponent townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
				
			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = "(" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")";
				break;
			case TOWNBLOCKS:
				slug = "(" + town.getTownBlocks().size() + ")";
				break;
			case RUINED:
				slug = "(" + town.getResidents().size() + ") " + (town.isRuined() ? Translation.of("msg_ruined"):"");
				break;
			case BANKRUPT:
				slug = "(" + town.getResidents().size() + ") " + (town.isBankrupt() ? Translation.of("msg_bankrupt"):"");
				break;
			case ONLINE:
				slug = "(" + TownyAPI.getInstance().getOnlinePlayersInTown(town).size() + ")";
				break;
			case FOUNDED:
				if (town.getRegistered() != 0)
					slug = "(" + TownyFormatter.registeredFormat.format(town.getRegistered()) + ")";
				break;
			case UPKEEP:
				slug = "(" + TownyEconomyHandler.getFormattedBalance(TownySettings.getTownUpkeepCost(town)) + ")";
				break;
			default:
				slug = "(" + town.getResidents().size() + ")";
				break;
			}
			
			townName = townName.append(Component.text(" - ", NamedTextColor.DARK_GRAY)).append(Component.text(slug, NamedTextColor.AQUA));
			
			if (town.isOpen())
				townName = townName.append(Component.space()).append(Translatable.of("status_title_open").component());

			Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
			if (TownyEconomyHandler.isActive())
				spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			townName = townName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", town).append("\n").append(spawnCost).component()));
			output.add(townName);
		}
		return output;
	}
	
	@SuppressWarnings("unchecked")
	private static List<TextComponent> gatherNationLines(ComparatorType compType) {
		List<TextComponent> output = new ArrayList<>();
		List<Nation> nations = new ArrayList<>(TownyUniverse.getInstance().getNations());

		//Sort nations
		nations.sort((Comparator<? super Nation>) compType.getComparator());
		DisplayedNationsListSortEvent nationListSortEvent = new DisplayedNationsListSortEvent(nations, compType);
		BukkitTools.fireEvent(nationListSortEvent);
		nations = nationListSortEvent.getNations();

		for (Nation nation : nations) {
			TextComponent nationName = Component.text(StringMgmt.remUnderscore(nation.getName()), NamedTextColor.AQUA)
					.clickEvent(ClickEvent.runCommand("/towny:nation spawn " + nation + " -ignore"));

			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = TownyEconomyHandler.getFormattedBalance(nation.getAccount().getCachedBalance());
				break;
			case TOWNBLOCKS:
				int rawNumTownsBlocks = nation.getTownBlocks().size();
				NationListDisplayedNumTownBlocksCalculationEvent tbEvent = new NationListDisplayedNumTownBlocksCalculationEvent(nation, rawNumTownsBlocks);
				BukkitTools.fireEvent(tbEvent);
				slug = tbEvent.getDisplayedValue() + "";
				break;
			case TOWNS:
				int rawNumTowns = nation.getTowns().size();
				NationListDisplayedNumTownsCalculationEvent tEvent = new NationListDisplayedNumTownsCalculationEvent(nation, rawNumTowns);
				BukkitTools.fireEvent(tEvent);
				slug = tEvent.getDisplayedValue() + "";
				break;
			case ONLINE:
				int rawNumOnlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation).size();
				NationListDisplayedNumOnlinePlayersCalculationEvent opEvent = new NationListDisplayedNumOnlinePlayersCalculationEvent(nation, rawNumOnlinePlayers);
				BukkitTools.fireEvent(opEvent);
				slug = opEvent.getDisplayedValue() + "";
				break;
			case FOUNDED:
				if (nation.getRegistered() != 0)
					slug = TownyFormatter.registeredFormat.format(nation.getRegistered());
				break;
			case UPKEEP:
				slug = TownyEconomyHandler.getFormattedBalance(TownySettings.getNationUpkeepCost(nation));
				break;
			default:
				int rawNumResidents = nation.getResidents().size();
				NationListDisplayedNumResidentsCalculationEvent rEvent = new NationListDisplayedNumResidentsCalculationEvent(nation, rawNumResidents);
				BukkitTools.fireEvent(rEvent);
				slug = rEvent.getDisplayedValue() + "";
				break;
			}
			
			nationName = nationName.append(Component.text(" - ", NamedTextColor.DARK_GRAY)).append(Component.text("(" + slug + ")", NamedTextColor.AQUA));

			if (nation.isOpen())
				nationName = nationName.append(Component.space()).append(Translatable.of("status_title_open").component());

			Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
			if (TownyEconomyHandler.isActive())
				spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));
			
			nationName = nationName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", nation).append("\n").append(spawnCost).component()));
			output.add(nationName);
		}
		return output;
	}
}
