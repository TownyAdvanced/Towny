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
            return town.getCommercialPlotTax() + town.getPlotTax();
        }
    },

    ARENA(2, "arena", "A"){	//Always PVP enabled.
    },

    EMBASSY(3, "embassy", "E") {  // For other towns to own a plot in your town.
    	@Override
        public double getTax(Town town) {
            return town.getEmbassyPlotTax() + town.getPlotTax();
        }
    },
    WILDS(4, "wilds", "W"){	//Follows wilderness protection settings, but town owned.
    },
    SPLEEF(5, "spleef", "+"){	//Follows wilderness protection settings, but town owned.
    },
    // These are subject to change:
/*
    PUBLIC(6, "") {  // Will have it's own permission set
    },

    MINE(7, "") {  // Will have it's own permission set within a y range
    },

    HOTEL(8, "") {  // Will stack multiple y-ranges and function like a micro town
    },

    JAIL(9, "") {  // Where people will spawn when they die in enemy (neutral) towns
    },*/
    ;

    private int id;
    private String name, asciiMapKey;
    private static final Map<Integer,TownBlockType> idLookup
          = new HashMap<Integer,TownBlockType>();
    private static final Map<String,TownBlockType> nameLookup
          = new HashMap<String,TownBlockType>();

    TownBlockType(int id, String name, String asciiMapKey) {
        this.id = id;
        this.name = name;
        this.asciiMapKey = asciiMapKey;
    }

    static {
        for(TownBlockType s : EnumSet.allOf(TownBlockType.class)) {
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

    public static TownBlockType lookup(int id) {
        return idLookup.get(id);
    }

    public static TownBlockType lookup(String name) {
        return nameLookup.get(name.toLowerCase());
    }
}