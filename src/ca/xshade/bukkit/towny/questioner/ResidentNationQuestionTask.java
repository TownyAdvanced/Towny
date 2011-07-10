package ca.xshade.bukkit.towny.questioner;

import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Nation;

public class ResidentNationQuestionTask extends TownyQuestionTask {
	protected Resident resident;
	protected Nation nation;
	
	public ResidentNationQuestionTask(Resident resident, Nation nation) {
		this.resident = resident;
		this.nation = nation;
	}
	
	public Resident getResident() {
		return resident;
	}
	
	public Nation getNation() {
		return nation;
	}
	
	@Override
	public void run() {
		
	}

}
