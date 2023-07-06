package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerHandler;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.NamespacedKey;
import org.bukkit.Server;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.TownBlock;
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
				plugin.getScheduler().run(player, () -> checkPlayer(player));
		} else {
			for (Player player : server.getOnlinePlayers())
				checkPlayer(player);
		}
	}
	
	public void checkPlayer(final Player player) {
		if (player.getHealth() <= 0)
			return;

		final TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player);
		// Is wilderness
		if (townBlock == null)
			return;

		Town playerTown = TownyAPI.getInstance().getTown(player);

		if (playerTown != null
			&& !playerTown.hasActiveWar()
			&& CombatUtil.isAlly(townBlock.getTownOrNull(), playerTown)
			&& !townBlock.getType().equals(TownBlockType.ARENA)) // only regen if not in an arena
			incHealth(player);
	}

	public void incHealth(Player player) {

		// Keep saturation above zero while in town.
		if (player.getSaturation() == 0)			
			player.setSaturation(1F);
		
		// Heal while in town.
		double currentHP = player.getHealth();
		double maxHP = player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
		if (currentHP < maxHP) {
			player.setHealth(Math.min(maxHP, ++currentHP));

			// Raise an event so other plugins can keep in sync.
			BukkitTools.fireEvent(new EntityRegainHealthEvent(player, currentHP, RegainReason.REGEN));
		}
	}
}
