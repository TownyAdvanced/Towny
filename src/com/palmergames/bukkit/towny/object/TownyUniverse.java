package com.palmergames.bukkit.towny.object;

import static com.palmergames.bukkit.towny.object.TownyObservableType.COLLECTED_NATION_TAX;
import static com.palmergames.bukkit.towny.object.TownyObservableType.COLLECTED_TONW_TAX;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_DAY;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.NEW_WORLD;
import static com.palmergames.bukkit.towny.object.TownyObservableType.PLAYER_LOGIN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.PLAYER_LOGOUT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_RESIDENT;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.REMOVE_TOWN_BLOCK;
import static com.palmergames.bukkit.towny.object.TownyObservableType.RENAME_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.RENAME_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TELEPORT_REQUEST;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_DAILY_TIMER;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_HEALTH_REGEN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_MOB_REMOVAL;
import static com.palmergames.bukkit.towny.object.TownyObservableType.TOGGLE_TELEPORT_WARMUP;
import static com.palmergames.bukkit.towny.object.TownyObservableType.UPKEEP_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.UPKEEP_TOWN;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_CLEARED;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_END;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_SET;
import static com.palmergames.bukkit.towny.object.TownyObservableType.WAR_START;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import javax.naming.InvalidNameException;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.EmptyNationException;
import com.palmergames.bukkit.towny.EmptyTownException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUtil;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.db.TownyFlatFileSource;
import com.palmergames.bukkit.towny.db.TownyHModFlatFileSource;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.tasks.DailyTimerTask;
import com.palmergames.bukkit.towny.tasks.HealthRegenTimerTask;
import com.palmergames.bukkit.towny.tasks.MobRemovalTimerTask;
import com.palmergames.bukkit.towny.tasks.ProtectionRegenTask;
import com.palmergames.bukkit.towny.tasks.RepeatingTimerTask;
import com.palmergames.bukkit.towny.tasks.SetDefaultModes;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.war.War;
import com.palmergames.bukkit.towny.war.WarSpoils;
import com.palmergames.bukkit.util.MinecraftTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.TimeMgmt;


public class TownyUniverse extends TownyObject {
	public static Towny plugin;
    private Hashtable<String, Resident> residents = new Hashtable<String, Resident>();
    private Hashtable<String, Town> towns = new Hashtable<String, Town>();
    private Hashtable<String, Nation> nations = new Hashtable<String, Nation>();
    private static Hashtable<String, TownyWorld> worlds = new Hashtable<String, TownyWorld>();
    private Hashtable<BlockLocation, ProtectionRegenTask> protectionRegenTasks = new Hashtable<BlockLocation, ProtectionRegenTask>();
    private Set<Block> protectionPlaceholders = new HashSet<Block>();
    //private static Hashtable<String, PlotBlockData> PlotChunks = new Hashtable<String, PlotBlockData>();

    // private List<Election> elections;
    private static TownyDataSource dataSource;
    private static CachePermissions cachePermissions = new CachePermissions();
    private static TownyPermissionSource permissionSource;
    
    private int townyRepeatingTask = -1;
    private int dailyTask = -1;
    private int mobRemoveTask = -1;
    private int healthRegenTask = -1;
    private int teleportWarmupTask = -1;
    private War warEvent;
    private String rootFolder;
    
    public TownyUniverse() {
            setName("");
            rootFolder = "";
    }
    
    public TownyUniverse(String rootFolder) {
            setName("");
            this.rootFolder = rootFolder;
    }
    
    public TownyUniverse(Towny plugin) {
            setName("");
            TownyUniverse.plugin = plugin;
    }
    
    public void newDay() {
            if (!isDailyTimerRunning())
                    toggleDailyTimer(true);
            //dailyTimer.schedule(new DailyTimerTask(this), 0);
            if (getPlugin().getServer().getScheduler().scheduleAsyncDelayedTask(getPlugin(), new DailyTimerTask(this)) == -1)
            	TownyMessaging.sendErrorMsg("Could not schedule newDay.");
            setChanged();
            notifyObservers(NEW_DAY);
    }
    
