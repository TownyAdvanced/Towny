package ca.xshade.bukkit.towny;

import java.util.TimerTask;

import ca.xshade.bukkit.towny.object.TownyUniverse;

public class TownyTimerTask extends TimerTask {
	protected TownyUniverse universe;
	protected Towny plugin;

	public TownyTimerTask(TownyUniverse universe) {
		this.universe = universe;
		this.plugin = universe.getPlugin();
	}

	@Override
	public void run() {

	}

}
