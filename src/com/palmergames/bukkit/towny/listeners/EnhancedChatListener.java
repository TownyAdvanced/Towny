package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.wowserman.api.KeywordManager;
import com.wowserman.api.PopulateKeywordEvent;
import com.wowserman.api.SearchForKeywordEvent;

public class EnhancedChatListener implements Listener {

	public EnhancedChatListener() {

	}

	public static final long RESIDENT_ID = KeywordManager.createID("towny-resident");
	public static final long TOWN_ID = KeywordManager.createID("towny-town");
	public static final long NATION_ID = KeywordManager.createID("towny-nation");

	@EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true) // Override the Default Player Keywords
	public void search(SearchForKeywordEvent event) {

		if (TownySettings.isEnhancedChatEnabled() == false)
			return;

		if (TownySettings.isEnhancedChatResidentNamesEnabled())
			for (Resident resident : TownyUniverse.getDataSource().getResidents()) {
				if (event.containsCustomKeyword(resident.getName()) == false)
					continue;

				event.setID(RESIDENT_ID);

				event.setContext(resident.getName());

				event.setKeyword(resident.getName());

				return;
			}

		if (TownySettings.isEnhancedChatResidentNamesEnabled())
			for (Player player : Bukkit.getOnlinePlayers()) {
				if (event.containsCustomKeyword(ChatColor.stripColor(player.getDisplayName())) == false)
					continue;

				event.setID(RESIDENT_ID);

				event.setContext(player.getName());

				event.setKeyword(ChatColor.stripColor(player.getDisplayName()));

				return;
			}
		if (TownySettings.isEnhancedChatTownNamesEnabled())
			for (Town town : TownyUniverse.getDataSource().getTowns()) {
				if (event.containsCustomKeyword(town.getName()) == false)
					continue;

				event.setID(TOWN_ID);

				event.setContext(town.getName());

				event.setKeyword(town.getName());

				return;
			}

		if (TownySettings.isEnhancedChatNationNamesEnabled())
			for (Nation nation : TownyUniverse.getDataSource().getNations()) {
				if (event.containsCustomKeyword(nation.getName()) == false)
					continue;

				event.setID(NATION_ID);

				event.setContext(nation.getName());

				event.setKeyword(nation.getName());

				return;
			}
	}

	@EventHandler(priority = EventPriority.HIGHEST) // Override the Default Player Keywords
	public void populate(PopulateKeywordEvent event) {

		if (TownySettings.isEnhancedChatEnabled() == false)
			return;

		if (event.equalsID(RESIDENT_ID) && TownySettings.isEnhancedChatResidentNamesEnabled()) {
			event.getDescription().add(ChatColor.DARK_GREEN + event.getContext());
			try {
				final Resident resident = TownyUniverse.getDataSource().getResident(event.getContext());
				event.getDescription()
						.add(resident.hasTown()
								? (ChatColor.DARK_AQUA + resident.getTown().getName()
										+ (resident.hasNation()
												? " " + ChatColor.GOLD
														+ resident.getTown().getNation().getName()
												: ""))
								: "");
				event.getDescription().add(ChatColor.GREEN + "$" + resident.getHoldingFormattedBalance());
				event.getDescription().add(ChatColor.DARK_GREEN + "[" + resident.getFriends().size() + "] " + ChatColor.GREEN + "Friends");
				event.getDescription().add("");
				event.getDescription().add(ChatColor.YELLOW + "Click for Resident Description.");

				event.getCommands().add("/towny:resident " + event.getContext());
			} catch (NotRegisteredException e) {

			}
		}

		if (event.equalsID(TOWN_ID)) {
			try {
				final Town town = TownyUniverse.getDataSource().getTown(event.getContext());
				event.getDescription().add(ChatColor.DARK_AQUA + town.getFormattedName());
				if (town.hasNation())
					event.getDescription().add(ChatColor.GOLD + town.getNation().getFormattedName());
				event.getDescription().add("");
				event.getDescription().add(ChatColor.GREEN + "" + town.getNumResidents() + " Residents");
				event.getDescription().add(ChatColor.GREEN + town.getHoldingFormattedBalance());
				event.getDescription().add("");
				event.getDescription().add(ChatColor.YELLOW + "Click to Teleport to Town's Spawn.");

				event.getCommands().add("/towny:town spawn " + event.getContext());
			} catch (NotRegisteredException e) {

			}
		}

		if (event.equalsID(NATION_ID) && TownySettings.isEnhancedChatNationNamesEnabled()) {
			try {
				final Nation nation = TownyUniverse.getDataSource().getNation(event.getContext());
				event.getDescription().add(ChatColor.GOLD + nation.getFormattedName());
				event.getDescription().add("");
				event.getDescription().add(ChatColor.DARK_AQUA + "Capital: " + nation.getCapital().getName());
				event.getDescription().add(ChatColor.GOLD + "" + nation.getNumTowns() + " Towns");
				event.getDescription().add(ChatColor.GREEN + nation.getHoldingFormattedBalance());
				event.getDescription().add("");
				event.getDescription().add(ChatColor.YELLOW + "Click to Open Nation's Town List.");

				event.getCommands().add("/towny:nation list " + nation.getName());
			} catch (NotRegisteredException e) {

			}
		}
	}
}
