package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * @author ElgarL
 * 
 */
public class TownyPerms {

	protected static final LinkedHashMap<String, Permission> registeredPermissions = new LinkedHashMap<>();
	protected static final HashMap<String, PermissionAttachment> attachments = new HashMap<>();
	private static final HashMap<String, List<String>> groupPermsMap = new HashMap<>();
	private static CommentedConfiguration perms;
	private static Towny plugin;
	private static final List<String> vitalGroups = Arrays.asList("nomad","towns.default","towns.mayor","towns.ranks","nations.default","nations.king","nations.ranks");
	private static final HashMap<UUID, String> residentPrefixMap = new HashMap<>();
	private static final String RANKPRIORITY_PREFIX = "towny.rankpriority.";
	private static final String RANKPREFIX_PREFIX = "towny.rankprefix.";
	
	public static void initialize(Towny plugin) {
		TownyPerms.plugin = plugin;
	}
	
	private static final MethodHandle permissions;

	// Setup reflection (Thanks to Codename_B for the reflection source)
	static {
		try {
			Field permissionsField = PermissionAttachment.class.getDeclaredField("permissions");
			permissionsField.setAccessible(true);
			permissions = MethodHandles.lookup().unreflectGetter(permissionsField);
		} catch (ReflectiveOperationException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Load the townyperms.yml file.
	 * If it doesn't exist create it from the resource file in the jar.
	 * 
	 * @param permsYMLPath - Path to townyperms.yml in the data directory.
	 * @throws TownyInitException - Thrown when if we fail to copy or load townyperms.yml.
	 */
	public static void loadPerms(@NotNull Path permsYMLPath) {
		try {
			InputStream resource = Towny.class.getResourceAsStream("/townyperms.yml");
			if (resource == null) {
				throw new TownyInitException("Could not find 'townyperms.yml' in the JAR.", TownyInitException.TownyError.PERMISSIONS);
			}
			Files.copy(resource, permsYMLPath);
		} catch (FileAlreadyExistsException ignored) {
			// Expected behaviour
		} catch (IOException e) {
			throw new TownyInitException("Could not copy townyperms.yml from JAR to '" + permsYMLPath + "'.",TownyInitException.TownyError.PERMISSIONS, e);
		}
		// read the townyperms.yml into memory
		perms = new CommentedConfiguration(permsYMLPath);
		if (!perms.load()) {
			throw new TownyInitException("Could not read townyperms.yml", TownyInitException.TownyError.PERMISSIONS);
		}

		groupPermsMap.clear();
		buildGroupPermsMap();
		checkForVitalGroups();
		buildComments();
		perms.save();

		/*
		 * Only do this once as we are really only interested in Towny perms.
		 */
		collectPermissions();
		
	}
	
	/**
	 * Check that all the vital groups Towny relies on are present in the townyperms.yml.
	 * @throws TownyInitException - Thrown when a vital group is missing.
	 */
	private static void checkForVitalGroups() {
		for (String group : vitalGroups) {
			if (!groupPermsMap.containsKey(group))
				throw new TownyInitException(getErrorMessageForGroup(group), TownyInitException.TownyError.PERMISSIONS);
		}
	}

	private static @NotNull String getErrorMessageForGroup(String group) {
		if (!group.contains("."))
			return "Your townyperms.yml is missing the " + group + " group. Maybe you renamed it?";
		
		String[] split = group.split("\\.");
		return "Your townyperms.yml's " + split[0] + " section is missing the " + split[1] + " group. Maybe you renamed it?"; 
	}

	/**
	 * Register a specific residents permissions with Bukkit.
	 * 
	 * @param resident - Resident to check if player is valid
	 * @param player - Player to register permission
	 */
	public static void assignPermissions(Resident resident, Player player) {

		if (resident == null) {
			if (player != null)
				resident = TownyAPI.getInstance().getResident(player);

			// failed to get resident
			if (resident == null)
				return;
		} else {
			player = resident.getPlayer();
		}

		/*
		 * Find the current attachment or create a new one (if the player is
		 * online)
		 */

		if (player == null || !player.isOnline()) {
			attachments.remove(resident.getName());
			return;
		}

		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld());
		if (world == null)
			return;

		final Player finalPlayer = player;
		final PermissionAttachment attachment = attachments.computeIfAbsent(resident.getName(), k -> finalPlayer.addAttachment(plugin));

		/*
		 * Set all our Towny default permissions using reflection else bukkit
		 * will perform a recalculation of perms for each addition.
		 */

		try {
			final Map<String, Boolean> orig = (Map<String, Boolean>) permissions.invoke(attachment);
			/*
			 * Clear the map (faster than removing the attachment and
			 * recalculating)
			*/
			orig.clear();

			if (world.isUsingTowny()) {
				/*
				 * Fill with the fresh perm nodes
			     */
				orig.putAll(TownyPerms.getResidentPerms(resident));
			}
			
			/*
			 * Tell bukkit to update it's permissions
			*/
			player.recalculatePermissions();
		} catch (final Throwable e) {
			e.printStackTrace();
		}
		
		/*
		 * Store the attachment for future reference
		 */
		attachments.put(resident.getName(), attachment);

		/*
		 * Set a prefix for the resident ranks.
		 */
		setResidentPrimaryRankPrefix(resident);

	}
	
	/**
	 * Should only be called when a player leaves the server.
	 * 
	 * @param name - Player's name to remove attachment of
	 */
	public static void removeAttachment(String name) {
		
		attachments.remove(name);
		
	}
	
	/**
	 * Update the permissions for all online residents
	 * 
	 */
	public static void updateOnlinePerms() {
		
		for (Player player : BukkitTools.getOnlinePlayers()) {
			assignPermissions(null, player);
		}
		
	}
	
	/**
	 * Update the permissions for all residents of a town (if online)
	 * 
	 * @param town - Town to target
	 */
	public static void updateTownPerms(Town town) {
		
		for (Resident resident: town.getResidents()) {
			assignPermissions(resident, null);
		}
		
	}
	
	/**
	 * Update the permissions for all residents of a nation (if online)
	 * 
	 * @param nation - Nation to target
	 */
	public static void updateNationPerms(Nation nation) {
		
		for (Town town: nation.getTowns()) {
			updateTownPerms(town);
		}
		
	}

	/**
	 * Fetch a list of permission nodes
	 * 
	 * @param path - path to permission nodes
	 * @return a List of permission nodes or an empty list.
	 */
	private static List<String> getList(String path) {

		if (perms.contains(path)) {
			return perms.getStringList(path);
		}
		return new ArrayList<>();
	}
	
	/**
	 * Returns a sorted map of this residents current permissions.
	 * 
	 * @param resident - Resident to check
	 * @return a sorted Map of permission nodes
	 */
	public static LinkedHashMap<String, Boolean> getResidentPerms(Resident resident) {
		// Start by adding the default perms everyone gets
		Set<String> permList = new HashSet<>(getDefault());
		
		//Check for town membership
		if (resident.hasTown()) {
			permList.addAll(getTownDefault(getTownName(resident)));
			
			// Is Mayor?
			if (resident.isMayor()) permList.addAll(getTownMayor());
				
			//Add town ranks here
			for (String rank: resident.getTownRanks()) {
				permList.addAll(getTownRankPermissions(rank));
			}
			
			//Check for nation membership
			if (resident.hasNation()) {
				permList.addAll(getNationDefault(getNationName(resident)));
				// Is King?
				if (resident.isKing()) permList.addAll(getNationKing());
							
				//Add nation ranks here
				for (String rank: resident.getNationRanks()) {
					permList.addAll(getNationRankPermissions(rank));
				}
			} else {
				permList.add("towny.nationless");
			}
			
			if (isPeaceful(resident.getTownOrNull()))
				permList.addAll(getList("peaceful"));
				
		} else {
			permList.add("towny.townless");
			permList.add("towny.nationless");
		}
		
		List<String> playerPermArray = sort(new ArrayList<String>(permList));
		LinkedHashMap<String, Boolean> newPerms = new LinkedHashMap<String, Boolean>();

		Boolean value = false;
		for (String permission : playerPermArray) {			
			if (permission.contains("{townname}")) {
				if (resident.hasTown()) {
					String placeholderPerm = permission.replace("{townname}", getTownName(resident));
					newPerms.put(placeholderPerm, true);
				}
			} else if (permission.contains("{nationname}")) {
				if (resident.hasNation()) {
					String placeholderPerm = permission.replace("{nationname}", getNationName(resident));
					newPerms.put(placeholderPerm, true);
				}
			} else {
				value = (!permission.startsWith("-"));
				newPerms.put((value ? permission : permission.substring(1)), value);
			}
		}
		return newPerms;
		
	}

	public static void registerPermissionNodes() {
		
		 plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new
		 Runnable(){

			@Override
			public void run() {

				Permission perm;
				
				/*
				 * Register Town ranks
				 */
				for (String rank : getTownRanks()) {
					perm = new
					Permission(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(rank),
					"User can grant this town rank to others..",
					PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_COMMAND_TOWN_RANK.getNode(), true);
				}
				
				/*
				 * Register Nation ranks
				 */
				for (String rank : getNationRanks()) {
					perm = new
					Permission(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(rank),
					"User can grant this nation rank to others..",
					PermissionDefault.FALSE, null);
					perm.addParent(PermissionNodes.TOWNY_COMMAND_NATION_RANK.getNode(), true);
				}
			}
			 
		 },1);
	}

