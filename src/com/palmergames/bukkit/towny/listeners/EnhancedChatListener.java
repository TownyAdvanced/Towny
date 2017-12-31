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

/**
 * @author Wowserman
 *
 */
public class EnhancedChatListener implements Listener {

	public EnhancedChatListener() {
		// Wow.
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
			event.setCommands(TownySettings.getEnhancedChatResidentCommands());
			event.setDescription(TownySettings.getEnhancedChatResidentDescription());
			event.setURL(TownySettings.getEnhancedChatResidentURL());
		}

		if (event.equalsID(TOWN_ID) && TownySettings.isEnhancedChatTownNamesEnabled()) {
			event.setCommands(TownySettings.getEnhancedChatTownCommands());
			event.setDescription(TownySettings.getEnhancedChatTownDescription());
			event.setURL(TownySettings.getEnhancedChatTownURL());
		}

		if (event.equalsID(NATION_ID) && TownySettings.isEnhancedChatNationNamesEnabled()) {
			event.setCommands(TownySettings.getEnhancedChatNationCommands());
			event.setDescription(TownySettings.getEnhancedChatNationDescription());
			event.setURL(TownySettings.getEnhancedChatNationURL());
		}
	}
}
