package com.palmergames.bukkit.towny.utils.dbHandlers.sql.object;

import java.sql.JDBCType;

public class SQLData<T> {
	JDBCType columnType;
	T value;
	
	public SQLData(T data, JDBCType columnType) {
		this.value = data;
		this.columnType = columnType;
	}

	public T getValue() {
		return value;
	}
	
	public JDBCType getColumnType() {
		return columnType;
	}
}
