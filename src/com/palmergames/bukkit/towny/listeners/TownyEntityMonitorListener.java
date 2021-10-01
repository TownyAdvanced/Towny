package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.deathprice.NationPaysDeathPriceEvent;
import com.palmergames.bukkit.towny.event.deathprice.PlayerPaysDeathPriceEvent;
import com.palmergames.bukkit.towny.event.deathprice.TownPaysDeathPriceEvent;
import com.palmergames.bukkit.towny.event.player.PlayerKilledPlayerEvent;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.jail.Jail;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.util.BukkitTools;

import net.citizensnpcs.api.CitizensAPI;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

//import org.bukkit.event.entity.EntityDamageEvent;

/**
 * @author Shade &amp; ElgarL
 *
 *         This class handles Player deaths and associated costs.
 *
 */
public class TownyEntityMonitorListener implements Listener {

	private final Towny plugin;

	public TownyEntityMonitorListener(Towny instance) {

		plugin = instance;
	}

	/**
	 * Handles players who have taken damage having their spawn cancelled.
	 * 
	 * @param event EntityDamageEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTakesDamage(EntityDamageEvent event) {
		if (!TownySettings.isDamageCancellingSpawnWarmup() 
				|| !event.getEntityType().equals(EntityType.PLAYER) 
				|| !TownyTimerHandler.isTeleportWarmupRunning() 
				|| (plugin.isCitizens2() && CitizensAPI.getNPCRegistry().isNPC(event.getEntity())))
			return;

		Resident resident = TownyUniverse.getInstance().getResident(event.getEntity().getUniqueId());

		if (resident != null && resident.getTeleportRequestTime() > 0) {
			TeleportWarmupTimerTask.abortTeleportRequest(resident);
			TownyMessaging.sendErrorMsg(event.getEntity(), Translatable.of("msg_err_teleport_cancelled_damage"));
		}
	}
	
	/**
	 * This handles PlayerDeathEvents on MONITOR in order to handle Towny features such as:
	 * - DeathPayments,
	 * - Jailing Players,
	 * - Awarding WarTimeDeathPoints.
	 * @param event The event.
	 * @throws NotRegisteredException When a towny object is not found.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		if (!TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		Player defenderPlayer = event.getEntity();
		Resident defenderResident = TownyUniverse.getInstance().getResident(defenderPlayer.getUniqueId());
		
		if (defenderResident == null) {
			// Usually an NPC or a Bot of some kind.
			return;
		}
		
		// Killed by another entity?			
		if (defenderPlayer.getLastDamageCause() instanceof EntityDamageByEntityEvent) {

			EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) defenderPlayer.getLastDamageCause();

			Entity attackerEntity = damageEvent.getDamager();
			Player attackerPlayer = null;
			Resident attackerResident = null;

			if (attackerEntity instanceof Projectile) { // Killed by projectile, try to narrow the true source of the kill.
				Projectile projectile = (Projectile) attackerEntity;
				if (projectile.getShooter() instanceof Player) { // Player shot a projectile.
					attackerPlayer = (Player) projectile.getShooter();
					attackerResident = townyUniverse.getResident(attackerPlayer.getUniqueId());
				} else { // Something else shot a projectile.
					try {
						attackerEntity = (Entity) projectile.getShooter(); // Mob shot a projectile.
					} catch (Exception e) { // This would be a dispenser kill, should count as environmental death.
					}
				}

			} else if (attackerEntity instanceof Player) {
				// This was a player kill
				attackerPlayer = (Player) attackerEntity;
				attackerResident = townyUniverse.getResident(attackerPlayer.getUniqueId());
				if (attackerResident == null)
					// Probably an NPC.
					return;
			}

			// This was a suicide, don't award money or jail.
			if (attackerPlayer != null && attackerPlayer == defenderPlayer)
				return;
			
			/*
			 * Player has died by a player: 
			 * 
			 * - Fire PlayerKilledPlayerEvent.
			 * 
			 * - charge death payment,
			 * - check for jailing attacking residents,
			 */
			if (attackerPlayer != null && attackerResident != null) {
				PlayerKilledPlayerEvent deathEvent = new PlayerKilledPlayerEvent(attackerPlayer, defenderPlayer, attackerResident, defenderResident, defenderPlayer.getLocation(), event);
				BukkitTools.getPluginManager().callEvent(deathEvent);

				deathPayment(attackerPlayer, defenderPlayer, attackerResident, defenderResident);			
				isJailingAttackers(attackerPlayer, defenderPlayer, attackerResident, defenderResident);
				
			/*
			 * Player has died from an entity but not a player & death price is not PVP only.
			 */
			} else if (!TownySettings.isDeathPricePVPOnly() && TownySettings.isChargingDeath()) {
				deathPayment(defenderPlayer, defenderResident);
			}

