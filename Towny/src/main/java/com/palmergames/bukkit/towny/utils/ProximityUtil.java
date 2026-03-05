package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.event.NationRangeAllowTownEvent;
import com.palmergames.bukkit.util.BukkitTools;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.util.MathUtil;

public class ProximityUtil {

	/*
	 * Town Proximity Methods
	 */

	public static void allowTownHomeBlockOrThrow(TownyWorld world, Coord key, @Nullable Town town, boolean newTown) throws TownyException {
		if (!world.hasTowns()) // No towns exist yet, we cannot be too close to any other town.
			return;

		if (newTown) { // Tests run only when there is a new town involved.
			if (TownySettings.getMinDistanceFromTownPlotblocks() > 0 || TownySettings.getNewTownMinDistanceFromTownPlots() > 0) {
				// Sometimes new towns have special min. distances from other towns.
				int minDistance = TownySettings.getNewTownMinDistanceFromTownPlots();
				if (minDistance <= 0)
					minDistance = TownySettings.getMinDistanceFromTownPlotblocks();

				// throws when a new town is being made to close to another town's land.
				if (world.getMinDistanceFromOtherTownsPlots(key) < minDistance)
					throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));
			}
		}

		if (TownySettings.getMinDistanceFromTownHomeblocks() > 0 ||
			TownySettings.getMaxDistanceBetweenHomeblocks() > 0 ||
			TownySettings.getMinDistanceBetweenHomeblocks() > 0 ||
			(newTown && TownySettings.getNewTownMinDistanceFromTownHomeblocks() > 0)) {

			final int distanceToNextNearestHomeblock = world.getMinDistanceFromOtherTownsHomeBlocks(key, town);
			// Sometimes new towns have special min. distances from other towns' homeblocks.
			int minDistance = newTown ? TownySettings.getNewTownMinDistanceFromTownHomeblocks() : 0;
			if (minDistance <= 0)
				minDistance = TownySettings.getMinDistanceFromTownHomeblocks();

			// throws when the town's homeblock is too close to another homeblock.
			if (distanceToNextNearestHomeblock < minDistance || distanceToNextNearestHomeblock < TownySettings.getMinDistanceBetweenHomeblocks()) 
				throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

			// throws when the town's homeblock would be too far from other towns' homeblocks.
			if (TownySettings.getMaxDistanceBetweenHomeblocks() > 0 &&
				distanceToNextNearestHomeblock > TownySettings.getMaxDistanceBetweenHomeblocks())
				throw new TownyException(Translatable.of("msg_too_far"));
		}
	}

	public static void allowTownClaimOrThrow(TownyWorld world, WorldCoord townBlockToClaim, @Nullable Town town, boolean outpost) throws TownyException {
		allowTownClaimOrThrow(world, townBlockToClaim, town, outpost, false);
	}

	public static void allowTownClaimOrThrow(TownyWorld world, WorldCoord townBlockToClaim, @Nullable Town town, boolean outpost, boolean trade) throws TownyException {
		// Check if the town has claims available.
		if (!town.hasUnlimitedClaims() && town.availableTownBlocks() <= 0)
			throw new TownyException(Translatable.of("msg_err_not_enough_blocks"));

		// Check if this is already claimed by someone, as long as this isn't a townblock being ceded to another town.
		if (!trade && !townBlockToClaim.isWilderness())
			throw new TownyException(Translatable.of("msg_already_claimed", townBlockToClaim.getTownOrNull()));

		// Check distance to other homeblocks.
		if (AreaSelectionUtil.isTooCloseToHomeBlock(townBlockToClaim, town))
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("homeblock")));

		// Check distance to other townblocks.
		if (world.getMinDistanceFromOtherTownsPlots(townBlockToClaim, town) < TownySettings.getMinDistanceFromTownPlotblocks())
			throw new TownyException(Translatable.of("msg_too_close2", Translatable.of("townblock")));

		// Check adjacent claims rules.
		testAdjacentClaimsRulesOrThrow(townBlockToClaim, town, outpost);

		// Check that we're on an edge if it is not an outpost.
		if (!outpost && !isEdgeBlock(town, townBlockToClaim) && !town.getTownBlocks().isEmpty())
			throw new TownyException(Translatable.of("msg_err_not_attached_edge"));
	}

	public static void testAdjacentClaimsRulesOrThrow(WorldCoord townBlockToClaim, Town town, boolean outpost) throws TownyException {
		int minAdjacentBlocks = TownySettings.getMinAdjacentBlocks();
		testAdjacentClaimsRulesOrThrow(townBlockToClaim, town, outpost, minAdjacentBlocks);
	}

	public static void testAdjacentClaimsRulesOrThrow(WorldCoord townBlockToClaim, Town town, boolean outpost, int minAdjacentBlocks) throws TownyException {
		if (!outpost && minAdjacentBlocks > 0 && townHasClaimedEnoughLandToBeRestrictedByAdjacentClaims(town, minAdjacentBlocks)) {
			// Only consider the first worldCoord, larger selection-claims will automatically "bubble" anyways.
			int numAdjacent = numAdjacentTownOwnedTownBlocks(town, townBlockToClaim);
			// The number of adjacement TBs is not enough and there is not a nearby outpost.
			if (numAdjacent < minAdjacentBlocks && numAdjacentOutposts(town, townBlockToClaim) == 0)
				throw new TownyException(Translatable.of("msg_min_adjacent_blocks", minAdjacentBlocks, numAdjacent));
		}
	}

	private static boolean townHasClaimedEnoughLandToBeRestrictedByAdjacentClaims(Town town, int minAdjacentBlocks) {
		if (minAdjacentBlocks == 3 && town.getTownBlocks().size() < 5)
			// Special rule that makes sure a town can claim a fifth plot after claiming a 2x2 square.
			return false;
		return town.getTownBlocks().size() > minAdjacentBlocks;
	}

	private static int numAdjacentTownOwnedTownBlocks(Town town, WorldCoord worldCoord) {
		return (int) worldCoord.getCardinallyAdjacentWorldCoords(true).stream()
			.filter(wc -> wc.hasTown(town))
			.count();
	}

	private static int numAdjacentOutposts(Town town, WorldCoord worldCoord) {
		return (int) worldCoord.getCardinallyAdjacentWorldCoords(true).stream()
			.filter(wc -> wc.hasTown(town))
			.map(WorldCoord::getTownBlockOrNull)
			.filter(Objects::nonNull)
			.filter(TownBlock::isOutpost)
			.count();
	}

	private static boolean isEdgeBlock(TownBlockOwner owner, WorldCoord worldCoord) {

		for (WorldCoord wc : worldCoord.getCardinallyAdjacentWorldCoords()) {
			if (wc.isWilderness()) {
				continue;
			}
			if (!wc.getTownBlockOrNull().isOwner(owner)) {
				continue;
			}
			return true;
		}
		return false;
	}

	/*
	 * Town Unclaim Methods
	 */

	public static void allowTownUnclaimOrThrow(TownyWorld world, WorldCoord townBlockToUnclaim, @Nullable Town town) throws TownyException {

		if (townBlockToUnclaim.isWilderness())
			throw new TownyException(Translatable.of("msg_err_empty_area_selection"));

		if (!townBlockToUnclaim.getTownBlockOrNull().getTownOrNull().equals(town))
			throw new TownyException(Translatable.of("msg_not_own_area"));

		if (townBlockToUnclaim.getTownBlock().isHomeBlock())
			throw new TownyException(Translatable.of("msg_err_cannot_unclaim_homeblock"));

		 testAdjacentUnclaimsRulesOrThrow(townBlockToUnclaim, town);
	}

	public static void testAdjacentUnclaimsRulesOrThrow(WorldCoord townBlockToUnclaim, Town town) throws TownyException {
		// Prevent unclaiming land that would reduce the number of adjacent claims of neighbouring plots below the threshold.
		int minAdjacentBlocks = TownySettings.getMinAdjacentBlocks();
		testAdjacentUnclaimsRulesOrThrow(townBlockToUnclaim, town, minAdjacentBlocks);
	}
	
	public static void testAdjacentUnclaimsRulesOrThrow(WorldCoord townBlockToUnclaim, Town town, int minAdjacentBlocks) throws TownyException {
		// Prevent unclaiming land that would reduce the number of adjacent claims of neighbouring plots below the threshold.
		if (minAdjacentBlocks > 0 && townHasClaimedEnoughLandToBeRestrictedByAdjacentClaims(town, minAdjacentBlocks)) {
			WorldCoord firstWorldCoord = townBlockToUnclaim;
			for (WorldCoord wc : firstWorldCoord.getCardinallyAdjacentWorldCoords(true)) {
				if (wc.isWilderness() || !wc.hasTown(town))
					continue;
				int numAdjacent = numAdjacentTownOwnedTownBlocks(town, wc);
				// The number of adjacement TBs is not enough and there is not a nearby outpost.
				if (numAdjacent - 1 < minAdjacentBlocks && numAdjacentOutposts(town, wc) == 0)
					throw new TownyException(Translatable.of("msg_err_cannot_unclaim_not_enough_adjacent_claims", wc.getX(), wc.getZ(), numAdjacent));
			}
		}
	}

	/*
	 * District add/remove methods
	 */

	public static void testAdjacentAddDistrictRulesOrThrow(WorldCoord townBlockToClaim, Town town, District district, int minAdjacentBlocks) throws TownyException {
		if (minAdjacentBlocks > 0 && townHasClaimedEnoughLandToBeRestrictedByAdjacentClaims(town, minAdjacentBlocks)) {
			int numAdjacent = numAdjacentDistrictTownBlocks(town, district, townBlockToClaim);
			// The number of adjacement TBs with the same District is not enough.
			if (numAdjacent < minAdjacentBlocks)
				throw new TownyException(Translatable.of("msg_min_adjacent_district_blocks", minAdjacentBlocks));
		}
	}

	private static int numAdjacentDistrictTownBlocks(Town town, District district, WorldCoord worldCoord) {
		return (int) worldCoord.getCardinallyAdjacentWorldCoords(true).stream()
			.filter(wc -> wc.hasTown(town) && wc.getTownBlockOrNull() != null)
			.map(wc -> wc.getTownBlockOrNull())
			.filter(tb -> tb.hasDistrict() && tb.getDistrict().equals(district))
			.count();
	}

	public static void testAdjacentRemoveDistrictRulesOrThrow(WorldCoord districtCoordBeingRemoved, Town town, District district, int minAdjacentBlocks) throws TownyException {
		// Prevent removing parts of Districts that would cause a district to split into two sections.
		if (minAdjacentBlocks > 0 && townHasClaimedEnoughLandToBeRestrictedByAdjacentClaims(town, minAdjacentBlocks)) {
			List<WorldCoord> allAdjacentDistrictWorldCoords = getAdjacentDistrictWorldCoords(town, district, districtCoordBeingRemoved, false);
			int districtPlots = allAdjacentDistrictWorldCoords.size();

			// There's enough District plots unclaiming this TownBlock wouldn't matter.
			if (districtPlots >= 7)
				return;

			// There's not enough District plots.
			if (districtPlots < minAdjacentBlocks)
				throw new TownyException(Translatable.of("msg_err_cannot_remove_from_district_not_enough_adjacent_claims", district.getName()));

			for (WorldCoord wc : allAdjacentDistrictWorldCoords) {
				if (wc.isWilderness() || !wc.hasTown(town) || !wc.getTownBlock().hasDistrict() || !wc.getTownBlock().getDistrict().getName().equals(district.getName()))
					continue;
				int numAdjacent = numAdjacentDistrictTownBlocks(town, district, wc);
				// The number of adjacement TBs with the same District is not enough
				if (numAdjacent - 1 < minAdjacentBlocks)
					throw new TownyException(Translatable.of("msg_err_cannot_remove_from_district_not_enough_adjacent_claims", wc.getX(), wc.getZ(), numAdjacent));
			}

			/*
			 * Handle the case where a single bridge plot connecting two sides of a District is being unclaimed.
			 */

			// Use only cardinally-adjacent districts now, a + district shape, with the centre being unclaimed.
			List<WorldCoord> cardinallyAdjacentDistrictWorldCoords = getAdjacentDistrictWorldCoords(town, district, districtCoordBeingRemoved, true);
			if (checkForTwoDistrictsPlotsOnOppositeSides(cardinallyAdjacentDistrictWorldCoords, district.getName()))
				return;

			// Same thing but testing for an X style district shape, with the centre being unclaimed.
			for (WorldCoord coord : cardinallyAdjacentDistrictWorldCoords)
				allAdjacentDistrictWorldCoords.remove(coord);

			if (checkForTwoDistrictsPlotsOnOppositeSides(allAdjacentDistrictWorldCoords, district.getName()))
				return;
		}
	}

	private static boolean checkForTwoDistrictsPlotsOnOppositeSides(List<WorldCoord> worldCoordsToTest, String districtName) throws TownyException {
		// We only want to pay attention to cases where the district plots are on opposite sides of the districtCoord being removed.
		if (worldCoordsToTest.size() != 2)
			return true;

		double distance = MathUtil.distance(worldCoordsToTest.get(0), worldCoordsToTest.get(1));
		// A District on either side of the townBlockToUnclaim would have a distance of 2. 1.4 is when they are on a "corner". 
		if (distance >= 2)
			throw new TownyException(Translatable.of("msg_err_cannot_remove_from_district_not_enough_adjacent_claims", districtName));

		// Safe to return.
		return true;
	}

	private static List<WorldCoord> getAdjacentDistrictWorldCoords(Town town, District district, WorldCoord worldCoord, boolean cardinalOnly) {
		return worldCoord.getCardinallyAdjacentWorldCoords(!cardinalOnly).stream()
			.filter(wc -> wc.hasTown(town) && wc.getTownBlockOrNull() != null)
			.map(wc -> wc.getTownBlockOrNull())
			.filter(tb -> tb.hasDistrict() && tb.getDistrict().equals(district))
			.map(TownBlock::getWorldCoord)
			.collect(Collectors.toList());
	}

	/*
	 * Nation Promixity Methods
	 */

	public static void testTownProximityToNation(Town town, Nation nation) throws TownyException {
		if (TownySettings.getNationProximityToCapital() <= 0)
			return;

		Town capital = nation.getCapital();
		if (!capital.hasHomeBlock() || !town.hasHomeBlock()) {
			throw new TownyException(Translatable.of("msg_err_homeblock_has_not_been_set"));
		}

		WorldCoord capitalCoord = capital.getHomeBlockOrNull().getWorldCoord();
		WorldCoord townCoord = town.getHomeBlockOrNull().getWorldCoord();

		if (!capitalCoord.getWorldName().equalsIgnoreCase(townCoord.getWorldName())) {
			throw new TownyException(Translatable.of("msg_err_nation_homeblock_in_another_world"));
		}

		List<Town> townsClosestToFarthest = sortTownsClosestToFarthest(nation);
		NationRangeAllowTownEvent nrate = new NationRangeAllowTownEvent(nation, town);
		if (isTownTooFarFromNation(town, capital, townsClosestToFarthest)) {
			nrate.setCancelled(true);
			nrate.callEvent();
			if (!nrate.isCancelled())
				return; // Another plugin has allowed the join
			throw new TownyException(Translatable.of("msg_err_town_not_close_enough_to_nation", town.getName()));
		}
		BukkitTools.ifCancelledThenThrow(nrate);
	}

	private static List<Town> sortTownsClosestToFarthest(Nation nation) {
		List<Town> sortedTowns = nation.getTowns().stream()
				.sorted(Comparator.comparingInt(t-> getDistanceFromCapital(t, nation)))
				.collect(Collectors.toList());
		return sortedTowns; 
	}

	private static int getDistanceFromCapital(Town town, Nation nation) {
		TownBlock capitalHomeblock = nation.getCapital().getHomeBlockOrNull();
		TownBlock townHomeblock = town.getHomeBlockOrNull();
		if (capitalHomeblock == null || townHomeblock == null)
			return Integer.MAX_VALUE;
		if (!capitalHomeblock.getWorld().equals(townHomeblock.getWorld()))
			return Integer.MAX_VALUE;
		return (int) MathUtil.distance(capitalHomeblock.getCoord(), townHomeblock.getCoord());
	}

	public static List<Town> gatherOutOfRangeTowns(Nation nation) {
		return gatherOutOfRangeTowns(nation, nation.getCapital());
	}

	public static List<Town> gatherOutOfRangeTowns(Nation nation, Town capital) {
		List<Town> removedTowns = new ArrayList<>();
		if (TownySettings.getNationProximityToCapital() <= 0)
			return removedTowns;

		final TownBlock capitalHomeBlock = capital.getHomeBlockOrNull();
		// This is unlikely to happen but if a capital has no homeblock by some error we don't want to remove every town.
		if (capitalHomeBlock == null)
			return removedTowns;

		List<Town> townsToCheck = sortTownsClosestToFarthest(nation);
		List<Town> localTownsToKeep = new ArrayList<>();
		townsToCheck.remove(capital);
		localTownsToKeep.add(capital);

		// We want to parse over the towns to check until we're no longer getting an above 0 amount of towns being removed.
		while (townsToCheck.size() > 0) {
			// Get a list of towns which are OK based on their range to the capital OR if they're close enough to a town in range.
			List<Town> recentValidTowns = getListOfInRangeTownsFromList(townsToCheck, localTownsToKeep, capital, nation);

			// Stop the loop if we haven't gotten any valid towns this pass.
			if (recentValidTowns.size() == 0)
				break;

			// Put any newly valid towns into the townToKeep List, remove them from being checked.
			for (Town validTown : recentValidTowns) {
				localTownsToKeep.add(validTown);
				townsToCheck.remove(validTown);
			}
		}

		// Finalize a list of out of range towns.
		return nation.getTowns().stream().filter(t -> !localTownsToKeep.contains(t)).collect(Collectors.toList());
	}

	private static List<Town> getListOfInRangeTownsFromList(List<Town> townsToCheck, List<Town> validTowns, Town capital, Nation nation) {
		List<Town> allowedTowns = new ArrayList<>();
		for (Town town : townsToCheck) {
			// Town is the capital we're measuring against.
			if (town.equals(capital))
				continue;
			// Check that the town missing is not missing a homeblock, and that the
			// homeblocks are in the same world, and the distance between.
			if (isTownCloseEnoughToNation(town, capital, townsToCheck, validTowns)) {
				allowedTowns.add(town);
			} else {
				NationRangeAllowTownEvent nrate = new NationRangeAllowTownEvent(nation, town);
				nrate.setCancelled(true);
				nrate.callEvent();
				if (!nrate.isCancelled()) // Another plugin has un-canceled the event
					allowedTowns.add(town);
			}
		}		
		return allowedTowns;
	}

	public static boolean isTownCloseEnoughToNation(Town town, Town newCapital, List<Town> townsToCheck, List<Town> validTowns) {
		if (closeEnoughToCapital(town, newCapital) || closeEnoughToOtherNationTowns(town, newCapital, townsToCheck, validTowns))
			return true;
		return false;
	}

	private static boolean closeEnoughToOtherNationTowns(Town town, Town newCapital, List<Town> townsToCheck, List<Town> validTowns) {
		double maxDistanceFromOtherTowns = TownySettings.getNationProximityToOtherNationTowns();
		double maxDistanceFromTheCapital = TownySettings.getNationProximityAbsoluteMaximum();

		// Other towns in the nation are not giving any proximity buff, only the capital is counted.
		if (maxDistanceFromOtherTowns <= 0)
			return false;

		// The town is too far from the nation's absolute cap on proximity from the capital homeblock.
		if (maxDistanceFromTheCapital > 0 && !closeEnoughToTown(town, newCapital, maxDistanceFromTheCapital))
			return false;

		// Try to find at least one town in the nation which is close enough to this town.
		for (Town validTown : validTowns) {
			if (closeEnoughToTown(validTown, town, maxDistanceFromOtherTowns))
				return true;
		}
		return false;
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

	public static boolean isTownTooFarFromNation(Town town, Town newCapital, List<Town> towns) {
		if (closeEnoughToCapital(town, newCapital) || closeEnoughToOtherNationTowns(town, newCapital, towns))
			return false;
		return true;
	}

	private static boolean closeEnoughToCapital(Town town, Town newCapital) {
		return closeEnoughToTown(town, newCapital, TownySettings.getNationProximityToCapital());
	}

	private static boolean closeEnoughToOtherNationTowns(Town town, Town newCapital, List<Town> towns) {
		double maxDistanceFromOtherTowns = TownySettings.getNationProximityToOtherNationTowns();
		double maxDistanceFromTheCapital = TownySettings.getNationProximityAbsoluteMaximum();

		// Other towns in the nation are not giving any proximity buff, only the capital is counted.
		if (maxDistanceFromOtherTowns <= 0)
			return false;

		// The town is too far from the nation's absolute cap on proximity from the capital homeblock.
		if (maxDistanceFromTheCapital > 0 && !closeEnoughToTown(town, newCapital, maxDistanceFromTheCapital))
			return false;

		// Try to find at least one town in the nation which is close enough to this town.
		return towns.stream()
				.filter(t -> !t.equals(town) && !t.isCapital())
				.anyMatch(t -> closeEnoughToTown(town, t, maxDistanceFromOtherTowns));
	}

	private static boolean closeEnoughToTown(Town town1, Town town2, double maxAllowedDistance) {
		if (!town1.hasHomeBlock() || !town2.hasHomeBlock())
			return false;

		WorldCoord town1Coord = town1.getHomeBlockOrNull().getWorldCoord();
		WorldCoord town2Coord = town2.getHomeBlockOrNull().getWorldCoord();
		if (!town1Coord.getWorldName().equals(town2Coord.getWorldName()))
			return false;
		return MathUtil.distance(town1Coord, town2Coord) <= maxAllowedDistance;
	}
}
