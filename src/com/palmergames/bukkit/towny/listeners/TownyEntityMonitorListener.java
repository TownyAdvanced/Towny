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
	public void onEntityDeath(EntityDeathEvent event) {

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
				wartimeDeathPoints(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

				if (TownySettings.isRemovingOnMonarchDeath())
					monarchDeath(attackerPlayer, defenderPlayer, attackerResident, defenderResident);

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

				if (TownySettings.getWarPointsForKill() > 0)
					plugin.getTownyUniverse().getWarEvent().townScored(attackerResident.getTown(), TownySettings.getWarPointsForKill());

			} catch (NotRegisteredException e) {
			}
	}

	private void monarchDeath(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		if (TownyUniverse.isWarTime()) {
			War warEvent = plugin.getTownyUniverse().getWarEvent();
			try {
				Nation defenderNation = defenderResident.getTown().getNation();
				if (warEvent.isWarringNation(defenderNation))
					if (defenderResident.isMayor())
						if (defenderResident.isKing()) {
							if (attackerResident != null && attackerResident.hasTown())
								warEvent.remove(attackerResident.getTown(), defenderNation);
							else
								warEvent.remove(defenderNation);
							TownyMessaging.sendGlobalMessage(defenderNation.getName() + "'s king was killed. Nation removed from war.");
						} else {
							if (attackerResident != null && attackerResident.hasTown())
								warEvent.remove(attackerResident.getTown(), defenderResident.getTown());
							else
								warEvent.remove(defenderResident.getTown());
							TownyMessaging.sendGlobalMessage(defenderResident.getTown() + "'s mayor was killed. Town removed from war.");
						}
			} catch (NotRegisteredException e) {
			}
		}
	}

	public void deathPayment(Player attackerPlayer, Player defenderPlayer, Resident attackerResident, Resident defenderResident) {

		if (attackerPlayer != null && TownyUniverse.isWarTime() && TownySettings.getWartimeDeathPrice() > 0)
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
		else if (TownySettings.isChargingDeath() && ((TownySettings.isDeathPricePVPOnly() && attackerPlayer != null) || (!TownySettings.isDeathPricePVPOnly() && attackerPlayer == null))) {

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

					if (!defenderResident.canPayFromHoldings(price))
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

}
