package com.palmergames.bukkit.towny.war.eventwar;

import com.palmergames.bukkit.towny.Towny;
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

import org.bukkit.entity.Player;

import java.util.Hashtable;

public class WarTimerTask extends TownyTimerTask {

	private War warEvent;

	public WarTimerTask(Towny plugin, War warEvent) {

		super(plugin);
		this.warEvent = warEvent;
	}

	@SuppressWarnings("static-access")
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
		for (Player player : this.warEvent.getOnlineWarriors()) {
			if (player == null || player.isFlying()) {
				this.warEvent.removeOnlineWarrior(player);
				continue;
			}
			numPlayers += 1;
			TownyMessaging.sendDebugMsg("[War] " + player.getName() + ": ");
			try {
				Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
				if (!resident.hasTown() || !warEvent.isWarringTown(resident.getTown()))
					continue;

				WorldCoord worldCoord = plugin.getCache(player).getLastTownBlock();
				if (!warEvent.isWarZone(worldCoord))
					continue;
				TownyMessaging.sendDebugMsg("[War]   warZone");
				if (player.getLocation().getBlockY() < TownySettings.getMinWarHeight())
					continue;
				TownyMessaging.sendDebugMsg("[War]   aboveMinHeight");
				TownBlock townBlock = worldCoord.getTownBlock(); //universe.getWorld(player.getWorld().getName()).getTownBlock(worldCoord);
				boolean healablePlots = TownySettings.getPlotsHealableInWar();
				if (healablePlots && (CombatUtil.isAlly(resident.getTown(), townBlock.getTown()))) {
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
				// This might not be needed at all anymore, if the plot isn't being healed they are likely an enemy (now that we aren't parsing over every online player.)
				if (!resident.getTown().getNation().hasEnemy(townBlock.getTown().getNation())) //TODO: Add town-town enemy 
					continue;
				TownyMessaging.sendDebugMsg("[War]   notAlly");
				//Enemy nation
				
				if (resident.isJailed())
					continue;

				boolean edgesOnly = TownySettings.getOnlyAttackEdgesInWar();
				if (edgesOnly && !isOnEdgeOfTown(townBlock, worldCoord, warEvent))
					continue;
				if (edgesOnly)
					TownyMessaging.sendDebugMsg("[War]   onEdge");

				//warEvent.damage(player, townBlock);
				if (plotList.containsKey(townBlock))
					plotList.get(townBlock).addAttacker(player);
				else {
					WarZoneData wzd = new WarZoneData();
					wzd.addAttacker(player);
					plotList.put(townBlock, wzd);
				}
				TownyMessaging.sendDebugMsg("[War]   damaged");

			} catch (NotRegisteredException ignored) {
			}
		}

		//Send health updates
		for (TownBlock tb : plotList.keySet()) {
			try {
				warEvent.updateWarZone(tb, plotList.get(tb));
			} catch (NotRegisteredException e) {
				TownyMessaging.sendDebugMsg("[War]   WarZone Update Failed");
			}
		}

		TownyMessaging.sendDebugMsg("[War] # Players: " + numPlayers);
	}	

	@SuppressWarnings("static-access")
	public static boolean isOnEdgeOfTown(TownBlock townBlock, WorldCoord worldCoord, War warEvent) {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		for (int i = 0; i < 4; i++)
			try {
				TownBlock edgeTownBlock = worldCoord.getTownyWorld().getTownBlock(new Coord(worldCoord.getX() + offset[i][0], worldCoord.getZ() + offset[i][1]));
				boolean sameTown = edgeTownBlock.getTown() == townBlock.getTown();
				if (!sameTown || (sameTown && !warEvent.isWarZone(edgeTownBlock.getWorldCoord()))) {
					return true;
				}
			} catch (NotRegisteredException e) {
				return true;
			}
		return false;
	}
}