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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * An object which manages the process of migrating towny config versions to 
 * up-to-date ones.
 */
public class ConfigMigrator {
	
	private final String migrationFilename;
	private final Version lastRunVersion;
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(Version.class, new VersionDeserializer()).create();
	private final CommentedConfiguration config;
	private final CommentedConfiguration townyperms;
	private final boolean earlyRun;
	private final Plugin plugin;
	private final RunnableMigrations runnableMigrations = new RunnableMigrations();
	
	public ConfigMigrator(@NotNull CommentedConfiguration config, @NotNull String filename, boolean earlyRun) {
		Objects.requireNonNull(config, "ConfigMigrator: config cannot be null");
		Objects.requireNonNull(filename, "ConfigMigrator: filename cannot be null");
		this.migrationFilename = filename;
		this.config = config;
		this.townyperms = TownyPerms.getTownyPermsFile();
		this.earlyRun = earlyRun;
		this.plugin = Towny.getPlugin();
		this.lastRunVersion = Version.fromString(config.getString(ConfigNodes.LAST_RUN_VERSION.getRoot(), "0.0.0.0"));
	}
	
	/**
	 * A ConfigMigrator constructor which another plugin can use.
	 * 
	 * @param plugin         Plugin object used for Logging and getting your
	 *                       resource folder.
	 * @param config         CommentedConfiguration config file.
	 * @param filename       String filename of your json file, ie:
	 *                       config-migration.json, stored in your resources folder.
	 * @param lastRunVersion Version representing your config's version, from which
	 *                       Towny will determine which config migrations to apply.
	 * @param earlyRun       boolean whether this is an early run, used for
	 *                       gathering config values used in the MOVE and REMOVE
	 *                       MigrationType.
	 */
	public ConfigMigrator(@NotNull Plugin plugin, @NotNull CommentedConfiguration config, @NotNull String filename, @NotNull Version lastRunVersion, boolean earlyRun) {
		Objects.requireNonNull(config, "ConfigMigrator: config cannot be null");
		Objects.requireNonNull(filename, "ConfigMigrator: filename cannot be null");
		Objects.requireNonNull(lastRunVersion, "ConfigMigrator: lastRunVersion cannot be null");
		Objects.requireNonNull(plugin, "ConfigMigrator: plugin cannot be null");
		this.plugin = plugin;
		this.config = config;
		this.migrationFilename = filename;
		this.lastRunVersion = lastRunVersion;
		this.earlyRun = earlyRun;
		this.townyperms = TownyPerms.getTownyPermsFile();
	}

	/**
	 * Migrates configuration to latest version available in the given JSON file.
	 */
	public void migrate() {

		boolean saveTownyperms = false;
		int totalChangeCount = 0;

		// Go through each migration element.
		for (Migration migration : readMigrator()) {
			// If a migration version is greater than our version, upgrade with it.
			if (lastRunVersion.compareTo(migration.version) < 0) {
				// Check if there are any applicable changes based on the early/normal run-order.
				int changeCount = getChangeCount(migration);
				if (changeCount == 0)
					continue;

				plugin.getLogger().info("Config: " + migration.version + " applying " + changeCount + " automatic update" + (changeCount == 1 ? "" : "s") + "...");
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
			case OVERWRITE -> {
				config.set(change.path, change.value);
				TownyMessaging.sendDebugMsg("Reseting config.yml value at " + change.path + " to " + change.value + ".");
			}
			case APPEND -> {
				String base = config.getString(change.path);
				config.set(change.path, base + change.value);
				TownyMessaging.sendDebugMsg("Adding " + change.value + " to config.yml value at " + change.path + ".");
			}
			case TOWN_LEVEL_ADD -> addTownLevelProperty(change.key, change.value);
			case NATION_LEVEL_ADD -> addNationLevelProperty(change.key, change.value);
			case TOWNYPERMS_ADD -> {
				addPermissions(change.path, change.value);
				TownyMessaging.sendDebugMsg("Updating townyperms.yml, adding " + change.value + " to " + change.path + " group.");
			}
			case REPLACE -> {
				Object value = config.get(change.path);
				if (value instanceof String string)
					config.set(change.path, string.replaceAll(change.key, change.value));
			}
			case MOVE -> {
				Object oldValue = config.get(change.path);
				if (oldValue != null)
					config.set(change.value, oldValue);
			}
			case REMOVE -> {
				Object path = config.get(change.path);
				if (path != null) {
					TownyMessaging.sendDebugMsg("Removing unneeded config entry: " + change.path);
					config.set(change.path, null);
				}
			}
			case RUNNABLE -> {
				Consumer<CommentedConfiguration> consumer = runnableMigrations.getByName(change.key);
				if (consumer != null)
					consumer.accept(config);
				else
					plugin.getLogger().warning("Config Migrator: Could not find runnable migration with key " + change.key);
			}
			default -> throw new UnsupportedOperationException("Unsupported Change type: " + change);
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
		try (InputStream is = plugin.getResource(migrationFilename)) {
			if (is == null) {
				plugin.getLogger().warning("Could not find config migrator file '" + migrationFilename + "' in the jar.");
				return Collections.emptyList();
			}
			
			try (Reader reader = new InputStreamReader(is)) {
				return GSON.fromJson(reader, new TypeToken<List<Migration>>(){}.getType());
			}
		} catch (IOException e) {
			return Collections.emptyList();
		}
	}
	
	@SuppressWarnings("unchecked")
	public void addTownLevelProperty(String key, String value) {
		 List<Map<?, ?>> mapList = config.getMapList("levels.town_level");
		 TownyMessaging.sendDebugMsg("Updating town_level with " + key + " set to " + value);
		
		for (Map<?, ?> genericMap : mapList) {
			Map<String, String> map = (Map<String, String>) genericMap;

			if (!map.containsKey(key))
				map.put(key, value);
		}
		 
		 config.set("levels.town_level", mapList);
	}

	@SuppressWarnings("unchecked")
	public void addNationLevelProperty(String key, String value) {
		List<Map<?, ?>> mapList = config.getMapList("levels.nation_level");
		TownyMessaging.sendDebugMsg("Updating nation_level with " + key + " set to " + value);

		for (Map<?, ?> genericMap : mapList) {
			Map<String, String> map = (Map<String, String>) genericMap;
			
			if (!map.containsKey(key))
				map.put(key, value);
		}

		config.set("levels.nation_level", mapList);
	}

	public RunnableMigrations getRunnableMigrations() {
		return runnableMigrations;
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
