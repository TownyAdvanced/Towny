package com.palmergames.bukkit.towny.object;

import org.jetbrains.annotations.NotNull;

public enum SpawnType {

	RESIDENT(Translatable.of("res_sing")),
	TOWN(Translatable.of("town_sing")),
	NATION(Translatable.of("nation_sing"));

	private final Translatable typeName;
	
	SpawnType(Translatable typeName) {
		this.typeName = typeName;
	}
	
	@NotNull
	public String getTypeName() {
		return this.typeName.translate();
	}
	
	@NotNull
	public Translatable typeName() {
		return this.typeName;
	}	
}
