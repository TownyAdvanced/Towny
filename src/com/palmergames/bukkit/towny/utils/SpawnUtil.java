package com.palmergames.bukkit.towny.utils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import com.palmergames.bukkit.towny.event.NationSpawnEvent;
import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.event.TownSpawnEvent;
import com.palmergames.bukkit.towny.event.teleport.ResidentSpawnEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.economy.Account;
import com.palmergames.bukkit.towny.object.spawnlevel.NationSpawnLevel;
import com.palmergames.bukkit.towny.object.spawnlevel.TownSpawnLevel;

import io.papermc.lib.PaperLib;
import org.bukkit.Bukkit;
import org.bukkit.Location;
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
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.permissions.TownyPermissionSource;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;

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

		// Get resident while testing they aren't on a cooldown or jailed.
		Resident resident = getResident(player);

		// Set up town and nation variables.
		final Town town = switch (spawnType) {
			case RESIDENT -> resident.getTownOrNull();
			case TOWN -> (Town) townyObject;
			default -> null;
		};

		final Nation nation = spawnType == SpawnType.NATION ? (Nation) townyObject : null;
		
		// Is this an admin spawning?
		boolean isTownyAdmin = isTownyAdmin(player);

		// Set up either the townSpawnLevel or nationSpawnLevel variable.
		// This determines whether a spawn is considered town, nation, public, allied, admin via TownSpawnLevel and NationSpawnLevel objects.
		// Accounts for costs, permission, config settings and messages.
		final TownSpawnLevel townSpawnLevel = switch (spawnType) {
			case RESIDENT -> isTownyAdmin ? TownSpawnLevel.ADMIN : TownSpawnLevel.TOWN_RESIDENT;
			case TOWN -> isTownyAdmin ? TownSpawnLevel.ADMIN : getTownSpawnLevel(player, resident, town, outpost, split.length == 0);
			default -> null;
		};
		
		final NationSpawnLevel nationSpawnLevel = spawnType == SpawnType.NATION ? isTownyAdmin ? NationSpawnLevel.ADMIN : getNationSpawnLevel(player, resident, nation, split.length == 0) : null;

		// Prevent spawn travel while in the config's disallowed zones.
		// Throws a TownyException if the player is disallowed.
		if (!isTownyAdmin)
			testDisallowedZones(player, resident, spawnType, TownySettings.getDisallowedTownSpawnZones());

		// Get cost required to spawn.
		final double travelCost = getTravelCost(player, town, nation, townSpawnLevel, nationSpawnLevel, spawnType);

		// Don't allow if they cannot pay.
		if (travelCost > 0 && !resident.getAccount().canPayFromHoldings(travelCost))
			throw new TownyException(notAffordMSG);

		getSpawnLoc(player, town, nation, spawnType, outpost, split).thenAccept(spawnLoc -> {
			// Fire a cancellable event right before a player would actually pay.
			// Throws a TownyException if the event is cancelled.
			try {
				sendSpawnEvent(player, spawnType, spawnLoc);
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				return;
			}

			// There is a cost to spawn, prompt with confirmation unless ignoreWarn is true.
			if (travelCost > 0) {
				// Get paymentMsg for the money.csv and the Account being paid.
				final String paymentMsg = getPaymentMsg(townSpawnLevel, nationSpawnLevel, spawnType);
				final Account payee = TownySettings.isTownSpawnPaidToTown() ? getPayee(town, nation, spawnType) : EconomyAccount.SERVER_ACCOUNT;
				initiateCostedSpawn(player, resident, spawnLoc, travelCost, payee, paymentMsg, ignoreWarn);
				// No Cost so skip confirmation system.
			} else
				initiateSpawn(player, spawnLoc);
		});
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
		PaperLib.getBedSpawnLocationAsync(outlawedPlayer, true).thenAccept(bed -> {
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
		initiatePluginTeleport(jailed, getIdealLocation(jailed), false);
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
	 * Get a resident or throw an exception.
	 * 
	 * @param player Player to get a resident from.
	 * @return resident Resident which will be teleporting.
	 * @throws TownyException thrown when the resident is null, on cooldown or
	 *                        jailed.
	 */
	private static Resident getResident(Player player) throws TownyException {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident == null)
			throw new TownyException(Translatable.of("msg_err_not_registered_1", player.getName()));
			
		// Test if the resident is in a teleport cooldown.
		if (TownySettings.getSpawnCooldownTime() > 0 && CooldownTimerTask.hasCooldown(resident.getName(), CooldownType.TELEPORT))
			throw new TownyException(Translatable.of("msg_err_cannot_spawn_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(resident.getName(), CooldownType.TELEPORT)));

		// Disallow jailed players from teleporting.
		if (resident.isJailed())
			throw new TownyException(Translatable.of("msg_cannot_spawn_while_jailed"));

		return resident;
	}

	/**
	 * Is this a player with the admin spawn node.
	 * 
	 * @param player Player to test permissions for.
	 * @return true if this player has towny.admin.spawn in their permission nodes.
	 */
	private static boolean isTownyAdmin(Player player) {
		return TownyUniverse.getInstance().getPermissionSource().isTownyAdmin(player);
	}
	
	/**
	 * Is this player not charged for spawning?
	 * 
	 * @param player Player to test permission for.
	 * @return true if this player has either free spawning nodes.
	 */
	private static boolean playerHasFreeSpawn(Player player) {
		TownyPermissionSource perms = TownyUniverse.getInstance().getPermissionSource();
		return perms.testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode())
				|| perms.testPermission(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCHARGE.getNode());
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
			if (!resident.hasTown()) {
				townSpawnLevel = TownSpawnLevel.UNAFFILIATED;
			} else if (resident.getTownOrNull() == town) {
				townSpawnLevel = outpost ? TownSpawnLevel.TOWN_RESIDENT_OUTPOST : TownSpawnLevel.TOWN_RESIDENT;
			} else if (resident.hasNation() && town.hasNation()) {
				Nation playerNation = resident.getNationOrNull();
				Nation targetNation = town.getNationOrNull();

				if (playerNation == targetNation) {
					if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
						throw new TownyException(Translatable.of("msg_err_ally_isnt_public", town));
					else
						townSpawnLevel = TownSpawnLevel.PART_OF_NATION;
				} else if (targetNation.hasEnemy(playerNation)) {
					// Prevent enemies from using spawn travel.
					throw new TownyException(Translatable.of("msg_err_public_spawn_enemy"));
				} else if (targetNation.hasAlly(playerNation)) {
					if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
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
					yield PaperLib.getBedSpawnLocationAsync(player, true).thenApply(bedLoc -> {
						if (bedLoc != null)
							return bedLoc;
						else if (town != null && town.hasSpawn())
							return town.getSpawnOrNull();
						else
							return plugin.getCache(player).getLastLocation().getWorld().getSpawnLocation();
					});
				} else if (town != null && town.hasSpawn())
					yield CompletableFuture.completedFuture(town.getSpawnOrNull());
				else
					yield CompletableFuture.completedFuture(plugin.getCache(player).getLastLocation().getWorld().getSpawnLocation());
			case TOWN:
				if (outpost)
					yield CompletableFuture.completedFuture(getOutpostSpawnLocation(town, split));
				else
					yield CompletableFuture.completedFuture(town.getSpawn());
			case NATION:
				yield CompletableFuture.completedFuture(nation.getSpawn());
		};
	}

	/**
	 * Complicated code that parses the given split to a named, numbered or
	 * unnumbered outpost.
	 * 
	 * @param town  Town which is being spawned to.
	 * @param split String[] arguments to parse the outpost location from.
	 * @return Location of the town's outpost spawn.
	 * @throws TownyException thrown when there are no outposts, or the outpost
	 *                        limit was capped.
	 */
	private static Location getOutpostSpawnLocation(Town town, String[] split) throws TownyException {
		if (!town.hasOutpostSpawn())
			throw new TownyException(Translatable.of("msg_err_outpost_spawn"));

		// No arguments, send them to the first outpost.
		if (split.length == 0)
			return town.getOutpostSpawn(1);

		Integer index = null;
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

		if (TownySettings.isOutpostLimitStoppingTeleports() 
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
				throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.getTypeName(), Translatable.of("msg_the_wilderness")));

			if (townAtPlayerLoc != null) {
				if (townAtPlayerLoc.hasOutlaw(player.getName()) && disallowedZones.contains("outlaw"))
					throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", "RTP", Translatable.of("msg_a_town_you_are_outlawed_in")));
				if (resident.hasNation() && townAtPlayerLoc.hasNation()) {
					if (CombatUtil.isEnemy(resident.getTownOrNull(), townAtPlayerLoc) && disallowedZones.contains("enemy"))
						throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.getTypeName(), Translatable.of("msg_enemy_areas")));
					Nation townLocNation = townAtPlayerLoc.getNationOrNull();
					Nation resNation = resident.getNationOrNull();
					if (!townLocNation.hasAlly(resNation) && !townLocNation.hasEnemy(resNation) && disallowedZones.contains("neutral"))
						throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.getTypeName(), Translatable.of("msg_neutral_towns")));
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
		case RESIDENT -> town == null ? EconomyAccount.SERVER_ACCOUNT : town.getAccount(); 
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
	private static void sendSpawnEvent(Player player, SpawnType spawnType, Location spawnLoc) throws TownyException {
		SpawnEvent spawnEvent = getSpawnEvent(player, spawnType, spawnLoc);
		Bukkit.getPluginManager().callEvent(spawnEvent);
		if (spawnEvent.isCancelled())
			throw new TownyException(spawnEvent.getCancelMessage());
	}
	
	/**
	 * Get a SpawnEvent.
	 * 
	 * @param player    Player spawning.
	 * @param spawnType SpawnType.
	 * @param spawnLoc  Location that the player will spawn at.
	 * @return SpawnEvent to be called.
	 */
	private static SpawnEvent getSpawnEvent(Player player, SpawnType spawnType, Location spawnLoc) {
		return switch(spawnType) {
		case RESIDENT -> new ResidentSpawnEvent(player, player.getLocation(), spawnLoc);
		case TOWN -> new TownSpawnEvent(player, player.getLocation(), spawnLoc);
		case NATION -> new NationSpawnEvent(player, player.getLocation(), spawnLoc);
		};
	}

	/**
	 * Handles the final spawning for residents/towns/nations.
	 * 
	 * @param player   Player being spawned.
	 * @param spawnLoc Location being spawned to.
	 */
	private static void initiateSpawn(Player player, Location spawnLoc) {
		if (TownyTimerHandler.isTeleportWarmupRunning() && !TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOWARMUP.getNode())) {
			// Use teleport warmup
			TownyMessaging.sendMsg(player, Translatable.of("msg_town_spawn_warmup", TownySettings.getTeleportWarmupTime()));
			TownyAPI.getInstance().requestTeleport(player, spawnLoc);
		} else {
			// Don't use teleport warmup
			if (player.getVehicle() != null)
				player.getVehicle().eject();
			PaperLib.teleportAsync(player, spawnLoc, TeleportCause.COMMAND);
			if (TownySettings.getSpawnCooldownTime() > 0 && !TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCOOLDOWN.getNode()))
				CooldownTimerTask.addCooldownTimer(player.getName(), CooldownType.TELEPORT);
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
	private static void initiateCostedSpawn(Player player, Resident resident, Location spawnLoc, double travelCost, Account payee, String paymentMsg, boolean ignoreWarn) {
		if (ignoreWarn || !TownySettings.isSpawnWarnConfirmationUsed())
			// Skipping the confirmation.
			payAndThenSpawn(player, resident, spawnLoc, travelCost, payee, paymentMsg);
		else
			// Sending the confirmation.
			Confirmation.runOnAccept(() -> payAndThenSpawn(player, resident, spawnLoc, travelCost, payee, paymentMsg))
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
	private static void payAndThenSpawn(Player player, Resident resident, Location spawnLoc, double travelCost, Account payee, String paymentMsg) {
		if (resident.getAccount().payTo(travelCost, payee, paymentMsg)) {
			TownyMessaging.sendMsg(player, Translatable.of("msg_cost_spawn", TownyEconomyHandler.getFormattedBalance(travelCost)));
			resident.setTeleportCost(travelCost);
			resident.setTeleportAccount(payee);
			initiateSpawn(player, spawnLoc);
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
		return PaperLib.getBedSpawnLocationAsync(resident.getPlayer(), true).thenApply(bed -> bed == null ? finalLoc : bed);
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
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> PaperLib.teleportAsync(resident.getPlayer(), loc, TeleportCause.PLUGIN),
			ignoreWarmup ? 0 : TownySettings.getTeleportWarmupTime() * 20L);
	}
	
	private static void initiatePluginTeleport(Resident resident, CompletableFuture<Location> loc, boolean ignoreWarmup) {
		loc.thenAccept(location -> initiatePluginTeleport(resident, location, ignoreWarmup));
	}
}
