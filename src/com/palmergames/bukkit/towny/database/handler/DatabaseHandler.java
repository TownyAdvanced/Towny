package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.database.dbHandlers.BaseTypeHandlers;
import com.palmergames.bukkit.towny.database.dbHandlers.ListHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.LocationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.NationHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.ResidentHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.SetHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownBlockHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyPermissionsHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.TownyWorldHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.UUIDHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.WorldCoordHandler;
import com.palmergames.bukkit.towny.database.handler.annotations.SQLString;
import com.palmergames.bukkit.towny.database.handler.annotations.SaveGetter;
import com.palmergames.bukkit.towny.database.type.TypeAdapter;
import com.palmergames.bukkit.towny.database.type.TypeContext;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

/**
 * The object which is responsible for converting objects from one format to another and
 * saving the mentioned format.
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseHandler {
	private final BukkitTask dbTask;
	
	public DatabaseHandler() {
		dbTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Towny.getPlugin(),
			this::processDBQueue, 5L, 60 * 20L); // Runs task every minute
	}

	Map<String, ObjectContext> getSaveGetterData(Saveable obj) {

		HashMap<String, ObjectContext> saveMap = new HashMap<>();

		// Get the save getters
		for (Method method : obj.getClass().getMethods()) {

			// Get the annotation from the method.
			SaveGetter saveGetter = method.getDeclaredAnnotation(SaveGetter.class);

			// Check if its present.
			if (saveGetter != null) {

				// Get the key name from the annotation.
				String key = saveGetter.keyName();
				
				// Get type
				Type type = method.getGenericReturnType();

				// Try to fetch the return value.
				Object value;
				try {
					value = method.invoke(obj);
				} catch (IllegalAccessException | InvocationTargetException e) {
					TownyMessaging.sendErrorMsg(e.getMessage());
					continue;
				}

				// Add to map.
				saveMap.put(key, new ObjectContext(value, type));
			}
		}

		return saveMap;
	}
	
	public void loadWorlds() {
		for (World world : Bukkit.getServer().getWorlds()) {
			try {
				
				TownyWorld wrappedWorld = new TownyWorld(world.getUID(), world.getName());
				TownyUniverse.getInstance().addWorld(wrappedWorld);
				// Save
				save(wrappedWorld);
			} catch (AlreadyRegisteredException e) {
				//e.printStackTrace();
			}
		}
	}
	
	public abstract void upgrade();

	// ---------- DB operation Methods ----------
	
	/**
	 * Queues the given object to be saved to the DB.
	 *
	 * @param obj The object to save.
	 */
	public abstract void save(@NotNull Saveable obj);

	/**
	 * Queues the given object to be removed from the DB.
	 * 
	 * @param obj The object to delete.
	 * @return A boolean indicating if successful or not.
	 */
	public abstract boolean delete(@NotNull Saveable obj);

	/**
	 * Queues all given objects to be saved to the DB.
	 * 
	 * @param objs The objects to save.
	 */
	public final void save(Saveable @NotNull ... objs) {
		Validate.notNull(objs);
		save(Arrays.asList(objs));
	}
	
	/**
	 * Queues the objects to be saved the database.
	 * 
	 * @param objs The objects to save.
	 */
	public final void save(@NotNull Collection<? extends Saveable> objs) {
		Validate.notNull(objs);
		
		for (Saveable obj : objs) {
			save(obj);
		}
	}

	/**
	 * Process all the current queued objects in the DB queue.
	 * This method should be ran off the main thread since it performs IO operations.
	 */
	public abstract void processDBQueue();

	/**
	 * Shutdown the database handler
	 */
	public void shutdown() {
		dbTask.cancel();
		processDBQueue();
	}
	
	// These methods will differ greatly between inheriting classes,
	// hence they are abstract.

	// ---------- Load All Methods ----------
	public abstract void loadAllResidents();
	public abstract void loadAllWorlds();
	public abstract void loadAllNations();
	public abstract void loadAllTowns();
	public abstract void loadAllTownBlocks();
	
	protected abstract void completeLoad();

	/**
	 * Loads all necessary objects for the database.
	 */
	public final void loadAll() {
		loadAllWorlds();
		loadAllNations();
		loadAllTowns();
		loadAllResidents();
		loadAllTownBlocks();
		// Loads all the bukkit worlds if they haven't been loaded.
		loadWorlds();
		completeLoad();
	}
	
	public final void saveAll() {
		TownyUniverse.getInstance().getTownBlocks().forEach(this::save);
		TownyUniverse.getInstance().getTowns().forEach(this::save);
		TownyUniverse.getInstance().getNations().forEach(this::save);
		TownyUniverse.getInstance().getWorlds().forEach(this::save);
		TownyUniverse.getInstance().getResidents().forEach(this::save);
	}
}
