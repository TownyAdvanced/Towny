package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.initialization.TownyInitException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.FileMgmt;
import org.bukkit.configuration.MemorySection;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionDefault;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;

/**
 * @author ElgarL
 * 
 */
public class TownyPerms {

	protected static LinkedHashMap<String, Permission> registeredPermissions = new LinkedHashMap<>();
	protected static HashMap<String, PermissionAttachment> attachments = new HashMap<>();
	private static HashMap<String, List<String>> groupPermsMap = new HashMap<>();
	private static CommentedConfiguration perms;
	private static Towny plugin;
	
	public static void initialize(Towny plugin) {
		TownyPerms.plugin = plugin;
	}
	
	private static Field permissions;

	// Setup reflection (Thanks to Codename_B for the reflection source)
	static {
		try {
			permissions = PermissionAttachment.class.getDeclaredField("permissions");
			permissions.setAccessible(true);
		} catch (SecurityException | NoSuchFieldException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load the townyperms.yml file.
	 * If it doesn't exist create it from the resource file in the jar.
	 * 
	 * @param filepath - Path to townyperms.yml
	 * @param defaultRes - Default townyperms.yml within the jar.
	 * @throws TownyInitException - When permission file cannot be loaded.
	 */
	public static void loadPerms(@NotNull String filepath, @NotNull String defaultRes) {

		String fullPath = filepath + File.separator + defaultRes;

		File file = FileMgmt.unpackResourceFile(fullPath, defaultRes, defaultRes);
		// read the townyperms.yml into memory
		perms = new CommentedConfiguration(file);
		if (!perms.load()) {
			throw new TownyInitException("Could not read townyperms.yml", TownyInitException.TownyError.PERMISSIONS);
		}

		groupPermsMap.clear();
		buildGroupPermsMap();
		buildComments();
		perms.save();

		/*
		 * Only do this once as we are really only interested in Towny perms.
		 */
		collectPermissions();
		
	}
	
	/**
	 * Register a specific residents permissions with Bukkit.
	 * 
	 * @param resident - Resident to check if player is valid
	 * @param player - Player to register permission
	 */
	public static void assignPermissions(Resident resident, Player player) {

		PermissionAttachment playersAttachment;
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		if (resident == null) {
			if (player != null)
				resident = townyUniverse.getResident(player.getUniqueId());

			// failed to get resident
			if (resident == null)
				return;
		} else {
			player = BukkitTools.getPlayer(resident.getName());
		}

		/*
		 * Find the current attachment or create a new one (if the player is
		 * online)
		 */

		if ((player == null) || !player.isOnline()) {
			attachments.remove(resident.getName());
			return;
		}

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(player.getWorld().getName());
		if (world == null)
			return;

		if (attachments.containsKey(resident.getName()))
			playersAttachment = attachments.get(resident.getName());
		else
			// DungeonsXL sometimes moves players which aren't online out of dungeon worlds causing an error in the log to appear.
			try {
				playersAttachment = BukkitTools.getPlayer(resident.getName()).addAttachment(plugin);
			} catch (Exception e) {
				return;
			}

		/*
		 * Set all our Towny default permissions using reflection else bukkit
		 * will perform a recalculation of perms for each addition.
		 */

		try {
			synchronized (playersAttachment) {
				@SuppressWarnings("unchecked")
				Map<String, Boolean> orig = (Map<String, Boolean>) permissions.get(playersAttachment);
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

					// System.out.print("Perms set for: " + resident.getName());
				}
				/*
				 * Tell bukkit to update it's permissions
				 */
				playersAttachment.getPermissible().recalculatePermissions();
			}
		} catch (IllegalArgumentException | IllegalAccessException e) {
			e.printStackTrace();
		}
		
		/*
		 * Store the attachment for future reference
		 */
		attachments.put(resident.getName(), playersAttachment);

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
	 * @return a List of permission nodes.
	 */
	private static List<String> getList(String path) {

		if (perms.contains(path)) {
			return perms.getStringList(path);
		}
		return null;
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
			permList.addAll(getTownDefault(resident.getTownOrNull()));
			
			// Is Mayor?
			if (resident.isMayor()) permList.addAll(getTownMayor());
				
			//Add town ranks here
			for (String rank: resident.getTownRanks()) {
				permList.addAll(getTownRank(rank));
			}
			
			//Check for nation membership
			if (resident.hasNation()) {
				permList.addAll(getNationDefault());
				// Is King?
				if (resident.isKing()) permList.addAll(getNationKing());
							
				//Add nation ranks here
				for (String rank: resident.getNationRanks()) {
					permList.addAll(getNationRank(rank));
				}
			}
		}
		
		List<String> playerPermArray = sort(new ArrayList<String>(permList));
		LinkedHashMap<String, Boolean> newPerms = new LinkedHashMap<String, Boolean>();

		Boolean value = false;
		for (String permission : playerPermArray) {			
			if (permission.contains("{townname}")) {
				if (resident.hasTown()) {
					String placeholderPerm = permission.replace("{townname}", resident.getTownOrNull().getName().toLowerCase());
					newPerms.put(placeholderPerm, true);
				}
			} else if (permission.contains("{nationname}")) {
				if (resident.hasNation()) {
					String placeholderPerm = permission.replace("{nationname}", resident.getTownOrNull().getNationOrNull().getName().toLowerCase());
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
					"User can grant this town rank to others..",
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

		List<String> permsList = getList("nomad");
		return (permsList == null)? new ArrayList<String>() : permsList;
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
	 * @param town - Town to target
	 * @return a list of permissions
	 */
	public static List<String> getTownDefault(Town town) {

		List<String> permsList = getList("towns.default");
		if ((permsList == null)) {
			List<String> emptyPermsList = new ArrayList<String>();
			emptyPermsList.add("towny.town." + town.getName().toLowerCase());
			return emptyPermsList;
		} else {
			permsList.add("towny.town." + town.getName().toLowerCase());
			return permsList;
		}
	}

	/**
	 * A town mayors permissions
	 *
	 * @return a list of permissions
	 */
	public static List<String> getTownMayor() {

		List<String> permsList = getList("towns.mayor");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to check permissions for
	 * @return a List of permissions
	 */
	public static List<String> getTownRank(String rank) {

		List<String> permsList = getList("towns.ranks." + rank);//.toLowerCase());
		return (permsList == null)? new ArrayList<String>() : permsList;
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
	 * @return a List of permissions
	 */
	public static List<String> getNationDefault() {

		List<String> permsList = getList("nations.default");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * A nations kings permissions
	 * 
	 * @return a List of permissions
	 */
	public static List<String> getNationKing() {

		List<String> permsList = getList("nations.king");
		return (permsList == null)? new ArrayList<String>() : permsList;
	}

	/**
	 * Get a specific ranks permissions
	 * 
	 * @param rank - Rank to get permissions of
	 * @return a List of Permissions
	 */
	public static List<String> getNationRank(String rank) {

		List<String> permsList = getList("nations.ranks." + rank);//.toLowerCase());
		return (permsList == null)? new ArrayList<String>() : permsList;
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
		for (String key : perms.getKeys(true)) {
			@SuppressWarnings("unchecked")
			List<String> nodes = (List<String>) perms.getList(key); 
			groupPermsMap.put(key, nodes);
		}
	}

	private static void buildComments() {
		perms.addComment("nomad",
				"#############################################################################################",
				"# This file contains custom permission sets which will be assigned to your players",
				"# depending on their current status.",
				"#",
				"# This is all managed by towny and pushed directly to CraftBukkits SuperPerms.",
				"# These will be in addition to any you manually assign in your specific permission plugin.",
				"#",
				"# You may assign any Permission nodes here, including those from other plugins.",
				"# You may also create any custom ranks you require.",
				"# You may change the names of any of the ranks except: nomad, default, mayor, king.",
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
	}

	public static CommentedConfiguration getTownyPermsFile() {
		return perms;
	}
	
}
