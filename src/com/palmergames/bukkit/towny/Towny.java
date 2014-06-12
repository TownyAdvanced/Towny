package com.palmergames.bukkit.towny; /* Localized on 2014-05-02 by Neder */

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;
import com.earth2me.essentials.Essentials;
import com.nijiko.permissions.PermissionHandler;
import com.palmergames.bukkit.metrics.Metrics;
import com.palmergames.bukkit.towny.command.*;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.listeners.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.*;
import com.palmergames.bukkit.towny.questioner.TownyQuestionTask;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.towny.war.flagwar.TownyWar;
import com.palmergames.bukkit.towny.war.flagwar.listeners.TownyWarBlockListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.TownyWarCustomListener;
import com.palmergames.bukkit.towny.war.flagwar.listeners.TownyWarEntityListener;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.JavaUtil;
import com.palmergames.util.StringMgmt;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Towny Plugin for Bukkit
 * 
 * Website: http://code.google.com/a/eclipselabs.org/p/towny/ Source:
 * http://code.google.com/a/eclipselabs.org/p/towny/source/browse/
 * 
 * @author Shade, ElgarL
 */

public class Towny extends JavaPlugin {

	private String version = "2.0.0";

	public static PermissionHandler permissionHandler;

	private final TownyPlayerListener playerListener = new TownyPlayerListener(this);
	private final TownyVehicleListener vehicleListener = new TownyVehicleListener(this);
	private final TownyBlockListener blockListener = new TownyBlockListener(this);
	private final TownyBlockPhysicsListener physicsListener = new TownyBlockPhysicsListener(this);
	private final TownyCustomListener customListener = new TownyCustomListener(this);
	private final TownyEntityListener entityListener = new TownyEntityListener(this);
	private final TownyWeatherListener weatherListener = new TownyWeatherListener(this);
	private final TownyEntityMonitorListener entityMonitorListener = new TownyEntityMonitorListener(this);
	private final TownyWorldListener worldListener = new TownyWorldListener(this);
	private final TownyWarBlockListener townyWarBlockListener = new TownyWarBlockListener(this);
	private final TownyWarCustomListener townyWarCustomListener = new TownyWarCustomListener(this);
	private final TownyWarEntityListener townyWarEntityListener = new TownyWarEntityListener(this);

	private TownyUniverse townyUniverse;

	private Map<String, PlayerCache> playerCache = Collections.synchronizedMap(new HashMap<String, PlayerCache>());

	private Essentials essentials = null;
	private boolean citizens2 = false;

	private boolean error = false;

	@Override
	public void onEnable() {

		System.out.println("====================      타우니      ========================");
		
		/*
		 * Register Metrics
		 */
		try {
		    Metrics metrics = new Metrics(this);
		    metrics.start();
		} catch (IOException e) {
			System.err.println("[Towny] Error setting up metrics");
		}

		version = this.getDescription().getVersion();

		townyUniverse = new TownyUniverse(this);

		// Setup classes
		BukkitTools.initialize(this);
		TownyTimerHandler.initialize(this);
		TownyEconomyHandler.initialize(this);
		TownyFormatter.initialize(this);
		TownyRegenAPI.initialize(this);
		PlayerCacheUtil.initialize(this);
		TownyPerms.initialize(this);

		if (load()) {
			// Setup bukkit command interfaces
			getCommand("townyadmin").setExecutor(new TownyAdminCommand(this));
			getCommand("townyworld").setExecutor(new TownyWorldCommand(this));
			getCommand("resident").setExecutor(new ResidentCommand(this));
			getCommand("towny").setExecutor(new TownyCommand(this));
			getCommand("town").setExecutor(new TownCommand(this));
			getCommand("국가").setExecutor(new NationCommand(this));
			getCommand("plot").setExecutor(new PlotCommand(this));

			TownyWar.onEnable();

			if (TownySettings.isTownyUpdating(getVersion()))
				update();

			// Register all child permissions for ranks
			TownyPerms.registerPermissionNodes();
		}

		registerEvents();

		TownyLogger.log.info("=============================================================");
		if (isError())
			TownyLogger.log.info("[경고] - ***** 안전 모드 ***** " + version);
		else
			TownyLogger.log.info("[타우니] 버전: " + version + " - 모드 활성화됨");
		TownyLogger.log.info("=============================================================");

		if (!isError()) {
			// Re login anyone online. (In case of plugin reloading)
			for (Player player : BukkitTools.getOnlinePlayers())
				if (player != null)
					try {
						getTownyUniverse().onLogin(player);
					} catch (TownyException x) {
						TownyMessaging.sendErrorMsg(player, x.getMessage());
					}
		}
	}

