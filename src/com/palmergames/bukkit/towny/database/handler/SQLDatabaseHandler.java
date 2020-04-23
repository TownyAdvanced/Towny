package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Saveable;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SQLDatabaseHandler extends DatabaseHandler {
	
	private Connection getConnection() {
		// TODO Fill-in method with proper connection fetching
		throw new UnsupportedOperationException("getConnection() Stub");
	}
	
	
	@Override
	public void save(Saveable obj) {
		// TODO Fill-in method with proper save algorithm
		throw new UnsupportedOperationException("save() Stub");
	}

	@Override
	public boolean delete(Saveable obj) {
		return false;
	}
	
	private <T> T load(ResultSet rs, @NotNull Class<T> clazz) throws SQLException {
		Constructor<T> objConstructor = null;
		try {
			objConstructor = clazz.getConstructor(UUID.class);
		} catch (NoSuchMethodException e) {
			TownyMessaging.sendErrorMsg("flag 1");
			e.printStackTrace();
		}

		T obj = null;
		try {
			Validate.isTrue(objConstructor != null);
			obj = objConstructor.newInstance((Object) null);
		} catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
			TownyMessaging.sendErrorMsg("flag 2");
			e.printStackTrace();
		}

		Validate.isTrue(obj != null);
		List<Field> fields = ReflectionUtil.getAllFields(obj, true);

		Map<String, Object> values = rowToMap(rs);
		for (Field field : fields) {
			Type type = field.getGenericType();
			Class<?> classType = field.getType();
			field.setAccessible(true);

			String fieldName = field.getName();

			if (values.get(fieldName) == null) {
				continue;
			}

			Object value;

			if (ReflectionUtil.isPrimitive(type)) {
				value = values.get(fieldName);
			} else if (field.getType().isEnum()) {
				// Assume value is a string
				value = ReflectionUtil.loadEnum((String) values.get(fieldName), classType);
			} else {
				value = fromFileString((String) values.get(fieldName), classType);
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
				TownyMessaging.sendErrorMsg("flag 3");
				e.printStackTrace();
				return null;
			}
		}

		return obj;
	}
	
	private Map<String, Object> rowToMap(ResultSet rs) throws SQLException {
		ResultSetMetaData rsMD = rs.getMetaData();
		int columns = rsMD.getColumnCount();
		
		Map<String, Object> columnMap = new HashMap<>(columns);
		
		for (int i = 1; i <= columns; ++i) {
			columnMap.put(rsMD.getColumnName(i), rs.getObject(i));
		}
		return columnMap;
	}
	
	// None of the methods below call their respective load functions
	// because that would require an useless extra query

	// TODO Shove all the boilerplate into one function
	
	@Override
	public void loadAllResidents() {
		try (Connection con = getConnection()) {
			try (Statement stmt = con.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + "RESIDENTS")) {
				while (rs.next()) {
					Resident resident = load(rs, Resident.class);
					
					if (resident == null) {
						String residentName = rs.getString("name");
						TownyMessaging.sendErrorMsg("Could not load resident " + (residentName != null ? residentName : "null"));
						continue;
					}

					try {
						TownyUniverse.getInstance().addResident(resident);
					} catch (AlreadyRegisteredException ex) {
						TownyMessaging.sendErrorMsg("Resident " + resident.getName() + " is already registered!");
					}
				}
			}
		} catch (SQLException ex) {
			TownyMessaging.sendErrorMsg("Error loading residents from SQL!");
			ex.printStackTrace();
		}
	}

	@Override
	public void loadAllWorlds() {
		try (Connection con = getConnection()) {
			try (Statement stmt = con.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + "WORLDS")) {
				while (rs.next()) {
					TownyWorld townyWorld = load(rs, TownyWorld.class);

					if (townyWorld == null) {
						String worldName = rs.getString("name");
						TownyMessaging.sendErrorMsg("Could not load world " + (worldName != null ? worldName : "null"));
						continue;
					}

					try {
						TownyUniverse.getInstance().addWorld(townyWorld);
					} catch (AlreadyRegisteredException ex) {
						TownyMessaging.sendErrorMsg("World " + townyWorld.getName() + " is already registered!");
					}
				}
			}
		} catch (SQLException ex) {
			TownyMessaging.sendErrorMsg("Error loading worlds from SQL!");
			ex.printStackTrace();
		}
	}

	@Override
	public void loadAllTowns() {
		try (Connection con = getConnection()) {
			try (Statement stmt = con.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + "TOWNS")) {
				while (rs.next()) {
					Town town = load(rs, Town.class);
					
					if (town == null) {
						String townName = rs.getString("name");
						TownyMessaging.sendErrorMsg("Could not load town " + (townName != null ? townName : "null"));
						continue;
					}

					try {
						TownyUniverse.getInstance().addTown(town);
					} catch (AlreadyRegisteredException ex) {
						TownyMessaging.sendErrorMsg("Town " + town.getName() + " is already registered!");
					}
				}
			}
		} catch (SQLException ex) {
			TownyMessaging.sendErrorMsg("Error loading towns from SQL!");
			ex.printStackTrace();
		}
	}

	@Override
	public void loadAllTownBlocks() {
		try (Connection con = getConnection()) {
			try (Statement stmt = con.createStatement();
				 ResultSet rs = stmt.executeQuery("SELECT * from " + TownySettings.getSQLTablePrefix() + "TOWNBLOCKS")) {
				while (rs.next()) {
					TownBlock townBlock = load(rs, TownBlock.class);

					if (townBlock == null) {
						String worldName = rs.getString("world");
						int x = rs.getInt("x");
						int z = rs.getInt("z");
						TownyMessaging.sendErrorMsg("Could not load townblock (" + worldName + ", " + x + ", " + z);
						continue;
					}

					try {
						TownyUniverse.getInstance().addTownBlock(townBlock);
					} catch (AlreadyRegisteredException ex) {
						TownyMessaging.sendErrorMsg("Townblock  " + townBlock.toString() + " is already registered!");
					}
				}
			}
		} catch (SQLException ex) {
			TownyMessaging.sendErrorMsg("Error loading townblocks from SQL!");
			ex.printStackTrace();
		}
	}
}