		/*
		 * Player has died from non-entity cause, ie: Environmental.
		 */
		} else {
			if (!TownySettings.isDeathPricePVPOnly() && TownySettings.isChargingDeath()) {
				deathPayment(defenderPlayer, defenderResident);
			}
		}
	}
	
	public void deathPayment(Player defenderPlayer, Resident defenderResident) {

		if (!TownyEconomyHandler.isActive())
			return;
		
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation());
		if (townBlock != null && (townBlock.getType().equals(TownBlockType.ARENA) || townBlock.getType().equals(TownBlockType.JAIL)))
			return;
		
		if (defenderPlayer != null && TownyUniverse.getInstance().getPermissionSource().testPermission(defenderPlayer, PermissionNodes.TOWNY_BYPASS_DEATH_COSTS.getNode()))
			return;
		
		if (defenderResident.isJailed())
			return;

		if (TownySettings.getDeathPrice() > 0) {

			double price = TownySettings.getDeathPrice();

			if (!TownySettings.isDeathPriceType()) {
				price = defenderResident.getAccount().getHoldingBalance() * price;
				if (TownySettings.isDeathPricePercentageCapped())
					if (price > TownySettings.getDeathPricePercentageCap())
						price = TownySettings.getDeathPricePercentageCap();
			}

			if (!defenderResident.getAccount().canPayFromHoldings(price))
				price = defenderResident.getAccount().getHoldingBalance();
			
			// Call event.
			PlayerPaysDeathPriceEvent ppdpe = new PlayerPaysDeathPriceEvent(defenderResident.getAccount(), price, defenderResident, null);
			Bukkit.getPluginManager().callEvent(ppdpe);
			if (!ppdpe.isCancelled()) {
				price = ppdpe.getAmount();

				defenderResident.getAccount().withdraw(price, "Death Payment");
				
				TownyMessaging.sendMsg(defenderPlayer, Translatable.of("msg_you_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		}

		if (TownySettings.getDeathPriceTown() > 0 && defenderResident.hasTown()) {

			Town town = defenderResident.getTownOrNull();
			double price = TownySettings.getDeathPriceTown();

			if (!TownySettings.isDeathPriceType())
				price = town.getAccount().getHoldingBalance() * price;

			if (!town.getAccount().canPayFromHoldings(price))
				price = town.getAccount().getHoldingBalance();

			// Call event.
			TownPaysDeathPriceEvent tpdpe = new TownPaysDeathPriceEvent(defenderResident.getAccount(), price, defenderResident, null, town);
			Bukkit.getPluginManager().callEvent(tpdpe);
			if (!tpdpe.isCancelled()) {
				price = tpdpe.getAmount();

				town.getAccount().withdraw(price, "Death Payment Town");

				TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_your_town_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		}

		if (TownySettings.getDeathPriceNation() > 0 && defenderResident.hasNation()) {

			Nation nation = defenderResident.getNationOrNull();
			double price = TownySettings.getDeathPriceNation();

			if (!TownySettings.isDeathPriceType())
				price = nation.getAccount().getHoldingBalance() * price;

			if (!nation.getAccount().canPayFromHoldings(price))
				price = nation.getAccount().getHoldingBalance();

			// Call event.
			NationPaysDeathPriceEvent npdpe = new NationPaysDeathPriceEvent(defenderResident.getAccount(), price, defenderResident, null, nation);
			Bukkit.getPluginManager().callEvent(npdpe);
			if (!npdpe.isCancelled()) {
				price = npdpe.getAmount();

				nation.getAccount().withdraw(price, "Death Payment Nation");

				TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_your_nation_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
			}
		}
	}

	public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
		
		if (!TownyEconomyHandler.isActive())
			return;

		if (defenderResident.isJailed())
			return;
		
		if (CombatUtil.isAlly(attackerResident.getName(), defenderResident.getName()))
			return;

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation());
		if (townBlock != null && (townBlock.getType().equals(TownBlockType.ARENA) || townBlock.getType().equals(TownBlockType.JAIL)))
			return;
		
		if (defenderPlayer != null && TownyUniverse.getInstance().getPermissionSource().testPermission(defenderPlayer, PermissionNodes.TOWNY_BYPASS_DEATH_COSTS.getNode()))
			return;

		if (attackerPlayer != null 
				&& WarUtil.hasSameWar(defenderResident, attackerResident)
				&& TownySettings.getWartimeDeathPrice() > 0 ) {
			// This will be handled in the EventWarListener.
			return;
			
		} else if (TownySettings.isChargingDeath() && attackerPlayer != null) {
			
			double total = 0.0;

			if (TownySettings.getDeathPrice() > 0) {
				double price = TownySettings.getDeathPrice();

				if (!TownySettings.isDeathPriceType()) {
					price = defenderResident.getAccount().getHoldingBalance() * price;
					if (TownySettings.isDeathPricePercentageCapped())
						if (price > TownySettings.getDeathPricePercentageCap())
							price = TownySettings.getDeathPricePercentageCap();
				}

				if (!defenderResident.getAccount().canPayFromHoldings(price))
					price = defenderResident.getAccount().getHoldingBalance();

				PlayerPaysDeathPriceEvent ppdpe = new PlayerPaysDeathPriceEvent(defenderResident.getAccount(), price, defenderResident, defenderPlayer);
				Bukkit.getPluginManager().callEvent(ppdpe);
				if (!ppdpe.isCancelled()) {
					price = ppdpe.getAmount();
					defenderResident.getAccount().payTo(price, attackerResident, "Death Payment");

					total = total + price;

					TownyMessaging.sendMsg(defenderPlayer, Translatable.of("msg_you_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			}

			if (TownySettings.getDeathPriceTown() > 0 && defenderResident.hasTown()) {

				Town town = defenderResident.getTownOrNull();
				double price = TownySettings.getDeathPriceTown();

				if (!TownySettings.isDeathPriceType())
					price = town.getAccount().getHoldingBalance() * price;

				if (!town.getAccount().canPayFromHoldings(price))
					price = town.getAccount().getHoldingBalance();

				TownPaysDeathPriceEvent tpdpe = new TownPaysDeathPriceEvent(town.getAccount(), price, defenderResident, defenderPlayer, town);
				Bukkit.getPluginManager().callEvent(tpdpe);
				if (!tpdpe.isCancelled()) {
					price = tpdpe.getAmount();
					town.getAccount().payTo(price, attackerResident, "Death Payment Town");

					total = total + price;

					TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_your_town_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			}

			if (TownySettings.getDeathPriceNation() > 0 && defenderResident.hasNation()) {
				
				Nation nation = defenderResident.getNationOrNull();
				double price = TownySettings.getDeathPriceNation();

				if (!TownySettings.isDeathPriceType())
					price = nation.getAccount().getHoldingBalance() * price;

				if (!nation.getAccount().canPayFromHoldings(price))
					price = nation.getAccount().getHoldingBalance();

				NationPaysDeathPriceEvent npdpe = new NationPaysDeathPriceEvent(nation.getAccount(), price, defenderResident, defenderPlayer, nation);
				Bukkit.getPluginManager().callEvent(npdpe);
				if (!npdpe.isCancelled()) {
					price = npdpe.getAmount();
					nation.getAccount().payTo(price, attackerResident, "Death Payment Nation");
					
					total = total + price;

					TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_your_nation_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
				}
			}

			if (attackerResident != null)
				TownyMessaging.sendMsg(attackerResident, Translatable.of("msg_you_gained_money_for_killing", TownyEconomyHandler.getFormattedBalance(total), defenderPlayer.getName()));
		}
	}
	
	public void isJailingAttackers(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
		if (TownySettings.isJailingAttackingEnemies() || TownySettings.isJailingAttackingOutlaws()) {
			Location loc = defenderPlayer.getLocation();

			// Not a Towny World.
			if (!TownyAPI.getInstance().isTownyWorld(loc.getWorld()))
				return;

			// Not in a Town.
			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(loc);
			if (townBlock == null)
				return;

			// Not in an arena plot.
			if (townBlock.getType() == TownBlockType.ARENA)
				return;

			// Not if they're already jailed.
			if (defenderResident.isJailed()) {
				if (!townBlock.isJail())
					TownyMessaging.sendGlobalMessage(Translatable.of("msg_killed_attempting_to_escape_jail", defenderPlayer.getName()));
				return;			
			}
			
			// Not if the killer has no Town.
			if (!attackerResident.hasTown()) 
				return;
			Town attackerTown = attackerResident.getTownOrNull();
			
			// Not if victim died in a town that isn't the attackerResident's town.
			if (!TownyAPI.getInstance().getTown(loc).getUUID().equals(attackerTown.getUUID()))
				return;
			
			// Not if the town has no jails.
			if (!attackerTown.hasJails()) 
				return;

			// Try outlaw jailing first
			if (!WarUtil.hasSameWar(defenderResident, attackerTown) && TownySettings.isJailingAttackingOutlaws() && attackerTown.hasOutlaw(defenderResident)) {
				// Not if they don't have the jailer node.
				if (!TownyUniverse.getInstance().getPermissionSource().testPermission(attackerPlayer, PermissionNodes.TOWNY_OUTLAW_JAILER.getNode()))
					return;
				
				// Send to jail. Hours are set later on.
				JailUtil.jailResident(defenderResident, attackerTown.getPrimaryJail(), 0, JailReason.OUTLAW_DEATH.getHours(), JailReason.OUTLAW_DEATH, attackerResident.getPlayer());
				return;

			// Try enemy jailing second
			} else if (WarUtil.hasSameWar(defenderResident, attackerTown) && TownySettings.isJailingAttackingEnemies()){
				
				// Not if the victim has no Town.
				if (!defenderResident.hasTown())
					return;
				Town defenderTown = defenderResident.getTownOrNull();
				
				// Not if they aren't considered enemies.
				if (!CombatUtil.isEnemy(attackerTown, defenderTown))
					return;

				// Attempt to send them to the Town's primary jail first if it is still in the war.
				if (TownyUniverse.getInstance().hasWarEvent(attackerTown.getPrimaryJail().getTownBlock())) {
					JailUtil.jailResident(defenderResident, attackerTown.getPrimaryJail(), 0, JailReason.PRISONER_OF_WAR.getHours(), JailReason.PRISONER_OF_WAR, attackerResident.getPlayer());
					return;
					
 				} else {
				// Find a jail that hasn't had its HP dropped to 0.
					for (Jail jail : attackerTown.getJails()) {
						if (TownyUniverse.getInstance().hasWarEvent(jail.getTownBlock())) {
							// Send to jail. Hours are set later on.
							JailUtil.jailResident(defenderResident, jail, 0, JailReason.PRISONER_OF_WAR.getHours(), JailReason.PRISONER_OF_WAR, attackerResident.getPlayer());
							return;
						}
					}
 				}
				// If we've gotten this far the player couldn't be jailed, send a message saying there was no jail.
				TownyMessaging.sendPrefixedTownMessage(attackerTown, Translatable.of("msg_war_player_cant_be_jailed_plot_fallen"));
			}
		}
	}
}
