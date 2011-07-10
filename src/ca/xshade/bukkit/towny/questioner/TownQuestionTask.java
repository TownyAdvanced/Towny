package ca.xshade.bukkit.towny.questioner;

import ca.xshade.bukkit.towny.object.Town;

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
