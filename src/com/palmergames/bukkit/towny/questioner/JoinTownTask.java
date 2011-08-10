package com.palmergames.bukkit.towny.questioner;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.util.ChatTools;

public class JoinTownTask extends ResidentTownQuestionTask {
	
	public JoinTownTask(Resident resident, Town town) {
		super(resident, town);
	}

	@Override
	public void run() {
		try {
			town.addResident(resident);
			towny.deleteCache(resident.getName());
			universe.getDataSource().saveResident(resident);
			universe.getDataSource().saveTown(town);
			
			getUniverse().sendTownMessage(town,  ChatTools.color(String.format(TownySettings.getLangString("msg_join_town"), resident.getName())));
		} catch (AlreadyRegisteredException e) {
			try {
				getUniverse().sendResidentMessage(resident, e.getError());
			} catch (TownyException e1) {
			}
		}
	}
}
