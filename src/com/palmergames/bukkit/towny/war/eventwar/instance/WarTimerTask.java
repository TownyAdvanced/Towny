package com.palmergames.bukkit.towny.war.eventwar.instance;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarType;

import org.bukkit.entity.Player;

import java.util.Hashtable;

public class WarTimerTask extends TownyTimerTask {

	private War warEvent;

	public WarTimerTask(Towny plugin, War warEvent) {

		super(plugin);
		this.warEvent = warEvent;
	}

	@Override
	public void run() {

//		//TODO: check if war has ended and end gracefully
//		if (!warEvent.isWarTime()) {
//			warEvent.end();
//			TownyAPI.getInstance().clearWarEvent();
//			plugin.resetCache();
//			TownyMessaging.sendDebugMsg("War ended.");
//			return;
//		}

		int numPlayers = 0;
		Hashtable<TownBlock, WarZoneData> plotList = new Hashtable<>();
		for (Player player : this.warEvent.getWarParticipants().getOnlineWarriors()) {
			if (player == null || player.isFlying()) {
				this.warEvent.getWarParticipants().removeOnlineWarrior(player);
				continue;
			}
			if (TownyAPI.getInstance().isWilderness(player.getLocation()) || player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
				continue;

			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
			if (!warEvent.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
				continue;
			
			numPlayers++;
			TownyMessaging.sendDebugMsg("[War] " + player.getName() + ": ");
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident == null || !resident.hasTown() || !warEvent.getWarParticipants().has(resident) || resident.isJailed())
				continue;

			if (TownySettings.getPlotsHealableInWar() && (CombatUtil.isAlly(resident.getTownOrNull(), townBlock.getTownOrNull()))) {
				if (plotList.containsKey(townBlock))
					plotList.get(townBlock).addDefender(player);
				else {
					WarZoneData wzd = new WarZoneData();
					wzd.addDefender(player);
					plotList.put(townBlock, wzd);
				}
				TownyMessaging.sendDebugMsg("[War]   healed");
				continue;
			}

			if ((this.warEvent.getWarType().equals(WarType.WORLDWAR) || this.warEvent.getWarType().equals(WarType.NATIONWAR)) 
			&& !CombatUtil.isEnemy(resident.getTownOrNull(), townBlock.getTownOrNull())) 
				continue;
			
			if (CombatUtil.isAlly(resident.getTownOrNull(), townBlock.getTownOrNull()) && !this.warEvent.getWarType().equals(WarType.CIVILWAR))
				continue;
			TownyMessaging.sendDebugMsg("[War]   notAlly");

			if (TownySettings.getOnlyAttackEdgesInWar() && !isOnEdgeOfTown(townBlock, townBlock.getWorldCoord(), warEvent))
				continue;

			if (plotList.containsKey(townBlock))
				plotList.get(townBlock).addAttacker(player);
			else {
				WarZoneData wzd = new WarZoneData();
				wzd.addAttacker(player);
				plotList.put(townBlock, wzd);
			}
			TownyMessaging.sendDebugMsg("[War]   damaged");
		}

		//Send health updates
		for (TownBlock tb : plotList.keySet())
			warEvent.getWarZoneManager().updateWarZone(tb, plotList.get(tb));

		TownyMessaging.sendDebugMsg("[War] # Players: " + numPlayers);
	}	

	public static boolean isOnEdgeOfTown(TownBlock townBlock, WorldCoord worldCoord, War warEvent) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == townBlock.getTown();
				if (!sameTown || (sameTown && !warEvent.getWarZoneManager().isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}