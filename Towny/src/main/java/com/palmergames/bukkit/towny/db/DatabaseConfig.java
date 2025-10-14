package com.palmergames.bukkit.towny.db;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.util.FileMgmt;

import java.nio.file.Path;
import java.util.Locale;

public enum DatabaseConfig {
	DATABASE(
			"database", "", ""),
	DATEBASE_VERSION("database.version", "2", "",
			"# The Database version number. Do not change."),
	DATABASE_LOAD("database.database_load", "flatfile", "",
			"# Valid load and save types are: flatfile and mysql."),
	DATABASE_SAVE("database.database_save", "flatfile"),
	DATABASE_SQL_HEADER(
			"database.sql",
			"",
			"",
			"# SQL database connection details (IF set to use mysql)."),
	DATABASE_HOSTNAME("database.sql.hostname", "localhost"),
	DATABASE_PORT("database.sql.port", "3306"),
	DATABASE_DBNAME("database.sql.dbname", "towny"),
	DATABASE_TABLEPREFIX("database.sql.table_prefix", "towny_"),
	DATABASE_USERNAME("database.sql.username", "root"),
	DATABASE_PASSWORD("database.sql.password", ""),
	DATABASE_FLAGS("database.sql.flags", "?verifyServerCertificate=false&useSSL=false&useUnicode=true&characterEncoding=utf-8"),
	DATABASE_SQL_DISABLE_BACKUP_WARNING("database.sql.disable_backup_warning", 
			"false",
			"",
			"# Disables the warning when a backup is made about not all Towny data being backed up when mysql is in use.",
			"# Set this to true when you fully understand what not having flatfile backups means, if your mysql database were to become unusable and you have not configured your own backup solution."),


	DATABASE_POOLING_HEADER(
		"database.sql.pooling",
		"",
		"",
		"# Modifiable settings to control the connection pooling.",
		"# Unless you actually know what you're doing and how Towny uses its mysql connection,",
		"# it is strongly recommended you do not change these settings."),
	DATABASE_POOLING_MAX_POOL_SIZE("database.sql.pooling.max_pool_size", "5"),
	DATABASE_POOLING_MAX_LIFETIME("database.sql.pooling.max_lifetime", "180000"),
	DATABASE_POOLING_CONNECTION_TIMEOUT("database.sql.pooling.connection_timeout", "5000");

	private final String Root;
	private final String Default;
	private final String[] comments;

	DatabaseConfig(String root, String def, String... comments) {

		this.Root = root;
		this.Default = def;
		this.comments = comments;
	}
	
	private static CommentedConfiguration databaseConfig, newDatabaseConfig;


	/**
	 * Retrieves the root for a config option
	 *
	 * @return The root for a config option
	 */
	public String getRoot() {

		return Root;
	}

	/**
	 * Retrieves the default value for a config path
	 *
	 * @return The default value for a config path
	 */
	public String getDefault() {

		return Default;
	}

	/**
	 * Retrieves the comment for a config path
	 *
	 * @return The comments for a config path
	 */
	public String[] getComments() {

		if (comments != null) {
			return comments;
		}

		String[] comments = new String[1];
		comments[0] = "";
		return comments;
	}
	
	/**
	 * Loads the database.yml file into memory.
	 * @param databaseConfigPath Path to database.yml.
	 */
	public static void loadDatabaseConfig(Path databaseConfigPath) {
		if (!FileMgmt.checkOrCreateFile(databaseConfigPath.toString())) {
			throw new TownyInitException("Failed to touch '" + databaseConfigPath + "'.", TownyInitException.TownyError.DATABASE_CONFIG);
		}
		// read the database.yml into memory
		databaseConfig = new CommentedConfiguration(databaseConfigPath);
		if (!databaseConfig.load())
			throw new TownyInitException("Database: Failed to load database.yml!", TownyInitException.TownyError.DATABASE_CONFIG);

		setDatabaseDefaults(databaseConfigPath);

		databaseConfig.save();
	}
	
	/**
	 * Builds a new database.yml reading old database.yml data.
	 */
	public static void setDatabaseDefaults(Path databaseConfigPath) {
		newDatabaseConfig = new CommentedConfiguration(databaseConfigPath);
		newDatabaseConfig.load();

		for (DatabaseConfig root : DatabaseConfig.values()) {
			String key = root.getRoot().toLowerCase(Locale.ROOT);
			if (root.getComments().length > 0)
				newDatabaseConfig.addComment(key, root.getComments());
			Object value = databaseConfig.get(key) != null
				? databaseConfig.get(key)
				: root.getDefault();
			newDatabaseConfig.set(key, value);
		}
		databaseConfig = newDatabaseConfig;
		newDatabaseConfig = null;
	}
	
	public static void setDatabaseVersion(int version) {
		databaseConfig.set("database.version", version);
		databaseConfig.save();
	}
	
	public static int getLatestDatabaseVersion() {
		return Integer.parseInt(DatabaseConfig.DATEBASE_VERSION.getDefault());
	}
	
	public static String getString(DatabaseConfig node) {

		return databaseConfig.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault());
	}
	
	public static int getInt(DatabaseConfig node) {

		try {
			return Integer.parseInt(databaseConfig.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			Towny.getPlugin().getLogger().severe(node.getRoot().toLowerCase(Locale.ROOT) + " from database.yml");
			return 0;
		}
	}

	public static boolean getBoolean(DatabaseConfig node) {
		return Boolean.parseBoolean(databaseConfig.getString(node.getRoot().toLowerCase(Locale.ROOT), node.getDefault()));
	}
}