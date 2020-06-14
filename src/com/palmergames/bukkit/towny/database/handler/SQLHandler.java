package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class SQLHandler {
	
	@FunctionalInterface
	private interface GenericResultSetFunction<T> {
		T accept(ResultSet rs) throws SQLException;
	}
	
	@FunctionalInterface
	public interface ResultSetConsumer {
		void accept(ResultSet rs) throws SQLException;
	}
	
	private String username, password;
	private String databaseType;
	private String dbName;
	private String tablePrefix = TownySettings.getSQLTablePrefix().toUpperCase();;
	private String connectionURL;
	private Connection con;
	
	public SQLHandler(String databaseType) {
		String dataFolderPath = TownyUniverse.getInstance().getRootFolder() + File.separator + "data";
		
		this.databaseType = databaseType;
		
		String hostname = TownySettings.getSQLHostName();
		String port = TownySettings.getSQLPort();
		dbName = TownySettings.getSQLDBName();

		String driver1;
		if (databaseType.equalsIgnoreCase("h2")) {
			driver1 = "org.h2.Driver";
			this.connectionURL = ("jdbc:h2:" + dataFolderPath + File.separator + dbName + ".h2db;AUTO_RECONNECT=TRUE");
			username = "sa";
			password = "sa";

		} else if (databaseType.equalsIgnoreCase("mysql")) {
			driver1 = "com.mysql.jdbc.Driver";

			this.connectionURL = "jdbc:mysql://" + hostname + ":" + port + "/" + dbName + "?"
				+ (!TownySettings.getSQLUsingSSL() ?  "verifyServerCertificate=false&useSSL=false&" : "")
				+ "useUnicode=true&characterEncoding=utf-8";

			username = TownySettings.getSQLUsername();
			password = TownySettings.getSQLPassword();

		} else {
			driver1 = "org.sqlite.JDBC";
			this.connectionURL = ("jdbc:sqlite:" + dataFolderPath + File.separator + dbName + ".db");
			username = null;
			password = null;
		}

		/*
		 * Register the driver (if possible)
		 */
		try {
			Driver driver = (Driver) Class.forName(driver1).newInstance();
			DriverManager.registerDriver(driver);
		} catch (Exception e) {
			System.out.println("[Towny] Driver error: " + e);
		}
			
	}
	
	public boolean testConnection() {
		if (getContext()) {
			TownyMessaging.sendDebugMsg("[Towny] Connected to Database");
			return true;
		} else {
			TownyMessaging.sendErrorMsg("Failed when connecting to Database");
			return false;
		}
	}

	/**
	 * open a connection to the SQL server.
	 *
	 * @return true if we successfully connected to the db.
	 */
	private boolean getContext() {

		try {
			if (con == null || con.isClosed() || (!this.databaseType.equals("sqlite") && !con.isValid(1))) {
				if (con != null && !con.isClosed()) {
					try {
						con.close();
					} catch (SQLException e) {
						/*
						 *  We're disposing of an old stale connection just be nice to the GC
						 *  as well as mysql, so ignore the error as there's nothing we can do
						 *  if it fails
						 */
					}
				}
				
				con = DriverManager.getConnection(this.connectionURL, this.username, this.password);

				return con != null && !con.isClosed();
			}

			return true;
		} catch (SQLException e) {
			TownyMessaging.sendErrorMsg("Error could not Connect to db " + this.connectionURL + ": " + e.getMessage());
			return false;
		}
	}
	
	public void enableForeignKeyConstraints() {
		if (databaseType.equalsIgnoreCase("sqlite")){
			executeUpdate("PRAGMA foreign_keys=ON", "Error enabling foreign keys for SQLITE!");
		}
	}
	
	public Collection<String> getColumnNames(String tableName, String errorMessage) {
		String queryStatement;
		final GenericResultSetFunction<String> columnFunction;

		switch (databaseType.toLowerCase()) {
			case "h2":
			case "mysql":
				queryStatement = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'" + tableName + "'";
				columnFunction = (rs) -> rs.getString("COLUMN_NAME");
				break;
			case "sqlite":
				queryStatement = "PRAGMA table_info('" + tableName + "')";
				columnFunction = (rs) -> rs.getString("name");
				break;
				
			// Should never happen, but just in case
			default:
				throw new UnsupportedOperationException("Invalid database type!");
		}

		final Set<String> columnNames = new HashSet<>();
		executeQuery(queryStatement, errorMessage, rs -> {
				while (rs.next())
					columnNames.add(columnFunction.accept(rs));
			});
		return columnNames;
	}

	public boolean executeUpdate(String updateStmt) {
		return executeUpdate(updateStmt, null);
	}
	
	public boolean executeUpdate(String updateStmt, @Nullable String errorMessage) {
		if (getContext()) {
			try {
				Statement stmt = con.createStatement();
				int rowsAffected = stmt.executeUpdate(updateStmt);
				// Return whether the update actually updated anything
				return rowsAffected > 0;
			} catch (SQLException ex) {
				if (errorMessage != null) {
					TownyMessaging.sendErrorMsg(errorMessage);
					TownyMessaging.sendErrorMsg("SQL Statement: " + updateStmt);
				}
				ex.printStackTrace();
			}
		}
		return false;
	}

	public void executeUpdates(String... updates) {
		executeUpdatesError(null, updates);
	}
	
	public void executeUpdatesError(@Nullable String errorMessage, @NotNull String... updates) {
		Objects.requireNonNull(updates);
		
		if (getContext()) {
			try (Statement stmt = con.createStatement()) {

				for (String update : updates) {
					// Try-catch around update to prevent one failed update from stopping the rest.
					try {
						stmt.executeUpdate(update);
					} catch (SQLException ex) {
						if (errorMessage != null) {
							TownyMessaging.sendErrorMsg(errorMessage);
							TownyMessaging.sendErrorMsg("SQL Statement: " + update);
							ex.printStackTrace();
						}
					}
				}
			} catch (SQLException ex) {
				if (errorMessage != null) {
					TownyMessaging.sendErrorMsg(errorMessage);
					ex.printStackTrace();
				}
			}
		}
	}
	
	public void executeQuery(String query, String errorMessage, ResultSetConsumer consumer) {
		if (getContext()) {
			try {
				try (Statement stmt = con.createStatement();
					 ResultSet rs = stmt.executeQuery(query)) {
					consumer.accept(rs);
				}
			} catch (SQLException ex) {
				if (errorMessage != null) {
					TownyMessaging.sendErrorMsg(errorMessage);
					ex.printStackTrace();
				}
			}
		}
	}

	/***
	 * 
	 * @param tableName Table Name
	 * @param columnDefs Collection of string arrays. The arrays are formatted where
	 *                   the first elements is the column name,
	 *                   second element is column type definition, and
	 *                   third element is foreign key constraint (empty if none for that column)
	 *                   
	 */
	public void alterTableColumns(String tableName, Collection<String[]> columnDefs) {
		Collection<String> existingColumns = getColumnNames(tableName, "Error fetching column names for " + tableName + "!");
		
		Collection<String[]> uniqueColumns = new ArrayList<>();

		// Compare column names against the column names in the table.
		for (String[] columnDef : columnDefs) {
			if (!existingColumns.contains(columnDef[0]))
				uniqueColumns.add(columnDef);
		}
		
		// No unique columns, so nothing to alter
		if (uniqueColumns.isEmpty())
			return;

		String[] updateStatements;
		
		// Check whether SQLite or MYSQL/H2
		if (databaseType.equalsIgnoreCase("sqlite")) {
			updateStatements = uniqueColumns.stream()
				.map(s -> "ALTER TABLE " + tableName + " ADD COLUMN "  +
					s[0] + " " +  s[1] + (!s[2].isEmpty() ? " REFERENCES " + s[2] : "") + ";")
				.toArray(String[]::new);
		}
		// MySQL or H2 or literally any other sane SQL format
		else {
			updateStatements = uniqueColumns.stream()
										.map(s -> "ALTER TABLE " + tableName + " ADD "  + 
											s[0] + " " +  s[1] +
											(!s[2].isEmpty() ? ", ADD FOREIGN KEY (" + s[0] + ") " + s[2] : "") + ";")
										.toArray(String[]::new);
			
		}

		executeUpdatesError("Error altering table " + tableName, updateStatements);
	}
}