	/*
	 * Getter/Setters for TownyPerms
	 */
	
	/**
	 * Default permissions everyone gets
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getDefault() {

		return getList("nomad");
	}

	/*
	 * Town permission section
	 */

	/**
	 * Fetch a list of all available town ranks
	 * 
	 * @return a list of rank names.
	 */
	public static List<String> getTownRanks() {

		return new ArrayList<String>(((MemorySection) perms.get("towns.ranks")).getKeys(false));
	}

	/**
	 * Default permissions everyone in a town gets
	 * 
	 * @param townName Name of town lowercased.
	 * @return a list of permissions
	 */
	public static List<String> getTownDefault(String townName) {

		List<String> permsList = getList("towns.default");
		permsList.add("towny.town." + townName);
		return permsList;
	}

	/**
	 * A town mayors permissions
	 *
	 * @return a list of permissions
	 */
	public static List<String> getTownMayor() {

		return getList("towns.mayor");
	}

	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to check permissions for
	 * @return a List of permissions
	 */
	public static List<String> getTownRankPermissions(String rank) {

		return getList("towns.ranks." + rank);//.toLowerCase());
	}

	/*
	 * Nation permission section
	 */

	/**
	 * Fetch a list of all available nation ranks
	 * 
	 * @return a list of rank names.
	 */
	public static List<String> getNationRanks() {

		return new ArrayList<String>(((MemorySection) perms.get("nations.ranks")).getKeys(false));
	}

