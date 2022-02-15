package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.damage.TownBlockPVPTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyDispenserDamageEntityEvent;
import com.palmergames.bukkit.towny.event.damage.TownyFriendlyFireTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.event.damage.WildernessPVPTestEvent;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Axolotl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Wolf;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.projectiles.BlockProjectileSource;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 
 * @author ElgarL,Shade,LlmDl
 * 
 */
public class CombatUtil {

	/**
	 * @deprecated As of 0.97.5.9, please use {@link #preventDamageCall(Entity, Entity, DamageCause)}
	 */
	@Deprecated
	public static boolean preventDamageCall(Towny plugin, Entity attacker, Entity defender, DamageCause cause) {
		return preventDamageCall(attacker, defender, cause);
	}

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param attacker - Entity attacking the Defender
	 * @param defender - Entity defending from the Attacker
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(Entity attacker, Entity defender, DamageCause cause) {

		TownyWorld world = TownyAPI.getInstance().getTownyWorld(defender.getWorld().getName());

		// World using Towny
		if (world == null || !world.isUsingTowny())
			return false;

		Player a = null;
		Player b = null;
		
		Entity directSource = attacker;

		/*
		 * Find the shooter if this is a projectile.
		 */
		if (attacker instanceof Projectile projectile) {
			
			Object source = projectile.getShooter();
			
			if (source instanceof Entity entity)
				directSource = entity;
			else if (source instanceof BlockProjectileSource blockProjectileSource) {
				if (CombatUtil.preventDispenserDamage(blockProjectileSource.getBlock(), defender, cause))
					return true;
			}
		}

		if (directSource instanceof Player player)
			a = player;
		if (defender instanceof Player player)
			b = player;

		// Allow players to injure themselves
		if (a == b && a != null && b != null)
			return false;

		return preventDamageCall(world, attacker, defender, a, b, cause);
	}

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
	 * 
	 * @param world - World in which DamageCall was issued
	 * @param attackingEntity - Entity attacking
	 * @param defendingEntity - Entity defending
	 * @param attackingPlayer - Player attacking
	 * @param defendingPlayer - Player defending
	 * @param cause - The DamageCause behind this DamageCall.
	 * @return true if we should cancel.
	 */
	private static boolean preventDamageCall(TownyWorld world, Entity attackingEntity, Entity defendingEntity, Player attackingPlayer, Player defendingPlayer, DamageCause cause) {

		Projectile projectileAttacker = null;
		if (attackingEntity instanceof Projectile projectile) {
			projectileAttacker = projectile;
			
			if (projectile.getShooter() instanceof Entity entity)
				attackingEntity = entity;
		}
		
		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defendingEntity.getLocation());
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attackingEntity.getLocation());
		/*
		 * We have an attacking player
		 */
		if (attackingPlayer != null) {

			boolean cancelled = false;
			
			/*
			 * Defender is a player.
			 */
			if (defendingPlayer != null) {
				
				/*
				 * Both townblocks are not Arena plots.
				 */
				if (!isArenaPlot(attackerTB, defenderTB)) {
					/*
					 * Check if we are preventing friendly fire between allies
					 * Check the attackers TownBlock and it's Town for their PvP status, else the world.
					 * Check the defenders TownBlock and it's Town for their PvP status, else the world.
					 */
					cancelled = preventFriendlyFire(attackingPlayer, defendingPlayer, world) || preventPvP(world, attackerTB) || preventPvP(world, defenderTB) || preventJailedPVP(defendingPlayer, attackingPlayer);
				}

				/*
				 * A player has attempted to damage a player. Throw a TownPlayerDamagePlayerEvent.
				 */
				TownyPlayerDamagePlayerEvent event = new TownyPlayerDamagePlayerEvent(defendingPlayer.getLocation(), defendingPlayer, cause, defenderTB, cancelled, attackingPlayer);
				BukkitTools.getPluginManager().callEvent(event);

				// A cancelled event should contain a message.
				if (event.isCancelled() && event.getMessage() != null)
					TownyMessaging.sendErrorMsg(attackingPlayer, event.getMessage());
				
				return event.isCancelled();

			/*
			 * Defender is not a player.
			 */
			} else {
				/*
				 * First test protections for Non-Player defenders who are being protected
				 * because they are specifically in Town-Claimed land.
				 */
				if (defenderTB != null) {
					
					/*
					 * Protect tamed dogs in town land which are not owned by the attacking player.
					 */
					if (defendingEntity instanceof Wolf wolf) {
						if (!isOwner(wolf, attackingPlayer)) {
							if (EntityTypeUtil.isProtectedEntity(defendingEntity))
								return !(defenderTB.getPermissions().pvp || TownyActionEventExecutor.canDestroy(attackingPlayer, wolf.getLocation(), Material.STONE));
						} else
							return false;
					}
					
					/*
					 * Farm Animals - based on whether this is allowed using the PlayerCache and then a cancellable event.
					 */
					if (defenderTB.getType() == TownBlockType.FARM && TownySettings.getFarmAnimals().contains(defendingEntity.getType().toString()))
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.WHEAT);

					/*
					 * Config's protected entities: Animals,WaterMob,NPC,Snowman,ArmorStand,Villager
					 */
					if (EntityTypeUtil.isProtectedEntity(defendingEntity)) 						
						return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), Material.DIRT);
				}
				
				/*
				 * Protect specific entity interactions (faked with Materials).
				 * Requires destroy permissions in either the Wilderness or in Town-Claimed land.
				 */
				Material material = switch (defendingEntity.getType()) {
					/*
					 * Below are the entities we specifically want to protect with this test.
					 * Any other entity will mean that block is still null and will not be
					 * tested with a destroy test.
					 */
					case ITEM_FRAME:
					case GLOW_ITEM_FRAME:
					case PAINTING:
					case ARMOR_STAND:
					case ENDER_CRYSTAL:
					case MINECART:
					case MINECART_CHEST:
					case MINECART_FURNACE:
					case MINECART_COMMAND:
					case MINECART_HOPPER:
						yield EntityTypeUtil.parseEntityToMaterial(defendingEntity.getType());
					default:
						yield null;
				};

				if (material != null) {
					//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
					return !TownyActionEventExecutor.canDestroy(attackingPlayer, defendingEntity.getLocation(), material);
				}
			}

		/*
		 * This is not an attack by a player....
		 */
		} else {

			/*
			 * If Defender is a player, Attacker is not.
			 */
			if (defendingPlayer != null) {

				/*
				 * If attackingEntity is a tamed Wolf and...
				 * Defender is a player and...
				 * Either player or wolf is in a non-PVP area
				 * 
				 * Prevent pvp and remove Wolf targeting.
				 */
				if (attackingEntity instanceof Wolf && (preventPvP(world, attackerTB) || preventPvP(world, defenderTB))) {
					((Wolf) attackingEntity).setTarget(null);
					((Wolf) attackingEntity).setAngry(false);
					return true;
				}
				
				if (attackingEntity instanceof LightningStrike 
					&& world.hasTridentStrike(attackingEntity.getEntityId())
					&& preventPvP(world, defenderTB)) {
					return true;
				}
				
			/*
			 * DefendingEntity is not a player.
			 * This is now non-player vs non-player damage.
			 */
			} else {
				
				/*
				 * The defending non-player is in the wilderness, do not prevent this combat.
				 */
				if (defenderTB == null)
					return false;

			    /*
			     * Prevents projectiles fired by non-players harming non-player entities.
			     * Could be a monster or it could be a dispenser.
			     */
				if (projectileAttacker != null && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					return true;
				}

				/*
				* Allow wolves to attack unprotected entites (such as skeletons), but not protected ones.
				*/
				if (attackingEntity instanceof Wolf wolf && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					if (isATamedWolfWithAOnlinePlayer(wolf)) {
						Player owner = BukkitTools.getPlayer(wolf.getOwner().getName());
						return !PlayerCacheUtil.getCachePermission(owner, defendingEntity.getLocation(), Material.AIR, ActionType.DESTROY);
					} else {
						wolf.setTarget(null);
						wolf.setAngry(false);
						return true;
					}
				}
				
				if (attackingEntity.getType().name().equals("AXOLOTL") && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), defendingEntity)) {
					//TODO: Targeting not actually removed
					((Axolotl) attackingEntity).setTarget(null);
					return true;
				}
			}
		}
		return false;
	}

	private static boolean preventJailedPVP(Player defendingPlayer, Player attackingPlayer) {
		if (TownySettings.doJailPlotsPreventPVP()) {
			Resident defendingResident = TownyAPI.getInstance().getResident(defendingPlayer.getUniqueId());
			Resident attackingResident = TownyAPI.getInstance().getResident(attackingPlayer.getUniqueId());
			TownBlock defTB = TownyAPI.getInstance().getTownBlock(defendingPlayer);
			TownBlock atkTB = TownyAPI.getInstance().getTownBlock(attackingPlayer);
			if (defendingResident == null || attackingResident == null)
				return false;
			if (defendingResident.isJailed() && defTB != null && defTB.isJail() || attackingResident.isJailed() && atkTB != null && atkTB.isJail())
				return true;
		}
		return false;
			
	}

	/**
	 * Is PvP disabled in this TownBlock?
	 * 
	 * @param townBlock - TownBlock to check
	 * @param world - World to check if TownBlock is NULL
	 * @return true if PvP is disallowed
	 */
	public static boolean preventPvP(TownyWorld world, TownBlock townBlock) {

		if (townBlock != null) {

			/*
			 * Check the attackers TownBlock and it's Town for their PvP status.
			 */
			TownBlockPVPTestEvent event = new TownBlockPVPTestEvent(townBlock, isPvP(townBlock));
			Bukkit.getPluginManager().callEvent(event);
			return !event.isPvp();

		} else {

			/*
			 * Attacker isn't in a TownBlock so check the wilderness PvP status.
			 */
			WildernessPVPTestEvent event = new WildernessPVPTestEvent(world, isWorldPvP(world));
			Bukkit.getPluginManager().callEvent(event);
			return !event.isPvp();
		}
	}
	
	private static boolean isPvP(@NotNull TownBlock townBlock) {
		
		if (townBlock.getTownOrNull().isAdminDisabledPVP())
			return false;

		// Checks PVP perm: 1. Plot PVP, 2. Town PVP, 3. World Force PVP 
		if (!townBlock.getPermissions().pvp && !townBlock.getTownOrNull().isPVP() && !townBlock.getWorld().isForcePVP()) 
			return false;
		
		if (townBlock.isHomeBlock() && townBlock.getWorld().isForcePVP() && TownySettings.isForcePvpNotAffectingHomeblocks())
			return false;
		
		return true;
	}

	/**
	 * Is PvP enabled in this world?
	 * 
	 * @param world - World to check
	 * @return true if the world is PvP
	 */
	public static boolean isWorldPvP(TownyWorld world) {
		return (world.isForcePVP() || world.isPVP());
	}
	
	/**
	 * Should we be preventing friendly fire?
	 * 
	 * @param attacker - Attacking Player
	 * @param defender - Defending Player (receiving damage)
	 * @param world - TownyWorld being tested.
	 * @return true if we should cancel damage.
	 */
	public static boolean preventFriendlyFire(Player attacker, Player defender, TownyWorld world) {

		/*
		 * Don't block potion use (self damaging) on ourselves.
		 */
		if (attacker == defender)
			return false;

		if ((attacker != null) && (defender != null))
			if (!world.isFriendlyFireEnabled() && CombatUtil.isAlly(attacker.getName(), defender.getName())) {
				if (isArenaPlot(attacker, defender))
					return false;

				TownyFriendlyFireTestEvent event = new TownyFriendlyFireTestEvent(attacker, defender, world);
				Bukkit.getPluginManager().callEvent(event);
	
				if (!event.isPVP() && !event.getCancelledMessage().isEmpty())
					TownyMessaging.sendErrorMsg(attacker, event.getCancelledMessage());
	
				return !event.isPVP();
			}
		return false;
	}

	/**
	 * Returns true if both players are in an arena townblock.
	 * @param attacker Attacking Player
	 * @param defender Defending Player
	 * @return true if both player in an Arena plot.
	 */
	public static boolean isArenaPlot(Player attacker, Player defender) {
		TownBlock attackerTB = TownyAPI.getInstance().getTownBlock(attacker);
		TownBlock defenderTB = TownyAPI.getInstance().getTownBlock(defender);
		return isArenaPlot(attackerTB, defenderTB);
	}
	
	/**
	 * Return true if both TownBlocks are Arena plots.
	 * 
	 * @param defenderTB TownBlock being tested.
	 * @param attackerTB TownBlock being tested.
	 * @return true if both TownBlocks are Arena plots.
	 */
	public static boolean isArenaPlot(TownBlock attackerTB, TownBlock defenderTB) {

		if (defenderTB != null && attackerTB != null && defenderTB.getType() == TownBlockType.ARENA && attackerTB.getType() == TownBlockType.ARENA)
			return true;
		return false;
	}

	/**
	 * Is the defending resident an ally of the attacking resident?
	 * 
	 * @param attackingResident - Attacking Resident (String)
	 * @param defendingResident - Defending Resident (Receiving Damage; String)
	 * @return true if the defender is an ally of the attacker.
	 */
	public static boolean isAlly(String attackingResident, String defendingResident) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		Resident residentA = townyUniverse.getResident(attackingResident);
		Resident residentB = townyUniverse.getResident(defendingResident);
		
		// Fast-fail
		if (residentA == null || residentB == null || !residentA.hasTown() || !residentB.hasTown())
			return false;
		
		return isAlly(residentA.getTownOrNull(), residentB.getTownOrNull());
	}

	/**
	 * Is the resident B an ally of resident A?
	 *
	 * @param a - Resident A in comparison
	 * @param b - Resident B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isAlly(Resident a, Resident b) {
		// Fast-fail
		if (a == null || b == null || !a.hasTown() || !b.hasTown())
			return false;

		return isAlly(a.getTownOrNull(), b.getTownOrNull());
	}

	/**
	 * Is town b an ally of town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isAlly(Town a, Town b) {

		if (isSameTown(a, b))
			return true;
		if (a.hasAlly(b))
			return true;
		if (isSameNation(a, b))
			return true;
		if (a.hasNation() && b.hasNation() && a.getNationOrNull().hasAlly(b.getNationOrNull()))
			return true;
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are in the same nation.
	 */
	public static boolean isSameNation(Town a, Town b) {

		if (isSameTown(a, b))
			return true;
		if (a.hasNation() && b.hasNation() && a.getNationOrNull().equals(b.getNationOrNull()))
			return true;
		return false;
	}

	/**
	 * Is town b in a nation with town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if they are allies.
	 */
	public static boolean isSameTown(Town a, Town b) {

		return a == b;
	}

	/**
	 * Is resident a in a nation with resident b?
	 * 
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same nation.
	 */
	public static boolean isSameNation(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;
				
		return isSameNation(a.getTownOrNull(), b.getTownOrNull());
	}
	
	
	/**
	 * Is resident a in a town with resident b?
	 * @param a - Resident A in comparison.
	 * @param b - Resident B in comparison.
	 * @return true if they are in the same town.
	 */
	public static boolean isSameTown(Resident a, Resident b) {
		if (!a.hasTown() || !b.hasTown())
			return false;

		return isSameTown(a.getTownOrNull(), b.getTownOrNull());
	}

	/**
	 * Can resident a attack resident b?
	 * 
	 * @param a - Resident A in comparison
	 * @param b - Resident B in comparison
	 * @return true if they can attack.
	 * @deprecated since 0.97.3.0 use {@link CombatUtil#isEnemy(String, String)} or {@link CombatUtil#isEnemy(Town, Town)}  
	 */
	@Deprecated
	public static boolean canAttackEnemy(String a, String b) {
		return isEnemy(a, b);
	}

	/**
	 * Test if all the listed nations are allies
	 * 
	 * @param possibleAllies - List of Nations (List&lt;Nation&gt;)
	 * @return true if they are all allies
	 */
	public static boolean areAllAllies(List<Nation> possibleAllies) {

		if (possibleAllies.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleAllies.size() - 1; i++)
				if (!possibleAllies.get(i).hasAlly(possibleAllies.get(i + 1)))
					return false;
			return true;
		}
	}
	
	/**
	 * Test if all the listed residents are friends
	 * 
	 * @param possibleFriends - List of Residents (List&lt;Resident&gt;)
	 * @return true if they are all friends
	 */
	public static boolean areAllFriends(List<Resident> possibleFriends) {

		if (possibleFriends.size() <= 1)
			return true;
		else {
			for (int i = 0; i < possibleFriends.size() - 1; i++)
				if (!possibleFriends.get(i).hasFriend(possibleFriends.get(i + 1)))
					return false;
			return true;
		}
	}

	/**
	 * Is resident b an enemy of resident a?
	 * 
	 * @param a - Resident A in comparison (String)
	 * @param b - Resident B in comparison (String)
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(String a, String b) {
		Resident residentA = TownyUniverse.getInstance().getResident(a);
		Resident residentB = TownyUniverse.getInstance().getResident(b);
		
		// Fast fail.
		if (residentA == null || residentB == null || !residentA.hasTown() || !residentB.hasTown())
			return false;

		if (isEnemy(residentA.getTownOrNull(), residentB.getTownOrNull()))
			return true;
		return false;
	}

	/**
	 * Is the resident B an enemy of resident A?
	 *
	 * @param a - Resident A in comparison
	 * @param b - Resident B in comparison
	 * @return true if B is an enemy.
	 */
	public static boolean isEnemy(Resident a, Resident b) {
		// Fast-fail
		if (a == null || b == null || !a.hasTown() || !b.hasTown())
			return false;

		return isEnemy(a.getTownOrNull(), b.getTownOrNull());
	}

	/**
	 * Is town b an enemy of town a?
	 * 
	 * @param a - Town A in comparison
	 * @param b - Town B in comparison
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(Town a, Town b) {

		if (a.hasEnemy(b))
			return true;
		if (!a.hasNation() || !b.hasNation())
			return false;
		if (isSameTown(a, b) || isSameNation(a, b))
			return false;
		if (a.getNationOrNull().hasEnemy(b.getNationOrNull()))
			return true;
		return false;
	}

	/**
	 * Does this WorldCoord fall within a plot owned by an enemy town?
	 * 
	 * @param player - Player
	 * @param worldCoord - Location
	 * @return true if it is an enemy plot.
	 */
	public static boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		if (resident != null && resident.hasTown() && worldCoord.hasTownBlock())
			return CombatUtil.isEnemy(resident.getTownOrNull(), worldCoord.getTownOrNull());
		return false;
	}
	
	/**
	 * 
	 * @param wolf Wolf being attacked by a player.
	 * @param attackingPlayer Player attacking the wolf.
	 * @return true when the attackingPlayer is the owner
	 */
	private static boolean isOwner(Wolf wolf, Player attackingPlayer) {
		return wolf.getOwner() instanceof HumanEntity owner && owner.getUniqueId().equals(attackingPlayer.getUniqueId());
	}
	
	private static boolean isATamedWolfWithAOnlinePlayer(Wolf wolf) {
		return wolf.getOwner() instanceof HumanEntity owner && Bukkit.getPlayer(owner.getUniqueId()) != null;
	}
	
	public static boolean preventDispenserDamage(Block dispenser, Entity entity, DamageCause cause) {
		TownBlock dispenserTB = WorldCoord.parseWorldCoord(dispenser).getTownBlockOrNull();
		TownBlock defenderTB = WorldCoord.parseWorldCoord(entity).getTownBlockOrNull();
		
		TownyWorld world = TownyAPI.getInstance().getTownyWorld(dispenser.getWorld().getName());
		if (world == null || !world.isUsingTowny())
			return false;
		
		boolean preventDamage = false;
		
		if (!isArenaPlot(dispenserTB, defenderTB))
			preventDamage = preventPvP(world, dispenserTB) || preventPvP(world, defenderTB);

		TownyDispenserDamageEntityEvent event = new TownyDispenserDamageEntityEvent(entity.getLocation(), entity, cause, defenderTB, preventDamage, dispenser);
		Bukkit.getPluginManager().callEvent(event);
		
		return event.isCancelled();
	}
}
