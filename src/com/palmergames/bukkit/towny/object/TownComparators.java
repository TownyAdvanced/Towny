package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.EconomyException;

import java.util.Comparator;

public class TownComparators {
	public static final Comparator<Town> BY_NUM_RESIDENTS = (t1, t2) -> t2.getNumResidents() - t1.getNumResidents();
	public static final Comparator<Town> BY_OPEN = (t1, t2) -> {
		
		// Both are open, fallback to population comparison.
		if (t1.isOpen() && t2.isOpen()) {
			return t2.getNumResidents() - t1.getNumResidents();
		}
		
		// Less than.
		if (t2.isOpen()) {
			return 1;
		} else {
			// Greater than.
			return -1;
		}
	};
	public static final Comparator<Town> BY_NAME = Comparator.comparing(TownyObject::getName);
	public static final Comparator<Town> BY_BANK_BALANCE = (t1, t2) -> {
		try {
			return Double.compare(t2.getAccount().getHoldingBalance(), t1.getAccount().getHoldingBalance());
		} catch (EconomyException e) {
			throw new RuntimeException("Failed to get balance. Aborting.");
		}
	};

	public static final Comparator<Town> BY_TOWNBLOCKS_CLAIMED = (t1, t2) -> Double.compare(t2.getTownBlocks().size(), t1.getTownBlocks().size());
	public static final Comparator<Town> BY_NUM_ONLINE = (t1, t2) -> TownyAPI.getInstance().getOnlinePlayers(t2).size() - TownyAPI.getInstance().getOnlinePlayers(t1).size();
	
}
