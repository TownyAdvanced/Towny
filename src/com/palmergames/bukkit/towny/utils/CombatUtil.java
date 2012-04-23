package com.palmergames.bukkit.towny.utils;

import java.util.List;

import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Villager;
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
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
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
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
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
	 * Only allow a Wolves owner to cause it damage, and residents with destroy
	 * permissions to damage passive animals and villagers while in a town.
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

		/*
		 * We have an attacking player
		 */
		if (attackingPlayer != null) {
			
			Coord coord = Coord.parseCoord(defendingEntity);
			TownBlock defenderTB = null;
			TownBlock attackerTB = null;
			
			try {
				attackerTB = world.getTownBlock(Coord.parseCoord(attackingEntity));
			} catch (NotRegisteredException ex) {
			}
			
			try {
				defenderTB = world.getTownBlock(coord);
			} catch (NotRegisteredException ex) {
			}
			
			/*
			 * If another player is the target
			 * or
			 * The target is in a TownBlock and...
			 *    the target is a tame wolf and we are not it's owner
			 */
			if ((defendingPlayer != null)
					|| ((defenderTB != null) &&
							((defendingEntity instanceof Wolf) && ((Wolf)defendingEntity).isTamed() && !((Wolf)defendingEntity).getOwner().equals((AnimalTamer) attackingEntity)))
							) {
				
				/*
				 * Defending player is in a warzone
				 */
				if (world.isWarZone(coord))
					return false;

				/*
				 * Check if we are preventing friendly fire between allies
				 */
				if (preventFriendlyFire(attackingPlayer, defendingPlayer))
					return true;

				/*
				 * Check the attackers TownBlock and it's Town for their PvP status, else the world.
				 */
				if (preventPvP(world, attackerTB))
					return true;
				
				/*
				 * Check the defenders TownBlock and it's Town for their PvP status, else the world.
				 */
				if (preventPvP(world, defenderTB))
					return true;
	
			} else {
				
				/*
				 * Defender is not a player so check for PvM
				 */
				if (defenderTB != null) {
					if ((defendingEntity instanceof Animals) || (defendingEntity instanceof Villager)) {
						try {
							/*
							 * Only allow the player to kill animals etc,
							 * if they are from the same town
							 * and have destroy permissions (grass) in the defending TownBlock
							 */
							if (defenderTB.getTown().equals(TownyUniverse.getDataSource().getResident(attackingPlayer.getName()).getTown())) {
								if (PlayerCacheUtil.getCachePermission(attackingPlayer, attackingPlayer.getLocation(), 3, ActionType.DESTROY))
									return false;
							}
						} catch (NotRegisteredException e) {
							/*
							 * The attacking player has no town.
							 * Only allow them to kill animals etc,
							 * if they have destroy permissions (grass) in the defending TownBlock
							 */
							if (PlayerCacheUtil.getCachePermission(attackingPlayer, attackingPlayer.getLocation(), 3, ActionType.DESTROY))
								return false;
						}
					}
				}
			}
		}

		return false;
	}

	/**
	 * Is PvP disabled in this TownBlock?
	 * Checks the world if the TownBlock is null.
	 * 
	 * @param townBlock
	 * @return true if PvP is disallowed
	 */
	public static boolean preventPvP(TownyWorld world, TownBlock townBlock) {
		
		if (townBlock != null) {
			try {
				
				/*
				 * Check the attackers TownBlock and it's Town for their PvP status
				 */
				if (!townBlock.getTown().isPVP() && !townBlock.getPermissions().pvp)
					return true;
			
			} catch (NotRegisteredException ex) {
				/*
				 * Failed to fetch the town data
				 * so check world PvP
				 */
				if (preventPvP(world))
					return true;
			}
			
		} else {
			
			/*
			 * Attacker isn't in a TownBlock so check the world PvP
			 */
			if (preventPvP(world))
				return true;
		}
		return false;
	}
	
	/**
	 * Is PvP disabled in this world?
	 * 
	 * @param world
	 * @return true if the world disallows PvP
	 */
	public static boolean preventPvP(TownyWorld world) {

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

		if ((attacker != null) && (defender != null))
			if (!TownySettings.getFriendlyFire() && CombatUtil.isAlly(attacker.getName(), defender.getName())) {
				try {
					TownBlock townBlock = new WorldCoord(defender.getWorld().getName(), Coord.parseCoord(defender)).getTownBlock();
					if (!townBlock.getType().equals(TownBlockType.ARENA))
						return true;
				} catch (TownyException x) {
					// World or TownBlock failure
					// But we are configured to prevent friendly fire in the wilderness too.
					return true;
				}
			}
		return false;
	}

	/**
	 * Is the defending resident an ally of the attacking resident?
	 * 
	 * @param attackingResident
	 * @param defendingResident
	 * @return true if the defender is an ally of the attacker.
	 */
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
