package com.palmergames.bukkit.towny.war.eventwar;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.command.commandobjects.RebelCommand;
import com.palmergames.bukkit.towny.command.commandobjects.StateCommand;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.war.eventwar.command.TownRedeemAddon;
import com.palmergames.bukkit.towny.war.eventwar.command.TownyAdminWarAddon;
import com.palmergames.bukkit.towny.war.eventwar.command.TownyWarAddon;
import com.palmergames.bukkit.towny.war.eventwar.db.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.db.WarMetaDataLoader;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarBukkitListener;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarNationListener;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarPVPListener;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarTownListener;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarTownyActionListener;
import com.palmergames.bukkit.towny.war.eventwar.listeners.EventWarTownyListener;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

public class WarUniverse {

	private static WarUniverse instance;
    private Map<UUID, War> wars = new ConcurrentHashMap<UUID, War>();

	// EventWar Listeners
	private final EventWarBukkitListener warBukkitListener = new EventWarBukkitListener();
	private final EventWarNationListener warNationListener = new EventWarNationListener();
	private final EventWarPVPListener warPVPListener = new EventWarPVPListener();
	private final EventWarTownListener warTownListener = new EventWarTownListener();
	private final EventWarTownyActionListener warActionListener = new EventWarTownyActionListener(Towny.getPlugin());
	private final EventWarTownyListener warTownyListener = new EventWarTownyListener();
    
	
	public static WarUniverse getInstance() {
		if (instance == null) {
            instance = new WarUniverse();
        }
        return instance;
	}

	public void clearAllObjects() {
		wars.clear();
	}
	
	public void load() {
		createFolders();
		loadWarList();
		loadWars();
		
		
		WarMetaDataLoader.initialize(Towny.getPlugin());

		WarMetaDataLoader.loadAll();
		
		registerEventWarListeners(Bukkit.getPluginManager());
		
		registerCommands();
		
		registerSpecialCommands();
		
		EventWarSettings.loadWarMaterialsLists();
	}

	// Event War's Listeners registered here.
	private void registerEventWarListeners(PluginManager pluginManager) {
		pluginManager.registerEvents(warBukkitListener, Towny.getPlugin());
		pluginManager.registerEvents(warNationListener, Towny.getPlugin());
		pluginManager.registerEvents(warPVPListener, Towny.getPlugin());
		pluginManager.registerEvents(warTownListener, Towny.getPlugin());
		pluginManager.registerEvents(warTownyListener, Towny.getPlugin());
		pluginManager.registerEvents(warActionListener, Towny.getPlugin());
	}
	
	private static void registerCommands() {
		new TownyWarAddon();
		new TownRedeemAddon();
		new TownyAdminWarAddon(Towny.getPlugin());
	}
	
