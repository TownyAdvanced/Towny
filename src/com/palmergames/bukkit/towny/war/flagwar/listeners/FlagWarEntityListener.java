package com.palmergames.bukkit.towny.war.flagwar.listeners;

import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityExplodeEvent;

import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import com.palmergames.bukkit.towny.war.flagwar.FlagWarConfig;

public class FlagWarEntityListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityExplode(EntityExplodeEvent event) {

		for (Block block : event.blockList())
			FlagWar.checkBlock(null, block, event);
	}
	
	@EventHandler
	public void onPlayerDamagePlayer(TownyPlayerDamagePlayerEvent event) {
		if (FlagWarConfig.isAllowingAttacks() && event.hasTownBlock()) {
			TownyWorld world = event.getTownBlock().getWorld();
			/*
			 * Defending player is in a warzone
			 */
			if (world.isWarZone(Coord.parseCoord(event.getEntity())) && !CombatUtil.preventFriendlyFire(event.getAttackingPlayer(), event.getVictimPlayer(), world))
				event.setCancelled(false);
		}
	}
}