	/**
	 * Default permissions everyone in a nation gets
	 * 
	 * @param nationName Name of nation lowercased.
	 * @return a List of permissions
	 */
	public static List<String> getNationDefault(String nationName) {

		List<String> permsList = getList("nations.default");
		permsList.add("towny.nation." + nationName);
		return permsList;
	}

	/**
	 * A nations kings permissions
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getNationKing() {

		return getList("nations.king");
	}
	
	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to get permissions of
	 * @return a List of Permissions
	 */
	public static List<String> getNationRankPermissions(String rank) {

		return getList("nations.ranks." + rank);//.toLowerCase());
	}
	
	/**
	 * Used to match a given rank to a case-sensitive Nation Rank.
	 * @param rank String representing the rank the user typed in.
	 * @return String of the NationRank which matches or null;
	 */
	@Nullable
	public static String matchNationRank(String rank) {
		for (String nationRank : getNationRanks()) {
			if (nationRank.equalsIgnoreCase(rank))
				return nationRank;
		}
		return null;
	}
	
	/**
	 * Used to match a given rank to a case-sensitive Town Rank.
	 * @param rank String representing the rank the user typed in.
	 * @return String of the TownRank which matches or null;
	 */
	@Nullable
	public static String matchTownRank(String rank) {
		for (String townRank : getTownRanks()) {
			if (townRank.equalsIgnoreCase(rank))
				return townRank;
		}
		return null;
	}

