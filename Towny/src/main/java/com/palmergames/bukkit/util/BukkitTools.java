package com.palmergames.bukkit.util;

import com.google.common.base.Charsets;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.exceptions.CancelledEventException;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.utils.MinecraftVersion;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.PluginManager;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * A class of functions related to Bukkit in general.
 * 
 * @author Shade (Chris H, ElgarL)
 * @version 1.0
 */

public class BukkitTools {

	@SuppressWarnings("unused")
	private static Towny plugin = null;
	
	public static void initialize(Towny plugin) {
		BukkitTools.plugin = plugin;
	}
	
	/**
	 * Get an array of all online players
	 * 
	 * @return array of online players
	 */
	public static Collection<? extends Player> getOnlinePlayers() {
		return getServer().getOnlinePlayers();
	}
	
	public static List<Player> matchPlayer(String name) {
		List<Player> matchedPlayers = new ArrayList<>();
		
		for (Player iterPlayer : Bukkit.getOnlinePlayers()) {
			String iterPlayerName = iterPlayer.getName();
			if (PluginIntegrations.getInstance().isNPC(iterPlayer)) {
				continue;
			}
			if (name.equalsIgnoreCase(iterPlayerName)) {
				// Exact match
				matchedPlayers.clear();
				matchedPlayers.add(iterPlayer);
				break;
			}
			if (iterPlayerName.toLowerCase(java.util.Locale.ENGLISH).contains(name.toLowerCase(java.util.Locale.ENGLISH))) {
				// Partial match
				matchedPlayers.add(iterPlayer);
			}
		}
		
		return matchedPlayers;
	}
	
	/**
	 * Given a name this method should only return a UUID that is stored in the server cache,
	 * without pinging Mojang servers.
	 * 
	 * @param name - Resident/Player name to get a UUID for.
	 * @return UUID of player or null if the player is not in the cache.
	 */
	public static UUID getUUIDSafely(String name) {
		final OfflinePlayer cached = getOfflinePlayerIfCached(name);
		
		return cached != null ? cached.getUniqueId() : null;
	}
	
	@Nullable
	public static Player getPlayerExact(String name) {
		return getServer().getPlayerExact(name);
	}
	
	@Nullable
	public static Player getPlayer(String playerId) {
		return getServer().getPlayer(playerId);
	}
	
	@Nullable
	public static Player getPlayer(UUID playerUUID) {
		return getServer().getPlayer(playerUUID);
	}
	
	public static boolean hasVanishedMeta(final @NotNull Player player) {
		for (MetadataValue meta : player.getMetadata("vanished")) {
			if (meta.asBoolean())
				return true;
		}
		
		return false;
	}
	
	/**
	 * Test whether an Player can see another Player. Staff on servers tend to enjoy
	 * their privacy while vanished.
	 * 
	 * @param seeing Player who is doing the seeing.
	 * @param seen   Player who is potentially vanished from the seeing Player.
	 * @return true if the seeing Player can see the seen Player.
	 */
	public static boolean playerCanSeePlayer(Player seeing, Player seen) {
		// PremiumVanish cannot hide a player unless the MC is 1.19.3+ and ProtocolLib
		// is of a specific version.
		if (Bukkit.getPluginManager().isPluginEnabled("PremiumVanish") &&
			MinecraftVersion.CURRENT_VERSION.isOlderThanOrEquals(MinecraftVersion.MINECRAFT_1_19_3)) {
			return !hasVanishedMeta(seen);
		}
		// Vanish plugins should be able to correctly set the results of player#canSee(Player).
		return seeing.canSee(seen);
	}

	public static Collection<? extends Player> getVisibleOnlinePlayers(CommandSender sender) {
		if (!(sender instanceof Player player))
			return Bukkit.getOnlinePlayers();
		
		return Bukkit.getOnlinePlayers().stream()
			.filter(p -> playerCanSeePlayer(player, p))
			.collect(Collectors.toCollection(ArrayList::new));
	}
	
