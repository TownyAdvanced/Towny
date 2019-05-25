package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.TownyTimerTask;

public class SiegeWarTimerTask extends TownyTimerTask {

	private final static long ONE_HOUR_MILLIS = 3600000;
	private static boolean timeForUpkeep;
	private static long nextUpkeepTime;


	static
	{
		timeForUpkeep = false;
		nextUpkeepTime = System.currentTimeMillis() + ONE_HOUR_MILLIS;
	}

	public SiegeWarTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		try {
			TownyMessaging.sendMsg("Now evaluating siege war timer task");

			if (System.currentTimeMillis() > nextUpkeepTime) {
				timeForUpkeep = true;
			}

			//Cycle through all sieges
			for (Siege siege : TownyUniverse.getDataSource().getSieges()) {
				if (!siege.isComplete()) {
					//Siege is active
					TownyMessaging.sendMsg("Now evaluating active siege between " +
							siege.getAttackingNation().getName() + " and " + siege.getDefendingTown().getName());

					if (TownySettings.isUsingEconomy() && timeForUpkeep) {
						siege.applyUpkeepCost(TownySettings.getWarSiegeAttackerCostPerHour());
					}


					//evaluate active siege
					//Here we check if the timer has elapsed
					//And win/lose etc as required

					//Do not delete sieges here!!!  Or else you get a concurrent modification exception
					//Just mark them as complete
					//In another loop, we will deal with deletion etc.
				}
			}

		} finally {
			if(timeForUpkeep) {
				timeForUpkeep = false;
				nextUpkeepTime = System.currentTimeMillis()+ ONE_HOUR_MILLIS;
			}
		}
	}
}