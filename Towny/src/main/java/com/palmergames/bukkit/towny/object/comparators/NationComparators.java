package com.palmergames.bukkit.towny.object.comparators;

import com.palmergames.bukkit.towny.object.Nation;

import java.util.Comparator;

/**
 * A list of static comparators used for organizing lists of {@link Nation}'s
 */
public class NationComparators {
	public static final Comparator<Nation> BY_NUM_TOWNS = Comparator.comparingInt(nation -> nation.getTowns().size());
}
