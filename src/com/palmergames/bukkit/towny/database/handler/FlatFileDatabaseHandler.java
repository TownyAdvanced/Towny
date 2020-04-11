package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.util.FileMgmt;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FlatFileDatabaseHandler extends DatabaseHandler {
	
	@Override
	public void save(Saveable obj) {
		HashMap<String, String> saveMap = new HashMap<>();

		// Get field data.
		convertMapData(getObjectMap(obj), saveMap);
		
		// Add save getter data.
		convertMapData(getSaveGetterData(obj), saveMap);
		
		// Save
		FileMgmt.mapToFile(saveMap, new File(obj.getSaveDirectory() + "test.data"));
	}

	@Override
	public <T> T load(File file, Class<T> clazz) {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		T obj = null;
		try {
			assert objConstructor != null;
			obj = objConstructor.newInstance("");
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		assert obj != null;
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		HashMap<String, String> values = loadFileIntoHashMap(file);
		for (Field field : fields) {
			Type type = field.getGenericType();
			field.setAccessible(true);

			String fieldName = field.getName();
			if (replacementKeys.containsKey(fieldName)) {
				fieldName = replacementKeys.get(fieldName);
			}

			if (values.get(fieldName) == null) {
				continue;
			}

			Object value;

			if (isPrimitive(type)) {
				value = loadPrimitive(values.get(fieldName), type);
			} else {
				value = fromFileString(values.get(fieldName), type);
			}

			if (value == null) {
				// ignore it as another already allocated value may be there.
				continue;
			}

			LoadSetter loadSetter = field.getAnnotation(LoadSetter.class);

			try {

				if (loadSetter != null) {
					Method method = obj.getClass().getMethod(loadSetter.setterName(), field.getType());
					method.invoke(obj, value);
				} else {
					field.set(obj, value);
				}
				
			} catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
		
		return obj;
	}
	
	private void convertMapData(Map<String, ObjectContext> from, Map<String, String> to) {
		for (Map.Entry<String, ObjectContext> entry : from.entrySet()) {
			TownyMessaging.sendErrorMsg(entry.getValue().getType() +"");
			String valueStr = toFileString(entry.getValue(), entry.getValue().getType());
			to.put(entry.getKey(), valueStr);
		}
	}
}
