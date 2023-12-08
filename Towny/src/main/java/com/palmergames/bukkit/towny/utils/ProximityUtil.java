package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.MathUtil;

public class ProximityUtil {

	public static void testTownProximityToNation(Town town, Nation nation) throws TownyException {
		if (TownySettings.getNationProximityToCapital() <= 0)
			return;

		if (!nation.getCapital().hasHomeBlock() || !town.hasHomeBlock()) {
			throw new TownyException(Translatable.of("msg_err_homeblock_has_not_been_set"));
		}

		WorldCoord capitalCoord = nation.getCapital().getHomeBlockOrNull().getWorldCoord();
		WorldCoord townCoord = town.getHomeBlockOrNull().getWorldCoord();

		if (!capitalCoord.getWorldName().equalsIgnoreCase(townCoord.getWorldName())) {
			throw new TownyException(Translatable.of("msg_err_nation_homeblock_in_another_world"));
		}

		if (!closeEnoughToCapital(town, nation)) {
			throw new TownyException(Translatable.of("msg_err_town_not_close_enough_to_nation", town.getName()));
		}
	}

	public static List<Town> gatherOutOfRangeTowns(Nation nation) {
		return gatherOutOfRangeTowns(nation, nation.getCapital());
	}

	public static List<Town> gatherOutOfRangeTowns(Nation nation, Town newCapital) {
		List<Town> removedTowns = new ArrayList<>();
		if (TownySettings.getNationProximityToCapital() <= 0)
			return removedTowns;

		Town capital = newCapital;
		final TownBlock capitalHomeBlock = capital.getHomeBlockOrNull();
		// This is unlikely to happen but if a capital has no homeblock by some error we don't want to remove every town.
		if (capitalHomeBlock == null)
			return removedTowns;

		final WorldCoord capitalCoord = capitalHomeBlock.getWorldCoord();
		WorldCoord townCoord;
		for (Town town : nation.getTowns()) {
			// Town is the capital we're measuring against.
			if (town.equals(newCapital))
				continue;
			// Town missing homeblock, they shouldn't be in the nation any more.
			if (!town.hasHomeBlock()) {
				removedTowns.add(town);
				continue;
			}
			townCoord = town.getHomeBlockOrNull().getWorldCoord();
			// Town homeblock in the wrong world, they shouldn't be in the nation any more.
			if (capitalCoord.getWorldName().equals(townCoord.getWorldName())){
				removedTowns.add(town);
				continue;
			}
			// Town homeblock too far away, they shouldn't be in the nation any more.
			if (isTownTooFarFromNation(town, nation))
				removedTowns.add(town);
		}
		return removedTowns;
	}

	public static void removeOutOfRangeTowns(Nation nation) {
		if (!nation.hasCapital() || TownySettings.getNationProximityToCapital() <= 0)
			return;

		List<Town> toRemove = gatherOutOfRangeTowns(nation);
		if (toRemove.isEmpty())
			return;

		toRemove.stream().forEach(town -> {
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_left_nation", nation.getName()));
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_town_left", town.getName()));
			town.removeNation();
			town.save();
		});
	}

	public static boolean isTownTooFarFromNation(Town town, Nation nation) {
		if (closeEnoughToCapital(town, nation))
			return false;
		if (closeEnoughToOtherNationTowns(town, nation))
			return false;
		return true;
	}

	private static boolean closeEnoughToCapital(Town town, Nation nation) {
		return closeEnoughToTown(town, nation.getCapital(), TownySettings.getNationProximityToCapital());
	}

	private static boolean closeEnoughToOtherNationTowns(Town town, Nation nation) {
		double maxDistanceFromOtherTowns = TownySettings.getNationProximityToOtherNationTowns();
		double maxDistanceFromTheCapital = TownySettings.getNationProximityAbsoluteMaximum();

		// The town is too far from the nation's absolute cap on proximity from the capital homeblock.
		if (maxDistanceFromTheCapital > 0 && !closeEnoughToTown(town, nation.getCapital(), maxDistanceFromTheCapital))
			return false;

		// Try to find at least one town in the nation which is close enough to this town.
		return nation.getTowns().stream()
				.filter(t -> !t.equals(town) && !t.isCapital())
				.anyMatch(t -> closeEnoughToTown(town, t, maxDistanceFromOtherTowns));
	}

	private static boolean closeEnoughToTown(Town town1, Town town2, double maxDistance) {
		if (!town1.hasHomeBlock() || !town2.hasHomeBlock())
			return false;

		WorldCoord town1Coord = town1.getHomeBlockOrNull().getWorldCoord();
		WorldCoord town2Coord = town2.getHomeBlockOrNull().getWorldCoord();
		if (!town1Coord.getWorldName().equals(town2Coord.getWorldName()))
			return false;
		return MathUtil.distance(town1Coord, town2Coord) <= maxDistance;
	}
}
