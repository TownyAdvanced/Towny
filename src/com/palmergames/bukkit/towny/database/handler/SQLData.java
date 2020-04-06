package com.palmergames.bukkit.towny.database.handler;

import java.sql.JDBCType;

public class SQLData {
	JDBCType columnType;
	Object value;
	
	public SQLData(Object data, JDBCType columnType) {
		this.value = data;
		this.columnType = columnType;
	}

	public Object getValue() {
		return value;
	}
	
	public JDBCType getColumnType() {
		return columnType;
	}

	@Override
	public String toString() {
		return "SQLData{" +
			"columnType=" + columnType +
			", value=" + value +
			'}';
	}
}
