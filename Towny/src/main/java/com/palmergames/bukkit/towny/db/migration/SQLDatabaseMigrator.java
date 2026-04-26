package com.palmergames.bukkit.towny.db.migration;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.DatabaseConfig;
import com.palmergames.bukkit.towny.db.SQLSchema;
import com.palmergames.bukkit.towny.db.TownySQLSource;
import com.palmergames.util.FileMgmt;
import org.jetbrains.annotations.ApiStatus;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@ApiStatus.Internal
public class SQLDatabaseMigrator {
	private final Towny plugin;
	
	public SQLDatabaseMigrator(final Towny plugin) {
		this.plugin = plugin;
	}
	
	public void migrateIfNeeded() {
		final int latestVersion = DatabaseConfig.getLatestDatabaseVersion();
		final int currentVersion = TownySettings.getDatabaseVersion();
		
		if (currentVersion >= latestVersion) {
			return;
		}

		if (!(TownyUniverse.getInstance().getDataSource() instanceof TownySQLSource sqlSource)) {
			// Update version but don't do anything else.
			DatabaseConfig.setDatabaseVersion(latestVersion);
			return;
		}
		
		if (!sqlSource.isReady()) {
			plugin.getSLF4JLogger().warn("Database: Aborting version upgrade due to the database being unreachable");
			return;
		}
		
		plugin.getSLF4JLogger().info("Database: Updating from v{} --> v{}", currentVersion, latestVersion);
		
		// Open up the jar to find all relevant migration files
		final List<String> migrationFiles = fetchMigrationFileNames(currentVersion);
		
		if (migrationFiles.isEmpty()) {
			plugin.getSLF4JLogger().error("Could not find any database migration files in the plugin jar for v{} --> {}", currentVersion, latestVersion);
			return;
		}
		
		for (final String migrationFile : migrationFiles) {
			String fileContents;
			try (final InputStream is = plugin.getClass().getResourceAsStream("/database_migration/" + migrationFile)) {

				if (is == null) {
					throw new FileNotFoundException("Could not find the expected /database_migration/" + migrationFile + " file in the plugin jar.");
				}

				// file integrity checks are possible but not necessary, if someone can access the Towny jar on your server they can already do worse things than DROPping your Towny database.
				fileContents = new String(is.readAllBytes(), StandardCharsets.UTF_8);
			} catch (IOException e) {
				plugin.getSLF4JLogger().error("An exception occurred when reading database migration file {} from the plugin jar", migrationFile, e);
				return;
			}
			
			// Replace placeholders/comments
			fileContents = fileContents
				.replaceAll("\\$PREFIX(_|)", SQLSchema.TABLE_PREFIX);
			
			final Pattern singleLineCommentPattern = Pattern.compile("--.*$", Pattern.MULTILINE);

			TownyMessaging.sendDebugMsg("Database: Executing migration file " + migrationFile);

			try (Connection connection = sqlSource.getConnection(); Statement statement = connection.createStatement()) {
				boolean allowFailure = false;

				for (String command : fileContents.split(";")) {
					command = command.trim();
					if (command.isEmpty()) {
						continue;
					}

					final Matcher commentMatcher = singleLineCommentPattern.matcher(command);
					while (commentMatcher.find()) {
						final String match = commentMatcher.group();

						final String commentContent = match.substring("--".length()).trim();
						if (commentContent.startsWith("fail-off")) {
							allowFailure = true;
						} else if (commentContent.startsWith("fail-on")) {
							allowFailure = false;
						}

						command = command.replace(match, "");
					}

					// Trim & check again after removing comments
					command = command.trim();
					if (command.isEmpty()) {
						continue;
					}

					TownyMessaging.sendDebugMsg("Executing " + command);
					try {
						statement.execute(command);
					} catch (SQLException e) {
						if (!allowFailure) {
							plugin.getSLF4JLogger().error("An exception occurred while executing SQL query '{}' during database migration {}", command, migrationFile, e);
							return;
						}

						TownyMessaging.sendDebugMsg(command + " failed with an exception, but it was allowed.");
					}
				}
			} catch (SQLException e) {
				plugin.getSLF4JLogger().error("An exception occurred while executing database migration {}", migrationFile, e);
				return;
			}
		}

		plugin.getSLF4JLogger().info("Database: Successfully performed {} migration{}", migrationFiles.size(), migrationFiles.size() == 1 ? "" : "s");
		DatabaseConfig.setDatabaseVersion(latestVersion);
	}
	
	private List<String> fetchMigrationFileNames(int currentVersion) {
		final URL root = plugin.getClass().getResource("");
		if (root == null) {
			return List.of();
		}

		try (final FileSystem fs = FileSystems.newFileSystem(root.toURI(), Collections.emptyMap()); Stream<Path> stream = Files.list(fs.getRootDirectories().iterator().next().resolve("/database_migration"))) {
			return stream.filter(path -> path.getFileName().toString().endsWith(".sql"))
				.filter(path -> {
					try {
						final int migrationVersion = Integer.parseInt(FileMgmt.getFileName(path));
						return migrationVersion > currentVersion; // only return migrations greater than the current version
					} catch (NumberFormatException e) {
						return false;
					}
				})
				.map(path -> path.getFileName().toString())
				.toList();
		} catch (IOException | URISyntaxException e) {
			plugin.getSLF4JLogger().warn("Failed to find migration files inside the jar", e);
			return List.of();
		}
	}
}
