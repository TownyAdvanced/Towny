package com.palmergames.bukkit.towny.object.comparators;

import java.util.Comparator;
import com.palmergames.bukkit.towny.object.Government;

public enum ComparatorType {
	RESIDENTS("msg_comptype_residents", GovernmentComparators.BY_NUM_RESIDENTS),
	TOWNBLOCKS("msg_comptype_townblocks", GovernmentComparators.BY_TOWNBLOCKS_CLAIMED),
	BALANCE("msg_comptype_balance", GovernmentComparators.BY_BANK_BALANCE),
	ONLINE("msg_comptype_online", GovernmentComparators.BY_NUM_ONLINE),
	TOWNS("msg_comptype_towns", NationComparators.BY_NUM_TOWNS),
	NAME("msg_comptype_name", GovernmentComparators.BY_NAME),
	OPEN("msg_comptype_open", GovernmentComparators.BY_OPEN);

	private final String name;
	private final Comparator<? extends Government> comparator;
	ComparatorType(String name, Comparator<? extends Government> comparator) {
		this.name = name;
		this.comparator = comparator;
	}
	public String getName() {
		return name;
	}
	public Comparator<? extends Government> getComparator() {
		return comparator;
	}
	
}
