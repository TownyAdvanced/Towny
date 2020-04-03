package com.palmergames.bukkit.towny.database.type;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * A class used for the transport of parameterized generic types. This class 
 * should be created in an anonymous context to properly extract generic type.
 * 
 * @param <T> The parameterized type to capture.
 * @author Suneet Tipirneni (Siris)
 */
public class TypeContext<T> {

	/**
	 * Gets the generic type of the wrapped class.
	 * 
	 * @return The Type of the wrapped.
	 */
	public Type getType() {
		Class<?> foo = this.getClass();
		ParameterizedType t = (ParameterizedType) foo.getGenericSuperclass();
		return t.getActualTypeArguments()[0];
	}
}