	public void SetWorldFlags() {

		for (Town town : TownyUniverse.getDataSource().getTowns()) {
			TownyMessaging.sendDebugMsg("[Towny] 다음 설정값을 생성합니다: " + town.getName());

			if (town.getWorld() == null) {
				TownyLogger.log.warning("[타우니 오류] 월드 파일에서 오류를 발견했습니다. 복구를 시도합니다");
				if (town.hasHomeBlock())
					try {
						TownyWorld world = town.getHomeBlock().getWorld();
						if (!world.hasTown(town)) {
							world.addTown(town);
							TownyUniverse.getDataSource().saveTown(town);
							TownyUniverse.getDataSource().saveWorld(world);
						}
					} catch (TownyException e) {
						// Error fetching homeblock
						TownyLogger.log.warning("[타우니 오류] 설정값을 읽어오지 못했습니다: " + town.getName());
					}
				else
					TownyLogger.log.warning("[타우니 오류] 홈블록이 없습니다 - 감지하지 못한 월드: " + town.getName());
			}
		}

	}

	@Override
	public void onDisable() {

		System.out.println("==============================================================");

		if (TownyUniverse.getDataSource() != null && error == false)
			TownyUniverse.getDataSource().saveQueues();

		if (error == false)
			TownyWar.onDisable();

		if (TownyUniverse.isWarTime())
			getTownyUniverse().getWarEvent().toggleEnd();

		TownyTimerHandler.toggleTownyRepeatingTimer(false);
		TownyTimerHandler.toggleDailyTimer(false);
		TownyTimerHandler.toggleMobRemoval(false);
		TownyTimerHandler.toggleHealthRegen(false);
		TownyTimerHandler.toggleTeleportWarmup(false);

		TownyRegenAPI.cancelProtectionRegenTasks();

		playerCache.clear();
		
		// Shut down our saving task.
		TownyUniverse.getDataSource().cancelTask();

		townyUniverse = null;

		System.out.println("[타우니] 버전: " + version + " - 모드 비활성화됨");
		System.out.println("=============================================================");

		TownyLogger.shutDown();
	}

	public boolean load() {

		Pattern pattern = Pattern.compile("-b(\\d*?)jnks", Pattern.CASE_INSENSITIVE);
		Matcher matcher = pattern.matcher(getServer().getVersion());

		// TownyEconomyHandler.setupEconomy();

		if (!townyUniverse.loadSettings()) {
			setError(true);
			// getServer().getPluginManager().disablePlugin(this);
			return false;
		}

		setupLogger();

		int bukkitVer = TownySettings.getMinBukkitVersion();

		if (!matcher.find() || matcher.group(1) == null) {

			TownyLogger.log.warning("[타우니 경고] 버킷 버전을 읽어오지 못했습니다.");
			TownyLogger.log.warning("[타우니 경고] 타우니는 버킷 빌드 " + bukkitVer + " 이상에서 정상적으로 작동합니다.");
			TownyLogger.log.warning("[타우니 경고] 버킷 버전을 체크해 주시고, 구버전/커스텀 버킷에 대해서는 테스트하지 않았습니다.");

		} else {

			int curBuild = Integer.parseInt(matcher.group(1));

			if (curBuild < bukkitVer) {

				TownyLogger.log.severe("[타우니 경고] 현재 사용 중인 버킷빌드 (" + curBuild + ") 는 구버전입니다!");
				TownyLogger.log.severe("[타우니 경고] 타우니는 버킷 빌드 " + bukkitVer + " 이상에서 정상적으로 작동합니다.");

			}
		}

		checkPlugins();

		SetWorldFlags();

		// make sure the timers are stopped for a reset
		TownyTimerHandler.toggleTownyRepeatingTimer(false);
		TownyTimerHandler.toggleDailyTimer(false);
		TownyTimerHandler.toggleMobRemoval(false);
		TownyTimerHandler.toggleHealthRegen(false);
		TownyTimerHandler.toggleTeleportWarmup(false);

		// Start timers
		TownyTimerHandler.toggleTownyRepeatingTimer(true);
		TownyTimerHandler.toggleDailyTimer(true);
		TownyTimerHandler.toggleMobRemoval(true);
		TownyTimerHandler.toggleHealthRegen(TownySettings.hasHealthRegen());
		TownyTimerHandler.toggleTeleportWarmup(TownySettings.getTeleportWarmupTime() > 0);
		resetCache();

		return true;
	}

