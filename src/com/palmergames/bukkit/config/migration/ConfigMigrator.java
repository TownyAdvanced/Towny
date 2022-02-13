package com.palmergames.bukkit.config.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.util.Version;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An object which manages the process of migrating towny config versions to 
 * up-to-date ones.
 */
public class ConfigMigrator {
	
	private final String migrationFilename;
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(Version.class, new VersionDeserializer()).create();
	private final CommentedConfiguration config;
	private final CommentedConfiguration townyperms;
	private final boolean earlyRun;
	
	public ConfigMigrator(CommentedConfiguration config, String filename, boolean earlyRun) {
		Objects.requireNonNull(config, filename);
		this.migrationFilename = filename;
		this.config = config;
		this.townyperms = TownyPerms.getTownyPermsFile();
		this.earlyRun = earlyRun;
	}

	/**
	 * Migrates configuration to latest version available in the given JSON file.
	 */
	public void migrate() {
		// Use the last run version as a reference.
		Version configVersion = Version.fromString(config.getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), "0.0.0.0"));
		boolean saveTownyperms = false;
		int totalChangeCount = 0;

		// Go through each migration element.
		for (Migration migration : readMigrator()) {
			// If a migration version is greater than our version, upgrade with it.
			if (configVersion.compareTo(migration.version) < 0) {
				// Check if there are any applicable changes based on the early/normal run-order.
				int changeCount = getChangeCount(migration);
				if (changeCount == 0)
					continue;

				Towny.getPlugin().getLogger().info("Config: " + migration.version + " applying " + changeCount + " automatic update" + (changeCount == 1 ? "" : "s") + " ...");
				for (Change change : migration.changes) {
					// Only perform earlyRun changes on earlyRun-typed Migrations and vice versa.
					if (change.type.early != earlyRun)
						continue;

					performChange(change);
					totalChangeCount++;
					if (change.type == MigrationType.TOWNYPERMS_ADD)
						saveTownyperms = true;
				}
			}
		}
		if (totalChangeCount > 0) {
			config.save();
			if (saveTownyperms)
				townyperms.save();
		}
	}
	
	private int getChangeCount(Migration migration) {
		int i = 0;
		for (Change change : migration.changes) {
			if (change.type.early != earlyRun)
				continue;
			i++;
		}
		return i;
	}

	private void performChange(Change change) {
		switch (change.type) {
			case OVERWRITE:
				config.set(change.path, change.value);
				TownyMessaging.sendDebugMsg("Reseting config.yml value at " + change.path + " to " + change.value + ".");
				break;
			case APPEND:
				String base = config.getString(change.path);
				config.set(change.path, base + change.value);
				TownyMessaging.sendDebugMsg("Adding " + change.value + " to config.yml value at " + change.path + ".");
				break;
			case TOWN_LEVEL_ADD:
				addTownLevelProperty(change.key, change.value);
				break;
			case NATION_LEVEL_ADD:
				addNationLevelProperty(change.key, change.value);
				break;
			case TOWNYPERMS_ADD:
				addPermissions(change.path, change.value);
				TownyMessaging.sendDebugMsg("Updating townyperms.yml, adding " + change.value + " to " + change.path + " group.");
				break;
			case REPLACE:
				Object value = config.get(change.path);
				if (value instanceof String string)
					config.set(change.path, string.replaceAll(change.key, change.value));
				break;
			case MOVE:
				Object oldValue = config.get(change.path);
				if (oldValue != null)
					config.set(change.value, oldValue);
				break;
			default:
				throw new UnsupportedOperationException("Unsupported Change type: " + change);
		}

		// Address any changes to the world files.
		if (change.worldAction != null) {
			for (TownyWorld world : TownyUniverse.getInstance().getTownyWorlds()) {
				TownyMessaging.sendDebugMsg("Updating " + world.getName() + " with " + change.value);
				change.worldAction.getAction().accept(world, change);
				world.save();
			}
		}
	}
	
	private void addPermissions(String key, String value) {
		List<String> groupNodes = TownyPerms.getPermsOfGroup(key);
		if (groupNodes.contains(value))
			return;
		groupNodes.add(value);
		townyperms.set(key, groupNodes);
	}

	private List<Migration> readMigrator() {
		try (Reader reader = new InputStreamReader(Towny.getPlugin().getResource(migrationFilename))) {
			
			return GSON.fromJson(reader, new TypeToken<List<Migration>>(){}.getType());
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}
	
	public void addTownLevelProperty(String key, String value) {
		 List<Map<?, ?>> mapList = config.getMapList("levels.town_level");
		 TownyMessaging.sendDebugMsg("Updating town_level with " + key + " set to " + value);
		
		 for (Map<?, ?> map : mapList) {
			 ((Map<String, String>)map).put(key, value);
		 }
		 
		 config.set("levels.town_level", mapList);
	}

	public void addNationLevelProperty(String key, String value) {
		List<Map<?, ?>> mapList = config.getMapList("levels.nation_level");
		TownyMessaging.sendDebugMsg("Updating nation_level with " + key + " set to " + value);

		for (Map<?, ?> map : mapList) {
			((Map<String, String>)map).put(key, value);
		}

		config.set("levels.nation_level", mapList);
	}

	/**
	 * Represents a collection of changes.
	 */
	static final class Migration {
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
}
