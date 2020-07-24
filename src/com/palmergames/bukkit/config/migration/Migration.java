package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.util.Version;

import java.util.List;

/**
 * Represents a collection of changes.
 */
class Migration {
	Version version;
	List<Change> changes;

	@Override
	public String toString() {
		return "Migration{" +
			"version=" + version +
			", changes=" + changes +
			'}';
	}
}