	/**
	 * Tests if this player is online.
	 * 
	 * @param name the name of the player.
	 * @return a true value if online
	 */
	public static boolean isOnline(String name) {
		return Bukkit.getPlayerExact(name) != null;
	}
	
	public static List<World> getWorlds() {
		return getServer().getWorlds();
	}
	
	public static World getWorld(String name) {
		return getServer().getWorld(name);
	}
	
	public static World getWorld(UUID worldUID) {
		return getServer().getWorld(worldUID);
	}

	public static UUID getWorldUUID(String name) {
		World world = getWorld(name);
		return world != null ? world.getUID() : null;
	}
	
	public static Server getServer() {
		return Bukkit.getServer();
	}
	
	public static PluginManager getPluginManager() {
		return getServer().getPluginManager();
	}
	
	/**
	 * Count the number of players online in each world
	 * 
	 * @return Map of world to online players.
	 */
	public static HashMap<String, Integer> getPlayersPerWorld() {

		HashMap<String, Integer> m = new HashMap<>();
		for (World world : getServer().getWorlds())
			m.put(world.getName(), 0);
		for (Player player :  getServer().getOnlinePlayers())
			m.put(player.getWorld().getName(), m.get(player.getWorld().getName()) + 1);
		return m;
	}

	/**
	 * Accepts an X or Z value and returns the associated Towny plot value.
	 * 
	 * @param value - Value to calculate for X or Z ({@link Integer})
	 * @return int of the relevant townblock x/z.
	 */
	public static int calcChunk(int value) {

		return (value * TownySettings.getTownBlockSize()) / 16;
	}


	@SuppressWarnings("deprecation")
	public static boolean hasPlayedBefore(String name) {
		return getServer().getOfflinePlayer(name).hasPlayedBefore();
	}
	
	/**
	 * Do not use without first using {@link #hasPlayedBefore(String)}
	 * 
	 * @param name - name of resident
	 * @return OfflinePlayer
	 */
	@SuppressWarnings("deprecation")
	public static OfflinePlayer getOfflinePlayer(String name) {

		return Bukkit.getOfflinePlayer(name);
	}
	
	@Nullable
	public static OfflinePlayer getOfflinePlayerIfCached(@NotNull String name) {
		return getServer().getOfflinePlayerIfCached(name);
	}
	
	public static OfflinePlayer getOfflinePlayerForVault(String name) {

		return Bukkit.getOfflinePlayer(getOfflinePlayerUUID(name));
	}
	
