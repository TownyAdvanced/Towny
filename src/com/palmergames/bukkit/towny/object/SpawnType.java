package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;

public enum SpawnType {

	RESIDENT(TownySettings.getLangString("res_sing")),
	TOWN(TownySettings.getLangString("town_sing")),
	NATION(TownySettings.getLangString("nation_sing"));

	private String typeName;
	
	SpawnType(String typeName) {
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return this.typeName;
	}
	
}
