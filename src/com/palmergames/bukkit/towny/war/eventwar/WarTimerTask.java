package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.util.BukkitTools;

public class WarTimerTask extends TownyTimerTask {

	War warEvent;

	public WarTimerTask(Towny plugin, War warEvent) {

		super(plugin);
		this.warEvent = warEvent;
	}

	@Override
	public void run() {

		//TODO: check if war has ended and end gracefully
		if (!warEvent.isWarTime()) {
			warEvent.end();
			universe.clearWarEvent();
			plugin.resetCache();
			TownyMessaging.sendDebugMsg("War ended.");
			return;
		}

		int numPlayers = 0;
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				numPlayers += 1;
				TownyMessaging.sendDebugMsg("[War] " + player.getName() + ": ");
				try {
					Resident resident = TownyUniverse.getDataSource().getResident(player.getName());
					if (resident.hasNation()) {
						Nation nation = resident.getTown().getNation();
						TownyMessaging.sendDebugMsg("[War]   hasNation");
						if (nation.isNeutral()) {
							if (warEvent.isWarringNation(nation))
								warEvent.nationLeave(nation);
							continue;
						}
						TownyMessaging.sendDebugMsg("[War]   notNeutral");
						if (!warEvent.isWarringNation(nation))
							continue;
						TownyMessaging.sendDebugMsg("[War]   warringNation");
						//TODO: Cache player coord & townblock
	
						WorldCoord worldCoord = new WorldCoord(player.getWorld().getName(), Coord.parseCoord(player));
						if (!warEvent.isWarZone(worldCoord))
							continue;
						TownyMessaging.sendDebugMsg("[War]   warZone");
						if (player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
							continue;
						TownyMessaging.sendDebugMsg("[War]   aboveMinHeight");
						TownBlock townBlock = worldCoord.getTownBlock(); //universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
						if (nation == townBlock.getTown().getNation() || townBlock.getTown().getNation().hasAlly(nation))
							continue;
						TownyMessaging.sendDebugMsg("[War]   notAlly");
						//Enemy nation
						
						boolean edgesOnly = TownySettings.getOnlyAttackEdgesInWar();
						if (edgesOnly && !isOnEdgeOfTown(townBlock, worldCoord, warEvent))
							continue;
						if (edgesOnly)
							TownyMessaging.sendDebugMsg("[War]   onEdge");
						
						warEvent.damage(player, townBlock);
						TownyMessaging.sendDebugMsg("[War]   damaged");
				
					}
				} catch (NotRegisteredException e) {
					continue;
				}
			}
		}

		TownyMessaging.sendDebugMsg("[War] # Players: " + numPlayers);
	}	
	
	public static boolean isOnEdgeOfTown(TownBlock townBlock, WorldCoord worldCoord, War warEvent) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == townBlock.getTown();
				TownyMessaging.sendDebugMsg("[WAR] Ole8pieTesting: (For townBlock:" + edgeTownBlock.getCoord().toString() + ")  SameTown:" + sameTown + "  IsWarZone:" + warEvent.isWarZone(edgeTownBlock.getWorldCoord()));
				if (!sameTown || (sameTown && !warEvent.isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}
