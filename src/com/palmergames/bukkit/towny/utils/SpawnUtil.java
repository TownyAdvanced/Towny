package com.palmergames.bukkit.towny.utils;

import java.util.List;
import java.util.Objects;

import com.palmergames.bukkit.towny.event.NationSpawnEvent;
import com.palmergames.bukkit.towny.event.SpawnEvent;
import com.palmergames.bukkit.towny.event.TownSpawnEvent;
import com.palmergames.bukkit.towny.event.teleport.ResidentSpawnEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.economy.Account;
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
import com.palmergames.bukkit.towny.object.NationSpawnLevel;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.SpawnType;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownSpawnLevel;
import com.palmergames.bukkit.towny.object.EconomyAccount;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask;
import com.palmergames.bukkit.towny.tasks.CooldownTimerTask.CooldownType;

public class SpawnUtil {

	private static Towny plugin;

	public static void initialize(Towny plugin) {
		SpawnUtil.plugin = plugin;
	}

	/**
	 * Central Util for /res, /t, /n, /ta spawn commands.
	 * 
	 * @param player       - Player using spawn command.
	 * @param split        - Remaining command arguments, used primarily for
	 *                     outposts.
	 * @param townyObject  - Either a town or nation depending on source command.
	 * @param notAffordMSG - Message shown when a player cannot afford their
	 *                     teleport.
	 * @param outpost      - Whether this is an outpost or not.
	 * @param ignoreWarn   - Whether to show confirmation for payment or just pay 
	 *                     without confirmation.
	 * @param spawnType    - SpawnType.RESIDENT/TOWN/NATION
	 * @throws TownyException - Thrown if any of the vital conditions are not met.
	 */
	public static void sendToTownySpawn(Player player, String[] split, TownyObject townyObject, String notAffordMSG, boolean outpost, boolean ignoreWarn, SpawnType spawnType) throws TownyException {
		try {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident == null)
			throw new TownyException(Translatable.of("msg_err_not_registered_1", player.getName()));
			
		// Test if the resident is in a teleport cooldown.
		if (TownySettings.getSpawnCooldownTime() > 0
				&& CooldownTimerTask.hasCooldown(resident.getName(), CooldownType.TELEPORT))
			throw new TownyException(Translatable.of("msg_err_cannot_spawn_x_seconds_remaining", CooldownTimerTask.getCooldownRemaining(resident.getName(), CooldownType.TELEPORT)));

		// Disallow jailed players from teleporting.
		if (resident.isJailed())
			throw new TownyException(Translatable.of("msg_cannot_spawn_while_jailed"));

		Town town = null;
		Nation nation = null;
		Location spawnLoc = null;
		TownSpawnLevel townSpawnPermission = null;
		NationSpawnLevel nationSpawnPermission = null;
		boolean isTownyAdmin = townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_SPAWN_ADMIN.getNode());

