package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.object.Town;

public class TownQuestionTask extends TownyQuestionTask {
	protected Town town;
	
	public TownQuestionTask(Town town) {
		this.town = town;
	}
	
	public Town getTown() {
		return town;
	}
	
	@Override
	public void run() {
		
	}

}
