package com.palmergames.bukkit.towny.object.comparators;

import java.util.Comparator;

import com.palmergames.bukkit.towny.object.Town;

/**
 * A list of static comparators used for organizing lists of {@link Town}'s
 */
public class TownComparators {
	public static final Comparator<Town> BY_FORSALE = (t1, t2) -> {

		// Both are for sale, fallback to population comparison.
		if (t1.isForSale() && t2.isForSale()) {
			return t2.getResidents().size() - t1.getResidents().size();
		}

		// Less than.
		if (t2.isForSale()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};
	public static final Comparator<Town> BY_RUINED = (t1, t2) -> {

		// Both are ruined, fallback to population comparison.
		if (t1.isRuined() && t2.isRuined()) {
			return t2.getResidents().size() - t1.getResidents().size();
		}

		// Less than.
		if (t2.isRuined()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};
	public static final Comparator<Town> BY_BANKRUPT = (t1, t2) -> {

		// Both are bankrupt, fallback to population comparison.
		if (t1.isBankrupt() && t2.isBankrupt()) {
			return t2.getResidents().size() - t1.getResidents().size();
		}

		// Less than.
		if (t2.isBankrupt()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};

}