	private static String getTownName(Resident resident) {
		return resident.getTownOrNull().getName().toLowerCase(Locale.ROOT);
	}

	private static String getNationName(Resident resident) {
		return resident.getNationOrNull().getName().toLowerCase(Locale.ROOT);
	}

	private static boolean isPeaceful(Town town) {
		return town.isNeutral() || (town.hasNation() && town.getNationOrNull().isNeutral()); 
	}

	public static boolean hasPeacefulNodes() {
		return !getList("peaceful").isEmpty();
	}

	/*
	 * Resident Primary Rank / Rank Prefix 
	 */
	public static String getResidentPrimaryRankPrefix(Resident resident) {
		return residentPrefixMap.getOrDefault(resident.getUUID(), setResidentPrimaryRankPrefix(resident));
	}

	private static String setResidentPrimaryRankPrefix(Resident resident) {
		String prefix = getPrimaryRankPrefix(resident);
		residentPrefixMap.put(resident.getUUID(), prefix);
		return prefix;
	}


	private static String getPrimaryRankPrefix(Resident resident) {
		String prefix = getHighestPriorityRankPrefix(resident);
		return prefix == null ? "" : prefix;
	}

	@Nullable
	private static String getHighestPriorityRankPrefix(Resident resident) {
		if (resident.hasNation() && !resident.getNationRanks().isEmpty()) {
			String rank = getHighestPriorityRank(resident, resident.getNationRanks(), r -> getNationRankPermissions(r));
			String prefix = getPrefixFromRank(getNationRankPermissions(rank));
			if (prefix != null)
				return prefix;
		}

		if (resident.hasTown() && !resident.getTownRanks().isEmpty()) {
			String rank = getHighestPriorityRank(resident, resident.getTownRanks(), r -> getTownRankPermissions(r));
			String prefix = getPrefixFromRank(getTownRankPermissions(rank));
			if (prefix != null)
				return prefix;
		}

		return null;
	}

	private static String getPrefixFromRank(List<String> nodes) {
		for (String node : nodes)
			if (node.startsWith(RANKPREFIX_PREFIX))
				return node.substring(RANKPREFIX_PREFIX.length());
		return null;
	}

	public static String getHighestPriorityRank(Resident resident, List<String> ranks, Function<String, List<String>> rankFunction) {
		Map<String, Integer> rankPriorityMap = new HashMap<>(); 
		for (String rank : ranks)
			rankPriorityMap.put(rank, getRankPriority(rankFunction.apply(rank)));
		return Collections.max(rankPriorityMap.entrySet(), Comparator.comparingInt(Map.Entry::getValue)).getKey();
	}

	private static int getRankPriority(List<String> nodes) {
		int topValue = 0;
		for (String node : nodes) {
			if (node.startsWith(RANKPRIORITY_PREFIX)) {
				int priorityValue = getNodePriority(node);
				if (topValue >= priorityValue)
					continue;
				topValue = priorityValue;
			}
		}
		return topValue;
	}

	private static int getNodePriority(String node) {
		try {
			return Integer.valueOf(node.substring(RANKPRIORITY_PREFIX.length()));
		} catch (NumberFormatException ignored) {
			return 0;
		}
	}
	/*
	 * Permission utility functions taken from GroupManager (which I wrote anyway).
	 */
	
	/**
	 * Update the list of permissions registered with bukkit
	 */
	public static void collectPermissions() {

		registeredPermissions.clear();

		for (Permission perm : BukkitTools.getPluginManager().getPermissions()) {
			registeredPermissions.put(perm.getName().toLowerCase(), perm);
		}

	}
	
