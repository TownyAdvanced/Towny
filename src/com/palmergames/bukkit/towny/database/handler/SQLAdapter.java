package com.palmergames.bukkit.towny.database.handler;

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
	}
	
	public static SQLAdapter adapt(String databaseType) {
		if (databaseType.equalsIgnoreCase("sqlite")) {
			return new SQLiteAdapter();
		}
		else {
			return new SQLAdapter();
		}
	}
}
