package ca.xshade.bukkit.towny.war;

import ca.xshade.bukkit.towny.TownyTimerTask;
import ca.xshade.bukkit.towny.object.TownyUniverse;

public class StartWarTimerTask extends TownyTimerTask {

	public StartWarTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		universe.getWarEvent().start();
	}

}
