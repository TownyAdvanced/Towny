package com.palmergames.bukkit.towny.object.comparators;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.TownyObject;

import java.util.Comparator;
/**
 * A list of static comparators used for organizing lists of {@link Government}'s
 */
public class GovernmentComparators {

	public static final Comparator<Government> BY_NUM_RESIDENTS = (t1, t2) -> t2.getResidents().size() - t1.getResidents().size();
	public static final Comparator<Government> BY_NAME = Comparator.comparing(TownyObject::getName);
	public static final Comparator<Government> BY_BANK_BALANCE = (g1, g2) -> Double.compare(g2.getAccount().getCachedBalance(), g1.getAccount().getCachedBalance());
	public static final Comparator<Government> BY_NUM_ONLINE = (g1, g2) -> TownyAPI.getInstance().getOnlinePlayers(g2).size() - TownyAPI.getInstance().getOnlinePlayers(g1).size();
	public static final Comparator<Government> BY_TOWNBLOCKS_CLAIMED = (g1, g2) -> Double.compare(g2.getTownBlocks().size(), g1.getTownBlocks().size());
	public static final Comparator<Government> BY_FOUNDED = (g1, g2) -> Long.compare(g1.getRegistered(), g2.getRegistered());
	public static final Comparator<Government> BY_OPEN = (t1, t2) -> {

		// Both are open, fallback to population comparison.
		if (t1.isOpen() && t2.isOpen()) {
			return t2.getResidents().size() - t1.getResidents().size();
		}

		// Less than.
		if (t2.isOpen()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};
	public static final Comparator<Government> BY_PUBLIC = (t1, t2) -> {

		// Both are open, fallback to population comparison.
		if (t1.isPublic() && t2.isPublic()) {
			return t2.getResidents().size() - t1.getResidents().size();
		}

		// Less than.
		if (t2.isPublic()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};
}
