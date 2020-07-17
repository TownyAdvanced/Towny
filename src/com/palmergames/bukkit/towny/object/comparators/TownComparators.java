package com.palmergames.bukkit.towny.object.comparators;

import com.palmergames.bukkit.towny.object.Town;

import java.util.Comparator;

/**
 * A list of static comparators used for organizing lists of {@link Town}'s
 */
public class TownComparators {
	public static final Comparator<Town> BY_TOWNBLOCKS_CLAIMED = (t1, t2) -> Double.compare(t2.getTownBlocks().size(), t1.getTownBlocks().size());
}