	private void checkPlugins() {

		List<String> using = new ArrayList<String>();
		Plugin test;

		if (TownySettings.isUsingPermissions()) {
			test = getServer().getPluginManager().getPlugin("GroupManager");
			if (test != null) {
				// groupManager = (GroupManager)test;
				this.getTownyUniverse().setPermissionSource(new GroupManagerSource(this, test));
				using.add(String.format("%s v%s", "GroupManager", test.getDescription().getVersion()));
			} else {
				test = getServer().getPluginManager().getPlugin("PermissionsEx");
				if (test != null) {
					// permissions = (PermissionsEX)test;
					getTownyUniverse().setPermissionSource(new PEXSource(this, test));
					using.add(String.format("%s v%s", "PermissionsEX", test.getDescription().getVersion()));
				} else {
					test = getServer().getPluginManager().getPlugin("bPermissions");
					if (test != null) {
						// permissions = (Permissions)test;
						getTownyUniverse().setPermissionSource(new bPermsSource(this, test));
						using.add(String.format("%s v%s", "bPermissions", test.getDescription().getVersion()));
					} else {
						test = getServer().getPluginManager().getPlugin("Permissions");
						if (test != null) {
							// permissions = (Permissions)test;
							getTownyUniverse().setPermissionSource(new Perms3Source(this, test));
							using.add(String.format("%s v%s", "Permissions", test.getDescription().getVersion()));
						} else {
							// Try Vault
							test = getServer().getPluginManager().getPlugin("Vault");
							if (test != null) {
								net.milkbowl.vault.chat.Chat chat = getServer().getServicesManager().load(net.milkbowl.vault.chat.Chat.class);
								if (chat == null) {
									// No Chat implementation
									test = null;
									// Fall back to BukkitPermissions below
								} else {
									getTownyUniverse().setPermissionSource(new VaultPermSource(this, chat));
									using.add(String.format("%s v%s", "Vault", test.getDescription().getVersion()));
								}
							}

							if (test == null) {
								getTownyUniverse().setPermissionSource(new BukkitPermSource(this));
								using.add("BukkitPermissions");
							}
						}
					}
				}
			}
		} else {
			// Not using Permissions
			getTownyUniverse().setPermissionSource(new NullPermSource(this));
		}

		if (TownySettings.isUsingEconomy()) {

			if (TownyEconomyHandler.setupEconomy())
				using.add(TownyEconomyHandler.getVersion());
			else
				TownyMessaging.sendErrorMsg("사용 가능한 이코노미 플러그인이 없습니다. iConomy 5.01이나 Vault 호환 이코노미 플러그인이 있는지 확인해주세요.");
		}

		test = getServer().getPluginManager().getPlugin("Essentials");
		if (test == null)
			TownySettings.setUsingEssentials(false);
		else if (TownySettings.isUsingEssentials()) {
			this.essentials = (Essentials) test;
			using.add(String.format("%s v%s", "Essentials", test.getDescription().getVersion()));
		}

		test = getServer().getPluginManager().getPlugin("Questioner");
		if (test == null)
			TownySettings.setUsingQuestioner(false);
		else if (TownySettings.isUsingQuestioner())
			using.add(String.format("%s v%s", "Questioner", test.getDescription().getVersion()));

		/*
		 * Test for Citizens2 so we can avoid removing their NPC's
		 */
		test = getServer().getPluginManager().getPlugin("Citizens");
		if (test != null) {
			citizens2 = test.getDescription().getVersion().startsWith("2");
		}

		if (using.size() > 0)
			TownyLogger.log.info("[Towny] 다음과 연동됨: " + StringMgmt.join(using, ", "));
	}

	private void registerEvents() {

		final PluginManager pluginManager = getServer().getPluginManager();

		if (!isError()) {
			// Have War Events get launched before regular events.
			pluginManager.registerEvents(townyWarBlockListener, this);
			pluginManager.registerEvents(townyWarEntityListener, this);

			// Manage player deaths and death payments
			pluginManager.registerEvents(entityMonitorListener, this);
			pluginManager.registerEvents(vehicleListener, this);
			pluginManager.registerEvents(weatherListener, this);
			pluginManager.registerEvents(townyWarCustomListener, this);
			pluginManager.registerEvents(customListener, this);
			pluginManager.registerEvents(worldListener, this);

			// Only register a physics listener if we need to.
			if (TownySettings.getRegenDelay() > 0)
				pluginManager.registerEvents(physicsListener, this);

		}

		// Always register these events.
		pluginManager.registerEvents(playerListener, this);
		pluginManager.registerEvents(blockListener, this);
		pluginManager.registerEvents(entityListener, this);

	}

