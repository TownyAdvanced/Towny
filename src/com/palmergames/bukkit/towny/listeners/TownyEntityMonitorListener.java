package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.War;
import com.palmergames.bukkit.towny.war.eventwar.WarSpoils;
import com.palmergames.bukkit.util.Colors;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

//import org.bukkit.event.entity.EntityDamageEvent;

/**
 * @author Shade & ElgarL
 * 
 *         This class handles Player deaths and associated costs.
 * 
 */
public class TownyEntityMonitorListener implements Listener {

	private final Towny plugin;

	public TownyEntityMonitorListener(Towny instance) {

		plugin = instance;
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onEntityDeath(EntityDeathEvent event) throws NotRegisteredException {

		Entity defenderEntity = event.getEntity();

		TownyWorld World = null;

		try {
			World = TownyUniverse.getDataSource().getWorld(defenderEntity.getLocation().getWorld().getName());
			if (!World.isUsingTowny())
				return;

		} catch (NotRegisteredException e) {
			// World not registered with Towny.
			return;
		}

		// Was this a player death?
		if (defenderEntity instanceof Player) {

			// Killed by another entity?			
			if (defenderEntity.getLastDamageCause() instanceof EntityDamageByEntityEvent) {

				EntityDamageByEntityEvent damageEvent = (EntityDamageByEntityEvent) defenderEntity.getLastDamageCause();

				Entity attackerEntity = damageEvent.getDamager();
				Player defenderPlayer = (Player) defenderEntity;
				Player attackerPlayer = null;
				Resident attackerResident = null;
				Resident defenderResident = null;

				try {
					defenderResident = TownyUniverse.getDataSource().getResident(defenderPlayer.getName());
				} catch (NotRegisteredException e) {
					return;
				}

				// Was this a missile?
				if (attackerEntity instanceof Projectile) {
					Projectile projectile = (Projectile) attackerEntity;
					if (projectile.getShooter() instanceof Player) {
						attackerPlayer = (Player) projectile.getShooter();

						try {
							attackerResident = TownyUniverse.getDataSource().getResident(attackerPlayer.getName());
						} catch (NotRegisteredException e) {
						}
					}

				} else if (attackerEntity instanceof Player) {
					// This was a player kill
					attackerPlayer = (Player) attackerEntity;
					try {
						attackerResident = TownyUniverse.getDataSource().getResident(attackerPlayer.getName());
					} catch (NotRegisteredException e) {
					}
				}

				/*
				 * If attackerPlayer or attackerResident are null at this point
				 * it was a natural death, not PvP.
				 */
				deathPayment(attackerPlayer, defenderPlayer, attackerResident, defenderResident);			
				if (attackerPlayer instanceof Player)
					isJailingAttackingEnemies(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

				if (TownyUniverse.isWarTime())
					wartimeDeathPoints(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

			}
		}

	}

	/*
	 * @Override
	 * public void onEntityDamage(EntityDamageEvent event) {
	 * if (event instanceof EntityDamageByEntityEvent) {
	 * EntityDamageByEntityEvent entityEvent = (EntityDamageByEntityEvent)event;
	 * Entity attackerEntity = entityEvent.getDamager();
	 * Entity defenderEntity = entityEvent.getEntity();
	 * 
	 * if (defenderEntity instanceof Player) {
	 * Player defenderPlayer = (Player) defenderEntity;
	 * Player attackerPlayer = null;
	 * if (defenderPlayer.getHealth() > 0)
	 * return;
	 * 
	 * Resident attackerResident = null;
	 * Resident defenderResident = null;
	 * 
	 * try {
	 * defenderResident =
	 * plugin.getTownyUniverse().getResident(defenderPlayer.getName());
	 * } catch (NotRegisteredException e) {
	 * return;
	 * }
	 * 
	 * if (attackerEntity instanceof Player) {
	 * attackerPlayer = (Player) attackerEntity;
	 * try {
	 * attackerResident =
	 * plugin.getTownyUniverse().getResident(attackerPlayer.getName());
	 * } catch (NotRegisteredException e) {
	 * }
	 * }
	 * 
	 * deathPayment(attackerPlayer, defenderPlayer, attackerResident,
	 * defenderResident);
	 * wartimeDeathPoints(attackerPlayer, defenderPlayer, attackerResident,
	 * defenderResident);
	 * 
	 * if (TownySettings.isRemovingOnMonarchDeath())
	 * monarchDeath(attackerPlayer, defenderPlayer, attackerResident,
	 * defenderResident);
	 * }
	 * }
	 * }
	 */
	private void wartimeDeathPoints(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		if (attackerPlayer != null && defenderPlayer != null && TownyUniverse.isWarTime())
			try {
				if (CombatUtil.isAlly(attackerPlayer.getName(), defenderPlayer.getName()))
					return;

				War warEvent = plugin.getTownyUniverse().getWarEvent();
				if (attackerResident.hasTown() && warEvent.isWarringTown(attackerResident.getTown()) && defenderResident.hasTown() && warEvent.isWarringTown(defenderResident.getTown())){
					if (TownySettings.isRemovingOnMonarchDeath())
						monarchDeath(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

					if (TownySettings.getWarPointsForKill() > 0){
						plugin.getTownyUniverse().getWarEvent().townScored(defenderResident.getTown(), attackerResident.getTown(), defenderPlayer, attackerPlayer, TownySettings.getWarPointsForKill());
					}
				}
			} catch (NotRegisteredException e) {
			}
	}

	private void monarchDeath(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		War warEvent = plugin.getTownyUniverse().getWarEvent();
		try {

			Nation defenderNation = defenderResident.getTown().getNation();
			Town defenderTown = defenderResident.getTown();
			if (warEvent.isWarringNation(defenderNation) && defenderResident.isKing()){
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeKingKilled(defenderNation));
				if (attackerResident != null)
					warEvent.remove(attackerResident.getTown(), defenderNation);
			}else if (warEvent.isWarringNation(defenderNation) && defenderResident.isMayor()) {
				TownyMessaging.sendGlobalMessage(TownySettings.getWarTimeMayorKilled(defenderTown));
				if (attackerResident != null)
					warEvent.remove(attackerResident.getTown(), defenderResident.getTown());
			}
		} catch (NotRegisteredException e) {
		}
	}

	public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) throws NotRegisteredException {

		if (attackerPlayer != null && TownyUniverse.isWarTime() && TownySettings.getWartimeDeathPrice() > 0 ) {
			try {
				if (attackerResident == null)
					throw new NotRegisteredException(String.format("The attackingResident %s has not been registered.", attackerPlayer.getName()));

				double price = TownySettings.getWartimeDeathPrice();
				double townPrice = 0;
				if (!defenderResident.canPayFromHoldings(price)) {
					townPrice = price - defenderResident.getHoldingBalance();
					price = defenderResident.getHoldingBalance();
				}

				if (price > 0) {
					defenderResident.payTo(price, attackerResident, "Death Payment (War)");
					TownyMessaging.sendMsg(attackerPlayer, "You robbed " + defenderResident.getName() + " of " + TownyEconomyHandler.getFormattedBalance(price) + ".");
					TownyMessaging.sendMsg(defenderPlayer, attackerResident.getName() + " robbed you of " + TownyEconomyHandler.getFormattedBalance(price) + ".");
				}

				// Resident doesn't have enough funds.
				if (townPrice > 0) {
					Town town = defenderResident.getTown();
					if (!town.canPayFromHoldings(townPrice)) {
						// Town doesn't have enough funds.
						townPrice = town.getHoldingBalance();
						try {
							plugin.getTownyUniverse().getWarEvent().remove(attackerResident.getTown(), town);
						} catch (NotRegisteredException e) {
							plugin.getTownyUniverse().getWarEvent().remove(town);
						}
					} else
						TownyMessaging.sendTownMessage(town, defenderResident.getName() + "'s wallet couldn't satisfy " + attackerResident.getName() + ". " + townPrice + " taken from town bank.");
					town.payTo(townPrice, attackerResident, String.format("Death Payment (War) (%s couldn't pay)", defenderResident.getName()));
				}
			} catch (NotRegisteredException e) {
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(attackerPlayer, "Could not take wartime death funds.");
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take wartime death funds.");
			}			
		} else if (TownySettings.isChargingDeath() && ((TownySettings.isDeathPricePVPOnly() && attackerPlayer != null) || (!TownySettings.isDeathPricePVPOnly() && attackerPlayer == null))  ) {
			if (TownyUniverse.getTownBlock(defenderPlayer.getLocation()) != null) {
				if (TownyUniverse.getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.ARENA || TownyUniverse.getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.JAIL)
					return;				
			}
			if (defenderResident.isJailed())
				return;

			double total = 0.0;

			try {
				if (TownySettings.getDeathPrice() > 0) {

					double price = TownySettings.getDeathPrice();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getHoldingBalance() * price;
					}

					if (!defenderResident.canPayFromHoldings(price))
						price = defenderResident.getHoldingBalance();

					if (attackerResident == null) {
						defenderResident.payTo(price, new WarSpoils(), "Death Payment");
					} else {
						defenderResident.payTo(price, attackerResident, "Death Payment");
					}
					total = total + price;

					TownyMessaging.sendMsg(defenderPlayer, "You lost " + TownyEconomyHandler.getFormattedBalance(price) + ".");
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take death funds.");
			}

			try {
				if (TownySettings.getDeathPriceTown() > 0) {

					double price = TownySettings.getDeathPriceTown();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getTown().getHoldingBalance() * price;
					}

					if (!defenderResident.getTown().canPayFromHoldings(price))
						price = defenderResident.getTown().getHoldingBalance();

					if (attackerResident == null) {
						defenderResident.getTown().payTo(price, new WarSpoils(), "Death Payment Town");
					} else {
						defenderResident.getTown().payTo(price, attackerResident, "Death Payment Town");
					}
					total = total + price;

					TownyMessaging.sendTownMessagePrefixed(defenderResident.getTown(), "Your town lost " + TownyEconomyHandler.getFormattedBalance(price) + ".");
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take death funds.");
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take town death funds.");
			}

			try {
				if (TownySettings.getDeathPriceNation() > 0) {
					double price = TownySettings.getDeathPriceNation();

					if (!TownySettings.isDeathPriceType()) {
						price = defenderResident.getTown().getNation().getHoldingBalance() * price;
					}

					if (!defenderResident.getTown().getNation().canPayFromHoldings(price))
						price = defenderResident.getTown().getNation().getHoldingBalance();

					if (attackerResident == null) {
						defenderResident.getTown().getNation().payTo(price, new WarSpoils(), "Death Payment Nation");
					} else {
						defenderResident.getTown().getNation().payTo(price, attackerResident, "Death Payment Nation");
					}
					total = total + price;

					TownyMessaging.sendNationMessagePrefixed(defenderResident.getTown().getNation(), "Your nation lost " + TownyEconomyHandler.getFormattedBalance(price) + ".");
				}
			} catch (EconomyException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take death funds.");
			} catch (NotRegisteredException e) {
				TownyMessaging.sendErrorMsg(defenderPlayer, "Could not take nation death funds.");
			}

			if (attackerResident != null) {
				TownyMessaging.sendMsg(attackerResident, "You gained " + TownyEconomyHandler.getFormattedBalance(total) + " for killing " + defenderPlayer.getName() + ".");

			}
		}
	}
	public void isJailingAttackingEnemies(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) throws NotRegisteredException {
		if (TownySettings.isJailingAttackingEnemies()) {
			Location loc = defenderPlayer.getLocation();
			if (!TownyUniverse.getDataSource().getWorld(defenderPlayer.getLocation().getWorld().getName()).isUsingTowny())
				return;
			if (TownyUniverse.getTownBlock(defenderPlayer.getLocation()) == null)
				return;
			if (TownyUniverse.getTownBlock(defenderPlayer.getLocation()).getType() == TownBlockType.ARENA)
				return;
			if (defenderResident.isJailed()) {
				if (TownyUniverse.getTownBlock(defenderPlayer.getLocation()).getType() != TownBlockType.JAIL) {
					TownyMessaging.sendGlobalMessage(Colors.Red + defenderPlayer.getName() + " was killed attempting to escape jail.");
					return;
				}							
				return;			
			}
			if (!attackerResident.hasTown()) { 
				return;
			} else {
				Town town = null;
				try {					
					town = attackerResident.getTown();
				} catch (NotRegisteredException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}			
			
				if (TownyUniverse.getTownBlock(loc) == null)
					return;
					
				try {
					if (TownyUniverse.getTownBlock(loc).getTown().getName() != attackerResident.getTown().getName()) 
						return;
				} catch (NotRegisteredException e1) {
					e1.printStackTrace();
				}
				if (!attackerResident.hasNation() || !defenderResident.hasNation()) 
					return;
				try {
					if (!attackerResident.getTown().getNation().getEnemies().contains(defenderResident.getTown().getNation())) 
						return;
				} catch (NotRegisteredException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}								
				if (!town.hasJailSpawn()) 
					return;
					
				Integer index = 1;				
				defenderResident.setJailed(defenderPlayer, index, town);
			}
		}
	}
}
