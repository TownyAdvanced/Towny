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
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.jail.JailReason;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.tasks.TeleportWarmupTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.jetbrains.annotations.Nullable;

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
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerTakesDamage(EntityDamageEvent event) {
		if (!(event.getEntity() instanceof Player player)
				|| !TownySettings.isDamageCancellingSpawnWarmup() 
				|| !TownyTimerHandler.isTeleportWarmupRunning() 
				|| PluginIntegrations.getInstance().checkCitizens(player))
			return;

		Resident resident = TownyAPI.getInstance().getResident(player);

		if (TeleportWarmupTimerTask.abortTeleportRequest(resident))
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_teleport_cancelled_damage"));}
	
	/**
	 * This handles PlayerDeathEvents on MONITOR in order to handle Towny features such as:
	 * - Throwing the PlayerKilledPlayerEvent,
	 * - DeathPayments,
	 * - Jailing Players,
	 * @param event PlayerDeathEvent.
	 */
	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerDeath(PlayerDeathEvent event) {
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getEntity().getWorld()))
			return;

		Player defenderPlayer = event.getEntity();
		Resident defenderResident = TownyUniverse.getInstance().getResident(defenderPlayer.getUniqueId());
		if (defenderResident == null) // Usually an NPC or a Bot of some kind.
			return;

		if (defenderPlayer.getLastDamageCause() instanceof EntityDamageByEntityEvent damageEvent)
			// Player has died from an entity that might be a player.
			resolvePlayerKilledByEntity(event, defenderPlayer, defenderResident, damageEvent);
		else if (!TownySettings.isDeathPricePVPOnly())
			// Player has died from non-entity, environmental cause, which is being punished with death costs.
			deathPayment(defenderPlayer, defenderResident, null);
	}

	private void resolvePlayerKilledByEntity(PlayerDeathEvent event, Player defenderPlayer, Resident defenderResident, EntityDamageByEntityEvent damageEvent) {

		Entity attackerEntity = damageEvent.getDamager();
		Player attackerPlayer = null;
		Resident attackerResident = null;

		if (attackerEntity instanceof Projectile projectile && projectile.getShooter() instanceof Player shooter)
			// Player shot a projectile.
			attackerPlayer = shooter;
		else if (attackerEntity instanceof Player player)
			// Player killed another player directly.
			attackerPlayer = player;

		if (attackerPlayer != null && attackerPlayer == defenderPlayer)
			// This was a suicide, don't award money or jail.
			return;

		if (attackerPlayer != null)
			attackerResident = TownyAPI.getInstance().getResident(attackerPlayer);

		/*
		 * Player has died by a player that can be resolved to a Resident. (Not an NPC.)
		 */
		if (attackerPlayer != null && attackerResident != null) {
			BukkitTools.fireEvent(new PlayerKilledPlayerEvent(attackerPlayer, defenderPlayer, attackerResident, defenderResident, defenderPlayer.getLocation(), event));
			deathPayment(defenderPlayer, defenderResident, attackerResident);
			isJailingAttackers(defenderPlayer, attackerResident, defenderResident);

		/*
		 * Player has died from an entity but not a player & death price is not PVP only.
		 */
		} else if (!TownySettings.isDeathPricePVPOnly()) {
			deathPayment(defenderPlayer, defenderResident, null);
		}
	}

	private void deathPayment(Player defenderPlayer, Resident defenderResident, @Nullable Resident attackerResident) {
		
		if (!TownyEconomyHandler.isActive()                        // Economy Off.
			|| !TownySettings.isChargingDeath()                    // No Death Costs.
			|| defenderResident.isJailed()                         // Dead resident was jailed.
			|| hasBypassNode(defenderResident)                     // Permission node bypassing death costs.
			|| residentsAllied(defenderResident, attackerResident) // Allied players killed each other.
			|| killedInInvalidTownBlockType(defenderPlayer)        // Player killed in Arena or Jail.
			)
			return;

		double total = 0.0;

		if (TownySettings.getDeathPrice() > 0)
			total = takeMoneyFromPlayer(defenderPlayer, attackerResident, defenderResident, total);

		if (TownySettings.getDeathPriceTown() > 0 && defenderResident.hasTown())
			total = takeMoneyFromPlayersTown(defenderPlayer, attackerResident, defenderResident, total);

		if (TownySettings.getDeathPriceNation() > 0 && defenderResident.hasNation())
			total = takeMoneyFromPlayersNation(defenderPlayer, attackerResident, defenderResident, total);

		if (attackerResident != null)
			TownyMessaging.sendMsg(attackerResident, Translatable.of("msg_you_gained_money_for_killing", TownyEconomyHandler.getFormattedBalance(total), defenderPlayer.getName()));
	}

	private double takeMoneyFromPlayer(Player defenderPlayer, Resident attackerResident, Resident defenderResident, double total) {
		double price = TownySettings.getDeathPrice();

		if (TownySettings.isDeathPricePercentBased()) {
			price = defenderResident.getAccount().getHoldingBalance() * price;
			if (TownySettings.isDeathPricePercentageCapped())
				price = Math.min(price, TownySettings.getDeathPricePercentageCap());
		}

		price = Math.min(price,  defenderResident.getAccount().getHoldingBalance());

		PlayerPaysDeathPriceEvent ppdpe = new PlayerPaysDeathPriceEvent(defenderResident.getAccount(), price, defenderResident, defenderPlayer);
		if (!BukkitTools.isEventCancelled(ppdpe)) {
			price = ppdpe.getAmount();

			if (attackerResident == null)
				defenderResident.getAccount().withdraw(price, "Death Payment");
			else 
				defenderResident.getAccount().payTo(price, attackerResident, "Death Payment");

			total += price;

			TownyMessaging.sendMsg(defenderPlayer, Translatable.of("msg_you_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
		}
		return total;
	}

	private double takeMoneyFromPlayersTown(Player defenderPlayer, Resident attackerResident, Resident defenderResident, double total) {
		Town town = defenderResident.getTownOrNull();
		double price = TownySettings.getDeathPriceTown();

		if (TownySettings.isDeathPricePercentBased())
			price = town.getAccount().getHoldingBalance() * price;

		price = Math.min(price,  defenderResident.getAccount().getHoldingBalance());

		TownPaysDeathPriceEvent tpdpe = new TownPaysDeathPriceEvent(town.getAccount(), price, defenderResident, defenderPlayer, town);
		if (!BukkitTools.isEventCancelled(tpdpe)) {
			price = tpdpe.getAmount();

			if (attackerResident == null)
				town.getAccount().withdraw(price, "Death Payment Town");
			else 
				town.getAccount().payTo(price, attackerResident, "Death Payment Town");

			total += price;

			TownyMessaging.sendTownMessagePrefixed(town, Translatable.of("msg_your_town_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
		}
		return total;
	}

	private double takeMoneyFromPlayersNation(Player defenderPlayer, Resident attackerResident, Resident defenderResident, double total) {
		Nation nation = defenderResident.getNationOrNull();
		double price = TownySettings.getDeathPriceNation();

		if (TownySettings.isDeathPricePercentBased())
			price = nation.getAccount().getHoldingBalance() * price;

		price = Math.min(price,  defenderResident.getAccount().getHoldingBalance());

		NationPaysDeathPriceEvent npdpe = new NationPaysDeathPriceEvent(nation.getAccount(), price, defenderResident, defenderPlayer, nation);
		if (!BukkitTools.isEventCancelled(npdpe)) {
			price = npdpe.getAmount();

			if (attackerResident == null)
				nation.getAccount().withdraw(price, "Death Payment Nation");
			else 
				nation.getAccount().payTo(price, attackerResident, "Death Payment Nation");

			total += price;

			TownyMessaging.sendNationMessagePrefixed(nation, Translatable.of("msg_your_nation_lost_money_dying", TownyEconomyHandler.getFormattedBalance(price)));
		}
		return total;
	}

	private boolean hasBypassNode(Resident defenderResident) {
		return defenderResident.hasPermissionNode(PermissionNodes.TOWNY_BYPASS_DEATH_COSTS.getNode());
	}

	private boolean residentsAllied(Resident defenderResident, Resident attackerResident) {
		return attackerResident != null && CombatUtil.isAlly(attackerResident, defenderResident);
	}

	private boolean killedInInvalidTownBlockType(Player defenderPlayer) {
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(defenderPlayer);
		return townBlock != null && (townBlock.getType().equals(TownBlockType.ARENA) || townBlock.getType().equals(TownBlockType.JAIL));
	}

	private void isJailingAttackers(Player defenderPlayer, Resident attackerResident, Resident defenderResident) {
		if (!TownySettings.isJailingAttackingOutlaws())
			return;

		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(defenderPlayer.getLocation());
		Town attackerTown = attackerResident.getTownOrNull();
		if (townBlock == null                              // Player died in the Wilderness.
			|| townBlock.getType() == TownBlockType.ARENA  // Player died in an Arena plot.
			|| !attackerResident.hasTown()                 // Attacker has no town.
			|| alreadyJailed(defenderResident, townBlock)  // Player was already jailed.
			|| !hasJailingNode(attackerResident)           // Attacker doesn't have permission to jail.
			|| !attackerTown.hasJails()                    // Town has no jails.
			|| !attackerTown.hasOutlaw(defenderResident)   // Player isn't an outlaw.
			|| !attackerTown.hasTownBlock(townBlock)       // Victim died in a town that isn't the attackerResident's town.
			)
			return;

		// Send to jail.
		JailUtil.jailResident(defenderResident, attackerTown.getPrimaryJail(), 1, TownySettings.getJailedOutlawJailHours(), JailReason.OUTLAW_DEATH, attackerResident.getPlayer());
	}

	private boolean hasJailingNode(Resident attackerResident) {
		return attackerResident.hasPermissionNode(PermissionNodes.TOWNY_OUTLAW_JAILER.getNode());
	}

	private boolean alreadyJailed(Resident defenderResident, TownBlock townBlock) {
		boolean jailed = defenderResident.isJailed();
		if (jailed && !townBlock.isJail()) // Send out message explaining player died during attempted escape.
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_killed_attempting_to_escape_jail", defenderResident.getName()));
		return jailed;
	}
}