	/**
	 * Sort a permission node list by parent/child
	 * 
	 * @param permList - List of permissions
	 * @return List sorted for priority
	 */
	private static List<String> sort(List<String> permList) {
		
		List<String> result = new ArrayList<String>();

		for (String key : permList) {
			String a = key.charAt(0) == '-' ? key.substring(1) : key;
			Map<String, Boolean> allchildren = getAllChildren(a, new HashSet<String>());
			if (allchildren != null) {

				ListIterator<String> itr = result.listIterator();

				while (itr.hasNext()) {
					String node = itr.next();
					String b = node.charAt(0) == '-' ? node.substring(1) : node;

					// Insert the parent node before the child
					if (allchildren.containsKey(b)) {
						itr.set(key);
						itr.add(node);
						break;
					}
				}
			}
			if (!result.contains(key))
				result.add(key);
		}

		return result;
	}

	/**
	 * Fetch all permissions which are registered with superperms.
	 * {can include child nodes)
	 * 
	 * @param includeChildren - If child nodes should be included
	 * @return List of all permission nodes
	 */
	public List<String> getAllRegisteredPermissions(boolean includeChildren) {

		List<String> perms = new ArrayList<String>();

		for (String key : registeredPermissions.keySet()) {
			if (!perms.contains(key)) {
				perms.add(key);

				if (includeChildren) {
					Map<String, Boolean> children = getAllChildren(key, new HashSet<String>());
					if (children != null) {
						for (String node : children.keySet())
							if (!perms.contains(node))
								perms.add(node);
					}
				}
			}

		}
		return perms;
	}

	/**
	 * Returns a map of ALL child permissions registered with bukkit
	 * null is empty
	 * 
	 * @param node - Parent node
	 * @param playerPermArray current list of perms to check against for
	 *            negations
	 * @return Map of child permissions
	 */
	public static Map<String, Boolean> getAllChildren(String node, Set<String> playerPermArray) {

		LinkedList<String> stack = new LinkedList<String>();
		Map<String, Boolean> alreadyVisited = new HashMap<String, Boolean>();
		stack.push(node);
		alreadyVisited.put(node, true);

		while (!stack.isEmpty()) {
			String now = stack.pop();

			Map<String, Boolean> children = getChildren(now);

			if ((children != null) && (!playerPermArray.contains("-" + now))) {
				for (String childName : children.keySet()) {
					if (!alreadyVisited.containsKey(childName)) {
						stack.push(childName);
						alreadyVisited.put(childName, children.get(childName));
					}
				}
			}
		}
		alreadyVisited.remove(node);
		if (!alreadyVisited.isEmpty())
			return alreadyVisited;

		return null;
	}
	
	/**
	 * Returns a map of the child permissions (1 node deep) as registered with
	 * Bukkit.
	 * null is empty
	 * 
	 * @param node - Parent node
	 * @return Map of child permissions
	 */
	public static Map<String, Boolean> getChildren(String node) {

		Permission perm = registeredPermissions.get(node.toLowerCase());
		if (perm == null)
			return null;

		return perm.getChildren();

	}

	public static List<String> getGroupList() {
		return new ArrayList<String>(groupPermsMap.keySet());
	}
	
	public static boolean mapHasGroup(String group) {
		return groupPermsMap.containsKey(group);
	}
	
	public static List<String> getPermsOfGroup(String group) {
		return mapHasGroup(group) ? (groupPermsMap.get(group) != null ? groupPermsMap.get(group): new ArrayList<String>()): new ArrayList<String>(); 
	}
	
	private static void buildGroupPermsMap() {
		perms.getKeys(true).stream().forEach(key -> groupPermsMap.put(key, perms.getStringList(key)));
	}

