package com.palmergames.bukkit.towny.db;

import java.io.File;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.util.FileMgmt;

public enum DatabaseConfig {
	DATABASE(
			"database",
			"",
			"",
			"# Valid load and save types are: flatfile and mysql."),
	DATABASE_LOAD("database.database_load", "flatfile"),
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
	private String[] comments;

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
	
	public static void loadDatabaseConfig(String filepath) {
		if (FileMgmt.checkOrCreateFile(filepath)) {
			File file = new File(filepath);

			// read the config.yml into memory
			databaseConfig = new CommentedConfiguration(file);
			if (!databaseConfig.load())
				throw new TownyInitException("Database: Failed to load database.yml!", TownyInitException.TownyError.DATABASE_CONFIG);

			setDatabaseDefaults(file);

			databaseConfig.save();
		}
	}
	
	/**
	 * Builds a new database.yml reading old database.yml data.
	 */
	public static void setDatabaseDefaults(File file) {

		newDatabaseConfig = new CommentedConfiguration(file);
		newDatabaseConfig.load();

		for (DatabaseConfig root : DatabaseConfig.values())
			if (root.getComments().length > 0)
				newDatabaseConfig.addComment(root.getRoot(), root.getComments());
			else
				newDatabaseConfig.set(root.getRoot(), (databaseConfig.get(root.getRoot().toLowerCase()) != null) ? databaseConfig.get(root.getRoot().toLowerCase()) : root.getDefault());

		databaseConfig = newDatabaseConfig;
		newDatabaseConfig = null;
	}
	
	public static String getString(DatabaseConfig node) {

		return databaseConfig.getString(node.getRoot().toLowerCase(), node.getDefault());
	}
	
	public static int getInt(DatabaseConfig node) {

		try {
			return Integer.parseInt(databaseConfig.getString(node.getRoot().toLowerCase(), node.getDefault()).trim());
		} catch (NumberFormatException e) {
			Towny.getPlugin().getLogger().severe(node.getRoot().toLowerCase() + " from database.yml");
			return 0;
		}
	}

}