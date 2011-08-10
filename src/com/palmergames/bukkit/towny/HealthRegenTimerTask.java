package com.palmergames.bukkit.towny;

import org.bukkit.Server;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownBlock;
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
				TownyWorld world = universe.getWorld(player.getWorld().getName());
				TownBlock townBlock = world.getTownBlock(coord);
					
				if (universe.isAlly(townBlock.getTown(), universe.getResident(player.getName()).getTown()))
					incHealth(player);
			} catch (TownyException x) {
			}
		}
		
		//if (TownySettings.getDebug())
		//	System.out.println("[Towny] Debug: Health Regen");
	}
	
	public void incHealth(Player player) {
		int currentHP = player.getHealth();
		if (currentHP < 20)
			player.setHealth(++currentHP);
	}
	
}
