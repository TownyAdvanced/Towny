package com.palmergames.bukkit.towny.object;

import com.palmergames.util.StringMgmt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dumptruckman
 */
public class TownBlockType {
	public static final TownBlockType RESIDENTIAL = new TownBlockType("Default"); // The default Block Type.
	public static final TownBlockType COMMERCIAL = new TownBlockType("Shop", new TownBlockData() {
		@Override
		public double getTax(Town town) {
			return town.getCommercialPlotTax() + town.getPlotTax();
		}
	}); // Just like residential but has additional tax
	public static final TownBlockType ARENA = new TownBlockType("Arena"); //Always PVP enabled.
	public static final TownBlockType EMBASSY = new TownBlockType("Embassy", new TownBlockData() {
		@Override
		public double getTax(Town town) {
			return town.getEmbassyPlotTax() + town.getPlotTax();
		}
	}); // For other towns to own a plot in your town.
	public static final TownBlockType WILDS = new TownBlockType("Wilds"); //Follows wilderness protection settings, but town owned.
	public static final TownBlockType INN = new TownBlockType("Inn"); //Allows use of beds outside your own plot.
	public static final TownBlockType JAIL = new TownBlockType("Jail"); //Enables setting the jail spawn.		
	public static final TownBlockType FARM = new TownBlockType("Farm"); //Follows wilderness protection settings, but town owned.
	public static final TownBlockType BANK = new TownBlockType("Bank"); // Enables depositing into town and nation banks, if that has been enabled in the config.
	
	private final String name;
	private final TownBlockData data;

	public TownBlockType(String name, TownBlockData data) {
		this.name = name;
		this.data = data;
	}
	
	public TownBlockType(String name) {
		this.name = name;
		this.data = new TownBlockData();
	}

	@Override
	public String toString() {
		return name;
	}

	public double getTax(Town town) {
		return data.getTax(town);
	}

	@Deprecated
	public int getId() {
		return 0;
	}

	public String getAsciiMapKey() {
		return data.getMapKey();
	}
	
	public double getCost() {
		return data.getCost();
	}

	public String getName() {
		return name;
	}
	
	public String getFormattedName() {
		return StringMgmt.capitalize(this.name);
	}
	
	private static final Map<Integer, String> legacyLookupMap = new HashMap<>();
	
	static {
		legacyLookupMap.put(0, "default");
		legacyLookupMap.put(1, "shop");
		legacyLookupMap.put(2, "arena");
		legacyLookupMap.put(3, "embassy");
		legacyLookupMap.put(4, "wilds");
		legacyLookupMap.put(6, "inn");
		legacyLookupMap.put(7, "jail");
		legacyLookupMap.put(8, "farm");
		legacyLookupMap.put(9, "bank");
	}

	/**
	 * @deprecated As of 0.97.5.0, please use {@link TownBlockTypeHandler#getType(String)} instead.
	 */
	@Nullable
	@Deprecated
	public static TownBlockType lookup(int id) {
		return TownBlockTypeHandler.getType(legacyLookupMap.getOrDefault(id, "default"));
	}

	/**
	 * @deprecated As of 0.97.5.0, please use {@link TownBlockTypeHandler#getType(String)} instead.
	 */
	@Nullable
	@Deprecated
	public static TownBlockType lookup(@NotNull String name) {
		return TownBlockTypeHandler.getType(name);
	}
	
	public TownBlockData getData() {
		return data;
	}

	@Override
	public boolean equals(Object other) {
		if (this == other)
			return true;
		
		if (!(other instanceof TownBlockType townBlockType))
			return false;
		
		return townBlockType.getName().equalsIgnoreCase(this.name);
	}
}