package com.palmergames.bukkit.towny.questioner;

import ca.xshade.bukkit.questioner.BukkitQuestionTask;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public abstract class TownyQuestionTask extends BukkitQuestionTask {
	protected Towny towny;
	protected TownyUniverse universe;
	
	public TownyUniverse getUniverse() {
		return universe;
	}
	
	public void setTowny(Towny towny) {
		this.towny = towny;
		this.universe = towny.getTownyUniverse();
	}
	
	@Override
	public abstract void run();
}
