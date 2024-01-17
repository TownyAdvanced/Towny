package com.palmergames.bukkit.towny.utils;

import java.util.List;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;

public class NationUtil {

	public static void addNationComponenents(Town town, StatusScreen screen, Translator translator) {
		// Shown in Hover Text: Towns [44]: James City, Carry Grove, Mason Town
		Nation nation = town.getNationOrNull();
		if (nation == null)
			return;

		List<String> towns = TownyFormatter.getFormattedNames(nation.getTowns());
		if (towns.size() > 10)
			TownyFormatter.shortenOverLengthList(towns, 11, translator);

		Component hover = buildNationComponentHover(town, translator, nation, towns);

		screen.addComponentOf("nation",
				TownyFormatter.colourKeyValue(translator.of("status_town_nation"), nation.getName() + TownyFormatter.formatPopulationBrackets(nation.getTowns().size())),
				hover.asHoverEvent(),
				ClickEvent.runCommand("/towny:nation " + nation.getName()));
	}

	private static Component buildNationComponentHover(Town town, Translator translator, Nation nation, List<String> towns) {
		Component hover = TownyComponents.miniMessage(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingNation(), nation.getFormattedName())))
				.append(Component.newline())
				.append(TownyComponents.miniMessage(getTownJoinedNationDate(town, translator)))
				.append(Component.newline())
				.append(TownyComponents.miniMessage(TownyFormatter.colourKeyValue(translator.of("status_nation_king"), nation.getCapital().getMayor().getFormattedName())))
				.append(Component.newline())
				.append(TownyComponents.miniMessage(TownyFormatter.colourKeyValue(translator.of("town_plu"), StringMgmt.join(towns, ", "))));

		int nationZoneSize = town.getNationZoneSize();
		if (nationZoneSize > 0)
			hover = hover.append(Component.newline().append(TownyComponents.miniMessage(
					TownyFormatter.colourKeyValue(translator.of("status_nation_zone_size"), town.isNationZoneEnabled() ? String.valueOf(nationZoneSize) : translator.of("status_off_bad")))));

		hover = hover.append(Component.newline()).append(translator.component("status_hover_click_for_more"));
		return hover;
	}

	private static String getTownJoinedNationDate(Town town, Translator translator) {
		return TownyFormatter.colourKeyValue(translator.of("status_joined_nation"),
				town.getJoinedNationAt() > 0 ? TownyFormatter.lastOnlineFormatIncludeYear.format(town.getJoinedNationAt()) : translator.of("status_unknown"));
	}

	public static boolean hasReachedMaximumAllies(Nation nation) {
		return TownySettings.getMaxNationAllies() >= 0 && nation.getAllies().size() >= TownySettings.getMaxNationAllies();
	}

	public static boolean hasReachedMaximumResidents(Nation nation) {
		int maxResidentsPerNation = TownySettings.getMaxResidentsPerNation();
		return maxResidentsPerNation > 0 && nation.getResidents().size() >= maxResidentsPerNation;
	}

	public static boolean canAddTownsResidentCount(Nation nation, int additionalResidents) {
		if (hasReachedMaximumResidents(nation))
			return false;
		int maxResidentPerNation = TownySettings.getMaxResidentsPerNation();
		return maxResidentPerNation == 0 || (nation.getResidents().size() + additionalResidents) <= maxResidentPerNation;
	}

	public static boolean hasReachedMaximumTowns(Nation nation) {
		int maxTownsPerNation = TownySettings.getMaxTownsPerNation();
		return maxTownsPerNation > 0 && nation.getTowns().size() >= maxTownsPerNation;
	}

}
