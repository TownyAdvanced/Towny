package com.palmergames.bukkit.towny.database.handler;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.database.Saveable;
import com.palmergames.bukkit.towny.database.handler.annotations.SaveGetter;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.util.FileMgmt;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.scheduler.BukkitTask;
import org.jetbrains.annotations.NotNull;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The object which is responsible for converting objects from one format to another and
 * saving the mentioned format.
 */
@SuppressWarnings("unchecked")
public abstract class DatabaseHandler {
	private final BukkitTask dbTask;
	
	public DatabaseHandler() {
		// Create flat storage files if they don't exist
		String rootDirPath = TownyUniverse.getInstance().getRootFolder();
		if (!FileMgmt.checkOrCreateFolders(rootDirPath, getDataFilePath(),
			getDataFilePath("plot-block-data"))
			|| !FileMgmt.checkOrCreateFiles(getDataFilePath("regen.txt"),
				getDataFilePath("snapshot_queue.txt"))) {
			TownyMessaging.sendErrorMsg("Could not create flatfile default files and folders.");
		}
		
		dbTask = Bukkit.getScheduler().runTaskTimerAsynchronously(Towny.getPlugin(),
			() -> {
				processDBQueue();
				saveSnapshotQueue();
				saveRegenList();
			}, 5L, 60 * 20L); // Runs task every minute
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
	
	private void loadCurrentWorlds() {
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
	
	private String getDataFilePath(String... path) {
		String dataFolder = TownyUniverse.getInstance().getRootFolder() + File.separator + "data";
		
		if (path == null)
			return dataFolder;
		
		// Avoid overhead for single path
		if (path.length == 1)
			return dataFolder + File.separator + path[0];
		else {
			StringBuilder pathBuilder = new StringBuilder(dataFolder);
			for (String currFile : path) {
				pathBuilder.append(File.separator);
				pathBuilder.append(currFile);
			}
			return pathBuilder.toString();
		}
	}
	
	private void loadSnapshotQueue() {
		TownyMessaging.sendDebugMsg("Loading Snapshot Queue");
		FileMgmt.readLinesInFile(getDataFilePath("snapshot_queue.txt"),
			"Error loading SnapShot Queue List.",
			(line) -> {
				String[] split = line.split(",");
				WorldCoord worldCoord = new WorldCoord(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]));
				TownyRegenAPI.addWorldCoord(worldCoord);
			}
		);
	}

	private void saveSnapshotQueue() {
		// Check if the snapshot queue actually needs to be saved
		if (TownyRegenAPI.shouldSaveSnapshotQueue()) {
			// Set the save state to false, so snapshots
			// added while saving the queue have to be resaved for concurrent safety
			TownyRegenAPI.setSaveSnapshotQueue(false);
			String filePath = getDataFilePath("snapshot_queue.txt");
			List<String> lines = new ArrayList<>();
			while (TownyRegenAPI.hasWorldCoords()) {
				WorldCoord worldCoord = TownyRegenAPI.getWorldCoord();
				lines.add(worldCoord.getWorldName() + "," + worldCoord.getX() + "," + worldCoord.getZ());
			}
			FileMgmt.listToFile(lines, filePath);
		}
	}
	
	private void loadRegenList() {
		TownyMessaging.sendDebugMsg("Loading Regen List");

		FileMgmt.readLinesInFile(getDataFilePath("regen.txt"),
			"Error loading regen list.",
			(line) -> {
				String[] split = line.split(",");
				PlotBlockData plotData = loadPlotData(split[0], Integer.parseInt(split[1]), Integer.parseInt(split[2]));
				if (plotData != null) {
					TownyRegenAPI.addPlotChunk(plotData, false);
				}
			}
		);
	}

	public PlotBlockData loadPlotData(String worldName, int x, int z) {

		try {
			TownyWorld world = TownyUniverse.getInstance().getWorld(worldName);
			TownBlock townBlock = new TownBlock(UUID.randomUUID(),x, z, world);

			return loadPlotData(townBlock);
		} catch (NotRegisteredException e) {
			// Failed to get world
			e.printStackTrace();
		}
		return null;
	}

	public PlotBlockData loadPlotData(TownBlock townBlock) {
		
		String fileName = getDataFilePath("plot-block-data",
			townBlock.getWorld().getName(),
			townBlock.getX() + "_" + townBlock.getZ() + "_" + TownySettings.getTownBlockSize() + ".data");

		if (FileMgmt.fileExists(fileName)) {
			PlotBlockData plotBlockData;
			try {
				plotBlockData = new PlotBlockData(townBlock);
			} catch (NullPointerException e1) {
				TownyMessaging.sendErrorMsg("Unable to load plotblockdata for townblock: " + townBlock.getWorldCoord().toString() + ". Skipping regeneration for this townBlock.");
				return null;
			}
			List<String> blockArr = new ArrayList<>();
			int version = 0;

			try (DataInputStream fin = new DataInputStream(new FileInputStream(fileName))) {

				//read the first 3 characters to test for version info
				fin.mark(3);
				byte[] key = new byte[3];
				fin.read(key, 0, 3);
				String versionKey = new String(key);
				// TODO hopefully this is right?
				if (versionKey.equalsIgnoreCase("VER")) {
					// Read the file version
					version = fin.read();
					plotBlockData.setVersion(version);

					// next entry is the plot height
					plotBlockData.setHeight(fin.readInt());
				} else {
					/*
					 * no version field so set height
					 * and push rest to queue
					 */
					plotBlockData.setVersion(version);
					// First entry is the plot height
					fin.reset();
					plotBlockData.setHeight(fin.readInt());
					blockArr.add(fin.readUTF());
					blockArr.add(fin.readUTF());
				}

				/*
				 * Load plot block data based upon the stored version number.
				 */
				if (version == 2) {
					// load remainder of file
					int temp;
					while ((temp = fin.readInt()) >= 0) {
						blockArr.add(temp + "");
					}
				}
				else {
					// load remainder of file
					String value;
					while ((value = fin.readUTF()) != null) {
						blockArr.add(value);
					}
				}

			} catch (EOFException ignored) {
			} catch (IOException e) {
				e.printStackTrace();
			}

			plotBlockData.setBlockList(blockArr);
			plotBlockData.resetBlockListRestored();
			return plotBlockData;
		}
		return null;
	}
	
	private void saveRegenList() {
		if (TownyRegenAPI.shouldSaveSnapshotQueue()) {
			TownyRegenAPI.setSaveRegenList(false);
			String filePath = getDataFilePath("regen.txt");
			List<String> lines = new ArrayList<>();
			for (PlotBlockData plotData : TownyRegenAPI.getPlotChunks().values()) {
				lines.add(plotData.getWorldName() + "," + plotData.getX() + "," + plotData.getZ());
			}
			FileMgmt.listToFile(lines, filePath);
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
	public final void save(@NotNull Saveable... objs) {
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
		saveRegenList();
		saveSnapshotQueue();
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
		loadCurrentWorlds();
		completeLoad();
		
		// The snapshot queue and regen list depend on loaded Towny objects.
		loadSnapshotQueue();
		loadRegenList();
	}
	
	public final void saveAll() {
		TownyUniverse.getInstance().getTownBlocks().forEach(this::save);
		TownyUniverse.getInstance().getTowns().forEach(this::save);
		TownyUniverse.getInstance().getNations().forEach(this::save);
		TownyUniverse.getInstance().getWorlds().forEach(this::save);
		TownyUniverse.getInstance().getResidents().forEach(this::save);
	}
}
