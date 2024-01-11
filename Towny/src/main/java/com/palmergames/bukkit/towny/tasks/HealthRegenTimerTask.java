package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;

import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.util.BukkitTools;

public class HealthRegenTimerTask extends TownyTimerTask {
	static {
		TownySettings.addReloadListener(NamespacedKey.fromString("towny:health-regen-task"), () -> TownyTimerHandler.toggleHealthRegen(TownySettings.hasHealthRegen()));
	}

	private final Server server;

	public HealthRegenTimerTask(Towny plugin, Server server) {

		super(plugin);
		this.server = server;
	}

	@Override
	public void run() {

		if (plugin.isFolia()) {
			for (Player player : server.getOnlinePlayers())
				plugin.getScheduler().run(player, () -> evaluatePlayer(player));
		} else {
			for (Player player : server.getOnlinePlayers())
				evaluatePlayer(player);
		}
	}
	
	public void evaluatePlayer(final Player player) {
		// Player is already dead.
		if (player.getHealth() <= 0)
			return;

		if (!TownyAPI.getInstance().isTownyWorld(player.getWorld()))
			return;

		Town town = TownyAPI.getInstance().getTown(player);
		// Player has no Town;
		if (town == null)
			return;

		// Heal and saturate if allowed based on Location.
		if (playerAllowedToHealHere(town, TownyAPI.getInstance().getTownBlock(player)))
			evaluateHealth(player);
	}

	private boolean playerAllowedToHealHere(Town playersTown, TownBlock tbAtPlayer) {
		if (tbAtPlayer == null)
			return false;
		Town townAtPlayer = tbAtPlayer.getTownOrNull();
		return !townAtPlayer.hasActiveWar() && CombatUtil.isAlly(townAtPlayer, playersTown) && !tbAtPlayer.getType().equals(TownBlockType.ARENA);
	}

	private void evaluateHealth(Player player) {
		// When enabled, keep saturation above zero while in town, preventing food level loss.
		if (TownySettings.preventSaturationLoss() && player.getSaturation() != 1F)
			player.setSaturation(1F);

		// Heal 1 HP while in town.
		final double currentHP = player.getHealth();
		final double futureHP = currentHP + 1;
		final double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();

		// Shrink gained to fit below the maxHP.
		final double gained = futureHP > maxHP ? 1.0 - (futureHP - maxHP) : 1.0;
		if (gained <= 0)
			return;

		// Drop back to Sync so we can throw the EntityRegainHealthEvent.
		plugin.getScheduler().run(()-> tryIncreaseHealth(player, currentHP, maxHP, gained));
	}

	private void tryIncreaseHealth(Player player, double currentHealth, double maxHealth, double gained) {
		EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, gained, RegainReason.REGEN);
		if (BukkitTools.isEventCancelled(event))
			return;

		player.setHealth(Math.min(maxHealth, event.getAmount() + currentHealth));
	}
}