		// Figure out which Town/NationSpawnLevel this is.
		// Resolve where the spawnLoc will be.
		switch (spawnType) {
		case RESIDENT:
			if (resident.hasTown())
				town = resident.getTown();
			if (TownySettings.getBedUse() && player.getBedSpawnLocation() != null)
				spawnLoc = player.getBedSpawnLocation();
			else if (town != null)
				spawnLoc = town.getSpawn();
			else
				spawnLoc = plugin.getCache(player).getLastLocation().getWorld().getSpawnLocation();

			if (isTownyAdmin) {
				townSpawnPermission = TownSpawnLevel.ADMIN;
			} else {
				townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
			}
			break;

		case TOWN:
			town = (Town) townyObject;
			if (outpost) {
				if (!town.hasOutpostSpawn())
					throw new TownyException(Translatable.of("msg_err_outpost_spawn"));

				Integer index = null;
				try {
					if (!split[split.length - 1].contains("name:")) {
						index = Integer.parseInt(split[split.length - 1]);
					} else { // So now it say's name:123
						split[split.length - 1] = split[split.length - 1].replace("name:", "").replace("_", " ");
						for (Location loc : town.getAllOutpostSpawns()) {
							TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(loc);
							if (tboutpost != null) {
								String name = !tboutpost.hasPlotObjectGroup() ? tboutpost.getName() : tboutpost.getPlotObjectGroup().getName();
								if (name.startsWith(split[split.length - 1])) {
									index = 1 + town.getAllOutpostSpawns().indexOf(loc);
								}
							}
						}
						if (index == null) { // If it persists to be null, so it's not been given a value, set it to the
												// fallback (1).
							index = 1;
						}
					}
				} catch (NumberFormatException e) {
					// invalid entry so assume the first outpost, also note: We DO NOT HAVE a number
					// now, which means: if you type abc, you get brought to that outpost.
					// Let's consider the fact however: an outpost name begins with "123" and there
					// are 123 Outposts. Then we put the prefix name:123 and that solves that.
					index = 1;
					// Trying to get Outpost names.
					split[split.length - 1] = split[split.length - 1].replace("_", " ");
					for (Location loc : town.getAllOutpostSpawns()) {
						TownBlock tboutpost = TownyAPI.getInstance().getTownBlock(loc);
						if (tboutpost != null) {
							String name = !tboutpost.hasPlotObjectGroup() ? tboutpost.getName() : tboutpost.getPlotObjectGroup().getName();
							if (name.startsWith(split[split.length - 1].toLowerCase())) {
								index = 1 + town.getAllOutpostSpawns().indexOf(loc);
							}
						}
					}
				} catch (ArrayIndexOutOfBoundsException i) {
					// Number not present so assume the first outpost.
					index = 1;
				}

				if (TownySettings.isOutpostLimitStoppingTeleports() && TownySettings.isOutpostsLimitedByLevels()
						&& town.isOverOutpostLimit() && (Math.max(1, index) > town.getOutpostLimit())) {
					throw new TownyException(Translatable.of("msg_err_over_outposts_limit", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
				}

				spawnLoc = town.getOutpostSpawn(Math.max(1, index));
			} else
				spawnLoc = town.getSpawn();

			// Determine conditions
			if (isTownyAdmin) {
				townSpawnPermission = TownSpawnLevel.ADMIN;
			} else if ((split.length == 0) && (!outpost)) {
				townSpawnPermission = TownSpawnLevel.TOWN_RESIDENT;
			} else {
				// split.length > 1
				if (!resident.hasTown()) {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				} else if (resident.getTown() == town) {
					townSpawnPermission = outpost ? TownSpawnLevel.TOWN_RESIDENT_OUTPOST : TownSpawnLevel.TOWN_RESIDENT;
				} else if (resident.hasNation() && town.hasNation()) {
					Nation playerNation = resident.getTown().getNation();
					Nation targetNation = town.getNation();

					if (playerNation == targetNation) {
						if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
							throw new TownyException(Translatable.of("msg_err_ally_isnt_public", town));
						else
							townSpawnPermission = TownSpawnLevel.PART_OF_NATION;
					} else if (targetNation.hasEnemy(playerNation)) {
						// Prevent enemies from using spawn travel.
						throw new TownyException(Translatable.of("msg_err_public_spawn_enemy"));
					} else if (targetNation.hasAlly(playerNation)) {
						if (!town.isPublic() && TownySettings.isAllySpawningRequiringPublicStatus())
							throw new TownyException(Translatable.of("msg_err_ally_isnt_public", town));
						else
							townSpawnPermission = TownSpawnLevel.NATION_ALLY;
					} else {
						townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
					}
				} else {
					townSpawnPermission = TownSpawnLevel.UNAFFILIATED;
				}
			}

			TownyMessaging.sendDebugMsg(townSpawnPermission.toString() + " " + townSpawnPermission.isAllowed(town));
			townSpawnPermission.checkIfAllowed(plugin, player, town);

			// Check the permissions
			if (!(isTownyAdmin || ((townSpawnPermission == TownSpawnLevel.UNAFFILIATED) ? town.isPublic()
					: townSpawnPermission.hasPermissionNode(plugin, player, town))))
				throw new TownyException(Translatable.of("msg_err_not_public"));

			// Prevent outlaws from spawning into towns they're considered an outlaw in.
			if (!isTownyAdmin && town.hasOutlaw(resident))
					throw new TownyException(Translatable.of("msg_error_cannot_town_spawn_youre_an_outlaw_in_town", town));

			break;
		case NATION:
			nation = (Nation) townyObject;
			spawnLoc = nation.getSpawn();

			// Determine conditions
			if (isTownyAdmin) {
				nationSpawnPermission = NationSpawnLevel.ADMIN;
			} else if (split.length == 0) {
				nationSpawnPermission = NationSpawnLevel.PART_OF_NATION;
			} else {
				// split.length > 1
				if (!resident.hasTown()) {
					nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
				} else if (resident.hasNation()) {
					Nation playerNation = resident.getTown().getNation();

					if (playerNation == nation) {
						nationSpawnPermission = NationSpawnLevel.PART_OF_NATION;
					} else if (nation.hasEnemy(playerNation)) {
						// Prevent enemies from using spawn travel.
						throw new TownyException(Translatable.of("msg_err_public_spawn_enemy"));
					} else if (nation.hasAlly(playerNation)) {
						nationSpawnPermission = NationSpawnLevel.NATION_ALLY;
					} else {
						nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
					}
				} else {
					nationSpawnPermission = NationSpawnLevel.UNAFFILIATED;
				}
			}
			nationSpawnPermission.checkIfAllowed(plugin, player, nation);

			// Check the permissions
			if (!(isTownyAdmin || ((nationSpawnPermission == NationSpawnLevel.UNAFFILIATED) ? nation.isPublic()
					: nationSpawnPermission.hasPermissionNode(plugin, player, nation))))
				throw new TownyException(Translatable.of("msg_err_nation_not_public"));

			break;
		}

		// Prevent spawn travel while in disallowed zones (if configured.)
		if (!isTownyAdmin) {
			List<String> disallowedZones = TownySettings.getDisallowedTownSpawnZones();

			if (!disallowedZones.isEmpty()) {
				Town townAtPlayerLoc = TownyAPI.getInstance().getTown(player.getLocation());
				
				if (townAtPlayerLoc == null && disallowedZones.contains("unclaimed"))
					throw new TownyException(Translatable.of("msg_err_x_spawn_disallowed_from_x", spawnType.getTypeName(), Translatable.of("msg_the_wilderness")));
				if (townAtPlayerLoc != null) {
					if (townAtPlayerLoc.hasOutlaw(player.getName()) && disallowedZones.contains("outlaw"))
						throw new TownyException(Translatable.of("msg_err_outlawed_players_no_teleport"));
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

		double travelCost = 0.0;
		String spawnPermission = null;
		Account payee = null;
		if (TownyEconomyHandler.isActive() && 
				(!townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_TOWNYADMIN_TOWN_SPAWN_FREECHARGE.getNode()) 
						&& !townyUniverse.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_SPAWN_ADMIN_NOCHARGE.getNode()))) {
			// Figure out costs, payee and spawnPermmission slug for money.csv log.
			switch (spawnType) {
			case RESIDENT:
			case TOWN:
				// Taking whichever is smaller, the cost of the spawn price set by the town, or
				// the cost set in the config (which is the maximum a town can set their
				// spawncost to.)
				travelCost = Math.min(townSpawnPermission.getCost(town), townSpawnPermission.getCost());
				spawnPermission = String.format(spawnType.getTypeName() + " (%s)", townSpawnPermission);
				payee = town.getAccount();
				break;
			case NATION:
				// Taking whichever is smaller, the cost of the spawn price set by the nation,
				// or the cost set in the config (which is the maximum a nation can set their
				// spawncost to.)
				travelCost = Math.min(nationSpawnPermission.getCost(nation), nationSpawnPermission.getCost());
				spawnPermission = String.format(spawnType.getTypeName() + " (%s)", nationSpawnPermission);
				payee = nation.getAccount();
				break;
			}
			if (!TownySettings.isTownSpawnPaidToTown())	payee = EconomyAccount.SERVER_ACCOUNT;
			
			if (travelCost > 0 && (resident.getAccount().getHoldingBalance() < travelCost))
				throw new TownyException(notAffordMSG);
		}
		// Allow for a cancellable event right before a player would actually pay.
		if (!sendSpawnEvent(player, spawnType, spawnLoc)) {
			return;
		}
		
		// Cost to spawn, prompt with confirmation unless ignoreWarn is true.
		if (TownyEconomyHandler.isActive() && travelCost > 0) {
			final double finalCost = travelCost;
			final Account finalPayee = payee;
			final String finalSpawnPerm = spawnPermission;
			final Location finalLoc = spawnLoc;
			
			// Skipping the confirmation.
			if (ignoreWarn || !TownySettings.isSpawnWarnConfirmationUsed()) {
				if (resident.getAccount().payTo(finalCost, finalPayee, finalSpawnPerm)) {
					TownyMessaging.sendMsg(player, Translatable.of("msg_cost_spawn", TownyEconomyHandler.getFormattedBalance(finalCost)));
					initiateSpawn(player, finalLoc);
				}
			} else {
			// Sending the confirmation.
				String title = Translatable.of("msg_spawn_warn", TownyEconomyHandler.getFormattedBalance(travelCost)).forLocale(player);
				Confirmation.runOnAccept(() -> {		
					if (resident.getAccount().payTo(finalCost, finalPayee, finalSpawnPerm)) {
						TownyMessaging.sendMsg(player, Translatable.of("msg_cost_spawn", TownyEconomyHandler.getFormattedBalance(finalCost)));
						initiateSpawn(player, finalLoc);
					}
				})
				.setTitle(title)
				.sendTo(player);
			}
		// No Cost so skip confirmation system.
		} else {
			initiateSpawn(player, spawnLoc);
		}
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Fires cancellable events before allowing someone to spawn.
	 * 
	 * @param player - Player being spawned to a Towny location.
	 * @param type - SpawnType (RESIDENT, TOWN, NATION).
	 * @param spawnLoc - Location being spawned to.
	 * @return true if uncancelled.
	 */
	public static boolean sendSpawnEvent(Player player, SpawnType type, Location spawnLoc) {
		SpawnEvent spawnEvent = null;
		switch (type) {
			case RESIDENT:
				spawnEvent = new ResidentSpawnEvent(player, player.getLocation(), spawnLoc);
				break;
		
			case TOWN:
				spawnEvent = new TownSpawnEvent(player, player.getLocation(), spawnLoc);
				break;
			case NATION:
				spawnEvent = new NationSpawnEvent(player, player.getLocation(), spawnLoc);
				break;
		}
	
		Bukkit.getPluginManager().callEvent(spawnEvent);
		
		if (spawnEvent.isCancelled()) {
			TownyMessaging.sendErrorMsg(player, spawnEvent.getCancelMessage());
			return false;
		}

		return true;
	}
	
	/**
	 * Handles the final spawning for residents/towns/nations.
	 * 
	 * @param player - Player being spawned. 
	 * @param spawnLoc - Location being spawned to.
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
	 * Handles moving outlaws from outside of towns they are outlawed in.
	 * @param town Town which they are outlawed in.
	 * @param outlaw Resident which is outlawed and being moved.
	 */
	public static void outlawTeleport(Town town, Resident outlaw) {
		Location spawnLocation = town.getWorld().getSpawnLocation();
		Player outlawedPlayer = outlaw.getPlayer();
		if (!TownySettings.getOutlawTeleportWorld().equals("")) {
			spawnLocation = Objects.requireNonNull(Bukkit.getWorld(TownySettings.getOutlawTeleportWorld())).getSpawnLocation();
		}
		// sets tp location to their bedspawn only if it isn't in the town they're being teleported from.
		Location bed = outlawedPlayer.getBedSpawnLocation();
		if (bed != null && TownyAPI.getInstance().getTown(bed) != town)
			spawnLocation = bed;
		if (outlaw.hasTown() && TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer) != null)
			spawnLocation = TownyAPI.getInstance().getTownSpawnLocation(outlawedPlayer);
		TownyMessaging.sendMsg(outlaw, Translatable.of("msg_outlaw_kicked", town));
		PaperLib.teleportAsync(outlaw.getPlayer(), spawnLocation, TeleportCause.PLUGIN);
	}
	
	public static void jailAwayTeleport(Resident jailed) {
		initiatePluginTeleport(jailed, getIdealLocation(jailed), false);
	}
	
	public static void jailTeleport(Resident jailed) {
		initiatePluginTeleport(jailed, jailed.getJailSpawn(), false);
	}

	/**
	 * Get the best location an resident can be teleported to
	 * @param resident
	 * @return bed spawn OR town spawn OR last world spawn
	 */
	private static Location getIdealLocation(Resident resident) {
		Town town = resident.getTownOrNull();
		Location loc = resident.getPlayer().getWorld().getSpawnLocation();

		if (town != null && town.hasSpawn())
			loc = town.getSpawnOrNull();

		Location bed = resident.getPlayer().getBedSpawnLocation();
		if (bed != null)
			loc = bed;
		
		return loc;
	}
	
	private static void initiatePluginTeleport(Resident resident, Location loc, boolean ignoreWarmup) {
		Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> PaperLib.teleportAsync(resident.getPlayer(), loc, TeleportCause.PLUGIN),
			ignoreWarmup ? 0 : TownySettings.getTeleportWarmupTime() * 20);
	}
}