	public static UUID getOfflinePlayerUUID(String name) {
		return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(Charsets.UTF_8));
	}
	
	public static String convertCoordtoXYZ(Location loc) {
		return loc.getWorld().getName() + " " + loc.getBlockX() + "," + loc.getBlockY() + "," + loc.getBlockZ();
	}
	
	public static List<String> getWorldNames() {
		return getWorlds().stream().map(World::getName).collect(Collectors.toList());
	}
	
	public static List<String> getWorldNames(boolean lowercased) {
		return lowercased ? getWorlds().stream().map(world -> world.getName().toLowerCase(Locale.ROOT)).collect(Collectors.toList()) : getWorldNames();
	}

	@SuppressWarnings("deprecation")
	public static Location getBedOrRespawnLocation(Player player) {
		return MinecraftVersion.CURRENT_VERSION.isOlderThanOrEquals(MinecraftVersion.MINECRAFT_1_20_3)
			? player.getBedSpawnLocation() : player.getRespawnLocation();
	}

	@SuppressWarnings({"deprecation", "RedundantCast", "ConstantValue"})
	public static String potionEffectName(final @NotNull PotionEffectType type) {
		return (type instanceof Keyed ? ((Keyed) type).getKey().getKey() : type.getName()).toLowerCase(Locale.ROOT);
	}

	@SuppressWarnings("deprecation")
	public static Objective objective(Scoreboard board, @NotNull String name, @NotNull String displayName) {
		return MinecraftVersion.CURRENT_VERSION.isOlderThanOrEquals(MinecraftVersion.MINECRAFT_1_19_1)
			? board.registerNewObjective(name, "dummy", displayName) : board.registerNewObjective(name, Criteria.DUMMY, displayName);
	}

	/**
	 * @param event The event to call
	 * @return {@code true} if the event is cancellable and was cancelled, otherwise {@code false}.
	 */
	public static boolean isEventCancelled(@NotNull Event event) {
		fireEvent(event);
		
		if (event instanceof Cancellable cancellable)
			return cancellable.isCancelled();
		else
			return false;
	}

	/**
	 * @param event CancellableTownyEvent to be fired which might be cancelled.
	 * @throws CancelledEventException with the Event's cancelMessage.
	 */
	public static void ifCancelledThenThrow(@NotNull CancellableTownyEvent event) throws CancelledEventException {
		fireEvent(event);
		if (event.isCancelled())
			throw new CancelledEventException(event);
	}

	public static void fireEvent(@NotNull Event event) {
		Bukkit.getPluginManager().callEvent(event);
	}

	/**
	 * Used to parse user-inputted material names into valid material names.
	 * 
	 * @param name String which should be a material. 
	 * @return String name of the material or null if no match could be made.
	 */
	@Nullable
	public static String matchMaterialName(String name) {
		Material mat = matchRegistry(Registry.MATERIAL, name);
		return mat == null ? null : mat.getKey().getKey().toUpperCase(Locale.ROOT); 
	}

	/**
	 * Our own copy of {@link Registry#match(String)}, since this method did not exist on the currently lowest supported version, 1.16.
	 */
	@Nullable
	public static <T extends Keyed> T matchRegistry(@NotNull Registry<T> registry, @NotNull String input) {
		final String filtered = input.toLowerCase(Locale.ROOT).replaceAll("\\s+", "_");
		if (filtered.isEmpty())
			return null;

		final NamespacedKey key = NamespacedKey.fromString(filtered);
		return key != null ? registry.get(key) : null;
	}

	/**
	 * Converts a namespaced key into a string. The namespace is included for non minecraft namespaced keys.
	 * @return The string representation of the key.
	 */
	public static String keyAsString(@NotNull NamespacedKey key) {
		return key.getNamespace().equals(NamespacedKey.MINECRAFT) ? key.getKey() : key.toString();
	}

	/**
	 * @deprecated Use {@link Server#getCommandMap()} instead.
	 */
	@Deprecated
	public static @NotNull CommandMap getCommandMap() throws ReflectiveOperationException {
		return getServer().getCommandMap();
	}
	
	public static CompletableFuture<Location> getRespawnLocation(final Player player) {
		if (MinecraftVersion.CURRENT_VERSION.isOlderThan(MinecraftVersion.MINECRAFT_1_21_5)) {
			return getRespawnLocationOld(player);
		}

		final Location potentialLocation = player.getRespawnLocation(false);
		if (potentialLocation == null) {
			return CompletableFuture.completedFuture(null);
		}

		return potentialLocation.getWorld().getChunkAtAsync(potentialLocation).thenApply(chunk -> player.getRespawnLocation(true));
	}

	@SuppressWarnings("deprecation") // remove me when 1.21.4 or below is no longer supported
	private static CompletableFuture<Location> getRespawnLocationOld(final Player player) {
		final Location potentialLocation = player.getPotentialBedLocation();
		if (potentialLocation == null) {
			return CompletableFuture.completedFuture(null);
		}

		return potentialLocation.getWorld().getChunkAtAsync(potentialLocation).thenApply(chunk -> player.getBedSpawnLocation());
	}
	
	@ApiStatus.Internal
	public static Collection<String> convertKeyedToString(@NotNull Collection<? extends Keyed> keys) {
		final Set<String> set = new HashSet<>();
		
		for (Keyed keyed : keys)
			set.add(keyAsString(keyed.getKey()));
		
		return set;
	}
}
