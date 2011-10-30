package com.palmergames.bukkit.towny.object;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dumptruckman
 */
public enum TownBlockType {
    RESIDENTIAL(0, "default") {  // The default Block Type.
    },

    COMMERCIAL(1, "Shop") {  // Just like residential but has additional tax
        @Override
        public double getTax(Town town) {
            return town.getCommercialPlotTax() + town.getPlotTax();
        }
    },

    ARENA(2, "arena"){	//Always PVP enabled.
    },

    EMBASSY(3, "embassy") {  // For other towns to own a plot in your town.
    	@Override
        public double getTax(Town town) {
            return town.getEmbassyPlotTax() + town.getPlotTax();
        }
    },
    WILDS(4, "wilds"){	//Follows wilderness protection settings, but town owned.
    },
    // These are subject to change:
/*
    PUBLIC(5, "") {  // Will have it's own permission set
    },

    MINE(6, "") {  // Will have it's own permission set within a y range
    },

    HOTEL(7, "") {  // Will stack multiple y-ranges and function like a micro town
    },

    JAIL(8, "") {  // Where people will spawn when they die in enemy (neutral) towns
    },*/
    ;

    private int id;
    private String name;
    private static final Map<Integer,TownBlockType> idLookup
          = new HashMap<Integer,TownBlockType>();
    private static final Map<String,TownBlockType> nameLookup
          = new HashMap<String,TownBlockType>();

    TownBlockType(int id, String name) {
        this.id = id;
        this.name = name;
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

    public static TownBlockType lookup(int id) {
        return idLookup.get(id);
    }

    public static TownBlockType lookup(String name) {
        return nameLookup.get(name.toLowerCase());
    }
}