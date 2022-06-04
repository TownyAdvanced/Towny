package com.palmergames.bukkit.towny.object.comparators;

import java.util.Comparator;

import com.palmergames.bukkit.towny.object.Alliance;
import com.palmergames.bukkit.towny.object.Town;

/**
 * A list of static comparators used for organizing lists of {@link Town}'s
 */
public class AllianceComparators {
	public static final Comparator<Alliance> BY_NUM_NATIONS = (a1, a2) -> a2.getMembers().size() - a1.getMembers().size();
}
