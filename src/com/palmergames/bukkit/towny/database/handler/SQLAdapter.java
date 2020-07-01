package com.palmergames.bukkit.towny.database.handler;

import java.util.Map;
import java.util.StringJoiner;

public class SQLAdapter {
	
	private SQLAdapter() {}

	/**
	 * 
	 * @param tableName Table Name
	 * @return A string array of size 2 where the first element represents the query statement, and
	 * 		   the second element represents the column name for the result set.	
	 */
	public String[] getColumnNameStmt(String tableName) {
		String queryStatement = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = N'" + tableName + "'";;
		String columnName = "COLUMN_NAME";
		
		return new String[] {queryStatement, columnName};
	}
	
	public String getAlterTableStatement(String tableName, String columnName, String columnDef, String foreignKeyDef) {
		String stmt = "ALTER TABLE " + tableName + " ADD " + columnName  + " " + columnDef;
		
		if (!foreignKeyDef.isEmpty())
			stmt += ", ADD FOREIGN KEY (" + columnName + ") REFERENCES " + foreignKeyDef;
		
		return stmt;
	}

	// Foreign key checks
	public boolean explicitForeignKeyEnable() {
		return false;
	}

	public String explicitForeignKeyStatement() {
		return "";
	}
	
	public String upsertStatement(String tableName, Map<String, String> insertionMap) {
		StringBuilder stmtBuilder = new StringBuilder();
		StringJoiner columnBuilder = new StringJoiner(", "),
					  valueBuilder = new StringJoiner(", "),
					  keyValueBuilder = new StringJoiner(", ");

		for (Map.Entry<String, String> entry : insertionMap.entrySet()) {
			columnBuilder.add(entry.getKey());
			valueBuilder.add(entry.getValue());
			keyValueBuilder.add(entry.getKey() + " = " + entry.getValue());
		}

		stmtBuilder.append("INSERT INTO ").append(tableName).append(" (")
					.append(columnBuilder.toString()).append(") VALUES (")
					.append(valueBuilder.toString()).append(") ON DUPLICATE KEY UPDATE ")
					.append(keyValueBuilder.toString()).append(";");
		
		return stmtBuilder.toString();
	}
	
	// SQLite Class
	private static class SQLiteAdapter extends SQLAdapter {
		
		@Override
		public String[] getColumnNameStmt(String tableName) {
			String queryStatement = "PRAGMA table_info('" + tableName + "')";
			String columnName = "name";

			return new String[] {queryStatement, columnName};
		}

		@Override
		public String getAlterTableStatement(String tableName, String columnName, String columnDef, String foreignKeyDef) {
			String stmt =  "ALTER TABLE " + tableName + " ADD COLUMN " + columnName  + " " + columnDef;
			
			if (!foreignKeyDef.isEmpty())
				stmt += " REFERENCES " + foreignKeyDef;
			
			return stmt;
		}

		@Override
		public boolean explicitForeignKeyEnable() {
			return true;
		}

		@Override
		public String explicitForeignKeyStatement() {
			return "PRAGMA foreign_keys=ON";
		}

		@Override
		public String upsertStatement(String tableName, Map<String, String> insertionMap) {
			StringBuilder stmtBuilder = new StringBuilder();
			StringJoiner columnBuilder = new StringJoiner(", "),
				valueBuilder = new StringJoiner(", "),
				keyValueBuilder = new StringJoiner(", ");

			for (Map.Entry<String, String> entry : insertionMap.entrySet()) {
				columnBuilder.add(entry.getKey());
				valueBuilder.add(entry.getValue());
				keyValueBuilder.add(entry.getKey() + " = " + entry.getValue());
			}

			stmtBuilder.append("INSERT INTO ").append(tableName).append(" (")
				.append(columnBuilder.toString()).append(") VALUES (")
				.append(valueBuilder.toString()).append(") ON CONFLICT (uniqueIdentifier) DO UPDATE SET ")
				.append(keyValueBuilder.toString()).append(";");

			return stmtBuilder.toString();
		}
	}
	
	// H2 Class
	private static class H2Adapter extends SQLAdapter {
		@Override
		public String upsertStatement(String tableName, Map<String, String> insertionMap) {
			StringBuilder stmtBuilder = new StringBuilder();
			StringJoiner columnBuilder = new StringJoiner(", "),
				valueBuilder = new StringJoiner(", ");

			for (Map.Entry<String, String> entry : insertionMap.entrySet()) {
				columnBuilder.add(entry.getKey());
				valueBuilder.add(entry.getValue());
			}

			stmtBuilder.append("MERGE INTO ").append(tableName).append(" (")
				.append(columnBuilder.toString()).append(") KEY (uniqueIdentifier) (")
				.append(valueBuilder.toString()).append(");");

			return stmtBuilder.toString();
		}
	} 
	
	public static SQLAdapter from(String databaseType) {
		switch (databaseType.toLowerCase()) {
			case "sqlite":
				return new SQLiteAdapter();
			case "h2":
				return new H2Adapter();
			case "mysql":
			default:
				return new SQLAdapter();
		}
	}
}
