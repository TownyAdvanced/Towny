package com.palmergames.bukkit.towny.utils;

import java.lang.reflect.Type;

public class PrimitiveLoader {
	public static Object load(String string, Type type) {
		if (type == int.class || type == Integer.class) {
			return Integer.parseInt(string);
		} else if (type == double.class || type == Double.class) {
			return Double.parseDouble(string);
		} else if (type == float.class || type == Float.class) {
			return Float.parseFloat(string);
		} else if (type == boolean.class || type == Boolean.class) {
			return Boolean.parseBoolean(string);
		} else if (type == long.class || type == Long.class) {
			return Long.parseLong(string);
		} else if (type == char.class || type == Character.class) {
			return string.charAt(0);
		} else {
			return string;
		}
	}
}