	private void registerSpecialCommands() {
		List<Command> commands = new ArrayList<>(2);
		commands.add(new StateCommand("state"));
		commands.add(new RebelCommand("rebel"));
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			commandMap.registerAll("towny", commands);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new TownyInitException("An issue has occured while registering war commands.", TownyInitException.TownyError.OTHER, e);
		}
	}
	
	/*
	 * War Stuff
	 */

	/**
	 * Used in loading only.
	 * 
	 * @param uuid UUID of the given war, taken from the War filename.
	 */
	public void newWarInternal(String uuid) {
		War war = new War(Towny.getPlugin(), UUID.fromString(uuid));
		addWar(war);
	}

	public void addWar(War war) {
		if (war.getWarUUID() == null)
			return;
		wars.put(war.getWarUUID(), war);
	}

	public void removeWar(War war) {
		wars.remove(war.getWarUUID());
		deleteWar(war);
		war = null;
	}
	
	@Nullable
	public War getWarEvent(UUID uuid) {
		return wars.get(uuid);
	}

	@Nullable
	public War getWarEvent(Player player) {
		Resident resident = TownyAPI.getInstance().getResident(player.getUniqueId());
		if (resident != null)
			return getWarEvent(resident);
		return null;
	}

	@Nullable
	public War getWarEvent(TownyObject obj) {
		if (obj instanceof TownyWorld)
			return null;
		if (obj instanceof Nation nation)
			obj = nation.getCapital();
		
		String warUUID = WarMetaDataController.getWarUUID(obj);
		if (warUUID != null)
			return getWarEvent(UUID.fromString(warUUID));
		return null;
	}

	public boolean hasWarEvent(TownyObject obj) {
		if (obj instanceof TownyWorld)
			return false;
		if (obj instanceof Nation nation)
			obj = nation.getCapital();

		String warUUID = WarMetaDataController.getWarUUID(obj);
		return warUUID != null;
	}

	public boolean isWarTime() {
		return !wars.isEmpty();
	}

	public Collection<War> getWars() {
		return Collections.unmodifiableCollection(wars.values());
	}

	public List<String> getWarNames() {
		List<String> names = new ArrayList<String>(wars.size());
		for (War war : getWars())
			names.add(war.getWarName());

		return names;
	}
	
	/*
	 * FlatFileDatabase Files
	 */
	
	private void createFolders() {
		FileMgmt.checkOrCreateFolders(
				Towny.getPlugin().getDataFolder() + File.separator + "wars",
				Towny.getPlugin().getDataFolder() + File.separator + "wars" + File.separator + "deleted"
				);
	}
	
	public boolean loadWarList() {
		TownyMessaging.sendDebugMsg("Loading War List");
		File[] warFiles = receiveObjectFiles("wars", ".txt");
		if (warFiles == null)
			return true;
		
		for (File war : warFiles) {
			String uuid = war.getName().replace(".txt", "");
			WarUniverse.getInstance().newWarInternal(uuid);
		}
		
		return true;
	}

	/**
	 * Util method for gathering towny object .txt files from their parent folder.
	 * ex: "residents" 
	 * @param folder - Towny object folder
	 * @param extension - Extension of the filetype to receive objects from.
	 * @return files - Files from inside the residents\towns\nations folder.
	 */
	private File[] receiveObjectFiles(String folder, String extension) {
		return new File(Towny.getPlugin().getDataFolder() + File.separator + folder).listFiles(file -> file.getName().toLowerCase().endsWith(extension));
	}
	
	public boolean loadWars() {
		TownyMessaging.sendDebugMsg("Loading Wars");
		for (War war : WarUniverse.getInstance().getWars()) {
			if (!loadWar(war)) {
				Towny.getPlugin().getLogger().severe("Loading Error: Could not read war data: '" + war.getWarUUID() + "'.");
				return false;
			}
		}
		return true;
	}
	
	
	public boolean loadWar(War war) {
		String line = "";
		String path = getWarFilename(war);
		File warFile = new File(path);
		if (warFile.exists() && warFile.isFile()) {
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(warFile);
			
			line = keys.get("name");
			if (line != null)
				war.setWarName(line);
			
			line = keys.get("type");
			if (line != null)
				war.setWarType(WarType.valueOf(line));
			
			line = keys.get("spoils");
			if (line != null)
				war.setWarSpoils(Double.valueOf(line));
			
			line = keys.get("ignoredUUIDs");
			if (line != null && !line.isEmpty()) {
				String[] uuids = line.split(",");
				List<UUID> list = new ArrayList<>();
				for (String token : uuids)
					list.add(UUID.fromString(token));
				war.getWarParticipants().setIgnoredUUIDs(list);
			}
			line = keys.get("nationsAtStart");
			if (line != null)
				war.setNationsAtStart(Integer.valueOf(line));
			line = keys.get("townsAtStart");
			if (line != null)
				war.setTownsAtStart(Integer.valueOf(line));
			line = keys.get("residentsAtStart");
			if (line != null)
				war.setResidentsAtStart(Integer.valueOf(line));
		}
		
		return true;
	}
	
	public String getWarFilename(War war) {
		return Towny.getPlugin().getDataFolder() + File.separator + "wars" + File.separator + war.getWarUUID() + ".txt";
	}
	
	
	public boolean saveWars() {
		TownyMessaging.sendDebugMsg("Saving Wars");
		for (War war : WarUniverse.getInstance().getWars())
			saveWar(war);
		return true;
	}
	
	public boolean saveWar(War war) {
		
		List<String> list = new ArrayList<>();
		
		list.add("name=" + war.getWarName());
		list.add("type=" + war.getWarType().name());
		list.add("spoils=" + war.getWarSpoils());
		list.add("nationsAtStart=" + war.getNationsAtStart());
		list.add("townsAtStart=" + war.getTownsAtStart());
		list.add("residentsAtStart=" + war.getResidentsAtStart());
		list.add("ignoredUUIDs=" + StringMgmt.join(war.getWarParticipants().getUUIDsToIgnore(),","));
		
//		this.queryQueue.add(new FlatFileSaveTask(list, getWarFilename(war)));
		TownyUniverse.getInstance().getDataSource().saveWar(list, getWarFilename(war));
		return true;
	}
	
	public void deleteWar(War war) {
		File file = new File(getWarFilename(war));
		TownyUniverse.getInstance().getDataSource().deleteWar(file);
	}

}
