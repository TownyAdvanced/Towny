package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class ResidentTownQuestionTask extends TownyQuestionTask {
	protected Resident resident;
	protected Town town;
	
	public ResidentTownQuestionTask(Resident resident, Town town) {
		this.resident = resident;
		this.town = town;
	}
	
	public Resident getResident() {
		return resident;
	}
	
	public Town getTown() {
		return town;
	}
	
	@Override
	public void run() {
		
	}

}