	private void update() {

		try {
			List<String> changeLog = JavaUtil.readTextFromJar("/ChangeLog.txt");
			boolean display = false;
			TownyLogger.log.info("------------------------------------");
			TownyLogger.log.info("[타우니] v" + getVersion() + "까지의 변경사항");
			String lastVersion = TownySettings.getLastRunVersion(getVersion()).split("_")[0];
			for (String line : changeLog) { // TODO: crawl from the bottom, then
											// past from that index.
				if (line.startsWith("v" + lastVersion))
					display = true;
				if (display && line.replaceAll(" ", "").replaceAll("\t", "").length() > 0)
					TownyLogger.log.info(line);
			}
			TownyLogger.log.info("------------------------------------");
		} catch (IOException e) {
			TownyMessaging.sendDebugMsg("ChangeLog.txt 를 읽지 못했습니다.");
		}
		TownySettings.setLastRunVersion(getVersion());
		
		TownyUniverse.getDataSource().saveAll();
		TownyUniverse.getDataSource().cleanup();
	}

	/**
	 * Fetch the TownyUniverse instance
	 * 
	 * @return TownyUniverse
	 */
	public TownyUniverse getTownyUniverse() {

		return townyUniverse;
	}

	public String getVersion() {

		return version;
	}

	/**
	 * @return the error
	 */
	public boolean isError() {

		return error;
	}

	/**
	 * @param error the error to set
	 */
	protected void setError(boolean error) {

		this.error = error;
	}

	// is permissions active
	public boolean isPermissions() {

		return TownySettings.isUsingPermissions();
	}

	// is Essentials active
	public boolean isEssentials() {

		return (TownySettings.isUsingEssentials() && (this.essentials != null));
	}

	// is Citizens2 active
	public boolean isCitizens2() {

		return citizens2;
	}

	/**
	 * @return Essentials object
	 * @throws TownyException
	 */
	public Essentials getEssentials() throws TownyException {

		if (essentials == null)
			throw new TownyException("Essentials 이 없거나, 활성화되지 않았습니다!");
		else
			return essentials;
	}

	public World getServerWorld(String name) throws NotRegisteredException {

		for (World world : BukkitTools.getWorlds())
			if (world.getName().equals(name))
				return world;

		throw new NotRegisteredException(String.format("월드 '$%s' 은(는) 등록되지 않았습니다.", name));
	}

	public boolean hasCache(Player player) {

		return playerCache.containsKey(player.getName().toLowerCase());
	}

	public void newCache(Player player) {

		try {
			getTownyUniverse();
			playerCache.put(player.getName().toLowerCase(), new PlayerCache(TownyUniverse.getDataSource().getWorld(player.getWorld().getName()), player));
		} catch (NotRegisteredException e) {
			TownyMessaging.sendErrorMsg(player, player.getWorld().getName() + "월드의 펄미션 캐쉬를 만들지 못했습니다.");
		}

	}

	public void deleteCache(Player player) {

		deleteCache(player.getName());
	}

	public void deleteCache(String name) {

		playerCache.remove(name.toLowerCase());
	}

