package com.palmergames.bukkit.towny.database.handler;

import java.lang.reflect.Type;

public class ObjectContext {
	private final Type type;
	private final Object value;
	
	public ObjectContext(Object value, Type type) {
		this.value = value;
		this.type = type;
	}

	public Type getType() {
		return type;
	}

	public Object getValue() {
		return value;
	}
}
