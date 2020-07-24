package com.palmergames.bukkit.config.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.Version;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.Objects;

/**
 * An object which manages the procces of migrating towny config versions to 
 * up-to-date ones.
 */
public class ConfigMigrator {
	
	private final String migrationFilename;
	private static final Gson GSON = new GsonBuilder()
		.registerTypeAdapter(Version.class, new VersionDeserializer()).create();
	private final CommentedConfiguration config;
	
	public ConfigMigrator(CommentedConfiguration config, String filename) {
		Objects.requireNonNull(config, filename);
		this.migrationFilename = filename;
		this.config = config;
	}

	/**
	 * Migrates configuration to latest version availibe in the given JSON file.
	 */
	public void migrate() {
		// Use the last run version as a reference.
		Version configVersion = new Version(TownySettings.getLastRunVersion(Towny.getPlugin().getVersion()));
		
		// Go through each migration element.
		for (Migration migration : readMigrator()) {
			// If a migration version is greater than our version, upgrade with it.
			if (configVersion.compareTo(migration.version) < 0) {
				// Perform all desired changes.
				for (Change change : migration.changes) {
					performChange(change);
				}
			}
		}
	}
	
	private void performChange(Change change) {
		String diff;
		switch (change.type) {
			case OVERWRITE:
				diff = change.path;
				break;
			case APPEND:
				String base = config.getString(change.path);
				diff = base + change.value;
				break;
			default:
				throw new UnsupportedOperationException("Unsupported Change type: " + change);
		}

		// Perform change.
		System.out.println("Updating " + change.path + "...");
		config.set(change.path, diff);
		
		// Address any changes to the world files.
		if (change.worldAction != null) {
			for (TownyWorld world : TownyUniverse.getInstance().getWorldMap().values()) {
				change.worldAction.getAction().accept(world,  diff);
			}
		}
	}
	
	private List<Migration> readMigrator() {
		InputStream file = Towny.getPlugin().getResource(migrationFilename);
		
		if (file == null) {
			throw new UnsupportedOperationException(migrationFilename + " was not found cannot upgrade config");
		}
		
		Reader reader = new InputStreamReader(file);

		return GSON.fromJson(reader, new TypeToken<List<Migration>>(){}.getType());
	}
}
