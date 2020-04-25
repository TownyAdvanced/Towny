package com.palmergames.bukkit.towny.database.handler;

public enum SQLStringType {
	CHAR("CHAR"),
	VARCHAR("VARCHAR"),
	BINARY("BINARY"),
	TEXT("TEXT"),
	TINY_TEXT("TINYTEXT"),
	MEDIUM_TEXT("MEDIUMTEXT"),
	LONG_TEXT("LONGTEXT"),
	BLOB("BLOB");
	

	private String columnName;
	SQLStringType(String columnName) {
		this.columnName = columnName;
	}

	public String getColumnName() {
		return columnName;
	}
}
