package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.TownyObject;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

public class ReflectionUtil {

	/**
	 * Fetches all the fields from the TownyObject.
	 * 
	 * @param townyObject The TownyObject to get the fields from.
	 * @param ignoreTransient Indicates whether or not to get transient fields or not.
	 * @return A list of Fields from the TownyObject.
	 */
	public static List<Field> getAllFields(TownyObject townyObject, boolean ignoreTransient) {
		
		// Get the class object.
		Class<?> type = townyObject.getClass();
		List<Field> fields = new ArrayList<>();

		// Use a stack to evaluate classes in proper top-down hierarchy.
		ArrayDeque<Class<?>> classStack = new ArrayDeque<>();
		
		// Iterate through superclasses.
		for (Class<?> c = type; c != null; c = c.getSuperclass()) {
			classStack.push(c);
		}
		
		for (Class<?> classType : classStack) {
			for (Field field : classType.getDeclaredFields()) {
				// Ignore transient fields.
				if (ignoreTransient && Modifier.isTransient(field.getModifiers())) {
					continue;
				}
				
				fields.add(field);
			}
		}
		
		return fields;
	}
}
