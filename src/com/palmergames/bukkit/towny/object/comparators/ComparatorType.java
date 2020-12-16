package com.palmergames.bukkit.towny.object.comparators;

public enum ComparatorType {
	RESIDENTS("Number of Residents"),
	TOWNBLOCKS("Number of Claimed Townblocks"),
	BALANCE("Bank Balance"),
	ONLINE("Online Players"),
	TOWNS("Number of Towns"),
	NAME("Alphabetical Order"),
	OPEN("Open Status");
	
	private final String name;
	ComparatorType(String name) {
		this.name = name;
	}
	public String getName() {
		return name;
	}
}
