package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.object.Resident;

public class ResidentQuestionTask extends TownyQuestionTask {
	protected Resident resident;
	
	public ResidentQuestionTask(Resident resident) {
		this.resident = resident;
	}
	
	public Resident getResident() {
		return resident;
	}
	
	@Override
	public void run() {
		
	}

}
