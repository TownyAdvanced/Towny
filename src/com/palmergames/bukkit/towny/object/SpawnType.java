package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;

public enum SpawnType {

	RESIDENT(TownySettings.getLangString("res_sing"), PermissionNodes.TOWNY_COMMAND_TOWNYADMIN.getNode()),
	TOWN(TownySettings.getLangString("town_sing"), PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_OTHER.getNode()),
	NATION(TownySettings.getLangString("nation_sing"), PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_NATION_SPAWN_OTHER.getNode());

	private String typeName;
	private String node;
	
	SpawnType(String typeName, String node) {
		this.typeName = typeName;
		this.node = node;
	}
	
	public String getTypeName() {
		return this.typeName;
	}
	
	public String getNode() {
		return this.node;
	}
	
}
