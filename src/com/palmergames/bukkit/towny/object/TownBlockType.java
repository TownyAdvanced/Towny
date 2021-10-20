package com.palmergames.bukkit.towny.object;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.palmergames.bukkit.towny.TownySettings;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dumptruckman
 */
public enum TownBlockType {
	RESIDENTIAL(0, "Default", "+"), // The default Block Type.
	COMMERCIAL(1, "Shop", "C") { // Just like residential but has additional tax

		@Override
		public double getTax(Town town) {

			return town.getCommercialPlotTax() + town.getPlotTax();
		}
	},
	ARENA(2, "Arena", "A"), //Always PVP enabled.
	EMBASSY(3, "Embassy", "E") {  // For other towns to own a plot in your town.

		@Override
		public double getTax(Town town) {

			return town.getEmbassyPlotTax() + town.getPlotTax();
		}
	},
	WILDS(4, "Wilds", "W"), //Follows wilderness protection settings, but town owned.
	INN(6, "Inn", "I"), //Allows use of beds outside your own plot.
	JAIL(7, "Jail", "J"), //Enables setting the jail spawn.		
	FARM(8, "Farm", "F"), //Follows wilderness protection settings, but town owned.
	BANK(9, "Bank", "B"), // Enables depositing into town and nation banks, if that has been enabled in the config.
	CUSTOM(10, "Custom", "C");

	private final int id;
	private final String name, asciiMapKey;
	private static final Map<Integer, TownBlockType> idLookup = new HashMap<>();
	private static final Map<String, TownBlockType> nameLookup = new HashMap<>();

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

	@Deprecated
	public double getTax(Town town) {

		return town.getPlotTax();
	}

	@Deprecated
	public int getId() {

		return id;
	}

	public String getAsciiMapKey() {
		TownBlockData data = TownBlockTypeHandler.getData(this.name);
		
		return data == null ? asciiMapKey : data.getMapKey();
	}
	
	public double getCost() {
		TownBlockData data = TownBlockTypeHandler.getData(this.name);

		return data == null ? 0.0 : data.getCost();
	}

	public String getName() {
		return name;
	}

	@Nullable
	@Deprecated
	public static TownBlockType lookup(int id) {
		return idLookup.get(id);
	}

	@Nullable
	public static TownBlockType lookup(@NotNull String name) {
		TownBlockType type = nameLookup.get(name.toLowerCase());
		
		if (type == null && TownBlockTypeHandler.exists(name)) // Type exists but it's not one of ours.
			type = TownBlockType.CUSTOM;

		return type;
	}
	
	public boolean equals(String type) {
		return type != null && this == lookup(type);
	}
}