package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.util.FileMgmt;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FlatFileDatabaseHandler extends DatabaseHandler {
	
	@Override
	public void save(Saveable obj) {
		HashMap<String, String> saveMap = new HashMap<>();

		// Get field data.
		convertMapData(getObjectMap(obj), saveMap);
		
		// Add save getter data.
		convertMapData(getSaveGetterData(obj), saveMap);

		TownyMessaging.sendErrorMsg(obj.getSavePath().toString());
		
		// Save
		FileMgmt.mapToFile(saveMap, obj.getSavePath());
	}

	@Override
	@Nullable
	public <T> T load(File file, @NotNull Class<T> clazz) {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(UUID.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

		T obj = null;
		try {
			Validate.isTrue(objConstructor != null);
			obj = objConstructor.newInstance((Object) null);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}

		Validate.isTrue(obj != null);
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		HashMap<String, String> values = loadFileIntoHashMap(file);
		for (Field field : fields) {
			Type type = field.getGenericType();
			field.setAccessible(true);

			String fieldName = field.getName();

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
				return null;
			}
		}
		
		return obj;
	}
	
	// ---------- File Getters ----------
	
	public File getResidentFile(UUID id) {
		return new File(Towny.getPlugin().getDataFolder() + "/data/residents/" + id + ".txt");
	}
	
	public File getTownFile(UUID id) {
		return new File(Towny.getPlugin().getDataFolder() + "/data/towns/" + id + ".txt");
	}
	
	public File getNationFile(UUID id) {
		return new File(Towny.getPlugin().getDataFolder() + "/data/nations/" + id + ".txt");
	}
	
	public File getWorldFile(UUID id) {
		return new File(Towny.getPlugin().getDataFolder() + "/data/worlds/" + id + ".txt");
	}

	// ---------- File Getters ----------
	
	// ---------- Loaders ----------
	
	@Override
	public Resident loadResident(UUID id) {
		File residentFileFile = getResidentFile(id);
		return load(residentFileFile, Resident.class);
	}
	
	@Override
	public Town loadTown(UUID id) {
		File townFile = getTownFile(id);
		return load(townFile, Town.class);
	}

	@Override
	public Nation loadNation(UUID id) {
		File nationFile = getNationFile(id);
		return load(nationFile, Nation.class);
	}

	@Override
	public TownyWorld loadWorld(UUID id) {
		File worldFile = getWorldFile(id);
		return load(worldFile, TownyWorld.class);
	}
	
	// ---------- Loaders ----------
	
	@Override
	public void loadAllResidents() {
		File resDir = new File(Towny.getPlugin().getDataFolder() + "/data/residents");
		String[] resFiles = resDir.list((dir, name) -> name.endsWith(".txt"));
		TownyMessaging.sendErrorMsg(Arrays.toString(resFiles));
		for (String fileName : resFiles) {
			TownyMessaging.sendErrorMsg(fileName);
			String idStr = fileName.replace(".txt", "");
			UUID id = UUID.fromString(idStr);
			Resident loadedResident = loadResident(id);
			
			// Store data.
			residents.put(id, loadedResident);
			
			// Cache name data.
			residentNameMap.put(loadedResident.getName(), loadedResident);
		}
	}

	@Override
	public void loadAllWorlds() {
		File worldsDir = new File(Towny.getPlugin().getDataFolder() + "/data/worlds");
		String[] worldFiles = worldsDir.list((dir, name) -> name.endsWith(".txt"));
		TownyMessaging.sendErrorMsg(Arrays.toString(worldFiles));
		for (String fileName : worldFiles) {
			TownyMessaging.sendErrorMsg(fileName);
			String idStr = fileName.replace(".txt", "");
			UUID id = UUID.fromString(idStr);
			TownyWorld loadedWorld = loadWorld(id);
			
			if (loadedWorld == null) {
				TownyMessaging.sendErrorMsg("Could not load" + fileName);
				continue;
			}

			// Store data.
			worlds.put(id, loadedWorld);

			// Cache name data.
			worldNameMap.put(loadedWorld.getName(), loadedWorld);
		}
	}

	private void convertMapData(Map<String, ObjectContext> from, Map<String, String> to) {
		for (Map.Entry<String, ObjectContext> entry : from.entrySet()) {
			String valueStr = toFileString(entry.getValue().getValue(), entry.getValue().getType());
			to.put(entry.getKey(), valueStr);
		}
	}

	@Override
	public void load() {
		
	}
}
