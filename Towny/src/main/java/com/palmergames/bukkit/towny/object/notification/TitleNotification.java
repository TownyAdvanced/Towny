package com.palmergames.bukkit.towny.object.notification;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

public class TitleNotification {
	private String titleNotification;
	private String subtitleNotification;
	private final @Nullable Town town;
	private final WorldCoord worldCoord;

	public TitleNotification(Town town, WorldCoord worldCoord) {
		super();
		this.town = town;
		this.worldCoord = worldCoord;
		makeTitles();
	}

	public Town getTown() {
		return town;
	}

	public WorldCoord getWorldCoord() {
		return worldCoord;
	}

	public String getTitleNotification() {
		return titleNotification;
	}

	public void setTitleNotification(String titleNotification) {
		this.titleNotification = Colors.translateColorCodes(titleNotification);
	}

	public String getSubtitleNotification() {
		return subtitleNotification;
	}

	public void setSubtitleNotification(String subtitleNotification) {
		this.subtitleNotification = Colors.translateColorCodes(subtitleNotification);
	}

	private void makeTitles() {
		if (worldCoord.isWilderness())
			makeWildernessTitles();
		else
			makeTownTitles();
	}

	private void makeTownTitles() {
		String title = TownySettings.getNotificationTitlesTownTitle();
		String subtitle = TownySettings.getNotificationTitlesTownSubtitle();
		
		HashMap<String, Object> placeholders = new HashMap<>();
		placeholders.put("{townname}", StringMgmt.remUnderscore(TownySettings.isNotificationsTownNamesVerbose() ? town.getFormattedName() : town.getName()));
		placeholders.put("{town_motd}", town.getBoard());
		placeholders.put("{town_residents}", town.getNumResidents());
		placeholders.put("{town_residents_online}", TownyAPI.getInstance().getOnlinePlayers(town).size());

		Nation nation = town.getNationOrNull();
		placeholders.put("{nationname}", nation == null ? "" : String.format(TownySettings.getNotificationTitlesNationNameFormat(), nation.getName()));
		placeholders.put("{nation_residents}", nation == null ? "" : nation.getNumResidents());
		placeholders.put("{nation_residents_online}", nation == null ? "" : TownyAPI.getInstance().getOnlinePlayers(nation).size());
		placeholders.put("{nation_motd}", nation == null ? "" : nation.getBoard());
		placeholders.put("{nationcapital}", !town.isCapital() ? "" : getCapitalSlug(town.getName(), nation.getName()));

		for(Map.Entry<String, Object> placeholder: placeholders.entrySet()) {
			title = title.replace(placeholder.getKey(), placeholder.getValue().toString());
			subtitle = subtitle.replace(placeholder.getKey(), placeholder.getValue().toString());
		}
		setTitleNotification(title);
		setSubtitleNotification(subtitle);
	}

	private void makeWildernessTitles() {
		String wildernessName = worldCoord.getTownyWorld().getFormattedUnclaimedZoneName();
		String title = TownySettings.getNotificationTitlesWildTitle();
		String subtitle = TownySettings.getNotificationTitlesWildSubtitle();
		if (title.contains("{wilderness}")) {
			title = title.replace("{wilderness}", wildernessName);
		}
		if (subtitle.contains("{wilderness}")) {
			subtitle = subtitle.replace("{wilderness}", wildernessName);
		}
		if (title.contains("{townname}")) {
			subtitle = subtitle.replace("{townname}", StringMgmt.remUnderscore(town.getName()));
		}
		if (subtitle.contains("{townname}")) {
			subtitle = subtitle.replace("{townname}", StringMgmt.remUnderscore(town.getName()));
		}
		setTitleNotification(title);
		setSubtitleNotification(subtitle);
	}

	private Object getCapitalSlug(String townName, String nationName) {
		String format = TownySettings.getNotificationTitlesNationCapitalFormat();
		if (format.contains("%t") || format.contains("%n"))
			return format.replace("%t", townName).replace("%n", nationName);
		else 
			return String.format(format, nationName, townName);
	}

}
