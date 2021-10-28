package com.palmergames.bukkit.towny.war.eventwar;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
import com.palmergames.bukkit.towny.war.eventwar.db.FlatfileDatabase;
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

		FlatfileDatabase.createFolders();
		FlatfileDatabase.loadWarList();
		FlatfileDatabase.loadWars();
		
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
		FlatfileDatabase.deleteWar(war);
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

}
