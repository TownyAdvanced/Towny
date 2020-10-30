package com.palmergames.bukkit.towny.war.eventwar;

public enum WarType {
	RIOT("Riot", false, 1, 100.0),
	TOWNWAR("Town vs Town War", false, 5, 100.0),
	CIVILWAR("National Civil War", false, 5, 100.0),
	NATIONWAR("Nation vs Nation War", false, 10, 1000.0),
	WORLDWAR("World War", true, -1, 10000.0);
	
	String name;
	boolean hasTownBlockHP;
	int lives;
	double baseSpoils;
	WarType(String name, boolean hasTownBlockHP, int lives, double baseSpoils) {
		this.name = name;
		this.hasTownBlockHP = hasTownBlockHP;
		this.lives = lives;
		this.baseSpoils = baseSpoils;
	}
	
	public String getName() {
		return name;
	}
}
