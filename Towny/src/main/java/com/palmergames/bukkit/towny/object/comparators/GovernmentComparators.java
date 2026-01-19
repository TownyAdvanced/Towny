package com.palmergames.bukkit.towny.object.comparators;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;

import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;
/**
 * A list of static comparators used for organizing lists of {@link Government}'s
 */
public class GovernmentComparators {

	public static final Comparator<Government> BY_NUM_RESIDENTS = Collections.reverseOrder(Comparator.comparingInt(town -> town.getResidents().size()));
	public static final Comparator<Government> BY_NAME = Comparator.comparing(TownyObject::getName);
	public static final Comparator<Government> BY_BANK_BALANCE = (g1, g2) -> Double.compare(g2.getAccount().getCachedBalance(false), g1.getAccount().getCachedBalance(false));
	public static final Comparator<Government> BY_NUM_ONLINE = Collections.reverseOrder(Comparator.comparingInt(town -> TownyAPI.getInstance().getOnlinePlayers(town).size()));
	public static final Comparator<Government> BY_TOWNBLOCKS_CLAIMED = (g1, g2) -> Double.compare(g2.getTownBlocks().size(), g1.getTownBlocks().size());
	public static final Comparator<Government> BY_FOUNDED = Comparator.comparingLong(Government::getRegistered);
	public static final Comparator<Government> BY_OPEN = (t1, t2) -> doCompare(t1, t2, Government::isOpen);
	public static final Comparator<Government> BY_PUBLIC = (t1, t2) -> doCompare(t1, t2, Government::isPublic);
	public static final Comparator<Government> BY_UPKEEP = (t1, t2) -> {
		if (!TownySettings.isTaxingDaily())
			return 0;
		
		double upkeep1 = 0;
		double upkeep2 = 0;
		
		if (t1 instanceof Town town)
			upkeep1 = TownySettings.getTownUpkeepCost(town);
		else if (t1 instanceof Nation nation)
			upkeep1 = TownySettings.getNationUpkeepCost(nation);
		
		if (t2 instanceof Town town)
			upkeep2 = TownySettings.getTownUpkeepCost(town);
		else if (t2 instanceof Nation nation)
			upkeep2 = TownySettings.getNationUpkeepCost(nation);
		
		return Double.compare(upkeep1, upkeep2);
	};

	private static int doCompare(Government t1, Government t2, Function<Government, Boolean> func) {
		if (func.apply(t1) && func.apply(t2))
			return t2.getResidents().size() - t1.getResidents().size();

		if (func.apply(t2)) // Less than.
			return 1;
		else // Greater than.
			return -1;
	}
}
