package com.palmergames.bukkit.towny.utils;

import java.util.BitSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.palmergames.bukkit.towny.event.NationSpawnEvent;
import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.event.TownSpawnEvent;
import com.palmergames.bukkit.towny.event.teleport.ResidentSpawnEvent;
import com.palmergames.bukkit.towny.event.teleport.SuccessfulTownyTeleportEvent;
import com.palmergames.bukkit.towny.event.teleport.UnjailedResidentTeleportEvent;
import com.palmergames.bukkit.towny.object.SpawnInformation;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.economy.TownyServerAccount;
import com.palmergames.bukkit.towny.object.spawnlevel.NationSpawnLevel;
import com.palmergames.bukkit.towny.object.spawnlevel.TownSpawnLevel;

import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.util.ItemLists;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.util.BukkitTools;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SpawnUtil {

	private static Towny plugin;

	public static void initialize(Towny plugin) {
		SpawnUtil.plugin = plugin;
	}

	/**
	 * Central method used for /res, /t, /n, /ta spawn commands.
	 * 
	 * @param player       Player using spawn command.
	 * @param split        Remaining command arguments, used primarily for outposts.
	 * @param townyObject  Either a town or nation depending on source command.
	 * @param notAffordMSG Message shown when a player cannot afford their teleport.
	 * @param outpost      Whether this is an outpost or not.
	 * @param ignoreWarn   Whether to show confirmation for payment or just pay
	 *                     without confirmation.
	 * @param spawnType    SpawnType.RESIDENT/TOWN/NATION
	 * @throws TownyException Thrown if any of the vital conditions are not met.
	 */
	public static void sendToTownySpawn(Player player, String[] split, TownyObject townyObject, String notAffordMSG, boolean outpost, boolean ignoreWarn, SpawnType spawnType) throws TownyException {

		Resident resident = TownyAPI.getInstance().getResidentOrThrow(player);

		// Set up town and nation variables.
		final Town town = switch (spawnType) {
			case RESIDENT -> resident.getTownOrNull();
			case TOWN -> (Town) townyObject;
			default -> null;
		};

		final Nation nation = spawnType == SpawnType.NATION ? (Nation) townyObject : null;

		//Get spawn information object. This allows us to pass data into the lambda below
		final SpawnInformation spawnInfo = getSpawnInformation(player, split.length == 0,
			notAffordMSG, outpost, spawnType, resident, town, nation);

		getSpawnLoc(player, town, nation, spawnType, outpost, split).thenAccept(spawnLoc -> {
			if (!spawnLoc.isWorldLoaded()) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_world_not_loaded"));
				return;
			}
			
			// Fire a cancellable event right before a player would actually pay.
			// Throws a TownyException if the event is cancelled.
			try {
				sendSpawnEvent(player, spawnType, spawnLoc, spawnInfo);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				return;
			}

			// There is a cost to spawn, prompt with confirmation unless ignoreWarn is true.
			if (spawnInfo.travelCost > 0) {
				// Get paymentMsg for the money.csv and the Account being paid.
				final String paymentMsg = getPaymentMsg(spawnInfo.townSpawnLevel, spawnInfo.nationSpawnLevel, spawnType);
				final Account payee = TownySettings.isTownSpawnPaidToTown() ? getPayee(town, nation, spawnType) : TownyServerAccount.ACCOUNT;
				initiateCostedSpawn(player, resident, spawnLoc, spawnInfo.travelCost, payee, paymentMsg, ignoreWarn, spawnInfo.cooldown);
				// No Cost so skip confirmation system.
			} else
				initiateSpawn(player, spawnLoc, spawnInfo.cooldown, 0, null);
		});
	}

	private static SpawnInformation getSpawnInformation(Player player, boolean noCmdArgs, String notAffordMSG, boolean outpost, SpawnType spawnType, Resident resident, Town town, Nation nation) {
		// Is this an admin spawning?
		final boolean isTownyAdmin = isTownyAdmin(player, resident);

		SpawnInformation spawnInformation = new SpawnInformation();
		try {
			// Check if the resident is denied spawning because of cooldowns or being jailed.
			testResidentAbility(resident);

			// Set up either the townSpawnLevel or nationSpawnLevel variable. (One of these will be null.)
			// This determines whether a spawn is considered town, nation, public, allied, admin via TownSpawnLevel and NationSpawnLevel objects.
			// Accounts for costs, permission, config settings and messages.
			spawnInformation.townSpawnLevel = getTownSpawnLevel(player, noCmdArgs, spawnType, resident, town, outpost, isTownyAdmin);
			spawnInformation.nationSpawnLevel = getNationSpawnLevel(player, noCmdArgs, spawnType, resident, nation, isTownyAdmin);

			// Get any applicable cooldown.
			spawnInformation.cooldown = getCooldown(player, resident.hasMode("adminbypass"), spawnInformation);

			// Prevent spawn travel while in the config's disallowed zones.
			// Throws a TownyException if the player is disallowed.
			if (!isTownyAdmin)
				testDisallowedZones(player, resident, spawnType, TownySettings.getDisallowedTownSpawnZones());

			// Get cost required to spawn.
			spawnInformation.travelCost= getTravelCost(player, town, nation, spawnInformation.townSpawnLevel, spawnInformation.nationSpawnLevel, spawnType);
			
			// Don't allow if they cannot pay.
			if (spawnInformation.travelCost > 0 && !resident.getAccount().canPayFromHoldings(spawnInformation.travelCost))
				throw new TownyException(notAffordMSG);

		} catch (TownyException te) {
			spawnInformation.eventCancelled = true;
			spawnInformation.eventCancellationMessage = te.getMessage();
		}
		return spawnInformation;
	}

	/**
	 * Handles moving outlaws from outside of towns they are outlawed in.
	 * 
	 * @param town   Town which they are outlawed in.
	 * @param outlaw Resident which is outlawed and being moved.
	 */
	public static void outlawTeleport(Town town, Resident outlaw) {
		Player outlawedPlayer = outlaw.getPlayer();
		if (outlawedPlayer == null)
			return;

		// sets tp location to their bedspawn only if it isn't in the town they're being teleported from.
		BukkitTools.getRespawnLocation(outlawedPlayer).thenAccept(bed -> {
			Location spawnLocation = town.getWorld().getSpawnLocation();
			if (!TownySettings.getOutlawTeleportWorld().equals(""))
				spawnLocation = Objects.requireNonNull(Bukkit.getWorld(TownySettings.getOutlawTeleportWorld())).getSpawnLocation();
			
			if (bed != null && TownyAPI.getInstance().getTown(bed) != town)
				spawnLocation = bed;
			
			if (outlaw.hasTown() && TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer) != null)
				spawnLocation = TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer);
			
			TownyMessaging.sendMsg(outlaw, Translatable.of("msg_outlaw_kicked", town));
			initiatePluginTeleport(outlaw, spawnLocation, true);
		});		
	}
	
	/**
	 * Used to teleport a jailed resident away from jail, upon gaining freedom.
	 * 
	 * @param jailed Resident which is being moved from jail.
	 */
	public static void jailAwayTeleport(Resident jailed) {
		getIdealLocation(jailed).thenAccept(loc -> {
			UnjailedResidentTeleportEvent event = new UnjailedResidentTeleportEvent(jailed, loc); 
			if (BukkitTools.isEventCancelled(event))
				return;

			initiatePluginTeleport(jailed, event.getLocation(), false);
		});
	}
	
	/**
	 * Used to teleport a newly jailed resident to jail.
	 * 
	 * @param jailed Resident going off to the slammer.
	 */
	public static void jailTeleport(Resident jailed) {
		initiatePluginTeleport(jailed, jailed.getJailSpawn(), false);
	}
	
	/**
	 * Checks that a resident is allowed to spawn.
	 * 
	 * @throws TownyException thrown when the resident is on cooldown or jailed.
	 */
	private static void testResidentAbility(Resident resident) throws TownyException {
		// Test if the resident is in a teleport cooldown.
		if (CooldownTimerTask.hasCooldown(resident.getName(), "teleport"))
			throw new TownyException(Translatable.of("msg_err_cannot_spawn_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(resident.getName(), "teleport")));

		// Disallow jailed players from teleporting.
		if (resident.isJailed())
			throw new TownyException(Translatable.of("msg_cannot_spawn_while_jailed"));
	}

	/**
	 * Is this a player with the admin spawn node.
	 * 
	 * @param player Player to test permissions for.
	 * @return true if this player has towny.admin or towny.admin.spawn in their permission nodes.
	 */
	private static boolean isTownyAdmin(Player player, Resident resident) {
		return TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player) || (!resident.hasMode("adminbypass") && hasPerm(player, PermissionNodes.TOWNY_SPAWN_ADMIN));
	}
	
	/**
	 * Is this player not charged for spawning?
	 * 
	 * @param player Player to test permission for.
	 * @return true if this player has either free spawning nodes.
	 */
	private static boolean playerHasFreeSpawn(Player player) {
		return hasPerm(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE)
				|| hasPerm(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCHARGE);
	}
	
	/**
	 * Get the TownSpawnLevel for this Spawn action, differentiating between /res
	 * spawn and /t spawn, returns null if this is a /n spawn.
	 * 
	 * @param player       Player doing the spawning.
	 * @param noCmdArgs    Whether the command had any subarguments passed on.
	 * @param spawnType    SpawnType involved, only cares about RESIDENT or TOWN.
	 * @param resident     Resident that is spawning.
	 * @param town         Town that the resident is spawning to.
	 * @param outpost      True if the player is spawning to an outpost.
	 * @param isTownyAdmin True if the player is treated as an admin.
	 * @return TownSpawnLevel The TownSpawnLevel object that will be used to
	 *         determine costs, cooldowns and permissions, or null if this is a /n
	 *         spawn-initiated spawn.
	 * @throws TownyException thrown when a spawn is not going to be allowed based
	 *                        on the TownSpawnLevel settings
	 */
	private static TownSpawnLevel getTownSpawnLevel(Player player, boolean noCmdArgs, SpawnType spawnType, Resident resident, Town town, boolean outpost, boolean isTownyAdmin) throws TownyException {
		return switch (spawnType) {
			case RESIDENT -> isTownyAdmin ? TownSpawnLevel.ADMIN : TownSpawnLevel.TOWN_RESIDENT;
			case TOWN -> isTownyAdmin ? TownSpawnLevel.ADMIN : getTownSpawnLevel(player, resident, town, outpost, noCmdArgs);
			default -> null;
		};
	}

	/**
	 * Get the TownSpawnLevel for this Spawn action.
	 * 
	 * @param player   Player doing the spawning.
	 * @param resident Resident object of the player spawning.
	 * @param town     Town the player is spawning to.
	 * @param outpost  True if this is an outpost spawn.
	 * @param noArg    True when the spawn command was run with no argument
	 *                 (spawning to their own town or first outpost.)
	 * @return townSpawnLevel TownSpawnLevel of this Spawn action.
	 * @throws TownyException thrown when a spawn is not going to be allowed based
	 *                        on the TownSpawnLevel settings.
	 */
	private static TownSpawnLevel getTownSpawnLevel(Player player, Resident resident, Town town, boolean outpost, boolean noArg) throws TownyException {
		TownSpawnLevel townSpawnLevel = null;
		if (noArg && !outpost) {
			townSpawnLevel = TownSpawnLevel.TOWN_RESIDENT;
		} else {
			// Arguments were used.
			if (TownySettings.trustedResidentsGetToSpawnToTown() && 
					(town.hasTrustedResident(resident) || 
							(resident.hasTown() && town.hasTrustedTown(resident.getTownOrNull())))) {
				townSpawnLevel = TownSpawnLevel.TOWN_RESIDENT;
			} else if (!resident.hasTown()) {
				townSpawnLevel = TownSpawnLevel.UNAFFILIATED;
			} else if (resident.getTownOrNull() == town) {
				townSpawnLevel = outpost ? TownSpawnLevel.TOWN_RESIDENT_OUTPOST : TownSpawnLevel.TOWN_RESIDENT;
			} else if (resident.hasNation() && town.hasNation()) {
				Nation playerNation = resident.getNationOrNull();
				Nation targetNation = town.getNationOrNull();

				if (playerNation == targetNation) {
					if (!town.isPublic() && 
						(TownySettings.isAllySpawningRequiringPublicStatus() && !resident.hasPermissionNode(PermissionNodes.TOWNY_SPAWN_NATION_BYPASS_PUBLIC.getNode())))
						throw new TownyException(Translatable.of("msg_err_ally_isnt_public", town));
					else
						townSpawnLevel = TownSpawnLevel.PART_OF_NATION;
				} else if (targetNation.hasEnemy(playerNation)) {
					if (town.isNeutral() && TownySettings.areEnemiesAllowedToSpawnToPeacefulTowns())
						// Let enemies spawn to peaceful towns.
						townSpawnLevel = TownSpawnLevel.TOWN_RESIDENT;
					else 
						// Prevent enemies from using spawn travel.
						throw new TownyException(Translatable.of("msg_err_public_spawn_enemy"));
				} else if (targetNation.hasAlly(playerNation)) {
					if (!town.isPublic() && 
						(TownySettings.isAllySpawningRequiringPublicStatus() && !resident.hasPermissionNode(PermissionNodes.TOWNY_SPAWN_ALLY_BYPASS_PUBLIC.getNode())))
						throw new TownyException(Translatable.of("msg_err_ally_isnt_public", town));
					else
						townSpawnLevel = TownSpawnLevel.NATION_ALLY;
				} else {
					townSpawnLevel = TownSpawnLevel.UNAFFILIATED;
				}
			} else {
				townSpawnLevel = TownSpawnLevel.UNAFFILIATED;
			}

			if (townSpawnLevel == TownSpawnLevel.UNAFFILIATED && !town.isPublic()) {
				if (!TownySettings.isConfigAllowingPublicTownSpawnTravel()) // The server doesn't allow any public town spawning.
					throw new TownyException(Translatable.of("msg_err_town_unaffiliated"));
				else 
					throw new TownyException(Translatable.of("msg_err_not_public"));
			}
		}

		// Check if the player has the permission/config allows for this type of spawning.
		// Throws exception if unallowed.
		townSpawnLevel.checkIfAllowed(player, town);

		return townSpawnLevel;
	}

	/**
	 * Get the NationSpawnLevel for this spawn action, or null if this was /res
	 * spawn or /t spawn.
	 * 
	 * @param player       Player doing the spawning.
	 * @param noCmdArgs    Whether the command had any subarguments passed on.
	 * @param spawnType    SpawnType involved, only cares about RESIDENT or TOWN.
	 * @param resident     Resident that is spawning.
	 * @param nation       Nation that is being spawned to.
	 * @param isTownyAdmin True if the player is treated as an admin.
	 * @return NationSpawnLevel or null if this was an instance of /res spawn or /t
	 *         spawn.
	 * @throws TownyException thrown when a spawn is not going to be allowed based
	 *                        on the TownSpawnLevel settings
	 */
	private static NationSpawnLevel getNationSpawnLevel(Player player, boolean noCmdArgs, SpawnType spawnType, Resident resident, Nation nation, boolean isTownyAdmin) throws TownyException {
		return spawnType == SpawnType.NATION ? isTownyAdmin ? NationSpawnLevel.ADMIN : getNationSpawnLevel(player, resident, nation, noCmdArgs) : null;
	}

	/**
	 * Get the NationSpawnLevel for this Spawn action.
	 * 
	 * @param player   Player doing the spawning.
	 * @param resident Resident object of the player spawning.
	 * @param nation   Nation the player is spawning to.
	 * @param noArg    True when the spawn command was run with no argument
	 *                 (spawning to their own nation.)
	 * @return nationSpawnLevel NationSpawnLevel of this Spawn action.
	 * @throws TownyException thrown when a spawn is not going to be allowed based
	 *                        on the NationSpawnLevel settings.
	 */
	private static NationSpawnLevel getNationSpawnLevel(Player player, Resident resident, Nation nation, boolean noArg) throws TownyException {
		NationSpawnLevel nationSpawnLevel = null;
		if (noArg) {
			nationSpawnLevel = NationSpawnLevel.PART_OF_NATION;
		} else {
			// Arguments were used.
			if (!resident.hasTown()) {
				nationSpawnLevel = NationSpawnLevel.UNAFFILIATED;
			} else if (resident.hasNation()) {
				Nation playerNation = resident.getNationOrNull();

				if (playerNation == nation) {
					nationSpawnLevel = NationSpawnLevel.PART_OF_NATION;
				} else if (nation.hasEnemy(playerNation)) {
					// Prevent enemies from using spawn travel.
					throw new TownyException(Translatable.of("msg_err_public_spawn_enemy"));
				} else if (nation.hasAlly(playerNation)) {
					nationSpawnLevel = NationSpawnLevel.NATION_ALLY;
				} else {
					nationSpawnLevel = NationSpawnLevel.UNAFFILIATED;
				}
			} else {
				nationSpawnLevel = NationSpawnLevel.UNAFFILIATED;
			}

			if (nationSpawnLevel == NationSpawnLevel.UNAFFILIATED && !nation.isPublic()) {
				if (!TownySettings.isConfigAllowingPublicNationSpawnTravel()) // The server doesn't allow any public nation spawning.
					throw new TownyException(Translatable.of("msg_err_nation_unaffiliated"));
				else 
					throw new TownyException(Translatable.of("msg_err_nation_not_public"));
			}
				
		}

		// Check if the player has the permission/config allows for this type of spawning.
		// Throws exception if unallowed.
		nationSpawnLevel.checkIfAllowed(player, nation);

		return nationSpawnLevel;
	}

	/**
	 * Get the cooldown time on a player using a spawn command.
	 * 
	 * @param player           Player doing the spawning action.
	 * @param hasAdminBypass   True if the player has the adminbypass mode enabled.
	 * @param spawnInformation SpawnInformation containing the useful TownSpawnLevel
	 *                         or NationSpawnLevel.
	 * @return number of seconds a player must wait until they can spawn again.
	 */
	private static int getCooldown(Player player, boolean hasAdminBypass, SpawnInformation spawnInformation) {
		return hasPerm(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCOOLDOWN) && !hasAdminBypass ? 0
			: spawnInformation.townSpawnLevel != null ? spawnInformation.townSpawnLevel.getCooldown() : spawnInformation.nationSpawnLevel.getCooldown();
	}
	
	/**
	 * Get the destination location of this Spawn action.
	 * 
	 * @param player    Player spawning.
	 * @param town      Town the player could be spawning to, or null.
	 * @param nation    Nation the player could be spawning to, or null.
	 * @param spawnType SpawnType of this Spawn action.
	 * @param outpost   True if the player is spawning to an outpost.
	 * @param split     String[] arguments of the command.
	 * @return spawnLoc Location the player will spawn to.
	 * @throws TownyException thrown when the eventual spawn location is invalid or
	 *                        the player is outlawed at that location.
	 */
	private static CompletableFuture<Location> getSpawnLoc(Player player, Town town, Nation nation, SpawnType spawnType, boolean outpost, String[] split) throws TownyException {
		return switch (spawnType) {
			case RESIDENT:
				if (TownySettings.getBedUse()) {
					yield BukkitTools.getRespawnLocation(player).thenApply(bedLoc -> {
						if (bedLoc != null)
							return bedLoc;
						else if (town != null && town.hasSpawn())
							return town.getSpawnOrNull();
						else
							return player.getWorld().getSpawnLocation();
					});
				} else if (town != null && town.hasSpawn())
					yield adaptSpawnLocation(town.getSpawn(), player);
				else
					yield CompletableFuture.completedFuture(player.getWorld().getSpawnLocation());
			case TOWN:
				if (outpost)
					yield adaptSpawnLocation(getOutpostSpawnLocation(player, town, split), player);
				else
					yield adaptSpawnLocation(town.getSpawn(), player);
			case NATION:
				yield adaptSpawnLocation(nation.getSpawn(), player);
		};
	}
	
	private static CompletableFuture<Location> adaptSpawnLocation(final @NotNull Location location, final @NotNull Player player) {
		if (!TownySettings.isSafeTeleportUsed())
			return CompletableFuture.completedFuture(location);
		
		return location.getWorld().getChunkAtAsync(location).thenApply(chunk -> getSafeLocation(location, player));
	}

	/**
	 * Tries to find a safe location nearby to teleport the player to
	 * if safety teleport is enabled, otherwise does nothing.
	 * 
	 * @param location Starting location
	 * @return A safe location nearby, same location if already safe
	 */
	private static Location getSafeLocation(Location location, Player p) {
		//if safety teleport isn't enabled do anything
		if (!TownySettings.isSafeTeleportUsed()) {
			return location;
		}
		
		if (isSafeLocation(location)) {
			return location;
		}

		if (TownySettings.isStrictSafeTeleportUsed()) {
			TownyMessaging.sendErrorMsg(p, Translatable.of("msg_spawn_cancel_safe_teleport"));
			return null;
		}
		
		final int range = 22;

		BitSet isLiquidMap = new BitSet(range * 2);
		BitSet isSolidMap = new BitSet(range * 2);
		
		// look for 20 blocks up and down for a safe location, 
		// if you can't find it fail the teleport
		// maybe add a translation key and add the player 
		// as a parameter to print him an error message
		
		Location temp = location.clone().subtract(0, range, 0);
		
		//build the linked lists
		for(int i = 0; i < range * 2; i++) {
			Material type = temp.getBlock().getType();
			
			if (ItemLists.LIQUID_BLOCKS.contains(type)) {
				isLiquidMap.set(i);
			}
			
			if (!ItemLists.NOT_SOLID_BLOCKS.contains(type)) {
				isSolidMap.set(i);
			}
			
			temp = temp.add(0,1,0);
		}
		
		// 1 -1 2 -2 ...
		for (int y = 0, steps = 1; steps <= 40; y = next(y), steps++) {
			// value     = bottom block
			// value + 1 = middle block
			// value + 2 = top block
			int value = y + range;
			if(!isSolidMap.get(value) || isLiquidMap.get(value)) {
				continue;
			}
			
			if (isSolidMap.get(value+1) || isLiquidMap.get(value+1)) {
				continue;
			}
			
			if (isSolidMap.get(value+2) || isLiquidMap.get(value+2)) {
				continue;
			}
			
			return location.clone().add(0, y + 1, 0);
		}
		
		TownyMessaging.sendErrorMsg(p, Translatable.of("msg_spawn_fail_safe_teleport"));
		return null;
	}
	
	private static int next(int i) {
		if (i <= 0) {
			i = -i;
			i++;
		} else {
			i = -i;
		}
		return i;
	}

	private static boolean isSafeLocation(Location location) {
		World world = location.getWorld();
		if (world == null) return false;

		// Check if location is in a block
		Block block = world.getBlockAt(location);
		Material type = block.getType();
		
		if (!ItemLists.NOT_SOLID_BLOCKS.contains(type) || ItemLists.LIQUID_BLOCKS.contains(type)) {
			return false;
		}

		// Check if block below is lava or water or nothing
		Block belowBlock = world.getBlockAt(location.clone().subtract(0, 1, 0));
		Material belowType = belowBlock.getType();
		if (ItemLists.NOT_SOLID_BLOCKS.contains(belowType) || ItemLists.LIQUID_BLOCKS.contains(type)) {
			return false;
		}

		// Check if the location is directly above a solid block
		Block aboveBlock = world.getBlockAt(location.clone().add(0, 1, 0));
		Material aboveType = aboveBlock.getType();
		if (!ItemLists.NOT_SOLID_BLOCKS.contains(aboveType) || ItemLists.LIQUID_BLOCKS.contains(aboveType)) {
			return false;
		}

		return true;
	}

	/**
	 * Complicated code that parses the given split to a named, numbered or
	 * unnumbered outpost.
	 * 
	 * @param player The Player doing the teleport.
	 * @param town  Town which is being spawned to.
	 * @param split String[] arguments to parse the outpost location from.
	 * @return Location of the town's outpost spawn.
	 * @throws TownyException thrown when there are no outposts, or the outpost
	 *                        limit was capped.
	 */
	private static Location getOutpostSpawnLocation(Player player, Town town, String[] split) throws TownyException {
		if (!town.hasOutpostSpawn())
			throw new TownyException(Translatable.of("msg_err_outpost_spawn"));

		Integer index = null;
		// No arguments or negative number, send them to the first outpost.
		if (split.length <= 0)
			index = 1;
		else {
			String userInput = split[split.length - 1];
			try {
				if (!userInput.contains("name:")) {
					index = Integer.parseInt(userInput);
				} else { // So now it say's name:123
					index = getOutpostIndexFromName(town, index, userInput.replace("name:", "").replace("_", " "));
				}
			} catch (NumberFormatException e) {
				// invalid entry so assume the first outpost, also note: We DO NOT HAVE a number
				// now, which means: if you type abc, you get brought to that outpost.
				// Let's consider the fact however: an outpost name begins with "123" and there
				// are 123 Outposts. Then we put the prefix name:123 and that solves that.
				index = getOutpostIndexFromName(town, index, userInput.replace("_", " "));
			} catch (ArrayIndexOutOfBoundsException i) {
				// Number not present so assume the first outpost.
				index = 1;
			}
		}

		if (!TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player)
			&& TownySettings.isOutpostLimitStoppingTeleports() 
			&& TownySettings.isOutpostsLimitedByLevels()
			&& town.isOverOutpostLimit() 
			&& Math.max(1, index) > town.getOutpostLimit()) {
			throw new TownyException(Translatable.of("msg_err_over_outposts_limit", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
		}

		return town.getOutpostSpawn(Math.max(1, index));
	}

	private static Integer getOutpostIndexFromName(Town town, Integer index, String userInput) {
		for (Location loc : town.getAllOutpostSpawns()) {
			TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(loc);
			if (tboutpost != null) {
				String name = !tboutpost.hasPlotObjectGroup() ? tboutpost.getName() : tboutpost.getPlotObjectGroup().getName();
				if (name.toLowerCase(Locale.ROOT).startsWith(userInput.toLowerCase(Locale.ROOT)))
					index = 1 + town.getAllOutpostSpawns().indexOf(loc);
			}
		}
		if (index == null) // If it persists to be null, so it's not been given a value, set it to the fallback (1).
			index = 1;
		return index;
	}

	/**
	 * Tests if a player is not allowed to spawn because they're not stood at the
	 * correct location, according the config's disallowedZones config setting.
	 * 
	 * @param player          Player doing the spawning.
	 * @param resident        Resident doing the spawning.
	 * @param spawnType       SpawnType being done.
	 * @param disallowedZones Config's list of DisallowedZones strings.
	 * @throws TownyException thrown when the player is not allowed to spawn because
	 *                        of where they are stood.
	 */
	private static void testDisallowedZones(Player player, Resident resident, SpawnType spawnType, List<String> disallowedZones) throws TownyException {
		if (!disallowedZones.isEmpty()) {
			Town townAtPlayerLoc = TownyAPI.getInstance().getTown(player.getLocation());
			
			if (townAtPlayerLoc == null && disallowedZones.contains("unclaimed"))
				throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.typeName(), Translatable.of("msg_the_wilderness")));

			if (townAtPlayerLoc != null) {
				if (townAtPlayerLoc.hasOutlaw(player.getName()) && disallowedZones.contains("outlaw"))
					throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", "RTP", Translatable.of("msg_a_town_you_are_outlawed_in")));
				if (resident.hasTown() && townAtPlayerLoc.hasNation()) {
					Nation townLocNation = townAtPlayerLoc.getNationOrNull();
					if (townLocNation.hasSanctionedTown(resident.getTownOrNull()))
						throw new TownyException(Translatable.of("msg_err_cannot_nation_spawn_your_town_is_sanctioned", townLocNation.getName()));
				}
				if (resident.hasNation() && townAtPlayerLoc.hasNation()) {
					if (CombatUtil.isEnemy(resident.getTownOrNull(), townAtPlayerLoc) && disallowedZones.contains("enemy"))
						throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.typeName(), Translatable.of("msg_enemy_areas")));
					Nation townLocNation = townAtPlayerLoc.getNationOrNull();
					Nation resNation = resident.getNationOrNull();
					if (!townLocNation.hasAlly(resNation) && !townLocNation.hasEnemy(resNation) && disallowedZones.contains("neutral"))
						throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.typeName(), Translatable.of("msg_neutral_towns")));
				}
			}
		}
	}

	/**
	 * Gets the cost to travel to the place they are spawning to.
	 * 
	 * @param player           Player spawning.
	 * @param town             Town the player could be going to, or null.
	 * @param nation           Nation the player could be going to, or null.
	 * @param townSpawnLevel   TownSpawnLevel that could be used, or null.
	 * @param nationSpawnLevel NationSpawnLevel that could be used, or null.
	 * @param spawnType        SpawnType being done.
	 * @return double cost of the spawn.
	 */
	private static double getTravelCost(Player player, Town town, Nation nation, TownSpawnLevel townSpawnLevel, NationSpawnLevel nationSpawnLevel, SpawnType spawnType) {
		if (!TownyEconomyHandler.isActive() || playerHasFreeSpawn(player))
			return 0.0;
		
		// If this is a "public" spawn and the Config doesn't allow mayors to override the Config price, use the Config price.
		if (!TownySettings.isPublicSpawnCostAffectedByTownSpawncost() &&
			(isPublicSpawn(townSpawnLevel) || isPublicSpawn(nationSpawnLevel)))
			return TownySettings.getSpawnTravelCost();

		return switch(spawnType) {
		case RESIDENT -> town == null ? 0.0 : Math.min(townSpawnLevel.getCost(town), townSpawnLevel.getCost());
		case TOWN -> Math.min(townSpawnLevel.getCost(town), townSpawnLevel.getCost());
		case NATION -> Math.min(nationSpawnLevel.getCost(nation), nationSpawnLevel.getCost());
		};
		
	}

	private static boolean isPublicSpawn(NationSpawnLevel nationSpawnLevel) {
		return NationSpawnLevel.UNAFFILIATED.equals(nationSpawnLevel);
	}

	private static boolean isPublicSpawn(TownSpawnLevel townSpawnLevel) {
		return TownSpawnLevel.UNAFFILIATED.equals(townSpawnLevel);
	}

	/**
	 * Get the payment message which will be used in the money.csv file.
	 * 
	 * @param townSpawnLevel   TownSpawnLevel that could be used, or null.
	 * @param nationSpawnLevel NationSpawnLevel that could be used, or null.
	 * @param spawnType        SpawnType being done.
	 * @return String message for the log.
	 */
	private static String getPaymentMsg(TownSpawnLevel townSpawnLevel, NationSpawnLevel nationSpawnLevel, SpawnType spawnType) {
		return switch(spawnType) {
		case RESIDENT -> String.format(spawnType.getTypeName() + " (%s)", townSpawnLevel);
		case TOWN -> String.format(spawnType.getTypeName() + " (%s)", townSpawnLevel);
		case NATION -> String.format(spawnType.getTypeName() + " (%s)", nationSpawnLevel);
		};
	}

	/**
	 * Get the account which will be paid.
	 * 
	 * @param town      Town which might get paid.
	 * @param nation    Nation which might get paid.
	 * @param spawnType SpawnType being done.
	 * @return Account which will be paid for the spawn, by the player.
	 */
	private static Account getPayee(Town town, Nation nation, SpawnType spawnType) {
		return switch(spawnType) {
		case RESIDENT -> town == null ? TownyServerAccount.ACCOUNT : town.getAccount(); 
		case TOWN -> town.getAccount();
		case NATION -> nation.getAccount();
		};
	}

	/**
	 * Fires cancellable events before allowing someone to spawn.
	 * 
	 * @param player    Player being spawned to a Towny location.
	 * @param spawnType SpawnType (RESIDENT, TOWN, NATION).
	 * @param spawnLoc  Location being spawned to.
	 * @throws TownyException when the event is cancelled.
	 */
	private static void sendSpawnEvent(Player player, SpawnType spawnType, Location spawnLoc, SpawnInformation spawnInformation) throws TownyException {
		BukkitTools.ifCancelledThenThrow(getSpawnEvent(player, spawnType, spawnLoc, spawnInformation));
	}
	
	/**
	 * Get a SpawnEvent.
	 * 
	 * @param player    Player spawning.
	 * @param spawnType SpawnType.
	 * @param spawnLoc  Location that the player will spawn at.
	 * @return SpawnEvent to be called.
	 */
	private static SpawnEvent getSpawnEvent(Player player, SpawnType spawnType, Location spawnLoc, SpawnInformation spawnInfo) {
		return switch(spawnType) {
		case RESIDENT -> new ResidentSpawnEvent(player, player.getLocation(), spawnLoc, spawnInfo.travelCost, spawnInfo.eventCancelled, spawnInfo.eventCancellationMessage);
		case TOWN -> new TownSpawnEvent(player, player.getLocation(), spawnLoc, spawnInfo.travelCost, spawnInfo.eventCancelled, spawnInfo.eventCancellationMessage);
		case NATION -> new NationSpawnEvent(player, player.getLocation(), spawnLoc, spawnInfo.travelCost, spawnInfo.eventCancelled, spawnInfo.eventCancellationMessage);
		};
	}

	/**
	 * Handles the final spawning for residents/towns/nations.
	 * 
	 * @param player   Player being spawned.
	 * @param spawnLoc Location being spawned to.
	 * @param cooldown The teleport cooldown in seconds to give to the player once teleported.
	 * @param cost The cost that this player has paid to teleport, used for refunds if the player aborts the teleport.
	 * @param refundAccount The account that the player paid the cost to, used for refunds if the player aborts the teleport.   
	 */
	private static void initiateSpawn(Player player, Location spawnLoc, int cooldown, double cost, @Nullable Account refundAccount) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		if (resident == null)
			return;

		boolean isUsingAdminBypass = resident.hasMode("adminbypass");
		if (TownyTimerHandler.isTeleportWarmupRunning() && (!hasPerm(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOWARMUP) || isUsingAdminBypass)) {
			// Use teleport warmup
			int warmupTime = TownyUniverse.getInstance().getPermissionSource().getGroupPermissionIntNode(player.getName(), PermissionNodes.TOWNY_TELEPORT_WARMUP_SECONDS.getNode());
			if (warmupTime == -1)
				warmupTime = TownySettings.getTeleportWarmupTime();
			TownyMessaging.sendMsg(player, Translatable.of("msg_town_spawn_warmup", warmupTime));
			long teleportTime = System.currentTimeMillis() + (warmupTime * 1000);
			TeleportWarmupTimerTask.requestTeleport(resident, teleportTime, spawnLoc, cooldown, refundAccount, cost);
		} else {
			// Don't use teleport warmup
			if (player.getVehicle() != null)
				player.getVehicle().eject();

			// Teleporting a player can cause the chunk to unload too fast, abandoning pets.
			addAndRemoveChunkTicket(WorldCoord.parseWorldCoord(player.getLocation()));

			final Location prior = player.getLocation();
			player.teleportAsync(spawnLoc, TeleportCause.COMMAND).thenAccept(successfulTeleport -> {
				if (successfulTeleport)
					BukkitTools.fireEvent(new SuccessfulTownyTeleportEvent(resident, spawnLoc, cost, prior));
			});

			if (cooldown > 0 && (!hasPerm(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCOOLDOWN) || isUsingAdminBypass))
				CooldownTimerTask.addCooldownTimer(player.getName(), "teleport", cooldown);
		}
	}

	/**
	 * Begin a costed teleportation.
	 * 
	 * @param player     Player being teleported.
	 * @param resident   Resident of the Player being teleported.
	 * @param spawnLoc   Location that the player is being teleported to.
	 * @param travelCost double Cost which the player has to be able to pay.
	 * @param payee      Account which will be paid.
	 * @param paymentMsg Message being left in the Towny money.csv log.
	 * @param ignoreWarn boolean which is true if the player opted to ignore the
	 *                   cost confirmation.
	 */
	private static void initiateCostedSpawn(Player player, Resident resident, Location spawnLoc, double travelCost, Account payee, String paymentMsg, boolean ignoreWarn, int cooldown) {
		if (ignoreWarn || !TownySettings.isSpawnWarnConfirmationUsed())
			// Skipping the confirmation.
			payAndThenSpawn(player, resident, spawnLoc, travelCost, payee, paymentMsg, cooldown);
		else
			// Sending the confirmation.
			Confirmation.runOnAccept(() -> payAndThenSpawn(player, resident, spawnLoc, travelCost, payee, paymentMsg, cooldown))
						.setTitle(Translatable.of("msg_spawn_warn", TownyEconomyHandler.getFormattedBalance(travelCost)))
						.sendTo(player);
	}

	/**
	 * If a player is able to pay the travel cost they will be teleported to the
	 * spawn location.
	 * 
	 * @param player     Player being teleported.
	 * @param resident   Resident of the Player being teleported.
	 * @param spawnLoc   Location that the player is being teleported to.
	 * @param travelCost double Cost which the player has to be able to pay.
	 * @param payee      Account which will be paid.
	 * @param paymentMsg Message being left in the Towny money.csv log.
	 */
	private static void payAndThenSpawn(Player player, Resident resident, Location spawnLoc, double travelCost, Account payee, String paymentMsg, int cooldown) {
		if (resident.getAccount().payTo(travelCost, payee, paymentMsg)) {
			TownyMessaging.sendMsg(player, Translatable.of("msg_cost_spawn", TownyEconomyHandler.getFormattedBalance(travelCost)));
			initiateSpawn(player, spawnLoc, cooldown, travelCost, payee);
		}
	}

	/**
	 * Get the best location an resident can be teleported to
	 * 
	 * @param resident Resident needing a location to spawn to.
	 * @return bed spawn OR town spawn OR last world spawn
	 */
	private static CompletableFuture<Location> getIdealLocation(Resident resident) {
		Town town = resident.getTownOrNull();
		Location loc = resident.getPlayer().getWorld().getSpawnLocation();

		if (town != null && town.hasSpawn())
			loc = town.getSpawnOrNull();

		Location finalLoc = loc;
		return BukkitTools.getRespawnLocation(resident.getPlayer()).thenApply(bed -> bed == null ? finalLoc : bed);
	}
	
	/**
	 * Teleport a player using the TeleportCause PLUGIN
	 * 
	 * @param resident     Resident being teleported.
	 * @param loc          Location the resident will go to.
	 * @param ignoreWarmup True if they will teleport instantly, false to use
	 *                     TeleportWarmupTime from the config.
	 */
	private static void initiatePluginTeleport(Resident resident, Location loc, boolean ignoreWarmup) {
		final Player player = resident.getPlayer();
		if (player == null)
			return;
		
		plugin.getScheduler().runLater(player, () -> resident.getPlayer().teleportAsync(loc, TeleportCause.PLUGIN),
			ignoreWarmup ? 0 : TownySettings.getTeleportWarmupTime() * 20L);
	}

	@SuppressWarnings("unused")
	private static void initiatePluginTeleport(Resident resident, CompletableFuture<Location> loc, boolean ignoreWarmup) {
		loc.thenAccept(location -> initiatePluginTeleport(resident, location, ignoreWarmup));
	}
	
	private static boolean hasPerm(Player player, PermissionNodes node) {
		return TownyUniverse.getInstance().getPermissionSource().testPermission(player, node.getNode());
	}

	/**
	 * On some servers, when the player is teleported the chunk they left will
	 * unload before the server ticks, causing any pets they have following them to
	 * be abandoned. This method will cause a chunk to remain loaded long enough for
	 * the pets to be teleported to the player naturally.
	 * 
	 * @param wc WorldCoord from which the player is leaving.
	 */
	public static void addAndRemoveChunkTicket(WorldCoord wc) {
		wc.loadChunks();
		Towny.getPlugin().getScheduler().runAsyncLater(() -> wc.unloadChunks(), 20L);
	}
}