	/**
	 * Fetch the current players cache
	 * Creates a new one, if one doesn't exist.
	 * 
	 * @param player
	 * @return the current (or new) cache for this player.
	 */
	public PlayerCache getCache(Player player) {

		if (!hasCache(player)) {
			newCache(player);
			getCache(player).setLastTownBlock(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)));
		}

		return playerCache.get(player.getName().toLowerCase());
	}

	/**
	 * Resets all Online player caches, retaining their location info.
	 */
	public void resetCache() {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				getCache(player).resetAndUpdate(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player))); // Automatically
																														// resets
																														// permissions.
	}

	/**
	 * Resets all Online player caches if their location equals this one
	 */
	public void updateCache(WorldCoord worldCoord) {

		for (Player player : BukkitTools.getOnlinePlayers())
			if (player != null)
				if (Coord.parseCoord(player).equals(worldCoord))
					getCache(player).resetAndUpdate(worldCoord); // Automatically
																	// resets
																	// permissions.
	}

	/**
	 * Resets all Online player caches if their location has changed
	 */
	public void updateCache() {

		WorldCoord worldCoord = null;

		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player));
				PlayerCache cache = getCache(player);
				if (cache.getLastTownBlock() != worldCoord)
					cache.resetAndUpdate(worldCoord);
			}
		}
	}

	/**
	 * Resets a specific players cache if their location has changed
	 * 
	 * @param player
	 */
	public void updateCache(Player player) {

		WorldCoord worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player));
		PlayerCache cache = getCache(player);

		if (cache.getLastTownBlock() != worldCoord)
			cache.resetAndUpdate(worldCoord);
	}

	/**
	 * Resets a specific players cache
	 * 
	 * @param player
	 */
	public void resetCache(Player player) {

		getCache(player).resetAndUpdate(new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player)));
	}

	public void setPlayerMode(Player player, String[] modes, boolean notify) {

		if (player == null)
			return;

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			resident.setModes(modes, notify);

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
		}
	}

	/**
	 * Remove ALL current modes (and set the defaults)
	 * 
	 * @param player
	 */
	public void removePlayerMode(Player player) {

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
			resident.clearModes();

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
		}

	}

	/**
	 * Fetch a list of all the players current modes.
	 * 
	 * @param player
	 * @return list of modes
	 */
	public List<String> getPlayerMode(Player player) {

		return getPlayerMode(player.getName());
	}

	public List<String> getPlayerMode(String name) {

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(name);
			return resident.getModes();

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
			return null;
		}
	}

	/**
	 * Check if the player has a specific mode.
	 * 
	 * @param player
	 * @param mode
	 * @return true if the mode is present.
	 */
	public boolean hasPlayerMode(Player player, String mode) {

		return hasPlayerMode(player.getName(), mode);
	}

	public boolean hasPlayerMode(String name, String mode) {

		try {
			Resident resident = TownyUniverse.getDataSource().getResident(name);
			return resident.hasMode(mode);

		} catch (NotRegisteredException e) {
			// Resident doesn't exist
			return false;
		}
	}

	public String getConfigPath() {

		return getDataFolder().getPath() + FileMgmt.fileSeparator() + "settings" + FileMgmt.fileSeparator() + "config.yml";
	}

	public Object getSetting(String root) {

		return TownySettings.getProperty(root);
	}

	public void log(String msg) {

		if (TownySettings.isLogging())
			TownyLogger.log.info(ChatColor.stripColor(msg));
	}

	public void setupLogger() {

		TownyLogger.setup(getTownyUniverse().getRootFolder(), TownySettings.isAppendingToLog());
	}

	public void appendQuestion(Questioner questioner, Question question) throws Exception {

		for (Option option : question.getOptions())
			if (option.getReaction() instanceof TownyQuestionTask)
				((TownyQuestionTask) option.getReaction()).setTowny(this);
		questioner.appendQuestion(question);
	}

	public boolean parseOnOff(String s) throws Exception {

		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception(String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
	}

	
	/**
	 * @return the playerListener
	 */
	public TownyPlayerListener getPlayerListener() {
	
		return playerListener;
	}

	
	/**
	 * @return the vehicleListener
	 */
	public TownyVehicleListener getVehicleListener() {
	
		return vehicleListener;
	}

	
	/**
	 * @return the physicsListener
	 */
	public TownyBlockPhysicsListener getPhysicsListener() {
	
		return physicsListener;
	}

	
	/**
	 * @return the entityListener
	 */
	public TownyEntityListener getEntityListener() {
	
		return entityListener;
	}

	
	/**
	 * @return the weatherListener
	 */
	public TownyWeatherListener getWeatherListener() {
	
		return weatherListener;
	}

	
	/**
	 * @return the entityMonitorListener
	 */
	public TownyEntityMonitorListener getEntityMonitorListener() {
	
		return entityMonitorListener;
	}

	
	/**
	 * @return the worldListener
	 */
	public TownyWorldListener getWorldListener() {
	
		return worldListener;
	}

	
	/**
	 * @return the townyWarBlockListener
	 */
	public TownyWarBlockListener getTownyWarBlockListener() {
	
		return townyWarBlockListener;
	}

	
	/**
	 * @return the townyWarCustomListener
	 */
	public TownyWarCustomListener getTownyWarCustomListener() {
	
		return townyWarCustomListener;
	}

	
	/**
	 * @return the townyWarEntityListener
	 */
	public TownyWarEntityListener getTownyWarEntityListener() {
	
		return townyWarEntityListener;
	}
}
