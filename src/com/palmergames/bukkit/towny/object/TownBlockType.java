package com.palmergames.bukkit.towny.object;

import java.util.HashMap;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @author dumptruckman
 */
public class TownBlockType {
	public static final TownBlockType RESIDENTIAL = new TownBlockType("Default"); // The default Block Type.
	public static final TownBlockType COMMERCIAL = new TownBlockType("Shop"); // Just like residential but has additional tax
	public static final TownBlockType ARENA = new TownBlockType("Arena"); //Always PVP enabled.
	public static final TownBlockType EMBASSY = new TownBlockType("Embassy"); // For other towns to own a plot in your town.
	public static final TownBlockType WILDS = new TownBlockType("Wilds"); //Follows wilderness protection settings, but town owned.
	public static final TownBlockType INN = new TownBlockType("Inn"); //Allows use of beds outside your own plot.
	public static final TownBlockType JAIL = new TownBlockType("Jail"); //Enables setting the jail spawn.		
	public static final TownBlockType FARM = new TownBlockType("Farm"); //Follows wilderness protection settings, but town owned.
	public static final TownBlockType BANK = new TownBlockType("Bank"); // Enables depositing into town and nation banks, if that has been enabled in the config.
	
	private final String name;
	private final TownBlockData data;
	private static final Map<Integer, TownBlockType> idLookup = new HashMap<>();
	private static final Map<String, TownBlockType> nameLookup = new HashMap<>();

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

	@Deprecated
	public double getTax(Town town) {

		return town.getPlotTax();
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

	@Nullable
	@Deprecated
	public static TownBlockType lookup(int id) {
		return idLookup.get(id);
	}

	@Nullable
	public static TownBlockType lookup(@NotNull String name) {
		TownBlockType type = nameLookup.get(name.toLowerCase());

		return type;
	}
	
	public TownBlockData getData() {
		return data;
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TownBlockType townBlockType))
			return false;
		
		return townBlockType.getName().equalsIgnoreCase(this.name);
	}
}