package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
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

		for (Siege siege : universe.getSieges()) {
			if (siege.isActive()) {
				//Siege is active

				TownyMessaging.sendDebugMsg("Now evaluating active siege between " +
						siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

				//evaluate active siege
				//Here we check if the timer has elapsed
				//And win/lose etc as required
			} else {
				//Siege is not active

				if(siege.isComplete()) {
					//Siege is complete, do nothing
				} else {
					//Siege is queued up.

					if (siege.getDefendingTown().getActiveSiege() == null) {
						//The town is ready for the next queued siege to start
						List<Siege> sieges = siege.getDefendingTown().getSieges();
						int siegeIndex = sieges.indexOf(siege);

						if(siegeIndex ==0){
							//This is the first siege in the list, activate it.
							activateSiege(siege);

						} else if (sieges.get(siegeIndex-1).isComplete()){
							//The previous siege in the list is complete, activate this one.
							activateSiege(siege);
						}
					}
				}
			}
		}
	}

	private void activateSiege(Siege siege) {
		TownyMessaging.sendMsg("Now activating siege between " +
				siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

		siege.setActive(true);
		siege.getDefendingTown().setActiveSiege(siege);

		//Set actual start
		//Set scheduled end
		//This would be a good place for notification
	}
}