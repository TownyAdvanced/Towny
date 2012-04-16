package com.palmergames.bukkit.towny.utils;

import java.util.List;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.entity.Wolf;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;

/**
 * 
 * @author ElgarL,Shade
 * 
 */
public class CombatUtil {

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Also only allow a Wolves owner to cause it damage, and town residents to
	 * damage passive animals.
	 * 
	 * @param attacker
	 * @param defender
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(Entity attacker, Entity defender) {

		try {
			TownyWorld world = TownyUniverse.getDataSource().getWorld(defender.getWorld().getName());

			// World using Towny
			if (!world.isUsingTowny())
				return false;

			Player a = null;
			Player b = null;

			/*
			 * Find the shooter if this is a projectile.
			 */
			if (attacker instanceof Projectile) {
				Projectile projectile = (Projectile) attacker;
				attacker = projectile.getShooter();
			}

			if (attacker instanceof Player)
				a = (Player) attacker;
			if (defender instanceof Player)
				b = (Player) defender;

			return preventDamageCall(world, attacker, defender, a, b);

		} catch (Exception e) {
			// Failed to fetch world
		}

		return false;

	}

	/**
	 * Tests the attacker against defender to see if we need to cancel
	 * the damage event due to world PvP, Plot PvP or Friendly Fire settings.
	 * Also only allow a Wolves owner to cause it damage, and town residents to
	 * damage passive animals.
	 * 
	 * @param world
	 * @param attackingEntity
	 * @param defendingEntity
	 * @param attackingPlayer
	 * @param defendingPlayer
	 * @return true if we should cancel.
	 */
	public static boolean preventDamageCall(TownyWorld world, Entity attackingEntity, Entity defendingEntity, Player attackingPlayer, Player defendingPlayer) {

		// World using Towny
		if (!world.isUsingTowny())
			return false;

		Coord coord = Coord.parseCoord(defendingEntity);

		if (attackingPlayer != null && defendingPlayer != null) {
			if (world.isWarZone(coord))
				return false;

			if (preventFriendlyFire(attackingPlayer, defendingPlayer))
				return true;
		}

		try {

			// Check TownBlock PvP status
			TownBlock defenderTB = world.getTownBlock(coord);
			TownBlock attackerTB = world.getTownBlock(Coord.parseCoord(attackingEntity));

			/*
			 * Check to prevent damage if...
			 * The world isn't forced PVP
			 * and
			 * The Defender isn't in a PVP area.
			 * or
			 * The Attacker isn't in a PVP area.
			 */
			if (!world.isForcePVP() && ((!defenderTB.getTown().isPVP() && !defenderTB.getPermissions().pvp) || (!attackerTB.getTown().isPVP() && !attackerTB.getPermissions().pvp))) {
				if (defendingPlayer != null && (attackingPlayer != null || attackingEntity instanceof Arrow || attackingEntity instanceof ThrownPotion))
					return true;

				if (defendingEntity instanceof Wolf) {
					Wolf wolf = (Wolf) defendingEntity;
					if (wolf.isTamed() && !wolf.getOwner().equals((AnimalTamer) attackingEntity)) {
						return true;
					}
				}

				if ((defendingEntity instanceof Animals) && (attackingPlayer != null)) {

					//Get destroy permissions (updates if none exist)
					boolean bDestroy = TownyUniverse.getCachePermissions().getCachePermission(attackingPlayer, attackingPlayer.getLocation(), TownyPermission.ActionType.DESTROY);

					// Don't allow players to kill animals in plots they don't have destroy permissions in.
					if (!bDestroy)
						return true;

					/*
					 * Resident resident =
					 * TownyUniverse.getDataSource().getResident(ap.getName());
					 * if ((!resident.hasTown()) || (resident.hasTown() &&
					 * (resident.getTown() != townblock.getTown())))
					 * return true;
					 */
				}
			}
		} catch (NotRegisteredException e) {
			// Not in a town
			if ((attackingPlayer != null) && (defendingPlayer != null) && (!world.isPVP()) && (!world.isForcePVP()))
				return true;
		}

		//if (plugin.getTownyUniverse().canAttackEnemy(ap.getName(), bp.getName()))
		//	return false;

		return false;
	}

	/**
	 * Is this World PVP?
	 * 
	 * @param world
	 * @return true if we should prevent PVP
	 */
	public static boolean preventDamagePvP(TownyWorld world) {

		// Universe is only PvP
		if (world.isForcePVP() || world.isPVP())
			return false;

		return true;
	}

	/**
	 * Should we be preventing friendly fire?
	 * 
	 * @param attacker
	 * @param defender
	 * @return true if we should cancel damage.
	 */
	public static boolean preventFriendlyFire(Player attacker, Player defender) {

		if (!TownySettings.getFriendlyFire() && CombatUtil.isAlly(attacker.getName(), defender.getName())) {
			try {
				TownBlock townBlock = new WorldCoord(defender.getWorld().getName(), Coord.parseCoord(defender)).getTownBlock();
				if (!townBlock.getType().equals(TownBlockType.ARENA))
					return true;
			} catch (TownyException x) {
				// world or townblock failure
				// But we want to prevent friendly fire in the wilderness too.
				return true;
			}
		}
		return false;
	}

	public static boolean isAlly(String attackingResident, String defendingResident) {

		try {
			Resident residentA = TownyUniverse.getDataSource().getResident(attackingResident);
			Resident residentB = TownyUniverse.getDataSource().getResident(defendingResident);
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

	/**
	 * Is town b an ally of town a?
	 * 
	 * @param a
	 * @param b
	 * @return true if they are allies.
	 */
	public static boolean isAlly(Town a, Town b) {

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

	/**
	 * Can resident a attack resident b?
	 * 
	 * @param a
	 * @param b
	 * @return true if they can attack.
	 */
	public static boolean canAttackEnemy(String a, String b) {

		try {
			Resident residentA = TownyUniverse.getDataSource().getResident(a);
			Resident residentB = TownyUniverse.getDataSource().getResident(b);
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
	
	/**
	 * Test if all the listed nations are allies
	 * 
	 * @param possibleAllies
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
	 * Is resident b an enemy of resident a?
	 * 
	 * @param a
	 * @param b
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(String a, String b) {

		try {
			Resident residentA = TownyUniverse.getDataSource().getResident(a);
			Resident residentB = TownyUniverse.getDataSource().getResident(b);
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

	/**
	 * Is town b an enemy of town a?
	 * 
	 * @param a
	 * @param b
	 * @return true if b is an enemy.
	 */
	public static boolean isEnemy(Town a, Town b) {

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

	/**
	 * Does this WorldCoord fall within a plot owned by an enemy town?
	 * 
	 * @param player
	 * @param worldCoord
	 * @return true if it is an enemy plot.
	 */
	public boolean isEnemyTownBlock(Player player, WorldCoord worldCoord) {

		try {
			return CombatUtil.isEnemy(TownyUniverse.getDataSource().getResident(player.getName()).getTown(), worldCoord.getTownBlock().getTown());
		} catch (NotRegisteredException e) {
			return false;
		}
	}
}
