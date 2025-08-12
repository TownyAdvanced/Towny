package com.palmergames.bukkit.towny.object.comparators;

import java.util.Comparator;
import java.util.function.Function;

import com.palmergames.bukkit.towny.object.Town;

/**
 * A list of static comparators used for organizing lists of {@link Town}'s
 */
public class TownComparators {
	public static final Comparator<Town> BY_FORSALE = (t1, t2) -> doCompare(t1, t2, Town::isForSale);
	public static final Comparator<Town> BY_RUINED = (t1, t2) -> doCompare(t1, t2, Town::isRuined);
	public static final Comparator<Town> BY_BANKRUPT = (t1, t2) -> doCompare(t1, t2, Town::isBankrupt);

	private static int doCompare(Town t1, Town t2, Function<Town, Boolean> func) {
		if (func.apply(t1) && func.apply(t2))
			return t2.getResidents().size() - t1.getResidents().size();

		if (func.apply(t2)) // Less than.
			return 1;
		else // Greater than.
			return -1;
	}
}
