package com.palmergames.bukkit.towny.object.comparators;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.Comparator;

/**
 * A list of static comparators used for organizing lists of {@link Nation}'s
 */
public class NationComparators {
	public static final Comparator<Nation> BY_NUM_TOWNS = (n1, n2) -> n2.getTowns().size() - n1.getTowns().size();
	public static final Comparator<Nation> BY_TOWNBLOCKS_CLAIMED = (n1, n2) -> Double.compare(n2.getNumTownblocks(), n1.getNumTownblocks());
}