    public void toggleTownyRepeatingTimer(boolean on) {
        if (on && !isTownyRepeatingTaskRunning()) {
        	townyRepeatingTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new RepeatingTimerTask(this), 0, MinecraftTools.convertToTicks(TownySettings.getPlotManagementSpeed()));
            if (townyRepeatingTask == -1)
            	TownyMessaging.sendErrorMsg("Could not schedule Towny Timer Task.");
        } else if (!on && isTownyRepeatingTaskRunning()) {
        	getPlugin().getServer().getScheduler().cancelTask(townyRepeatingTask);
            townyRepeatingTask = -1;
        }
        setChanged();
	}
        
    public void toggleMobRemoval(boolean on) {
        if (on && !isMobRemovalRunning()) {
                mobRemoveTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new MobRemovalTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getMobRemovalSpeed()));
                if (mobRemoveTask == -1)
                	TownyMessaging.sendErrorMsg("Could not schedule mob removal loop.");
        } else if (!on && isMobRemovalRunning()) {
                getPlugin().getServer().getScheduler().cancelTask(mobRemoveTask);
                mobRemoveTask = -1;
        }
        setChanged();
        notifyObservers(TOGGLE_MOB_REMOVAL);
	}
	
	public void toggleDailyTimer(boolean on) {
		if (on && !isDailyTimerRunning()) {
            long timeTillNextDay = TownyUtil.townyTime();
            TownyMessaging.sendMsg("Time until a New Day: " + TimeMgmt.formatCountdownTime(timeTillNextDay));
            dailyTask = getPlugin().getServer().getScheduler().scheduleAsyncRepeatingTask(getPlugin(), new DailyTimerTask(this), MinecraftTools.convertToTicks(timeTillNextDay), MinecraftTools.convertToTicks(TownySettings.getDayInterval()));
            if (dailyTask == -1)
            	TownyMessaging.sendErrorMsg("Could not schedule new day loop.");
		} else if (!on && isDailyTimerRunning()) {
            getPlugin().getServer().getScheduler().cancelTask(dailyTask);
            dailyTask = -1;
		}
		setChanged();
		notifyObservers(TOGGLE_DAILY_TIMER);
	}
	
	public void toggleHealthRegen(boolean on) {
		if (on && !isHealthRegenRunning()) {
            healthRegenTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new HealthRegenTimerTask(this, plugin.getServer()), 0, MinecraftTools.convertToTicks(TownySettings.getHealthRegenSpeed()));
            if (healthRegenTask == -1)
            	TownyMessaging.sendErrorMsg("Could not schedule health regen loop.");
		} else if (!on && isHealthRegenRunning()) {
            getPlugin().getServer().getScheduler().cancelTask(healthRegenTask);
            healthRegenTask = -1;
		}
		setChanged();
		notifyObservers(TOGGLE_HEALTH_REGEN);
	}
	
	public void toggleTeleportWarmup(boolean on) {
		if (on && !isTeleportWarmupRunning()) {
			teleportWarmupTask = getPlugin().getServer().getScheduler().scheduleSyncRepeatingTask(getPlugin(), new TeleportWarmupTimerTask(this), 0, 20);
			if (teleportWarmupTask == -1)
				TownyMessaging.sendErrorMsg("Could not schedule teleport warmup loop.");
		} else if (!on && isTeleportWarmupRunning()) {
			getPlugin().getServer().getScheduler().cancelTask(teleportWarmupTask);
			teleportWarmupTask = -1;
		}
	    setChanged();
	    notifyObservers(TOGGLE_TELEPORT_WARMUP);
	}
        
	public boolean isTownyRepeatingTaskRunning() {
		return townyRepeatingTask != -1;

    }
	public boolean isMobRemovalRunning() {
		return mobRemoveTask != -1;
	}
        
	public boolean isDailyTimerRunning() {
        return dailyTask != -1;
	}
	
	public boolean isHealthRegenRunning() {
		return healthRegenTask != -1;
	}

    public boolean isTeleportWarmupRunning() {
        return teleportWarmupTask != -1;
    }

        public void onLogin(Player player) throws AlreadyRegisteredException, NotRegisteredException {
                Resident resident;
                
                // Test and kick any players with invalid names.
                if ((player.getName().trim() == null) || (player.getName().contains(" "))) {
                	player.kickPlayer("Invalid name!");
                	return;
                }
                
                if (!hasResident(player.getName())) {
                    newResident(player.getName());
                    resident = getResident(player.getName());
                        
                    TownyMessaging.sendMessage(player, TownySettings.getRegistrationMsg(player.getName()));
                    resident.setRegistered(System.currentTimeMillis());
                    if (!TownySettings.getDefaultTownName().equals(""))
                        try {
                            Town town = getTown(TownySettings.getDefaultTownName());
                            town.addResident(resident);
                            getDataSource().saveTown(town);
                        } catch (NotRegisteredException e) {
                        } catch (AlreadyRegisteredException e) {
                        }
                        
                        getDataSource().saveResident(resident);
                        getDataSource().saveResidentList();

                } else {
                    resident = getResident(player.getName());
                    resident.setLastOnline(System.currentTimeMillis());
                        
                    getDataSource().saveResident(resident);
                }
                

                try {
                	TownyMessaging.sendTownBoard(player, resident.getTown());
                } catch (NotRegisteredException e) {
                }

                if (isWarTime())
                        getWarEvent().sendScores(player, 3);
                
                //Schedule to setup default modes when the player has finished loading
                if (getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), new SetDefaultModes(this, player, false),1) == -1)
                	TownyMessaging.sendErrorMsg("Could not set default modes for " + player.getName() + ".");
                
                
                
        setChanged();
        notifyObservers(PLAYER_LOGIN);
        }

        public void onLogout(Player player) {
                try {
                        Resident resident = getResident(player.getName());
                        resident.setLastOnline(System.currentTimeMillis());
                        getDataSource().saveResident(resident);
                } catch (NotRegisteredException e) {
                }
        setChanged();
        notifyObservers(PLAYER_LOGOUT);
        }
        
        /**
         * Teleports the player to his town's spawn location. If town doesn't have a
         * spawn or player has no town, and teleport is forced, then player is sent
         * to the world's spawn location.
         * 
         * @param player
         * @param 
         */
        /*
        public void townSpawn(Player player, boolean forceTeleport) {
                try {
                        Resident resident = plugin.getTownyUniverse().getResident(player.getName());
                        Town town = resident.getTown();
                        player.teleport(town.getSpawn());
                        //show message if we are using Economy and are charging for spawn travel.
                        if (!plugin.isTownyAdmin(player) && TownySettings.isUsingEconomy() && TownySettings.getTownSpawnTravelPrice() != 0)
                                plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_cost_spawn"),
                                                TownySettings.getTownSpawnTravelPrice() + TownyEconomyObject.getEconomyCurrency()));
                        //player.teleportTo(town.getSpawn());
                } catch (TownyException x) {
                        if (forceTeleport) {
                                player.teleport(player.getWorld().getSpawnLocation());
                                //player.teleportTo(player.getWorld().getSpawnLocation());
                                plugin.sendDebugMsg("onTownSpawn: [forced] "+player.getName());
                        } else
                                plugin.sendErrorMsg(player, x.getError());
                }
        }
        */
        
        public Location getTownSpawnLocation(Player player) throws TownyException {
                try {
                        Resident resident = getResident(player.getName());
                        Town town = resident.getTown();
                        return town.getSpawn();
                } catch (TownyException x) {
                        throw new TownyException("Unable to get spawn location");
                }
        }

        public void newResident(String name) throws AlreadyRegisteredException, NotRegisteredException {
                String filteredName;
                try {
                        filteredName = checkAndFilterName(name);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                
                if (residents.containsKey(filteredName.toLowerCase()))
                        throw new AlreadyRegisteredException("A resident with the name " + filteredName + " is already in use.");
                
                residents.put(filteredName.toLowerCase(), new Resident(filteredName));
        setChanged();
        notifyObservers(NEW_RESIDENT);
        }

        public void newTown(String name) throws AlreadyRegisteredException, NotRegisteredException {
                String filteredName;
                try {
                        filteredName = checkAndFilterName(name);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                
                if (towns.containsKey(filteredName.toLowerCase()))
                        throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");
                
                towns.put(filteredName.toLowerCase(), new Town(filteredName));
        setChanged();
        notifyObservers(NEW_TOWN);
        }
        
        /**
         * Returns the world a town belongs to
         * 
         * @param town
         * @return
         */
        public static TownyWorld getTownWorld(String townName) {
        	
        	for (TownyWorld world: worlds.values()){
        		if (world.hasTown(townName))
        			return world;
        	}

        	return null;
        }

        public void newNation(String name) throws AlreadyRegisteredException, NotRegisteredException {
                String filteredName;
                try {
                        filteredName = checkAndFilterName(name);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                
                if (nations.containsKey(filteredName.toLowerCase()))
                        throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");
                
                nations.put(filteredName.toLowerCase(), new Nation(filteredName));
        setChanged();
        notifyObservers(NEW_NATION);
        }

        public void newWorld(String name) throws AlreadyRegisteredException, NotRegisteredException {
                String filteredName = name;
                /*
                try {
                        filteredName = checkAndFilterName(name);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                */
                if (worlds.containsKey(filteredName.toLowerCase()))
                        throw new AlreadyRegisteredException("The world " + filteredName + " is already in use.");
                
                worlds.put(filteredName.toLowerCase(), new TownyWorld(filteredName));
        setChanged();
        notifyObservers(NEW_WORLD);
        }
        
        public String checkAndFilterName(String name) throws InvalidNameException {
                String out = TownySettings.filterName(name);
                
                if (!TownySettings.isValidName(out))
                        throw new InvalidNameException(out + " is an invalid name.");
                
                return out;
        }
        
        public String[] checkAndFilterArray(String[] arr){
        	String[] out = arr;
        	int count = 0;
        	
        	for (String word: arr) {
        		out[count] = TownySettings.filterName(word);
        		count++;
        	}
            
            return out;
    }
        

        public boolean hasResident(String name) {
                try {
					return residents.containsKey(checkAndFilterName(name).toLowerCase());
				} catch (InvalidNameException e) {
					return false;
				}
        }

        public boolean hasTown(String name) {
                return towns.containsKey(name.toLowerCase());
        }
        
        public boolean hasNation(String name) {
                return nations.containsKey(name.toLowerCase());
        }

        public void renameTown(Town town, String newName) throws AlreadyRegisteredException, NotRegisteredException {
                
                String filteredName;
                try {
                        filteredName = checkAndFilterName(newName);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                
                if (hasTown(filteredName))
                        throw new AlreadyRegisteredException("The town " + filteredName + " is already in use.");

                // TODO: Delete/rename any invites.

                List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
                
                //Tidy up old files
                // Has to be done here else the town no longer exists and the move command may fail.
                getDataSource().deleteTown(town);
                
                String oldName = town.getName();
                towns.remove(oldName.toLowerCase());
                town.setName(filteredName);
                
                towns.put(filteredName.toLowerCase(), town);
                
                //Check if this is a nation capitol
                if (town.isCapital()) {
                	Nation nation = town.getNation();
                	nation.setCapital(town);
                	getDataSource().saveNation(nation);
                }
                
                Town oldTown = new Town(oldName);
                
                try {
                        town.pay(town.getHoldingBalance(), "Rename Town - Empty account of new town name.");
                        oldTown.payTo(oldTown.getHoldingBalance(), town, "Rename Town - Transfer to new account");
                } catch (EconomyException e) {
                }
                
                for (Resident resident : toSave) {
                        getDataSource().saveResident(resident);
                }
                
                getDataSource().saveTown(town);
                getDataSource().saveTownList();
                getDataSource().saveWorld(town.getWorld());

                setChanged();
        notifyObservers(RENAME_TOWN);
        }
        
        public void renameNation(Nation nation, String newName) throws AlreadyRegisteredException, NotRegisteredException {
                
                String filteredName;
                try {
                        filteredName = checkAndFilterName(newName);
                } catch (InvalidNameException e) {
                        throw new NotRegisteredException(e.getMessage());
                }
                
                if (hasNation(filteredName))
                        throw new AlreadyRegisteredException("The nation " + filteredName + " is already in use.");

                // TODO: Delete/rename any invites.

                List<Town> toSave = new ArrayList<Town>(nation.getTowns());
                
                String oldName = nation.getName();
                nations.put(filteredName.toLowerCase(), nation);
                //Tidy up old files
                getDataSource().deleteNation(nation);
                                
                nations.remove(oldName.toLowerCase());
                nation.setName(filteredName);
                Nation oldNation = new Nation(oldName);
                
                if (plugin.isEcoActive())
	                try {
	                        nation.pay(nation.getHoldingBalance(), "Rename Nation - Empty account of new nation name.");
	                        oldNation.payTo(oldNation.getHoldingBalance(), nation, "Rename Nation - Transfer to new account");
	                } catch (EconomyException e) {
	                }
                
                for (Town town : toSave) {
                        getDataSource().saveTown(town);
                }
                
                getDataSource().saveNation(nation);
                getDataSource().saveNationList();
                
                //search and update all ally/enemy lists
                List<Nation> toSaveNation = new ArrayList<Nation>(getNations());
                for (Nation toCheck : toSaveNation)
                        if (toCheck.hasAlly(oldNation) || toCheck.hasEnemy(oldNation)) {
                                try {
                                        if (toCheck.hasAlly(oldNation)) {
                                                toCheck.removeAlly(oldNation);
                                                toCheck.addAlly(nation);
                                        } else {
                                                toCheck.removeEnemy(oldNation);
                                                toCheck.addEnemy(nation);
                                        }
                                } catch (NotRegisteredException e) {
                                        // TODO Auto-generated catch block
                                        e.printStackTrace();
                                }
                        }
                        else
                                toSave.remove(toCheck);
                
                for (Nation toCheck : toSaveNation)
                        getDataSource().saveNation(toCheck);            
                
                setChanged();
        notifyObservers(RENAME_NATION);
        }

        public Resident getResident(String name) throws NotRegisteredException {
                Resident resident = null;
				try {
					resident = residents.get(checkAndFilterName(name).toLowerCase());
				} catch (InvalidNameException e) {
				}
                if (resident == null)
                        throw new NotRegisteredException(name + " is not registered.");
                
                /*
                {
                        // Attempt to load the resident and fix the files.
                        
                        try {
                                newResident(name);
                                resident = residents.get(name.toLowerCase());
                                getDataSource().loadResident(resident);
                                //getDataSource().saveTown(resident.getTown());
                                getDataSource().saveResidentList();
                        } catch (AlreadyRegisteredException e) {
                                throw new NotRegisteredException("Failed to re-register " + name);
                        } catch (NotRegisteredException e) {
                                throw new NotRegisteredException(name + " is not registered.");
                        }
                        
                        //
                }
                */
                return resident;
        }

        /*
        @Deprecated
        public void sendMessage(Player player, List<String> lines) {
                sendMessage(player, lines.toArray(new String[0]));
        }
        @Deprecated
        public void sendTownMessage(Town town, List<String> lines) {
                sendTownMessage(town, lines.toArray(new String[0]));
        }
        @Deprecated
        public void sendNationMessage(Nation nation, List<String> lines) {
                sendNationMessage(nation, lines.toArray(new String[0]));
        }
        @Deprecated
        public void sendGlobalMessage(List<String> lines) {
                sendGlobalMessage(lines.toArray(new String[0]));
        }
        @Deprecated
        public void sendGlobalMessage(String line) {
                for (Player player : getOnlinePlayers()) {
                        player.sendMessage(line);
                        plugin.log("[Global Message] " + player.getName() + ": " + line);
                }
        }
        @Deprecated
        public void sendMessage(Player player, String[] lines) {
                for (String line : lines) {
                        player.sendMessage(line);
                        //plugin.log("[send Message] " + player.getName() + ": " + line);
                }
        }
        @Deprecated
        public void sendResidentMessage(Resident resident, String[] lines) throws TownyException {
                for (String line : lines)
                        plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
                Player player = getPlayer(resident);
                for (String line : lines)
                        player.sendMessage(line);
                
        }
        @Deprecated
        public void sendTownMessage(Town town, String[] lines) {
                for (String line : lines)
                        plugin.log("[Town Msg] " + town.getName() + ": " + line);
                for (Player player : getOnlinePlayers(town)){
                        for (String line : lines)
                                player.sendMessage(line);
                }
        }
        @Deprecated
        public void sendNationMessage(Nation nation, String[] lines) {
                for (String line : lines)
                        plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
                for (Player player : getOnlinePlayers(nation))
                        for (String line : lines)
                                player.sendMessage(line);
        }
        @Deprecated
        public void sendGlobalMessage(String[] lines) {
                for (String line : lines)
                        plugin.log("[Global Msg] " + line);
                for (Player player : getOnlinePlayers())
                        for (String line : lines)
                                player.sendMessage(line);
        }
        @Deprecated
        public void sendResidentMessage(Resident resident, String line) throws TownyException {
                plugin.log("[Resident Msg] " + resident.getName() + ": " + line);
                Player player = getPlayer(resident);
                player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
        }
        @Deprecated
        public void sendTownMessage(Town town, String line) {
                plugin.log("[Town Msg] " + town.getName() + ": " + line);
                for (Player player : getOnlinePlayers(town))
                        player.sendMessage(TownySettings.getLangString("default_towny_prefix") + line);
        }
        @Deprecated
        public void sendNationMessage(Nation nation, String line) {
                plugin.log("[Nation Msg] " + nation.getName() + ": " + line);
                for (Player player : getOnlinePlayers(nation))
                        player.sendMessage(line);
        }
        @Deprecated
        public void sendTownBoard(Player player, Town town) {
                for (String line : ChatTools.color(Colors.Gold + "[" + town.getName() + "] " + Colors.Yellow + town.getTownBoard()))
                        player.sendMessage(line);
        }
        */
        
        public Player getPlayer(Resident resident) throws TownyException {
            for (Player player : getOnlinePlayers())
                    if (player.getName().equals(resident.getName()))
                            return player;
            throw new TownyException("Resident is not online");
        }

        public Player[] getOnlinePlayers() {
                return plugin.getServer().getOnlinePlayers();
        }

        public List<Player> getOnlinePlayers(ResidentList residents) {
                ArrayList<Player> players = new ArrayList<Player>();
                for (Player player : getOnlinePlayers())
                        if (residents.hasResident(player.getName()))
                                players.add(player);
                return players;
        }
        
        public List<Player> getOnlinePlayers(Town town) {
                ArrayList<Player> players = new ArrayList<Player>();
                for (Player player : getOnlinePlayers())
                        if (town.hasResident(player.getName()))
                                players.add(player);
                return players;
        }

        public List<Player> getOnlinePlayers(Nation nation) {
                ArrayList<Player> players = new ArrayList<Player>();
                for (Town town : nation.getTowns())
                        players.addAll(getOnlinePlayers(town));
                return players;
        }
        
        /** isWilderness
         * 
         * returns true if this block is in the wilderness
         * 
         * @param block
         * @return
         */
        public boolean isWilderness(Block block) {
                
                WorldCoord worldCoord;
                
                try {
                        worldCoord = new WorldCoord(getWorld(block.getWorld().getName()), Coord.parseCoord(block));
                } catch (NotRegisteredException e) {
                        // No record so must be Wilderness
                        return true;
                }
                
                try {
                        return worldCoord.getTownBlock().getTown() == null;
                } catch (NotRegisteredException e) {
                        // Must be wilderness
                        return true;
                }

        }
        
        /** getTownName
         * 
         * returns the name of the Town this location lies within
         * if no town is registered it returns null
         * 
         * @param loc
         * @return
         */
        public String getTownName(Location loc) {
                
                try {
                        WorldCoord worldCoord = new WorldCoord(getWorld(loc.getWorld().getName()), Coord.parseCoord(loc));
                        return worldCoord.getTownBlock().getTown().getName();
                } catch (NotRegisteredException e) {
                        // No data so return null
                        return null;
                }
                
                
        }
        
        /** getTownBlock
         * 
         * returns TownBlock this location lies within
         * if no block is registered it returns null
         * 
         * @param loc
         * @return
         */
        public TownBlock getTownBlock(Location loc) {
                
        	TownyMessaging.sendDebugMsg("Fetching TownBlock");
        	
                try {
                        WorldCoord worldCoord = new WorldCoord(getWorld(loc.getWorld().getName()), Coord.parseCoord(loc));
                        return worldCoord.getTownBlock();
                } catch (NotRegisteredException e) {
                        // No data so return null
                        return null;
                }     
        }

        public List<Resident> getResidents() {
                return new ArrayList<Resident>(residents.values());
        }

        public Set<String> getResidentKeys() {
                return residents.keySet();
        }
        
        public Set<String> getTownsKeys() {
            return towns.keySet();
        }
        
        public Set<String> getNationsKeys() {
            return nations.keySet();
        }

        public List<Town> getTowns() {
                return new ArrayList<Town>(towns.values());
        }

        public List<Nation> getNations() {
                return new ArrayList<Nation>(nations.values());
        }

        public List<TownyWorld> getWorlds() {
                return new ArrayList<TownyWorld>(worlds.values());
        }
        
        public List<Town> getTownsWithoutNation() {
                List<Town> townFilter = new ArrayList<Town>();
                for (Town town : getTowns())
                        if (!town.hasNation())
                                townFilter.add(town);
                return townFilter;
        }
        
        public List<Resident> getResidentsWithoutTown() {
                List<Resident> residentFilter = new ArrayList<Resident>();
                for (Resident resident : getResidents())
                        if (!resident.hasTown())
                                residentFilter.add(resident);
                return residentFilter;
        }

        public List<Resident> getActiveResidents() {
                List<Resident> activeResidents = new ArrayList<Resident>();
                for (Resident resident : getResidents())
                        if (isActiveResident(resident))
                                activeResidents.add(resident);
                return activeResidents;
        }

        public boolean isActiveResident(Resident resident) {
                return ((System.currentTimeMillis() - resident.getLastOnline() < (20*TownySettings.getInactiveAfter())) || (plugin.isOnline(resident.getName())));
        }
        
        public List<Resident> getResidents(String[] names) {
                List<Resident> matches = new ArrayList<Resident>();
                for (String name : names)
                        try {
                                matches.add(getResident(name));
                        } catch (NotRegisteredException e) {
                        }
                return matches;
        }
        
        public List<Town> getTowns(String[] names) {
                List<Town> matches = new ArrayList<Town>();
                for (String name : names)
                        try {
                                matches.add(getTown(name));
                        } catch (NotRegisteredException e) {
                        }
                return matches;
        }
        
        public List<Nation> getNations(String[] names) {
                List<Nation> matches = new ArrayList<Nation>();
                for (String name : names)
                        try {
                                matches.add(getNation(name));
                        } catch (NotRegisteredException e) {
                        }
                return matches;
        }
        
        public List<String> getStatus(TownBlock townBlock) {
            return TownyFormatter.getStatus(townBlock);
        }
        
        public List<String> getStatus(Resident resident) {
                return TownyFormatter.getStatus(resident);
        }

        public List<String> getStatus(Town town) {
                return TownyFormatter.getStatus(town);
        }

        public List<String> getStatus(Nation nation) {
                return TownyFormatter.getStatus(nation);
        }
        
        public List<String> getStatus(TownyWorld world) {
                return TownyFormatter.getStatus(world);
        }
        
        public List<String> getTaxStatus(Resident resident) {
            return TownyFormatter.getTaxStatus(resident);
    }

        public Town getTown(String name) throws NotRegisteredException {
                Town town = towns.get(name.toLowerCase());
                if (town == null)
                        throw new NotRegisteredException(name + " is not registered.");
                return town;
        }

        public Nation getNation(String name) throws NotRegisteredException {
                Nation nation = nations.get(name.toLowerCase());
                if (nation == null)
                        throw new NotRegisteredException(name + " is not registered.");
                return nation;
        }
        
        public String getRootFolder() {
                if (plugin != null)
                        return plugin.getDataFolder().getPath();
                else
                        return rootFolder;
        }

        public boolean loadSettings() {
                try {
                        FileMgmt.checkFolders(new String[]{
                                        getRootFolder(),
                                        getRootFolder() + FileMgmt.fileSeparator() + "settings",
                                        getRootFolder() + FileMgmt.fileSeparator() + "logs"}); // Setup the logs folder here as the logger will not yet be enabled.

                        TownySettings.loadConfig(getRootFolder() + FileMgmt.fileSeparator() + "settings" + FileMgmt.fileSeparator() + "config.yml", plugin.getVersion());
                        TownySettings.loadLanguage(getRootFolder() + FileMgmt.fileSeparator() + "settings", "english.yml");

                } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        return false;
                } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                }
                
                // Setup any defaults before we load the database.
                Coord.setCellSize(TownySettings.getTownBlockSize());
                
                System.out.println("[Towny] Database: [Load] " + TownySettings.getLoadDatabase() + " [Save] " + TownySettings.getSaveDatabase());
                if (!loadDatabase(TownySettings.getLoadDatabase())) {
                        System.out.println("[Towny] Error: Failed to load!");
                        return false;
                }
                
                try {
                		getDataSource().cleanupBackups();
                        setDataSource(TownySettings.getSaveDatabase());
                        getDataSource().initialize(plugin, this);
                        try {
                                getDataSource().backup();
                                getDataSource().deleteUnusedResidentFiles();
                        } catch (IOException e) {
                                System.out.println("[Towny] Error: Could not create backup.");
                                e.printStackTrace();
                                return false;
                        }
                        
                        //if (TownySettings.isSavingOnLoad())
                        //      townyUniverse.getDataSource().saveAll();
                } catch (UnsupportedOperationException e) {
                        System.out.println("[Towny] Error: Unsupported save format!");
                        return false;
                }
                
                
                return true;
        }

        public boolean loadDatabase(String databaseType) {
                try {
                        setDataSource(databaseType);
                } catch (UnsupportedOperationException e) {
                        return false;
                }

                getDataSource().initialize(plugin, this);
                
                // make sure all tables are clear before loading
                worlds.clear();
                nations.clear();
                towns.clear();
                residents.clear();
                
                return getDataSource().loadAll();
        }

        public static TownyWorld getWorld(String name) throws NotRegisteredException {
                TownyWorld world = worlds.get(name.toLowerCase());
                /*
                if (world == null) {
                        try {
                                newWorld(name);
                        } catch (AlreadyRegisteredException e) {
                                throw new NotRegisteredException("Not registered, but already registered when trying to register.");
                        } catch (NotRegisteredException e) {
                                e.printStackTrace();
                        }
                        world = worlds.get(name.toLowerCase());
                        */
                        if (world == null)
                        	throw new NotRegisteredException("World not registered!");
                                //throw new NotRegisteredException("Could not create world " + name.toLowerCase());
                //}
                return world;
        }
        
        public boolean isAlly(String a, String b) {
                try {
                        Resident residentA = getResident(a);
                        Resident residentB = getResident(b);
                        if (residentA.getTown() == residentB.getTown())
                                return true;
                        if (residentA.getTown().getNation() == residentB.getTown().getNation())
                                return true;
                        if (residentA.getTown().getNation().hasAlly(residentB.getTown().getNation()))
                                return true;
                } catch (NotRegisteredException e) {
                        return false;
                }
                return false;
        }

        public boolean isAlly(Town a, Town b) {
                try {
                        if (a == b)
                                return true;
                        if (a.getNation() == b.getNation())
                                return true;
                        if (a.getNation().hasAlly(b.getNation()))
                                return true;
                } catch (NotRegisteredException e) {
                        return false;
                }
                return false;
        }
        
        public boolean canAttackEnemy(String a, String b) {
	        try {
                Resident residentA = getResident(a);
                Resident residentB = getResident(b);
                if (residentA.getTown() == residentB.getTown())
                    return false;
                if (residentA.getTown().getNation() == residentB.getTown().getNation())
                    return false;
                Nation nationA = residentA.getTown().getNation();
                Nation nationB = residentB.getTown().getNation();
                if (nationA.isNeutral() || nationB.isNeutral())
                	return false;
                if (nationA.hasEnemy(nationB))
                    return true;
	        } catch (NotRegisteredException e) {
                return false;
	        }
	        return false;
        }
        
        public boolean isEnemy(String a, String b) {
		        try {
		                Resident residentA = getResident(a);
		                Resident residentB = getResident(b);
		                if (residentA.getTown() == residentB.getTown())
		                        return false;
		                if (residentA.getTown().getNation() == residentB.getTown().getNation())
		                        return false;
		                if (residentA.getTown().getNation().hasEnemy(residentB.getTown().getNation()))
		                        return true;
		        } catch (NotRegisteredException e) {
		                return false;
		        }
		        return false;
		}
		
		public boolean isEnemy(Town a, Town b) {
		        try {
		                if (a == b)
		                        return false;
		                if (a.getNation() == b.getNation())
		                        return false;
		                if (a.getNation().hasEnemy(b.getNation()))
		                        return true;
		        } catch (NotRegisteredException e) {
		                return false;
		        }
		        return false;
		}
		
        public void setDataSource(String databaseType) throws UnsupportedOperationException {
                if (databaseType.equalsIgnoreCase("flatfile"))
                        setDataSource(new TownyFlatFileSource());
                else if (databaseType.equalsIgnoreCase("flatfile-hmod"))
                        setDataSource(new TownyHModFlatFileSource());
                else
                        throw new UnsupportedOperationException();
        }
        
        public void setDataSource(TownyDataSource dataSource) {
                TownyUniverse.dataSource = dataSource;
        }

        public static TownyDataSource getDataSource() {
                return dataSource;
        }
        
    public void setPermissionSource(TownyPermissionSource permissionSource) {
        TownyUniverse.permissionSource = permissionSource;
    }

    public static TownyPermissionSource getPermissionSource() {
        return permissionSource;
    }
	
	public static CachePermissions getCachePermissions() {
	    return cachePermissions;
	}

    public boolean isWarTime() {
    	return warEvent != null ? warEvent.isWarTime() : false;
    }

    public void collectNationTaxes() throws EconomyException {
    	for (Nation nation : new ArrayList<Nation>(nations.values())) {
    		collectNationTaxes(nation);
    	}		
    	setChanged();
    	notifyObservers(COLLECTED_NATION_TAX);
    }

    public void collectNationTaxes(Nation nation) throws EconomyException {
    	if (nation.getTaxes() > 0)
            for (Town town : new ArrayList<Town>(nation.getTowns())) {
            	if (town.isCapital() || !town.hasUpkeep())
                	continue;
                if (!town.payTo(nation.getTaxes(), nation, "Nation Tax")) {
                	try {
                		TownyMessaging.sendNationMessage(nation, TownySettings.getCouldntPayTaxesMsg(town, "nation"));
                        nation.removeTown(town);
                    } catch (EmptyNationException e) {
                        // Always has 1 town (capital) so ignore
                    } catch (NotRegisteredException e) {
                    }
                    getDataSource().saveTown(town);
                    getDataSource().saveNation(nation);
                } else
                	TownyMessaging.sendTownMessage(town, TownySettings.getPayedTownTaxMsg() + nation.getTaxes());
            }
    }

    public void collectTownTaxes() throws EconomyException {
        for (Town town : new ArrayList<Town>(towns.values())) {
                collectTownTaxes(town);
        }
        setChanged();
        notifyObservers(COLLECTED_TONW_TAX);
    }

    public void collectTownTaxes(Town town) throws EconomyException {
    	//Resident Tax
        if (town.getTaxes() > 0)
                for (Resident resident : new ArrayList<Resident>(town.getResidents()))
                        if (town.isMayor(resident) || town.hasAssistant(resident)) {
                                try {
                                	TownyMessaging.sendResidentMessage(resident, TownySettings.getTaxExemptMsg());
                                } catch (TownyException e) {
                                }
                                continue;
                        }
        else if(town.isTaxPercentage())
        {
            double cost = resident.getHoldingBalance() * town.getTaxes()/100;
            resident.payTo(cost, town, "Town Tax (Percentage)");
                                try {
                                        TownyMessaging.sendResidentMessage(resident, TownySettings.getPayedResidentTaxMsg() + cost);
                                } catch (TownyException e) {
                                }
        }
        else if (!resident.payTo(town.getTaxes(), town, "Town Tax")) {
        	TownyMessaging.sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, "town"));
            	try {
            		//town.removeResident(resident);
                    resident.clear();
            } catch (EmptyTownException e) {
            }
            getDataSource().saveResident(resident);
            getDataSource().saveTown(town);
        } else
        	try {
        		TownyMessaging.sendResidentMessage(resident, TownySettings.getPayedResidentTaxMsg() + town.getTaxes());
            } catch (TownyException e1) {
            }
                        
        	//Plot Tax
        	if (town.getPlotTax() > 0 || town.getCommercialPlotTax() > 0) {
                Hashtable<Resident,Integer> townPlots = new Hashtable<Resident,Integer>();
                Hashtable<Resident,Double> townTaxes = new Hashtable<Resident,Double>();
                for (TownBlock townBlock : new ArrayList<TownBlock>(town.getTownBlocks())) {
                    if (!townBlock.hasResident())
                        continue;
                    try {
                        Resident resident = townBlock.getResident();
                        if (town.isMayor(resident) || town.hasAssistant(resident)) {
                            continue;
                        }
                        if (!resident.payTo(townBlock.getType().getTax(town), town, String.format("Plot Tax (%s)", townBlock.getType()))) {
                        	TownyMessaging.sendTownMessage(town,  String.format(TownySettings.getLangString("msg_couldnt_pay_plot_taxes"), resident));
                            townBlock.setResident(null);
                            getDataSource().saveResident(resident);
                            getDataSource().saveWorld(townBlock.getWorld());
                        } else {
                            townPlots.put(resident, (townPlots.containsKey(resident) ? townPlots.get(resident) : 0) + 1);
                            townTaxes.put(resident, (townTaxes.containsKey(resident) ? townTaxes.get(resident) : 0) +
                                    townBlock.getType().getTax(town));
                        }
                    } catch (NotRegisteredException e) {
                    }
                }
                for (Resident resident : townPlots.keySet()) {
                	try {
                		int numPlots = townPlots.get(resident);
                        double totalCost = townTaxes.get(resident);
                        TownyMessaging.sendResidentMessage(resident, String.format(TownySettings.getLangString("msg_payed_plot_cost"), totalCost, numPlots, town.getName()));
                } catch (TownyException e) {
                }
            }
        }
    }

        public void startWarEvent() {
                this.warEvent = new War(plugin, TownySettings.getWarTimeWarningDelay());
        setChanged();
        notifyObservers(WAR_START);
        }
        
        public void endWarEvent() {
                if (isWarTime())
                        warEvent.toggleEnd();
                // Automatically makes warEvent null
        setChanged();
        notifyObservers(WAR_END);
        }
        
        public void clearWarEvent() {
                getWarEvent().cancelTasks(getPlugin().getServer().getScheduler());
                setWarEvent(null);
        setChanged();
        notifyObservers(WAR_CLEARED);
        }
        
        //TODO: throw error if null
        public War getWarEvent() {
                return warEvent;
        }

        public void setWarEvent(War warEvent) {
                this.warEvent = warEvent;
        setChanged();
        notifyObservers(WAR_SET);
        }
        
        public Towny getPlugin() {
                return plugin;
        }

        public void setPlugin(Towny plugin) {
                TownyUniverse.plugin = plugin;
        }

        public void removeWorld(TownyWorld world) throws UnsupportedOperationException {
                getDataSource().deleteWorld(world);
                throw new UnsupportedOperationException();
        }
        
        public void removeNation(Nation nation) {
        	
        	//search and remove from all ally/enemy lists
            List<Nation> toSaveNation = new ArrayList<Nation>();
            for (Nation toCheck : new ArrayList<Nation>(getNations()))
                    if (toCheck.hasAlly(nation) || toCheck.hasEnemy(nation)) {
                            try {
                                    if (toCheck.hasAlly(nation))
                                            toCheck.removeAlly(nation);
                                    else
                                            toCheck.removeEnemy(nation);
                                    
                                    toSaveNation.add(toCheck);
                            } catch (NotRegisteredException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                            }
                    }  
            
            for (Nation toCheck : toSaveNation)
                    getDataSource().saveNation(toCheck);
            
            //Delete nation and save towns
            getDataSource().deleteNation(nation);
            List<Town> toSave = new ArrayList<Town>(nation.getTowns());
            nation.clear();
            if (plugin.isEcoActive())
	            try {
	            	nation.payTo(nation.getHoldingBalance(), new WarSpoils(), "Remove Nation");
	            } catch (EconomyException e) {
	            }
            nations.remove(nation.getName().toLowerCase());
            // Clear accounts
            if(TownySettings.isUsingEconomy())
            	nation.removeAccount();
                                                     
            plugin.updateCache();
            for (Town town : toSave)
            	getDataSource().saveTown(town);
            getDataSource().saveNationList();

            setChanged();
            notifyObservers(REMOVE_NATION);
        }

        ////////////////////////////////////////////
        
        
        public void removeTown(Town town) {
        	
        	removeTownBlocks(town);
        	
            List<Resident> toSave = new ArrayList<Resident>(town.getResidents());
            TownyWorld world = town.getWorld();
            
            try {
                    if (town.hasNation()) {
                            Nation nation = town.getNation();
                            nation.removeTown(town);
                                    
                            getDataSource().saveNation(nation);
                    }
                    town.clear();
            } catch (EmptyNationException e) {
                    removeNation(e.getNation());
                    TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_del_nation"), e.getNation()));
            } catch (NotRegisteredException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
            }
            if (plugin.isEcoActive())
	            try {
	                    town.payTo(town.getHoldingBalance(), new WarSpoils(), "Remove Town");
	            } catch (EconomyException e) {
	            }
            
            for (Resident resident : toSave) {
                    removeResident(resident);
                    getDataSource().saveResident(resident);
            }
            
            towns.remove(town.getName().toLowerCase());
            // Clear accounts
            if(TownySettings.isUsingEconomy())
            	town.removeAccount();
            plugin.updateCache();

            getDataSource().deleteTown(town);
            getDataSource().saveTownList();
            getDataSource().saveWorld(world);

            setChanged();
            notifyObservers(REMOVE_TOWN);
        }

        public void removeResident(Resident resident) {
                
                Town town =  null;
                
                if (resident.hasTown())
                        try {
                                town = resident.getTown();
                        } catch (NotRegisteredException e1) {
                                // TODO Auto-generated catch block
                                e1.printStackTrace();
                        }       
                        
                //getDataSource().deleteResident(resident);
                //residents.remove(resident.getName().toLowerCase());
                try {
                        if (town != null) {     
                                town.removeResident(resident);
                                getDataSource().saveTown(town);
                        }
                        resident.clear();
                } catch (EmptyTownException e) {
                        removeTown(town);

                } catch (NotRegisteredException e) {
                        // TODO Auto-generated catch block
                        // town not registered
                        e.printStackTrace();
                }
                //String name = resident.getName();
                //residents.remove(name.toLowerCase());
                //plugin.deleteCache(name);
                //getDataSource().saveResidentList();
        setChanged();
        notifyObservers(REMOVE_RESIDENT);
        }
        
        
	public void removeResidentList(Resident resident) {
		String name = resident.getName();    
        
        //search and remove from all friends lists
		List<Resident> toSave = new ArrayList<Resident>();

		for (Resident toCheck : new ArrayList<Resident>(getResidents())) {
			TownyMessaging.sendDebugMsg("Checking friends of: " + toCheck.getName());
    		if (toCheck.hasFriend(resident)) {
    			try {
    				TownyMessaging.sendDebugMsg("       - Removing Friend: " + resident.getName());
                    toCheck.removeFriend(resident);
                    toSave.add(toCheck);
                } catch (NotRegisteredException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                }
    		}
    	}
            
    	for (Resident toCheck : toSave)
    		getDataSource().saveResident(toCheck);
        
        //Wipe and delete resident
        try {
                resident.clear();
        } catch (EmptyTownException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
        }
        getDataSource().deleteResident(resident);               
        
        residents.remove(name.toLowerCase());
        // Clear accounts
        if(TownySettings.isUsingEconomy() && TownySettings.isDeleteEcoAccount())
        	resident.removeAccount();
        plugin.deleteCache(name);
        getDataSource().saveResidentList();
          
	}
        
        /////////////////////////////////////////////

	public void removeTownBlock(TownBlock townBlock) {
    	Resident resident = null;
        Town town = null;
        try {
        	resident = townBlock.getResident();
        } catch (NotRegisteredException e) {
        }
        try {
        	town = townBlock.getTown();
        } catch (NotRegisteredException e) {
        }
        TownyWorld world = townBlock.getWorld();
        world.removeTownBlock(townBlock);
        
        getDataSource().saveWorld(world);
        getDataSource().deleteTownBlock(townBlock);
        
        if (resident != null)
        	getDataSource().saveResident(resident);
        if (town != null)
        	getDataSource().saveTown(town);
        
        if (townBlock.getWorld().isUsingPlotManagementDelete())
        	deleteTownBlockIds(townBlock);
        
        // Move the plot to be restored
        if (townBlock.getWorld().isUsingPlotManagementRevert()) {
        	PlotBlockData plotData = TownyRegenAPI.getPlotChunkSnapshot(townBlock);
        	if (plotData != null && !plotData.getBlockList().isEmpty()) {
        		TownyRegenAPI.addPlotChunk(plotData, true);
        	}
        }

        setChanged();
        notifyObservers(REMOVE_TOWN_BLOCK);
    }
        
		public void deleteTownBlockIds(TownBlock townBlock) {
        	
        	//Block block = null;
        	World world = null;
        	int plotSize = TownySettings.getTownBlockSize();
        	
        	TownyMessaging.sendDebugMsg("Processing deleteTownBlockIds");
        	
        	try {
				world = plugin.getServerWorld(townBlock.getWorld().getName());
				/*
				if (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()), MinecraftTools.calcChunk(townBlock.getZ())))
					return;
				*/
				int height = world.getMaxHeight()-1;
				int worldx = townBlock.getX()*plotSize, worldz = townBlock.getZ()*plotSize;
				
				for (int z = 0; z < plotSize; z++)
	        		for (int x = 0; x < plotSize; x++)
	        			for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
	        				Block block = world.getBlockAt(worldx + x, y, worldz + z);
	        				if (townBlock.getWorld().isPlotManagementDeleteIds(block.getTypeId())) {
	        					block.setType(Material.AIR);
	        				}
	        				block = null;
	        			}
			} catch (NotRegisteredException e1) {
				// Failed to get world.
				e1.printStackTrace();
			}
        	
        	//block = null;
        	world = null;        	
        }
        
		/**
		 * Deletes all of a specified block type from a TownBlock
		 * 
		 * @param townBlock
		 * @param material
		 */
		public void deleteTownBlockMaterial(TownBlock townBlock, int material) {
        	
        	//Block block = null;
        	int plotSize = TownySettings.getTownBlockSize();
        	
        	TownyMessaging.sendDebugMsg("Processing deleteTownBlockId");
        	
        	try {
				World world = plugin.getServerWorld(townBlock.getWorld().getName());
				/*
				if (!world.isChunkLoaded(MinecraftTools.calcChunk(townBlock.getX()), MinecraftTools.calcChunk(townBlock.getZ())))
					return;
				*/
				int height = world.getMaxHeight()-1;
				int worldx = townBlock.getX()*plotSize, worldz = townBlock.getZ()*plotSize;
				
				for (int z = 0; z < plotSize; z++)
	        		for (int x = 0; x < plotSize; x++)
	        			for (int y = height; y > 0; y--) { //Check from bottom up else minecraft won't remove doors
	        				Block block = world.getBlockAt(worldx + x, y, worldz + z);
	        				if (block.getTypeId() == material) {
	        					block.setType(Material.AIR);
	        				}
	        				block = null;
	        			}
			} catch (NotRegisteredException e1) {
				// Failed to get world.
				e1.printStackTrace();
			}
        	
        }
		
		
        public void removeTownBlocks(Town town) {
                for (TownBlock townBlock : new ArrayList<TownBlock>(town.getTownBlocks()))
                        removeTownBlock(townBlock);
        }

       
		public void collectTownCosts() throws EconomyException, TownyException {
			for (Town town : new ArrayList<Town>(towns.values()))
				if (town.hasUpkeep()) {
					double upkeep = TownySettings.getTownUpkeepCost(town);
					
					if (upkeep > 0) {
						// Town is paying upkeep
						if (!town.pay(TownySettings.getTownUpkeepCost(town), "Town Upkeep")) {
							removeTown(town);
							TownyMessaging.sendGlobalMessage(town.getName() + TownySettings.getLangString("msg_bankrupt_town"));
						}
					} else {
						// Negative upkeep
						if (TownySettings.isUpkeepPayingPlots()) {
							// Pay each plot owner a share of the negative upkeep
							for (TownBlock townBlock : new ArrayList<TownBlock>(town.getTownBlocks())) {
								if (townBlock.hasResident())
									townBlock.getResident().pay((upkeep / town.getTotalBlocks()), "Negative Town Upkeep - Plot income");
								else
									town.pay((upkeep / town.getTotalBlocks()), "Negative Town Upkeep - Plot income");
							}
							
						} else {
							//Not paying plot owners so just pay the town
							town.pay(upkeep, "Negative Town Upkeep");
						}
						
					}
				}
					
			setChanged();
			notifyObservers(UPKEEP_TOWN);
        }
        
        public void collectNationCosts() throws EconomyException {
                for (Nation nation : new ArrayList<Nation>(nations.values())) {
                        if (!nation.pay(TownySettings.getNationUpkeepCost(nation), "Nation Upkeep")) {
                                removeNation(nation);
                                TownyMessaging.sendGlobalMessage(nation.getName() + TownySettings.getLangString("msg_bankrupt_nation"));
                        }
                        if (nation.isNeutral())
                                if (!nation.pay(TownySettings.getNationNeutralityCost(), "Nation Neutrality Upkeep")) {
                                        try {
                                                nation.setNeutral(false);
                                        } catch (TownyException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                        }
                                        getDataSource().saveNation(nation);
                                        TownyMessaging.sendNationMessage(nation, TownySettings.getLangString("msg_nation_not_neutral"));
                                }
                }

        setChanged();
        notifyObservers(UPKEEP_NATION);
        }
        
        public List<TownBlock> getAllTownBlocks() {
                List<TownBlock> townBlocks = new ArrayList<TownBlock>();
                for (TownyWorld world : getWorlds())
                        townBlocks.addAll(world.getTownBlocks());
                return townBlocks;
        }
        
        
        public void sendUniverseTree(CommandSender sender) {
                for (String line : getTreeString(0))
                        sender.sendMessage(line);
        }
        
        @Override
        public List<String> getTreeString(int depth) {
                List<String> out = new ArrayList<String>();
                out.add(getTreeDepth(depth) + "Universe ("+getName()+")");
                if (plugin != null) {
                        out.add(getTreeDepth(depth+1) + "Server ("+plugin.getServer().getName()+")");
                        out.add(getTreeDepth(depth+2) + "Version: " + plugin.getServer().getVersion());
                        out.add(getTreeDepth(depth+2) + "Players: " + plugin.getServer().getOnlinePlayers().length + "/" + plugin.getServer().getMaxPlayers());
                        out.add(getTreeDepth(depth+2) + "Worlds (" + plugin.getServer().getWorlds().size() + "): " + Arrays.toString(plugin.getServer().getWorlds().toArray(new World[0])));
                }
                out.add(getTreeDepth(depth+1) + "Worlds (" + getWorlds().size() + "):");
                for (TownyWorld world : getWorlds())
                        out.addAll(world.getTreeString(depth+2));
                
                out.add(getTreeDepth(depth+1) + "Nations (" + getNations().size() + "):");
                for (Nation nation : getNations())
                        out.addAll(nation.getTreeString(depth+2));
                
                Collection<Town> townsWithoutNation = getTownsWithoutNation();
                out.add(getTreeDepth(depth+1) + "Towns (" + townsWithoutNation.size() + "):");
                for (Town town : townsWithoutNation)
                        out.addAll(town.getTreeString(depth+2));
                
                Collection<Resident> residentsWithoutTown = getResidentsWithoutTown();
                out.add(getTreeDepth(depth+1) + "Residents (" + residentsWithoutTown.size() + "):");
                for (Resident resident : residentsWithoutTown)
                        out.addAll(resident.getTreeString(depth+2));
                return out;
        }

        public boolean areAllAllies(List<Nation> possibleAllies) {
                if (possibleAllies.size() <= 1)
                        return true;
                else {
                        for (int i = 0; i < possibleAllies.size() - 1; i++)
                                if (!possibleAllies.get(i).hasAlly(possibleAllies.get(i+1)))
                                        return false;
                        return true;
                }
        }

        public void sendMessageTo(ResidentList residents, String msg, String modeRequired) {
                for (Player player : getOnlinePlayers(residents))
                        if (plugin.hasPlayerMode(player, modeRequired))
                                player.sendMessage(msg);
        }
        
        public List<Resident> getValidatedResidents(Object sender, String[] names) {
        	List<Resident> invited = new ArrayList<Resident>();
        	for (String name : names) {
        		List<Player> matches = plugin.getServer().matchPlayer(name);
        		if (matches.size() > 1) {
                    String line = "Multiple players selected";
                    for (Player p : matches)
                            line += ", " + p.getName();
                    		TownyMessaging.sendErrorMsg(sender, line);
        		} else if (matches.size() == 1) {
        			// Match found online
                    try {
                            Resident target = getResident(matches.get(0).getName());
                            invited.add(target);
                    } catch (TownyException x) {
                    	TownyMessaging.sendErrorMsg(sender, x.getError());
                    }
        		} else {
        			// No online matches so test for offline.
        			Resident target;
					try {
						target = getResident(name);
						invited.add(target);
					} catch (NotRegisteredException x) {
						TownyMessaging.sendErrorMsg(sender, x.getError());
					}
        		}
        	}
        	return invited;
        }
        
    public List<Resident> getOnlineResidents(Player player, String[] names) {
    	List<Resident> invited = new ArrayList<Resident>();
        for (String name : names) {
        	List<Player> matches = plugin.getServer().matchPlayer(name);
            if (matches.size() > 1) {
            	String line = "Multiple players selected";
                for (Player p : matches)
                    line += ", " + p.getName();
                		TownyMessaging.sendErrorMsg(player, line);
            } else if (matches.size() == 1)
            	try {
                    Resident target = plugin.getTownyUniverse().getResident(matches.get(0).getName());
                    invited.add(target);
            	} catch (TownyException x) {
            		TownyMessaging.sendErrorMsg(player, x.getError());
            }
        }
        return invited;
}
    /*
    private static List<Resident> getResidents(Player player, String[] names) {
        List<Resident> invited = new ArrayList<Resident>();
        for (String name : names)
            try {
                Resident target = plugin.getTownyUniverse().getResident(name);
                invited.add(target);
            } catch (TownyException x) {
                plugin.sendErrorMsg(player, x.getError());
            }
        return invited;
    }
    */
        
    public List<Resident> getOnlineResidents(ResidentList residentList) {
    	List<Resident> onlineResidents = new ArrayList<Resident>();
		for (Player player : plugin.getServer().getOnlinePlayers()) {
			for (Resident resident : residentList.getResidents()) {
				if (resident.getName().equalsIgnoreCase(player.getName()))
					onlineResidents.add(resident);
			}
		}
		
		return onlineResidents;
	}
        
	public void requestTeleport(Player player, Town town, double cost) {
        try {
            TeleportWarmupTimerTask.requestTeleport(getResident(player.getName().toLowerCase()), town, cost);
        } catch (TownyException x) {
        	TownyMessaging.sendErrorMsg(player, x.getError());
        }

        setChanged();
        notifyObservers(TELEPORT_REQUEST);
    }
    
    public void abortTeleportRequest(Resident resident) {
        TeleportWarmupTimerTask.abortTeleportRequest(resident);
    }
    
    public void addWarZone(WorldCoord worldCoord) {
    	worldCoord.getWorld().addWarZone(worldCoord);
    	plugin.updateCache(worldCoord);
    }
    
    public void removeWarZone(WorldCoord worldCoord) {
    	worldCoord.getWorld().removeWarZone(worldCoord);
    	plugin.updateCache(worldCoord);
    }
    
    public boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {
    	try {
			return isEnemy(getResident(player.getName()).getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException e) {
			return false;
		}
    }
    
    public boolean hasProtectionRegenTask(BlockLocation blockLocation) {
    	for(BlockLocation location : protectionRegenTasks.keySet()) {
    		if (location.isLocation(blockLocation)) {
    			return true;
    		}
    	}
    	return false;
    }
    
    public ProtectionRegenTask GetProtectionRegenTask(BlockLocation blockLocation) {
    	for(BlockLocation location : protectionRegenTasks.keySet()) {
    		if (location.isLocation(blockLocation)) {
    			return protectionRegenTasks.get(location);
    		}
    	}
    	return null;
    }
    
    public void addProtectionRegenTask(ProtectionRegenTask task) {
        protectionRegenTasks.put(task.getBlockLocation(), task);
    }
    
    public void removeProtectionRegenTask(ProtectionRegenTask task) {
        protectionRegenTasks.remove(task.getBlockLocation());
        if (protectionRegenTasks.isEmpty())
        	protectionPlaceholders.clear();
    }
    
    public void cancelProtectionRegenTasks() {
        for(ProtectionRegenTask task : protectionRegenTasks.values()) {
            plugin.getServer().getScheduler().cancelTask(task.getTaskId());
            task.replaceProtections();
        }
        protectionRegenTasks.clear();
        protectionPlaceholders.clear();
    }
    
    public boolean isPlaceholder(Block block) {
        return protectionPlaceholders.contains(block);
    }
    
    public void addPlaceholder(Block block) {
        protectionPlaceholders.add(block);
    }
    
    public void removePlaceholder(Block block) {
        protectionPlaceholders.remove(block);
    }
}
