package com.palmergames.bukkit.config.migration;

/**
 * Represents a primitive change.
 */
class Change {
	MigrationType type;
	String path;
	String key;
	String value;
	WorldMigrationAction worldAction;
	

	@Override
	public String toString() {
		return "Change{" +
			"type=" + type +
			", key=" + path +
			", value='" + value + 
			", worldAction='" + worldAction + '\'' +
			'}';
	}
}
