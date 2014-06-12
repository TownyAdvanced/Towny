package com.palmergames.bukkit.towny.object;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

/**
 * @author dumptruckman
 */
public enum TownBlockType {
	RESIDENTIAL(0, "기본", "+") {  // The default Block Type. // 기본 마을블록 타입입니다.
	},

	COMMERCIAL(1, "상점", "C") {  // Just like residential but has additional tax // 기본 마을블록와 같지만, 세금이 더 많이 붙습니다.

		@Override
		public double getTax(Town town) {

			return town.getCommercialPlotTax() + town.getPlotTax();
		}
	},

	ARENA(2, "전장", "A") {	//Always PVP enabled. // 항상 PvP를 할 수 있습니다.
	},

	EMBASSY(3, "대사관", "E") {  // For other towns to own a plot in your town. // 다른 마을에서 이 토지를 소유할 수 있습니다.

		@Override
		public double getTax(Town town) {

			return town.getEmbassyPlotTax() + town.getPlotTax();
		}
	},
	WILDS(4, "야생", "W") {	//Follows wilderness protection settings, but town owned. // 마을에 소속되어 있지만, 야생구역의 설정을 따릅니다.
	},
	SPLEEF(5, "보호구역", "+") {	//Follows wilderness protection settings, but town owned. // 마을에 소속되어 있지만, 야생구역의 설정을 따릅니다.
	},
	INN(6, "여관", "I") {	//Allows use of beds outside your own plot. // 자신의 토지가 아닐 경우에도 침대를 사용할 수 있습니다.
	},

	// These are subject to change:
/*
 * PUBLIC(6, "") { // Will have it's own permission set
 * },
 * 
 * MINE(7, "") { // Will have it's own permission set within a y range
 * },
 * 
 * HOTEL(8, "") { // Will stack multiple y-ranges and function like a micro town
 * },
 * 
 * JAIL(9, "") { // Where people will spawn when they die in enemy (neutral)
 * towns
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