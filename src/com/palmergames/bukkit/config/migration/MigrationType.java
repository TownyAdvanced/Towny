package com.palmergames.bukkit.config.migration;

public enum MigrationType {
	OVERWRITE(false),
	APPEND(false),
	NATION_LEVEL_ADD(false),
	TOWN_LEVEL_ADD(false),
	TOWNYPERMS_ADD(false),
	REPLACE(false),
	REMOVE(true),
	MOVE(true);
	
	public boolean early;
	MigrationType(boolean early) {
		this.early = early;
	}
}
