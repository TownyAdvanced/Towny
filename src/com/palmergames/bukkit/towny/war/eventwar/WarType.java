package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;

public enum WarType {
	// TODO: Make hasTownBlocksSwitchTowns configurable here.
	RIOT(
		"Riot",
		EventWarSettings.riotDelay(),
		EventWarSettings.riotCooldown(),
		false,                                     // No TownBlockHP
		EventWarSettings.riotsMayorDeathEnabled(), 
		false,                                     // No hasTownBlocksSwitchTowns 
		EventWarSettings.riotsWinnerTakesOverTown(), 
		EventWarSettings.riotResidentLives(), 
		EventWarSettings.riotMayorLives(), 
		EventWarSettings.riotBaseSpoils(), 
		EventWarSettings.riotPointsPerKill(),
		EventWarSettings.riotTokenCost()),
	TOWNWAR(
		"Town vs Town War",
		EventWarSettings.townWarDelay(),
		EventWarSettings.townWarCooldown(),
		EventWarSettings.townWarTownBlockHPEnabled(), 
		EventWarSettings.townWarMayorDeathEnabled(), 
		false,                                     // No hasTownBlocksSwitchTowns
		EventWarSettings.townWarWinnerTakesOverTown(), 
		EventWarSettings.townWarResidentLives(), 
		EventWarSettings.townWarMayorLives(), 
		EventWarSettings.townWarBaseSpoils(), 
		EventWarSettings.townWarPointsPerKill(),
		EventWarSettings.townWarTokenCost()),
	CIVILWAR(
		"National Civil War",
		EventWarSettings.civilWarDelay(),
		EventWarSettings.civilWarCooldown(),
		EventWarSettings.civilWarTownBlockHPEnabled(), 
		EventWarSettings.civilWarMayorDeathEnabled(), 
		false,                                     // No hasTownBlocksSwitchTowns
		EventWarSettings.civilWarWinnerTakesOverNation(), 
		EventWarSettings.civilWarResidentLives(), 
		EventWarSettings.civilWarMayorLives(), 
		EventWarSettings.civilWarBaseSpoils(), 
		EventWarSettings.civilWarPointsPerKill(),
		EventWarSettings.civilWarTokenCost()),
	NATIONWAR(
		"Nation vs Nation War",
		EventWarSettings.nationWarDelay(),
		EventWarSettings.nationWarCooldown(),
		EventWarSettings.nationWarTownBlockHPEnabled(), 
		EventWarSettings.nationWarMayorDeathEnabled(), 
		false,                                     // No hasTownBlocksSwitchTowns
		EventWarSettings.nationWarWinnerConquersTowns(), 
		EventWarSettings.nationWarResidentLives(), 
		EventWarSettings.nationWarMayorLives(), 
		EventWarSettings.nationWarBaseSpoils(), 
		EventWarSettings.nationWarPointsPerKill(),
		EventWarSettings.nationWarTokenCost()),
	WORLDWAR(
		"World War", 
		EventWarSettings.worldWarDelay(),
		EventWarSettings.worldWarCooldown(),
		EventWarSettings.worldWarTownBlockHPEnabled(), 
		EventWarSettings.worldWarMayorDeathEnabled(), 
		false,                                     // No hasTownBlocksSwitchTowns
		EventWarSettings.worldWarWinnerConquersTowns(), 
		EventWarSettings.worldWarResidentLives(), 
		EventWarSettings.worldWarMayorLives(), 
		EventWarSettings.worldWarBaseSpoils(), 
		EventWarSettings.worldWarPointsPerKill(),
		EventWarSettings.worldWarTokenCost());
	
	String name;
	public int delay;
	public long cooldown;
	public boolean hasTownBlockHP;
	public boolean hasMayorDeath;
	public boolean hasTownBlocksSwitchTowns;
	public boolean hasTownConquering;
	public int residentLives;
	public int mayorLives;
	public int pointsPerKill;
	public int tokenCost;
	public double baseSpoils;
	
	/**
	 * 
	 * @param name - Base name used.
	 * @param hasTownBlockHP - Whether townblocks have HP and are fought over for points.
	 * @param hasMayorDeath - Whether killing the mayor or king will remove the town/nation from the war.
	 * @param lives - How many lives each player gets before being removed from the war.
	 * @param baseSpoils - How much money is added to the war.
	 */
	WarType(String name, int delay, long cooldown, boolean hasTownBlockHP, boolean hasMayorDeath, boolean hasTownBlocksSwitchTowns, boolean hasTownConquering, int residentLives, int mayorLives, double baseSpoils, int pointsPerKill, int tokenCost) {
		this.name = name;
		this.delay = delay;
		this.cooldown = cooldown;
		this.hasTownBlockHP = hasTownBlockHP;
		this.hasMayorDeath = hasMayorDeath;
		this.hasTownBlocksSwitchTowns = hasTownBlocksSwitchTowns;
		this.hasTownConquering = hasTownConquering;
		this.residentLives = residentLives;
		this.mayorLives = mayorLives;
		this.baseSpoils = baseSpoils;
		this.pointsPerKill = pointsPerKill;
		this.tokenCost = tokenCost;
	}
	
	public String getName() {
		return name;
	}
	
	@Override
	public String toString() {
		return name;
	}
}
