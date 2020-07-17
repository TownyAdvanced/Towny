package com.palmergames.bukkit.towny.object;

public enum SpawnType {

	RESIDENT(Translation.of("res_sing")),
	TOWN(Translation.of("town_sing")),
	NATION(Translation.of("nation_sing"));

	private String typeName;
	
	SpawnType(String typeName) {
		this.typeName = typeName;
	}
	
	public String getTypeName() {
		return this.typeName;
	}
	
}
