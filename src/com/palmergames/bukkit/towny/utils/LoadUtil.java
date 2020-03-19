package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.utils.loadHandlers.LoadSetter;
import com.palmergames.bukkit.towny.utils.loadHandlers.LoadHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class LoadUtil {

	public HashMap<Type, LoadHandler<?>> loadHandlers = new HashMap<>();
	public HashMap<Field, Method> setters = new HashMap<>();
	
	public <T> void load(Class<T> clazz, HashMap<String, String> saveData) {
		Constructor<T> constructor;
		T object;
		try {
			constructor = clazz.getConstructor(String.class);
			object = constructor.newInstance("name");
		} catch (Exception e) {
			throw new UnsupportedOperationException();
		}
		
	}

	public void parse(TownyObject object) {
		for (Field field : ReflectionUtil.getAllFields(object, true)) {
			// Check if the field has the annotation.
			LoadSetter setterAnnotation = field.getDeclaredAnnotation(LoadSetter.class);
			if (setterAnnotation != null) {

				// Capture setter name from annotation.
				String setterName = setterAnnotation.setterName();

				try {
					// Methods are *inferred* to take a setter parameter with the same data type,
					// as the associated field.
					Method setterMethod = object.getClass().getMethod(setterName, field.getType());
					setters.put(field, setterMethod);
				} catch (NoSuchMethodException e) {
					// This is a fail-fast either it works or we shouldn't.
					throw new RuntimeException("Could not load setter: " + setterName);
				}

			}

			field.setAccessible(true);
		}
	}

	public void loadField(String value, Field field, Object object) {
		
		Object fieldValue = handle(value, field.getGenericType());
		
		if (usingSetter(field)) {
			Method setter = setters.get(field);
			setter.setAccessible(true);
			try {
				setter.invoke(object, fieldValue);
				setter.setAccessible(false);
				return;
			} catch (Exception e) {
				throw new UnsupportedOperationException();
			}
		}
		
		try {
			field.set(object, fieldValue);
			field.setAccessible(false);
		} catch (Exception e) {
			throw new UnsupportedOperationException();
		}
		
	}

	public Object parseString(String str, Field field) {

		Type type = field.getGenericType();

		return handle(str, type);
	}

	private List<Resident> parseResidentList(String str) {

		List<Resident> residents = new ArrayList<>();

		// Split the names
		String[] resNames = str.split(",");

		for (String resName : resNames) {
			try {
				TownyUniverse.getInstance().getDataSource().getResident(resName);
			} catch (Exception e) {
				continue;
			}
		}

		return residents;
	}

	private TownyPermission parsePermission(String str) {
		TownyPermission townyPermission = new TownyPermission();
		townyPermission.load(str);

		return townyPermission;
	}

	public void registerLoadHandler(Type type, LoadHandler<?> handler) {
		TownyMessaging.sendErrorMsg("added " + type);
		loadHandlers.put(type, handler);
	}

	public Object handle(String str, Type type) {

		if (!loadHandlers.containsKey(type)) {
			throw new UnsupportedOperationException("There is not load handler for " + type);
		}

		LoadHandler<?> handler = loadHandlers.get(type);

		TownyMessaging.sendErrorMsg(handler.load(str) + "");

		return handler.load(str);
	}
	
	private boolean usingSetter(Field field) {
		return setters.get(field) != null;
	}
}
