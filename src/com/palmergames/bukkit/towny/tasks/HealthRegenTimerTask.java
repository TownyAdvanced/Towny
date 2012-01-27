package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent.RegainReason;

import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class HealthRegenTimerTask extends TownyTimerTask {

private Server server;
	
	public HealthRegenTimerTask(TownyUniverse universe, Server server) {
		super(universe);
		this.server = server;
	}
	
	@Override
	public void run() {
		if (universe.isWarTime())
			return;
		
		for (Player player : server.getOnlinePlayers()) {
			if (player.getHealth() <= 0)
				continue;
			
			Coord coord = Coord.parseCoord(player);
			try {
				TownyWorld world = TownyUniverse.getDataSource().getWorld(player.getWorld().getName());
				TownBlock townBlock = world.getTownBlock(coord);
					
				if (universe.isAlly(townBlock.getTown(), TownyUniverse.getDataSource().getResident(player.getName()).getTown()))
					if (!townBlock.getType().equals(TownBlockType.ARENA)) // only regen if not in an arena
						incHealth(player);
			} catch (TownyException x) {
			}
		}
		
		//if (TownySettings.getDebug())
		//	System.out.println("[Towny] Debug: Health Regen");
	}
	
	public void incHealth(Player player) {
		int currentHP = player.getHealth();
		if (currentHP < 20) {
			player.setHealth(++currentHP);
			
			// Raise an event so other plugins can keep in sync.
			EntityRegainHealthEvent event = new EntityRegainHealthEvent(player, ++currentHP, RegainReason.REGEN);
			Bukkit.getServer().getPluginManager().callEvent(event);
			
		}
	}
	
}
