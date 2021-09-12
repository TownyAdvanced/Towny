package com.palmergames.bukkit.towny.war.eventwar;

public enum WarType {
	// TODO: Make all these settings configurable.
	RIOT("Riot", false, WarZoneConfig.riotsMayorDeathEnabled(), false, false, WarZoneConfig.riotResidentLives(), WarZoneConfig.riotMayorLives(), WarZoneConfig.riotBaseSpoils(), WarZoneConfig.riotPointsPerKill()),
	TOWNWAR("Town vs Town War", WarZoneConfig.townWarTownBlockHPEnabled(), WarZoneConfig.townWarMayorDeathEnabled(), false, WarZoneConfig.townWarWinnerTakesOverTown(), WarZoneConfig.townWarResidentLives(), WarZoneConfig.townWarMayorLives(), WarZoneConfig.townWarBaseSpoils(), WarZoneConfig.townWarPointsPerKill()),
	CIVILWAR("National Civil War", WarZoneConfig.civilWarTownBlockHPEnabled(), WarZoneConfig.civilWarMayorDeathEnabled(), false, WarZoneConfig.civilWarWinnerTakesOverNation(), WarZoneConfig.civilWarResidentLives(), WarZoneConfig.civilWarMayorLives(), WarZoneConfig.civilWarBaseSpoils(), WarZoneConfig.civilWarPointsPerKill()),
	NATIONWAR("Nation vs Nation War", WarZoneConfig.nationWarTownBlockHPEnabled(), WarZoneConfig.nationWarMayorDeathEnabled(), false, WarZoneConfig.nationWarWinnerConquersTowns(), WarZoneConfig.nationWarResidentLives(), WarZoneConfig.nationWarMayorLives(), WarZoneConfig.nationWarBaseSpoils(), WarZoneConfig.nationWarPointsPerKill()),
	WORLDWAR("World War", WarZoneConfig.worldWarTownBlockHPEnabled(), WarZoneConfig.worldWarMayorDeathEnabled(), false, WarZoneConfig.worldWarWinnerConquersTowns(), WarZoneConfig.worldWarResidentLives(), WarZoneConfig.worldWarMayorLives(), WarZoneConfig.worldWarBaseSpoils(), WarZoneConfig.worldWarPointsPerKill());
	
	String name;
	public boolean hasTownBlockHP;
	public boolean hasMayorDeath;
	public boolean hasTownBlocksSwitchTowns;
	public boolean hasTownConquering;
	public int residentLives;
	public int mayorLives;
	public int pointsPerKill;
	public double baseSpoils;
	
	/**
	 * 
	 * @param name - Base name used.
	 * @param hasTownBlockHP - Whether townblocks have HP and are fought over for points.
	 * @param hasMayorDeath - Whether killing the mayor or king will remove the town/nation from the war.
	 * @param lives - How many lives each player gets before being removed from the war.
	 * @param baseSpoils - How much money is added to the war.
	 */
	WarType(String name, boolean hasTownBlockHP, boolean hasMayorDeath, boolean hasTownBlocksSwitchTowns, boolean hasTownConquering, int residentLives, int mayorLives, double baseSpoils, int pointsPerKill) {
		this.name = name;
		this.hasTownBlockHP = hasTownBlockHP;
		this.hasMayorDeath = hasMayorDeath;
		this.hasTownBlocksSwitchTowns = hasTownBlocksSwitchTowns;
		this.hasTownConquering = hasTownConquering;
		this.residentLives = residentLives;
		this.mayorLives = mayorLives;
		this.baseSpoils = baseSpoils;
		this.pointsPerKill = pointsPerKill;
	}
	
	public String getName() {
		return name;
	}
}
