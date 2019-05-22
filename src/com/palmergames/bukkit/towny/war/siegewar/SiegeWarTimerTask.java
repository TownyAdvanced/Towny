package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;

import java.util.List;

public class SiegeWarTimerTask extends TownyTimerTask {

	public SiegeWarTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {
		TownyMessaging.sendMsg("Now evaluating siege war timer task");

		//Cycle through all sieges
		for (Siege siege : TownyUniverse.getDataSource().getSieges()) {
			if (!siege.isComplete()) {
				//Siege is active

				TownyMessaging.sendMsg("Now evaluating active siege between " +
						siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

				//evaluate active siege
				//Here we check if the timer has elapsed
				//And win/lose etc as required

				//Do not delete sieges here!!!  Or else you get a concurrent modification exception
				//Just mark them as complete
				//In another loop, we will deal with deletion etc.
			}
		}
	}
}