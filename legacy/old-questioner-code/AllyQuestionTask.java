package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public class AllyQuestionTask extends TownyQuestionTask {

	protected Resident resident;
	protected Nation targetNation;
	protected Nation requestingNation;

	public AllyQuestionTask(Resident resident, Nation nation) {

		this.resident = resident;
		this.targetNation = nation;
		
		try {
			this.requestingNation = resident.getTown().getNation();
		} catch (NotRegisteredException e) {
			e.printStackTrace();
		}
	}

	public Nation getTargetNation() {

		return targetNation;
	}
	
	public Nation getRequestingNation() {
		return requestingNation;
	}

	@Override
	public void run() {}
}