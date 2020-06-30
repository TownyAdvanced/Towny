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
import java.util.function.Consumer;

public class SQLHandler {
	
	@FunctionalInterface
	public interface ResultSetConsumer {
		void accept(ResultSet rs) throws SQLException;
	}
	
	private String username, password;
	private String databaseType;
	private String dbName;
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

	public void executeUpdatesError(@Nullable String errorMessage, @NotNull Collection<String> updates) {
		if (!updates.isEmpty()) {
			executeUpdatesError(errorMessage, updates.toArray(new String[0]));
		}
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
}
