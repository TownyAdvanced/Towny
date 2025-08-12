package com.palmergames.bukkit.towny.object.comparators;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.Towny;
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
import com.palmergames.bukkit.towny.event.town.TownListDisplayedNumResidentsCalculationEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.Pair;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

public class ComparatorCaches {
	
	private static final LoadingCache<ComparatorType, List<Pair<UUID, Component>>> townCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<>() {
				public @NotNull List<Pair<UUID, Component>> load(@NotNull ComparatorType compType) {
					return gatherTownLines(compType);
				}
			});
	
	private static final LoadingCache<ComparatorType, List<Pair<UUID, Component>>> nationCompCache = CacheBuilder.newBuilder()
			.expireAfterWrite(10, TimeUnit.MINUTES)
			.build(new CacheLoader<>() {
				public @NotNull List<Pair<UUID, Component>> load(@NotNull ComparatorType compType) {
					return gatherNationLines(compType);
				}
			}); 
	
	public static List<Pair<UUID, Component>> getTownListCache(ComparatorType compType) {
		try {
			return townCompCache.get(compType);
		} catch (ExecutionException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "exception occurred while updating town comparator cache", e);
			return new ArrayList<>();
		}
	}
	
	public static List<Pair<UUID, Component>> getNationListCache(ComparatorType compType) {
		try {
			return nationCompCache.get(compType);
		} catch (ExecutionException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "exception occurred while updating nation comparator cache", e);
			return new ArrayList<>();
		}
	}
	
	@SuppressWarnings("unchecked")
	private static List<Pair<UUID, Component>> gatherTownLines(ComparatorType compType) {
		List<Pair<UUID, Component>> output = new ArrayList<>();
		List<Town> towns = TownyUniverse.getInstance().getTowns().stream().filter(Town::isVisibleOnTopLists).collect(Collectors.toList());
		towns.sort((Comparator<? super Town>) compType.getComparator());

		boolean spawningFullyDisabled = !TownySettings.isConfigAllowingTownSpawn() && !TownySettings.isConfigAllowingPublicTownSpawnTravel()
				&& !TownySettings.isConfigAllowingTownSpawnNationTravel() && !TownySettings.isConfigAllowingTownSpawnNationAllyTravel();

		for (Town town : towns) {
			Component townName = Component.text(StringMgmt.remUnderscore(town.getName()), NamedTextColor.AQUA);
				
			String slug = "";
			switch (compType) {
			case BALANCE:
				slug = "(" + TownyEconomyHandler.getFormattedBalance(town.getAccount().getCachedBalance()) + ")";
				break;
			case TOWNBLOCKS:
				slug = "(" + town.getTownBlocks().size() + ")";
				break;
			case FORSALE:
				slug = "(" + getResidentCount(town) + ") " + (town.isForSale() ? Translation.of("status_forsale", TownyEconomyHandler.getFormattedBalance(town.getForSalePrice())) : "");
				break;
			case RUINED:
				slug = "(" + getResidentCount(town) + ") " + (town.isRuined() ? Translation.of("msg_ruined"):"");
				break;
			case BANKRUPT:
				slug = "(" + getResidentCount(town) + ") " + (town.isBankrupt() ? Translation.of("msg_bankrupt"):"");
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
				slug = "(" + getResidentCount(town) + ")";
				break;
			}
			
			townName = townName.append(Component.text(" - ", NamedTextColor.DARK_GRAY)).append(Component.text(slug, NamedTextColor.AQUA));
			
			if (town.isOpen())
				townName = townName.append(Component.space()).append(Translatable.of("status_title_open").component());

			if (!spawningFullyDisabled) {
				Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
				if (TownyEconomyHandler.isActive())
					spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

				townName = townName.clickEvent(ClickEvent.runCommand("/towny:town spawn " + town + " -ignore"));
				townName = townName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", town).append("\n").append(spawnCost).component()));
			}
			output.add(Pair.pair(town.getUUID(), townName));
		}
		
		return output;
	}

	private static int getResidentCount(Town town) {
		TownListDisplayedNumResidentsCalculationEvent resCountEvent = new TownListDisplayedNumResidentsCalculationEvent(town.getResidents().size(), town);
		BukkitTools.fireEvent(resCountEvent);
		return resCountEvent.getDisplayedValue();
	}

	@SuppressWarnings("unchecked")
	private static List<Pair<UUID, Component>> gatherNationLines(ComparatorType compType) {
		List<Pair<UUID, Component>> output = new ArrayList<>();
		List<Nation> nations = new ArrayList<>(TownyUniverse.getInstance().getNations());

		//Sort nations
		nations.sort((Comparator<? super Nation>) compType.getComparator());
		DisplayedNationsListSortEvent nationListSortEvent = new DisplayedNationsListSortEvent(nations, compType);
		BukkitTools.fireEvent(nationListSortEvent);
		nations = nationListSortEvent.getNations();

		boolean spawningFullyDisabled = !TownySettings.isConfigAllowingNationSpawn() && !TownySettings.isConfigAllowingPublicNationSpawnTravel()
				&& !TownySettings.isConfigAllowingNationSpawnAllyTravel();

		for (Nation nation : nations) {
			Component nationName = Component.text(StringMgmt.remUnderscore(nation.getName()), NamedTextColor.AQUA);

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

			if (!spawningFullyDisabled) {
				Translatable spawnCost = Translatable.of("msg_spawn_cost_free");
				if (TownyEconomyHandler.isActive())
					spawnCost = Translatable.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));

				nationName = nationName.clickEvent(ClickEvent.runCommand("/towny:nation spawn " + nation + " -ignore"));
				nationName = nationName.hoverEvent(HoverEvent.showText(Translatable.of("msg_click_spawn", nation).append("\n").append(spawnCost).component()));
			}
			output.add(Pair.pair(nation.getUUID(), nationName));
		}
		return output;
	}
}