	private static void buildComments() {
		perms.addComment("nomad",
				"#############################################################################################",
				"# This file contains custom permission sets which will be assigned to your players          #",
				"# depending on their current status. This file uses YAML formatting. Do not include tabs    #",
				"# and be very careful to align the spacing preceding the - symbols. Improperly editing this #",
				"# file will prevent Towny from loading correctly.                                           #",
				"#                                                                                           #",
				"# This is all managed by towny and pushed directly to CraftBukkit's SuperPerms.             #",
				"# These will be give in addition to any you manually assign in your specific permission     #",
				"# plugin. Note: You do not need to assign any Towny permission nodes to your players in     #",
				"# your server's permission plugin, ie: LuckPerms.                                           #",
				"#                                                                                           #",
				"# You may assign any Permission nodes here, including those from other plugins.             #",
				"#                                                                                           #",
				"# You may also create any custom ranks you require. Creating ranks can be done using the    #",
				"# /ta townyperms townrank addrank [name] or by carefully editing this file.                 #",
				"# You can add permission to a rank/group using the                                          #",
				"# /ta townyperms group [name] addperm [node] command.                                       #",
				"#                                                                                           #",
				"# You may change the names of any of the ranks except: nomad, default, mayor, king, ranks,  #",
				"# peaceful.                                                                                 #",
				"#                                                                                           #",
				"# If you want to, you can negate permissions nodes from nodes by doing the following:       #",
				"# Ex:                                                                                       #",
				"#    - towny.command.plot.*                                                                 #",
				"#    - -towny.command.plot.set.jail                                                         #",
				"# In this example the user is given full rights to all of the /plot command nodes,          #",
				"# but has had their ability to set a plot to a Jail plot type disabled.                     #",
				"#                                                                                           #",
				"# The towns.ranks and nations.ranks sections support adding prefix and priorities, this     #",
				"# is done using two nodes: towny.rankpriority.# and towny.rankprefix.<prefix_here>.         #",
				"# Residents will have their ranks parsed until one rank is determined to be the highest     #",
				"# priority, this rank will then be searched for a prefix node. This prefix can be shown     #",
				"# using the %townyadvanced_resident_primary_rank% placeholder for PlaceholderAPI. A prefix  #",
				"# from a Nation rank will take precendence over a prefix from a Town rank.                  #",
				"# Ex:                                                                                       #",
				"#    - towny.rankpriority.100                                                               #",
				"#    - towny.rankprefix.&a<&2Sheriff&a>                                                     #",
				"#                                                                                           #",
				"#############################################################################################",
				"",
				"",
				"# The 'nomad' permissions are given to all players in all Towny worlds, townless and players who are part of a town.");
		
		perms.addComment("towns", "", "# This section of permissions covers players who are members of a town.");
		
		perms.addComment("towns.default", "", "# 'default' is the permission set which is auto assigned to any normal town member.");
		
		perms.addComment("towns.mayor", "", "# Mayors get these permissions in addition to the default set.");
		
		perms.addComment("towns.ranks", 
				"", 
				"# Ranks contain additional permissions residents will be", 
				"# granted if they are assigned that specific rank.");
		
		if (perms.getKeys(true).contains("towns.ranks.assistant"))
			perms.addComment("towns.ranks.assistant", "", "# assistants are able to grant VIP and helper rank.");
		
		if (perms.getKeys(true).contains("towns.ranks.donator"))
			perms.addComment("towns.ranks.donator", "", "# Currently only an example rank holder with no extra permissions.");
		
		if (perms.getKeys(true).contains("towns.ranks.vip"))
			perms.addComment("towns.ranks.vip", "", "# Currently only an example rank holder with no extra permissions.");
		
		if (perms.getKeys(true).contains("towns.ranks.sheriff"))
			perms.addComment("towns.ranks.sheriff", "", "# Sheriff rank is able to jail other town members.");
		
		perms.addComment("nations", "", "# This section of permissions covers players who are members of any town in a nation.");
		
		perms.addComment("nations.default", "", "# All nation members get these permissions.");
		
		perms.addComment("nations.king", "", "# Kings get these permissions in addition to the default set.");
		
		perms.addComment("peaceful", "", "# Nodes that are given to players who are in a peaceful/neutral town or nation.");
	}

	public static CommentedConfiguration getTownyPermsFile() {
		return perms;
	}
	
}
