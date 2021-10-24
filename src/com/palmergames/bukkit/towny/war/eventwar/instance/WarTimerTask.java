package com.palmergames.bukkit.towny.war.eventwar.instance;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;

import org.bukkit.entity.Player;

import java.util.Hashtable;

public class WarTimerTask extends TownyTimerTask {

	private War war;

	public WarTimerTask(Towny plugin, War warEvent) {

		super(plugin);
		this.war = warEvent;
	}

	@Override
	public void run() {
		int numPlayers = 0;
		Hashtable<TownBlock, WarZoneData> plotList = new Hashtable<>();
		for (Player player : war.getWarParticipants().getOnlineWarriors()) {
			if (player == null || player.isFlying()) {
				war.getWarParticipants().removeOnlineWarrior(player);
				continue;
			}
			if (TownyAPI.getInstance().isWilderness(player.getLocation()) || player.getLocation().getBlockY() < EventWarSettings.getMinWarHeight())
				continue;

			TownBlock townBlock = TownyAPI.getInstance().getTownBlock(player.getLocation());
			if (!war.getWarZoneManager().isWarZone(townBlock.getWorldCoord()))
				continue;
			
			numPlayers++;
			TownyMessaging.sendDebugMsg("[War] " + player.getName() + ": ");
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident == null || !resident.hasTown() || !war.getWarParticipants().has(resident) || resident.isJailed())
				continue;

			if (EventWarSettings.getPlotsHealableInWar() && (CombatUtil.isAlly(resident.getTownOrNull(), townBlock.getTownOrNull()))) {
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

			if ((war.getWarType().equals(WarType.WORLDWAR) || war.getWarType().equals(WarType.NATIONWAR)) 
			&& !CombatUtil.isEnemy(resident.getTownOrNull(), townBlock.getTownOrNull())) 
				continue;
			
			if (CombatUtil.isAlly(resident.getTownOrNull(), townBlock.getTownOrNull()) && !war.getWarType().equals(WarType.CIVILWAR))
				continue;
			TownyMessaging.sendDebugMsg("[War]   notAlly");

			if (!isOnEdgeOfTown(townBlock, townBlock.getWorldCoord(), war))
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
			war.getWarZoneManager().updateWarZone(tb, plotList.get(tb));

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