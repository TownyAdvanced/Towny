package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Nation;

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
