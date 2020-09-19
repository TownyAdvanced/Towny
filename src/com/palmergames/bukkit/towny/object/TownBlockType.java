package com.palmergames.bukkit.towny.object;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dumptruckman
 */
public enum TownBlockType {
	RESIDENTIAL(0, "default", "+") {  // The default Block Type.
	},

	COMMERCIAL(1, "Shop", "C") {  // Just like residential but has additional tax

		@Override
		public double getTax(Town town) {

			return town.getTaxCollector().getCommercialPlotTax() + town.getTaxCollector().getPlotTax();
		}
	},

	ARENA(2, "Arena", "A") {	//Always PVP enabled.
	},

	EMBASSY(3, "Embassy", "E") {  // For other towns to own a plot in your town.

		@Override
		public double getTax(Town town) {

			return town.getTaxCollector().getEmbassyPlotTax() + town.getTaxCollector().getPlotTax();
		}
	},
	WILDS(4, "Wilds", "W") {	//Follows wilderness protection settings, but town owned.
	},
	SPLEEF(5, "Spleef", "+") {	//Follows wilderness protection settings, but town owned.
	},
	INN(6, "Inn", "I") {	//Allows use of beds outside your own plot.
	},
	JAIL(7, "Jail", "J") {	//Enables setting the jail spawn.		
	},
	FARM(8, "Farm", "F") {	//Follows wilderness protection settings, but town owned.
	},
	BANK(9, "Bank", "B") { // Enables depositing into town and nation banks, if that has been enabled in the config.		
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
	private static final Map<Integer, TownBlockType> idLookup = new HashMap<Integer, TownBlockType>();
	private static final Map<String, TownBlockType> nameLookup = new HashMap<String, TownBlockType>();

	TownBlockType(int id, String name, String asciiMapKey) {

		this.id = id;
		this.name = name;
		this.asciiMapKey = asciiMapKey;
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

		return town.getTaxCollector().getPlotTax();
	}

	public int getId() {

		return id;
	}

	public String getAsciiMapKey() {

		return asciiMapKey;
	}

	public static TownBlockType lookup(int id) {

		return idLookup.get(id);
	}

	public static TownBlockType lookup(String name) {

		return nameLookup.get(name.toLowerCase());
	}
}