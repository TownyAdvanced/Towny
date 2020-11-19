package com.palmergames.bukkit.towny.object;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.palmergames.bukkit.towny.TownySettings;

/**
 * @author dumptruckman
 */
public enum TownBlockType {
	RESIDENTIAL(0, "default", "+", 0.0) {  // The default Block Type.
	},

	COMMERCIAL(1, "Shop", "C", TownySettings.getPlotSetCommercialCost()) {  // Just like residential but has additional tax

		@Override
		public double getTax(Town town) {

			return town.getCommercialPlotTax() + town.getPlotTax();
		}
	},

	ARENA(2, "Arena", "A", TownySettings.getPlotSetArenaCost()) {	//Always PVP enabled.
	},

	EMBASSY(3, "Embassy", "E", TownySettings.getPlotSetEmbassyCost()) {  // For other towns to own a plot in your town.

		@Override
		public double getTax(Town town) {

			return town.getEmbassyPlotTax() + town.getPlotTax();
		}
	},
	WILDS(4, "Wilds", "W", TownySettings.getPlotSetWildsCost()) {	//Follows wilderness protection settings, but town owned.
	},
	SPLEEF(5, "Spleef", "+", 0.0) {	//Follows wilderness protection settings, but town owned.
	},
	INN(6, "Inn", "I", TownySettings.getPlotSetInnCost()) {	//Allows use of beds outside your own plot.
	},
	JAIL(7, "Jail", "J", TownySettings.getPlotSetInnCost()) {	//Enables setting the jail spawn.		
	},
	FARM(8, "Farm", "F", TownySettings.getPlotSetInnCost()) {	//Follows wilderness protection settings, but town owned.
	},
	BANK(9, "Bank", "B", TownySettings.getPlotSetBankCost()) { // Enables depositing into town and nation banks, if that has been enabled in the config.		
	}

	// These are subject to change, and may not necessarily be added:
/*
 * PUBLIC(10, "") { // Will have it's own permission set
 * },
 * 
 * MINE(11, "") { // Will have it's own permission set within a y range
 * },
 * 
 * HOTEL(12, "") { // Will stack multiple y-ranges and function like a micro town
 * },
 */
	;

	private int id;
	private String name, asciiMapKey;
	private double cost;
	private static final Map<Integer, TownBlockType> idLookup = new HashMap<Integer, TownBlockType>();
	private static final Map<String, TownBlockType> nameLookup = new HashMap<String, TownBlockType>();

	TownBlockType(int id, String name, String asciiMapKey, double cost) {

		this.id = id;
		this.name = name;
		this.asciiMapKey = asciiMapKey;
		this.cost = cost;
	}

	static {
		for (TownBlockType s : EnumSet.allOf(TownBlockType.class)) {
			idLookup.put(s.getId(), s);
			nameLookup.put(s.toString().toLowerCase(), s);
		}
	}

	@Override
	public String toString() {

		return name;
	}

	public double getTax(Town town) {

		return town.getPlotTax();
	}

	public int getId() {

		return id;
	}

	public String getAsciiMapKey() {

		return asciiMapKey;
	}
	
	public double getCost() {
		
		return cost;
	}

	public static TownBlockType lookup(int id) {

		return idLookup.get(id);
	}

	public static TownBlockType lookup(String name) {

		return nameLookup.get(name.toLowerCase());
	}
}