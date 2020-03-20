package com.palmergames.bukkit.towny.utils.dbHandlers.flatfile;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class TypeContext<T> {
	public Type getType() {
		Class<?> foo = this.getClass();
		ParameterizedType t = (ParameterizedType) foo.getGenericSuperclass();
		return t.getActualTypeArguments()[0];
	}
}
