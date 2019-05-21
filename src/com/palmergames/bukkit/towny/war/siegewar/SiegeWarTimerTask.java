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
		TownyMessaging.sendDebugMsg("Now evaluating siege war timer task");
		//Cycle through all sieges

		for (Siege siege : universe.getSieges()) {
			if (siege.isActive()) {

				TownyMessaging.sendDebugMsg("Now evaluating active siege between " +
						siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

				//evaluate active siege
				//Here we check if the timer has elapsed
			} else {
				//if siege is next in the queue AND the active slot is empty, activate the siege.
				if (siege.getDefendingTown().getActiveSiege() == null) {

					List<Siege> siegeQueue = siege.getDefendingTown().getSieges();

					if (siegeQueue.size() == 1 || siegeQueue.indexOf(siege) == 0) {
						activateSiege(siege);
					}
				}
			}
		}
	}

	private void activateSiege(Siege siege) {
		TownyMessaging.sendDebugMsg("Now activating siege between " +
				siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

		siege.setActive(true);
		siege.getDefendingTown().setActiveSiege(siege);

		//Set actual start
		//Set scheduled end
		//This would be a good place for notification
	}
}