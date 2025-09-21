package com.palmergames.bukkit.towny.object.comparators;

import java.util.Comparator;
import com.palmergames.bukkit.towny.object.Government;

public enum ComparatorType {
	RESIDENTS("msg_comptype_residents", GovernmentComparators.BY_NUM_RESIDENTS, "by residents"),
	TOWNBLOCKS("msg_comptype_townblocks", GovernmentComparators.BY_TOWNBLOCKS_CLAIMED, "by townblocks"),
	BALANCE("msg_comptype_balance", GovernmentComparators.BY_BANK_BALANCE, "by balance"),
	ONLINE("msg_comptype_online", GovernmentComparators.BY_NUM_ONLINE, "by online"),
	TOWNS("msg_comptype_towns", NationComparators.BY_NUM_TOWNS, "by towns"),
	FORSALE("msg_comptype_forsale", TownComparators.BY_FORSALE, "by forsale"),
	FOUNDED("msg_comptype_founded", GovernmentComparators.BY_FOUNDED, "by founded"),
	NAME("msg_comptype_name", GovernmentComparators.BY_NAME, "by name"),
	OPEN("msg_comptype_open", GovernmentComparators.BY_OPEN, "by open"),
	PUBLIC("msg_comptype_public", GovernmentComparators.BY_PUBLIC, "by public"),
	RUINED("msg_comptype_ruined", TownComparators.BY_RUINED, "by ruined"),
	BANKRUPT("msg_comptype_bankrupt", TownComparators.BY_BANKRUPT, "by bankrupt"),
	UPKEEP("msg_comptype_upkeep", GovernmentComparators.BY_UPKEEP, "by upkeep");

	private final String name;
	private final Comparator<? extends Government> comparator;
	private final String commandString;
	ComparatorType(String name, Comparator<? extends Government> comparator, String commandString) {
		this.name = name;
		this.comparator = comparator;
		this.commandString = commandString;
	}
	public String getName() {
		return name;
	}
	public Comparator<? extends Government> getComparator() {
		return comparator;
	}
	public String getCommandString() {
		return commandString;
	}
	
}
