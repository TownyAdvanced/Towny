package com.palmergames.bukkit.config.migration;

public enum MigrationType {
	OVERWRITE(false),
	APPEND(false),
	NATION_LEVEL_ADD(false),
	TOWN_LEVEL_ADD(false),
	TOWNYPERMS_ADD(false),
	FARMBLOCK_ADD(false),
	REPLACE(false),
	REMOVE(true),
	MOVE(true),
	RUNNABLE(true);
	
	public final boolean early;
	
	MigrationType(boolean early) {
		this.early = early;
	}

	public boolean isEarly() {
		return early;
	}
}
