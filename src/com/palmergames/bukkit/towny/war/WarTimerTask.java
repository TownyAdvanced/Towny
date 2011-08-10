package com.palmergames.bukkit.towny.war;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyTimerTask;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class WarTimerTask extends TownyTimerTask {
	War warEvent;
	
	public WarTimerTask(War warEvent) {
		super(warEvent.getTownyUniverse());
		this.warEvent = warEvent;
	}

	@Override
	public void run() {
		//TODO: check if war has ended and end gracefully
		if (!warEvent.isWarTime()) {
			warEvent.end();
			universe.clearWarEvent();
			universe.getPlugin().updateCache();
			universe.getPlugin().sendDebugMsg("War ended.");
			return;
		}
		
		int numPlayers = 0;
		for (Player player : universe.getOnlinePlayers()) {
			numPlayers += 1;
			plugin.sendDebugMsg("[War] "+player.getName()+": ");
			try {
				Resident resident = universe.getResident(player.getName());
				if (resident.hasNation()) {
					Nation nation = resident.getTown().getNation();
					plugin.sendDebugMsg("[War]   hasNation");
					if (nation.isNeutral()) {
						if (warEvent.isWarringNation(nation))
							warEvent.nationLeave(nation);
						continue;
					}
					plugin.sendDebugMsg("[War]   notNeutral");
					if (!warEvent.isWarringNation(nation))
						continue;
					plugin.sendDebugMsg("[War]   warringNation");
					//TODO: Cache player coord & townblock
					
					WorldCoord worldCoord = new WorldCoord(universe.getWorld(player.getWorld().getName()), Coord.parseCoord(player));
					if (!warEvent.isWarZone(worldCoord))
						continue;
					plugin.sendDebugMsg("[War]   warZone");
					if (player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
						continue;
					plugin.sendDebugMsg("[War]   aboveMinHeight");
					TownBlock townBlock = worldCoord.getTownBlock(); //universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
					if (nation == townBlock.getTown().getNation() || townBlock.getTown().getNation().hasAlly(nation))
						continue;
					plugin.sendDebugMsg("[War]   notAlly");
					//Enemy nation
					warEvent.damage(resident.getTown(), townBlock);
					plugin.sendDebugMsg("[War]   damaged");
				}
			} catch(NotRegisteredException e) {
				continue;
			}
		}
		
		plugin.sendDebugMsg("[War] # Players: " + numPlayers);
	}
}
